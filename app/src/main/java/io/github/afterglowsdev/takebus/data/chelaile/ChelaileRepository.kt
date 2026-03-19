package io.github.afterglowsdev.takebus.data.chelaile

import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import okhttp3.Call
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class ChelaileRepository {
    private val json = Json { ignoreUnknownKeys = true }
    private val httpClient = OkHttpClient.Builder().build()
    private val userId = randomBrowserId()

    suspend fun resolveCity(location: GeoPoint): City {
        val query = buildQuery(
            linkedMapOf(
                "type" to "gpsRealtimeCity",
                "lat" to location.lat.toString(),
                "lng" to location.lng.toString(),
                "gpstype" to Defaults.GPSTYPE,
                "s" to "android",
                "v" to Defaults.CITYLIST_VERSION,
                "src" to "webapp_default",
                "userId" to ""
            )
        )
        val body = request("${Defaults.HOST}/cdatasource/citylist?$query", defaultHeaders())
        val root = json.parseToJsonElement(body).asObject()
        val city = root.obj("data")?.obj("gpsRealtimeCity") ?: error("Unable to resolve city")
        return City(
            id = city.string("cityId") ?: error("Missing cityId"),
            name = city.string("cityName") ?: city.string("name")
        )
    }

    suspend fun getHomeStationCards(city: City, location: GeoPoint, limit: Int = 5): List<HomeStationCard> =
        coroutineScope {
            getNearbyStations(city.id, location)
                .take(limit)
                .map { station ->
                    async {
                        val detail = getStationDetails(city.id, location, station)
                        val grouped = detail.lines
                            .groupBy { it.line.lineNo }
                            .map { (lineNo, entries) ->
                                val sorted = entries.sortedWith(stationLineOrdering)
                                HomeLineGroup(
                                    lineNo = lineNo,
                                    entries = sorted,
                                    bestEntry = sorted.first()
                                )
                            }
                            .sortedWith(homeLineOrdering)
                        HomeStationCard(
                            station = detail.station,
                            lines = grouped
                        )
                    }
                }
                .awaitAll()
        }

    suspend fun getStationScreen(cityId: String, location: GeoPoint, stationId: String): StationDetails {
        return getStationDetails(
            cityId = cityId,
            location = location,
            station = NearbyStation(
                id = stationId,
                name = "",
                distanceMeters = null,
                lat = null,
                lng = null
            )
        )
    }

    suspend fun search(cityId: String, location: GeoPoint?, keyword: String): SearchResults {
        val data = action(
            handler = "basesearch/client/clientSearch.action",
            cityId = cityId,
            location = location,
            params = linkedMapOf(
                "key" to keyword,
                "count" to "12"
            ),
            accept = "text/plain,*/*"
        )

        val lines = data.array("lines")
            .mapNotNull { element ->
                val item = element.asObjectOrNull() ?: return@mapNotNull null
                val lineNo = item.string("lineNo") ?: item.string("name") ?: return@mapNotNull null
                val lineId = item.string("lineId") ?: return@mapNotNull null
                SearchLineHit(
                    lineId = lineId,
                    lineNo = lineNo,
                    direction = item.int("direction") ?: 0,
                    startSn = item.string("startSn").orEmpty(),
                    endSn = item.string("endSn").orEmpty()
                )
            }
            .distinctBy { "${it.lineNo}_${it.direction}_${it.lineId}" }

        val stations = data.array("stations")
            .mapNotNull { element ->
                val item = element.asObjectOrNull() ?: return@mapNotNull null
                val stationId = item.string("sId") ?: return@mapNotNull null
                SearchStationHit(
                    stationId = stationId,
                    name = item.string("sn") ?: return@mapNotNull null,
                    lat = item.double("lat"),
                    lng = item.double("lng")
                )
            }
            .distinctBy { it.stationId }

        return SearchResults(lines = lines, stations = stations)
    }

    suspend fun getLineScreen(
        cityId: String,
        location: GeoPoint,
        lineNo: String,
        stationId: String? = null,
        stationName: String? = null
    ): LineScreenData = coroutineScope {
        val candidates = searchLineCandidates(cityId, location, lineNo)
        if (candidates.isEmpty()) {
            error("No line found for $lineNo")
        }

        val directions = candidates
            .distinctBy { it.lineId }
            .map { candidate ->
                async {
                    val routeRoot = action(
                        handler = "bus/line!lineRoute.action",
                        cityId = cityId,
                        location = location,
                        params = linkedMapOf("lineId" to candidate.lineId)
                    )

                    val routeLine = routeRoot.obj("line")?.let(::parseLineBrief) ?: candidate
                    val routeStops = routeRoot.array("stations")
                        .mapNotNull { it.asObjectOrNull()?.let(::parseRouteStop) }
                        .sortedBy { it.order }
                    val selectedStop = selectStop(routeStops, stationId, stationName, location)
                    val nextStopName = routeStops.firstOrNull { it.order == selectedStop.order + 1 }?.name
                        ?: selectedStop.name

                    val detailRoot = encryptedAction(
                        handler = "bus/line!encryptedLineDetail.action",
                        cityId = cityId,
                        location = location,
                        businessParams = linkedMapOf(
                            "lineId" to routeLine.lineId,
                            "lineName" to routeLine.name,
                            "direction" to routeLine.direction.toString(),
                            "stationName" to selectedStop.name,
                            "nextStationName" to nextStopName,
                            "lineNo" to routeLine.lineNo,
                            "targetOrder" to selectedStop.order.toString()
                        )
                    )

                    val detailLine = detailRoot.obj("line")?.let(::parseLineBrief) ?: routeLine
                    val detailStops = detailRoot.array("stations")
                        .mapNotNull { it.asObjectOrNull()?.let(::parseRouteStop) }
                        .sortedBy { it.order }
                    val resolvedStops = if (detailStops.isNotEmpty()) detailStops else routeStops
                    val resolvedSelected = resolvedStops.firstOrNull { it.id == selectedStop.id }
                        ?: resolvedStops.minByOrNull { abs(it.order - selectedStop.order) }
                        ?: selectedStop

                    val buses = detailRoot.array("buses")
                        .mapNotNull { it.asObjectOrNull()?.let(::parseBusMarker) }
                        .sortedBy { abs(it.order - resolvedSelected.order) }

                    LineDirectionPanel(
                        line = detailLine,
                        tip = detailRoot.obj("tip")?.string("desc").orEmpty(),
                        targetOrder = detailRoot.int("targetOrder") ?: resolvedSelected.order,
                        selectedStop = resolvedSelected,
                        nextStopName = nextStopName,
                        stations = resolvedStops,
                        buses = buses
                    )
                }
            }
            .awaitAll()
            .sortedBy { it.line.direction }

        LineScreenData(lineNo = lineNo, directions = directions)
    }

    private suspend fun getStationDetails(
        cityId: String,
        location: GeoPoint,
        station: NearbyStation
    ): StationDetails {
        val root = encryptedAction(
            handler = "bus/stop!encryptedStnDetail.action",
            cityId = cityId,
            location = location,
            businessParams = linkedMapOf(
                "stationId" to station.id,
                "destSId" to "-1"
            )
        )

        val stationInfo = NearbyStation(
            id = root.string("sId") ?: station.id,
            name = root.string("sn") ?: station.name,
            distanceMeters = root.int("distance")?.takeIf { it >= 0 } ?: station.distanceMeters,
            lat = station.lat,
            lng = station.lng
        )
        val lines = root.array("lines")
            .mapNotNull { it.asObjectOrNull()?.let(::parseStationLine) }
            .sortedWith(stationLineOrdering)
        return StationDetails(station = stationInfo, lines = lines)
    }

    private suspend fun getNearbyStations(cityId: String, location: GeoPoint): List<NearbyStation> {
        val data = action(
            handler = "bus/stop!nearPhysicalStns.action",
            cityId = cityId,
            location = location,
            params = linkedMapOf(
                "cityState" to "2",
                "gpstype" to Defaults.GPSTYPE
            )
        )
        return data.array("nearStations")
            .mapNotNull { element ->
                val item = element.asObjectOrNull() ?: return@mapNotNull null
                NearbyStation(
                    id = item.string("sId") ?: return@mapNotNull null,
                    name = item.string("sn") ?: return@mapNotNull null,
                    distanceMeters = item.int("distance"),
                    lat = item.double("lat"),
                    lng = item.double("lng")
                )
            }
            .distinctBy { it.id }
    }

    private suspend fun searchLineCandidates(
        cityId: String,
        location: GeoPoint,
        lineNo: String
    ): List<LineBrief> {
        val searchHits = action(
            handler = "basesearch/client/clientSearchList.action",
            cityId = cityId,
            location = location,
            params = linkedMapOf(
                "key" to lineNo,
                "type" to "1"
            ),
            accept = "text/plain,*/*"
        )
            .array("lines")
            .mapNotNull { it.asObjectOrNull()?.let(::parseLineBrief) }
            .filter { normalizeLine(it.lineNo) == normalizeLine(lineNo) }

        if (searchHits.size >= 2) {
            return searchHits
        }

        val fallbackHits = action(
            handler = "bus/cityLineList",
            cityId = cityId,
            location = location,
            params = linkedMapOf("lineName" to lineNo)
        )
            .obj("allLines")
            ?.values
            ?.flatMap { value ->
                value.asArrayOrNull().orEmpty().mapNotNull { it.asObjectOrNull()?.let(::parseLineBrief) }
            }
            .orEmpty()
            .filter { normalizeLine(it.lineNo) == normalizeLine(lineNo) }

        return (searchHits + fallbackHits)
            .distinctBy { it.lineId }
            .sortedBy { it.direction }
    }

    private suspend fun action(
        handler: String,
        cityId: String,
        location: GeoPoint?,
        params: LinkedHashMap<String, String>,
        accept: String = "*/*"
    ): JsonObject {
        val query = buildQuery(
            LinkedHashMap<String, String>().apply {
                putAll(params)
                putAll(sharedParams(cityId, location))
            }
        )
        val body = request("${Defaults.HOST}/api/$handler?$query", defaultHeaders(accept))
        val root = json.parseToJsonElement(stripMarkers(body)).asObject()
        val jsonr = root.obj("jsonr") ?: error("Malformed response")
        val status = jsonr.string("status")
        if (status != "00") {
            error(jsonr.string("errmsg") ?: "API error $status")
        }
        return jsonr.obj("data") ?: JsonObject(emptyMap())
    }

    private suspend fun encryptedAction(
        handler: String,
        cityId: String,
        location: GeoPoint?,
        businessParams: LinkedHashMap<String, String>
    ): JsonObject {
        val rawSign = businessParams.entries.joinToString("&") { (key, value) -> "$key=$value" }
        val cryptoSign = md5(rawSign + Defaults.SIGN_SALT)
        val data = action(
            handler = handler,
            cityId = cityId,
            location = location,
            params = LinkedHashMap<String, String>().apply {
                putAll(businessParams)
                put("cryptoSign", cryptoSign)
            }
        )
        val encryptedPayload = data.string("encryptResult") ?: error("Missing encrypted payload")
        return json.parseToJsonElement(decrypt(encryptedPayload)).asObject()
    }

    private fun parseLineBrief(item: JsonObject): LineBrief {
        val lineNo = item.string("lineNo") ?: item.string("name").orEmpty()
        return LineBrief(
            lineId = item.string("lineId").orEmpty(),
            lineNo = lineNo,
            name = item.string("name") ?: lineNo,
            direction = item.int("direction") ?: 0,
            startSn = item.string("startSn").orEmpty(),
            endSn = item.string("endSn") ?: item.string("destinationName").orEmpty(),
            state = item.int("state") ?: 0,
            desc = item.string("desc").orEmpty().ifBlank { item.string("shortDesc").orEmpty() },
            firstTime = item.string("firstTime"),
            lastTime = item.string("lastTime"),
            stationCount = item.int("stationsNum")
        )
    }

    private fun parseStationLine(item: JsonObject): StationLine {
        val line = parseLineBrief(item.obj("line") ?: JsonObject(emptyMap()))
        val state = item.array("stnStates").firstOrNull()?.asObjectOrNull()
        val travelSeconds = state?.int("travelTime")
        val valueMinutes = state?.int("value")
        val etaMinutes = when {
            travelSeconds != null && travelSeconds > 0 -> ceil(travelSeconds / 60.0).toInt()
            valueMinutes != null && valueMinutes >= 0 -> valueMinutes
            else -> null
        }
        val etaText = when {
            line.state < 0 && line.desc.isNotBlank() -> line.desc
            etaMinutes == 0 -> "Arriving"
            etaMinutes != null -> "${etaMinutes} min"
            line.desc.isNotBlank() -> line.desc
            else -> "-"
        }
        return StationLine(
            line = line,
            targetOrder = item.obj("targetStation")?.int("order") ?: 0,
            targetStationName = item.obj("targetStation")?.string("sn").orEmpty(),
            nextStationName = item.obj("nextStation")?.string("sn").orEmpty(),
            etaMinutes = etaMinutes,
            etaText = etaText,
            travelSeconds = travelSeconds,
            state = line.state,
            desc = line.desc
        )
    }

    private fun parseRouteStop(item: JsonObject): RouteStop {
        return RouteStop(
            id = item.string("sId").orEmpty(),
            name = item.string("sn").orEmpty(),
            order = item.int("order") ?: 0,
            lat = item.double("lat"),
            lng = item.double("lng"),
            wgsLat = item.double("wgsLat"),
            wgsLng = item.double("wgsLng")
        )
    }

    private fun parseBusMarker(item: JsonObject): BusMarker {
        val order = item.int("specialOrder") ?: item.int("order") ?: 0
        val etaSeconds = item.array("travels")
            .firstOrNull()
            ?.asObjectOrNull()
            ?.int("travelTime")
        val etaMinutes = etaSeconds?.let { ceil(it / 60.0).toInt() }
        return BusMarker(
            busId = item.string("busId").orEmpty(),
            order = order,
            distanceToStationMeters = item.int("distanceToSc"),
            etaMinutes = etaMinutes,
            etaText = when {
                etaMinutes == null -> "-"
                etaMinutes == 0 -> "Arriving"
                else -> "${etaMinutes} min"
            },
            timestampText = item.string("timeStr").orEmpty()
        )
    }

    private fun selectStop(
        stops: List<RouteStop>,
        stationId: String?,
        stationName: String?,
        location: GeoPoint
    ): RouteStop {
        if (stationId != null) {
            stops.firstOrNull { it.id == stationId }?.let { return it }
        }
        if (stationName != null) {
            val target = normalizeText(stationName)
            stops.firstOrNull { normalizeText(it.name) == target }?.let { return it }
        }
        return stops.minByOrNull { stop ->
            val lat = stop.wgsLat ?: stop.lat ?: return@minByOrNull Double.MAX_VALUE
            val lng = stop.wgsLng ?: stop.lng ?: return@minByOrNull Double.MAX_VALUE
            haversineMeters(location.lat, location.lng, lat, lng)
        } ?: stops.firstOrNull() ?: error("No stops available")
    }

    private fun sharedParams(cityId: String, location: GeoPoint?): LinkedHashMap<String, String> {
        return linkedMapOf<String, String>().apply {
            put("cityId", cityId)
            put("s", "h5")
            put("v", Defaults.VERSION)
            put("vc", Defaults.VC)
            put("src", Defaults.SRC)
            put("userId", userId)
            put("h5Id", userId)
            put("sign", Defaults.SIGN)
            if (location != null) {
                put("lat", location.lat.toString())
                put("lng", location.lng.toString())
                put("geo_lat", location.lat.toString())
                put("geo_lng", location.lng.toString())
                put("gpstype", Defaults.GPSTYPE)
            }
        }
    }

    private fun defaultHeaders(accept: String = "*/*"): Headers {
        return Headers.Builder()
            .add("referer", "${Defaults.HOST}/customer_ch5/?1=1&randomTime=${System.currentTimeMillis()}&src=${Defaults.SRC}")
            .add("user-agent", Defaults.USER_AGENT)
            .add("accept", accept)
            .build()
    }

    private suspend fun request(url: String, headers: Headers): String {
        val request = Request.Builder()
            .url(url)
            .headers(headers)
            .get()
            .build()
        return httpClient.newCall(request).awaitBody()
    }

    private fun buildQuery(params: LinkedHashMap<String, String>): String {
        return params.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    }

    private fun stripMarkers(raw: String): String {
        return raw.trim()
            .removePrefix("**YGKJ")
            .removeSuffix("YGKJ##")
            .removePrefix("YGKJ##")
            .removeSuffix("**YGKJ")
    }

    private fun md5(value: String): String {
        return MessageDigest.getInstance("MD5")
            .digest(value.toByteArray())
            .joinToString("") { byte -> "%02x".format(byte) }
    }

    private fun decrypt(base64: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val key = SecretKeySpec(Defaults.AES_KEY.toByteArray(), "AES")
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(Base64.getDecoder().decode(base64)).toString(StandardCharsets.UTF_8)
    }

    private fun normalizeLine(value: String): String = normalizeText(value).replace(Regex("[^0-9a-z]"), "")

    private fun normalizeText(value: String): String = value.replace("\\s+".toRegex(), "").lowercase()

    private fun haversineMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadiusMeters = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return 2 * earthRadiusMeters * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun randomBrowserId(): String {
        val suffix = List(6) { ('a'..'z').random() }.joinToString("")
        return "browser_${System.currentTimeMillis().toString(36)}_$suffix"
    }

    private companion object {
        val stationLineOrdering = compareBy<StationLine>(
            { if (it.state == 0) 0 else 1 },
            { it.etaMinutes ?: Int.MAX_VALUE },
            { it.line.direction }
        )

        val homeLineOrdering = compareBy<HomeLineGroup>(
            { if (it.bestEntry.state == 0) 0 else 1 },
            { it.bestEntry.etaMinutes ?: Int.MAX_VALUE },
            { it.lineNo }
        )
    }
}

