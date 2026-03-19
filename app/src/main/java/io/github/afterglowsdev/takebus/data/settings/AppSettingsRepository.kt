package io.github.afterglowsdev.takebus.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "takebus_settings")

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

enum class FontScaleOption(val multiplier: Float) {
    SMALL(0.92f),
    MEDIUM(1.0f),
    LARGE(1.1f)
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontScale: FontScaleOption = FontScaleOption.MEDIUM
)

class AppSettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val fontScaleKey = stringPreferencesKey("font_scale")

    val settings: Flow<AppSettings> = context.dataStore.data.map(::mapSettings)

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[themeKey] = themeMode.name
        }
    }

    suspend fun setFontScale(fontScale: FontScaleOption) {
        context.dataStore.edit { preferences ->
            preferences[fontScaleKey] = fontScale.name
        }
    }

    private fun mapSettings(preferences: Preferences): AppSettings {
        val themeMode = preferences[themeKey]
            ?.let { stored -> ThemeMode.entries.firstOrNull { it.name == stored } }
            ?: ThemeMode.SYSTEM
        val fontScale = preferences[fontScaleKey]
            ?.let { stored -> FontScaleOption.entries.firstOrNull { it.name == stored } }
            ?: FontScaleOption.MEDIUM
        return AppSettings(themeMode = themeMode, fontScale = fontScale)
    }
}

