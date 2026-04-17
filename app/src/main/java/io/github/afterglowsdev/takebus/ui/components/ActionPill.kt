package io.github.afterglowsdev.takebus.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ActionPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LiquidGlassSurface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        cornerRadius = 999.dp,
        glowAlpha = 0.2f
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
