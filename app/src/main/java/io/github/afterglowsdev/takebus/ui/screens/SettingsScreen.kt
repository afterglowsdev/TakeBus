package io.github.afterglowsdev.takebus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.afterglowsdev.takebus.R
import io.github.afterglowsdev.takebus.data.settings.AppSettings
import io.github.afterglowsdev.takebus.data.settings.FontScaleOption
import io.github.afterglowsdev.takebus.data.settings.ThemeMode

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    settings: AppSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onFontScaleChange: (FontScaleOption) -> Unit,
    onOpenAbout: () -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(
            top = 28.dp,
            bottom = contentPadding.calculateBottomPadding() + 28.dp
        )
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.displaySmall
            )
        }
        item {
            SettingGroup(title = "Appearance") {
                ThemeMode.entries.forEach { mode ->
                    OptionRow(
                        title = when (mode) {
                            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                            ThemeMode.DARK -> stringResource(R.string.theme_dark)
                            ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                        },
                        selected = settings.themeMode == mode,
                        onClick = { onThemeModeChange(mode) }
                    )
                }
            }
        }
        item {
            SettingGroup(title = "Font Size") {
                FontScaleOption.entries.forEach { option ->
                    OptionRow(
                        title = when (option) {
                            FontScaleOption.SMALL -> stringResource(R.string.font_small)
                            FontScaleOption.MEDIUM -> stringResource(R.string.font_medium)
                            FontScaleOption.LARGE -> stringResource(R.string.font_large)
                        },
                        selected = settings.fontScale == option,
                        onClick = { onFontScaleChange(option) }
                    )
                }
            }
        }
        item {
            SettingGroup(title = "About") {
                OptionRow(
                    title = "About This App",
                    subtitle = "Icon, repository, license, and learning-use warning",
                    selected = false,
                    onClick = onOpenAbout
                )
            }
        }
    }
}

@Composable
private fun SettingGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge
        )
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 12.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun OptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    },
                    shape = CircleShape
                )
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(7.dp)
                        .background(MaterialTheme.colorScheme.onPrimary, CircleShape)
                )
            }
        }
    }
}
