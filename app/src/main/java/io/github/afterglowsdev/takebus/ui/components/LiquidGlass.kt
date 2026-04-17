package io.github.afterglowsdev.takebus.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    cornerRadius: Dp = 28.dp,
    glowAlpha: Float = 0.16f,
    content: @Composable BoxScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val baseColor = colorScheme.surface.copy(alpha = if (colorScheme.surface.luminance() > 0.5f) 0.62f else 0.54f)
    val edgeColor = colorScheme.onSurface.copy(alpha = 0.2f)

    Surface(
        modifier = modifier,
        shape = shape,
        color = baseColor,
        border = BorderStroke(1.dp, edgeColor),
        tonalElevation = 0.dp,
        shadowElevation = 16.dp
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = glowAlpha),
                            Color.Transparent,
                            colorScheme.primary.copy(alpha = glowAlpha * 0.65f)
                        )
                    )
                )
                .graphicsLayer {
                    this.clip = true
                    this.shape = RoundedCornerShape(cornerRadius)
                }
                .fillMaxSize(),
            content = content
        )
    }
}

fun Modifier.liquidBackdrop(shape: Shape = RoundedCornerShape(32.dp)): Modifier {
    return this
        .clip(shape)
        .background(Color.White.copy(alpha = 0.05f))
        .blur(0.1.dp)
}
