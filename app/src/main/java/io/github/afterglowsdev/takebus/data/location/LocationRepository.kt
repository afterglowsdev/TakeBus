package io.github.afterglowsdev.takebus.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import io.github.afterglowsdev.takebus.data.chelaile.GeoPoint
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

class LocationRepository(private val context: Context) {

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): GeoPoint = suspendCancellableCoroutine { continuation ->
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
            continuation.resume(
                GeoPoint(
                    lat = freshestLastKnown.latitude,
                    lng = freshestLastKnown.longitude
                )
            )
            return@suspendCancellableCoroutine
        }

        val enabledProvider = providers.firstOrNull { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }

        if (enabledProvider == null) {
            continuation.resumeWithException(IllegalStateException("No location provider available"))
            return@suspendCancellableCoroutine
        }

        LocationManagerCompat.getCurrentLocation(
            locationManager,
            enabledProvider,
            null,
            ContextCompat.getMainExecutor(context)
        ) { location ->
            if (location == null) {
                continuation.resumeWithException(IllegalStateException("Unable to fetch location"))
            } else {
                continuation.resume(
                    GeoPoint(
                        lat = location.latitude,
                        lng = location.longitude
                    )
                )
            }
        }
    }
}

