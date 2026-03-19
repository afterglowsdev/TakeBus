package io.github.afterglowsdev.takebus.data.chelaile

data class GeoPoint(
    val lat: Double,
    val lng: Double
)

data class City(
    val id: String,
    val name: String?
)

data class NearbyStation(
    val id: String,
    val name: String,
    val distanceMeters: Int?,
    val lat: Double?,
    val lng: Double?
)

data class LineBrief(
    val lineId: String,
    val lineNo: String,
    val name: String,
    val direction: Int,
    val startSn: String,
    val endSn: String,
    val state: Int,
    val desc: String,
    val firstTime: String?,
    val lastTime: String?,
    val stationCount: Int?
) {
    val directionLabel: String
        get() = "$startSn -> $endSn"
}

data class StationLine(
    val line: LineBrief,
    val targetOrder: Int,
    val targetStationName: String,
    val nextStationName: String,
    val etaMinutes: Int?,
    val etaText: String,
    val travelSeconds: Int?,
    val state: Int,
    val desc: String
)

data class StationDetails(
    val station: NearbyStation,
    val lines: List<StationLine>
)

data class HomeLineGroup(
    val lineNo: String,
    val entries: List<StationLine>,
    val bestEntry: StationLine
)

data class HomeStationCard(
    val station: NearbyStation,
    val lines: List<HomeLineGroup>
)

data class SearchStationHit(
    val stationId: String,
    val name: String,
    val lat: Double?,
    val lng: Double?
)

data class SearchLineHit(
    val lineId: String,
    val lineNo: String,
    val direction: Int,
    val startSn: String,
    val endSn: String
) {
    val directionLabel: String
        get() = "$startSn -> $endSn"
}

data class SearchResults(
    val lines: List<SearchLineHit>,
    val stations: List<SearchStationHit>
)

data class RouteStop(
    val id: String,
    val name: String,
    val order: Int,
    val lat: Double?,
    val lng: Double?,
    val wgsLat: Double?,
    val wgsLng: Double?
)

data class BusMarker(
    val busId: String,
    val order: Int,
    val distanceToStationMeters: Int?,
    val etaMinutes: Int?,
    val etaText: String,
    val timestampText: String
)

data class LineDirectionPanel(
    val line: LineBrief,
    val tip: String,
    val targetOrder: Int,
    val selectedStop: RouteStop,
    val nextStopName: String?,
    val stations: List<RouteStop>,
    val buses: List<BusMarker>
)

data class LineScreenData(
    val lineNo: String,
    val directions: List<LineDirectionPanel>
)

