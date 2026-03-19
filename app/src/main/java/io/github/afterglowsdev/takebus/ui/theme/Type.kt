package io.github.afterglowsdev.takebus.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

val BaseTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        lineHeight = 38.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 16.sp
    )
)

fun Typography.scaled(multiplier: Float): Typography {
    return copy(
        displaySmall = displaySmall.scaled(multiplier),
        headlineMedium = headlineMedium.scaled(multiplier),
        titleLarge = titleLarge.scaled(multiplier),
        titleMedium = titleMedium.scaled(multiplier),
        bodyLarge = bodyLarge.scaled(multiplier),
        bodyMedium = bodyMedium.scaled(multiplier),
        bodySmall = bodySmall.scaled(multiplier),
        labelLarge = labelLarge.scaled(multiplier)
    )
}

private fun TextStyle.scaled(multiplier: Float): TextStyle {
    return copy(
        fontSize = fontSize.scale(multiplier),
        lineHeight = lineHeight.scale(multiplier)
    )
}

private fun TextUnit.scale(multiplier: Float): TextUnit = (value * multiplier).sp

