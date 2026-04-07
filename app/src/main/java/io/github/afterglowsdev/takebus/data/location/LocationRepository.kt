package io.github.afterglowsdev.takebus.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.util.Consumer
import io.github.afterglowsdev.takebus.data.chelaile.GeoPoint
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

class LocationRepository(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): GeoPoint {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )

        val freshestLastKnown = providers
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { location -> location.time }

        if (freshestLastKnown != null && System.currentTimeMillis() - freshestLastKnown.time < 120_000L) {
            return freshestLastKnown.toGeoPoint()
        }

        val enabledProvider = providers.firstOrNull { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }

        if (enabledProvider == null) {
            return freshestLastKnown?.toGeoPoint()
                ?: throw IllegalStateException("No location provider available")
        }

        val resolvedLocation = withTimeoutOrNull(10_000L) {
            suspendCancellableCoroutine<GeoPoint> { continuation ->
                val cancellationSignal = CancellationSignal()
                continuation.invokeOnCancellation {
                    cancellationSignal.cancel()
                }

                runCatching {
                    LocationManagerCompat.getCurrentLocation(
                        locationManager,
                        enabledProvider,
                        cancellationSignal,
                        ContextCompat.getMainExecutor(context),
                        Consumer<Location> { location ->
                            if (!continuation.isActive) return@Consumer
                            when {
                                location != null -> continuation.resume(location.toGeoPoint())
                                freshestLastKnown != null -> continuation.resume(freshestLastKnown.toGeoPoint())
                                else -> continuation.resumeWithException(
                                    IllegalStateException("Unable to fetch location")
                                )
                            }
                        }
                    )
                }.getOrElse { throwable ->
                    if (!continuation.isActive) return@suspendCancellableCoroutine
                    if (throwable is CancellationException) {
                        throw throwable
                    }
                    freshestLastKnown?.let {
                        continuation.resume(it.toGeoPoint())
                    } ?: continuation.resumeWithException(throwable)
                }
            }
        }

        return resolvedLocation
            ?: freshestLastKnown?.toGeoPoint()
            ?: throw IllegalStateException("Location request timed out")
    }
}

private fun Location.toGeoPoint(): GeoPoint = GeoPoint(
    lat = latitude,
    lng = longitude
)
