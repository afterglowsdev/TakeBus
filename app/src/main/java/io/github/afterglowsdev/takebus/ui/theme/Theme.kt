package io.github.afterglowsdev.takebus.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.afterglowsdev.takebus.data.settings.FontScaleOption
import io.github.afterglowsdev.takebus.data.settings.ThemeMode

private val LightColors = lightColorScheme(
    primary = InkBlack,
    onPrimary = PaperWhite,
    background = PaperWhite,
    onBackground = InkBlack,
    surface = Color.White,
    onSurface = InkBlack,
    outline = SoftGray
)

private val DarkColors = darkColorScheme(
    primary = PaperWhite,
    onPrimary = InkBlack,
    background = DeepBlack,
    onBackground = PaperWhite,
    surface = InkBlack,
    onSurface = PaperWhite,
    outline = SlateGray
)

@Composable
fun TakeBusTheme(
    themeMode: ThemeMode,
    fontScale: FontScaleOption,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = BaseTypography.scaled(fontScale.multiplier),
        content = content
    )
}