private object Defaults {
    const val HOST = "https://web.chelaile.net.cn"
    const val SRC = "wechat_shaoguan"
    const val VERSION = "9.1.2"
    const val VC = "1"
    const val SIGN = "1"
    const val GPSTYPE = "wgs"
    const val CITYLIST_VERSION = "3.80.0"
    const val SIGN_SALT = "qwihrnbtmj"
    const val AES_KEY = "422556651C7F7B2B5C266EED06068230"
    const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Mobile Safari/537.36"
}

private suspend fun Call.awaitBody(): String = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : okhttp3.Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isCancelled) return
            continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            response.use { safeResponse ->
                if (!safeResponse.isSuccessful) {
                    continuation.resumeWithException(
                        IOException("HTTP ${safeResponse.code}: ${safeResponse.body?.string().orEmpty()}")
                    )
                    return
                }
                val body = safeResponse.body?.string()
                if (body == null) {
                    continuation.resumeWithException(IOException("Empty response body"))
                } else {
                    continuation.resume(body)
                }
            }
        }
    })
}

private fun JsonElement.asObject(): JsonObject = this as? JsonObject ?: JsonObject(emptyMap())

private fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

private fun JsonElement?.asArrayOrNull(): JsonArray? = this as? JsonArray

private fun JsonObject.obj(key: String): JsonObject? = this[key].asObjectOrNull()

private fun JsonObject.array(key: String): List<JsonElement> = this[key].asArrayOrNull().orEmpty()

private fun JsonObject.string(key: String): String? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.contentOrNull?.takeIf { it.isNotBlank() }
}

private fun JsonObject.int(key: String): Int? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.intOrNull
}

private fun JsonObject.double(key: String): Double? {
    val primitive = this[key] as? JsonPrimitive ?: return null
    return primitive.doubleOrNull
}
