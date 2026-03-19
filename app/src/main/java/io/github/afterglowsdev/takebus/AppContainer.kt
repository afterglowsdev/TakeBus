package io.github.afterglowsdev.takebus

import android.content.Context
import io.github.afterglowsdev.takebus.data.chelaile.ChelaileRepository
import io.github.afterglowsdev.takebus.data.location.LocationRepository
import io.github.afterglowsdev.takebus.data.settings.AppSettingsRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settingsRepository = AppSettingsRepository(appContext)
    val locationRepository = LocationRepository(appContext)
    val chelaileRepository = ChelaileRepository()
}

