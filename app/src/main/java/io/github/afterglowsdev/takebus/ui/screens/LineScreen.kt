package io.github.afterglowsdev.takebus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.afterglowsdev.takebus.data.chelaile.BusMarker
import io.github.afterglowsdev.takebus.data.chelaile.ChelaileRepository
import io.github.afterglowsdev.takebus.data.chelaile.LineDirectionPanel
import io.github.afterglowsdev.takebus.data.chelaile.LineScreenData
import io.github.afterglowsdev.takebus.data.chelaile.RouteStop
import io.github.afterglowsdev.takebus.ui.SessionState
import io.github.afterglowsdev.takebus.ui.components.LoadingPanel
import io.github.afterglowsdev.takebus.ui.components.MessagePanel

private sealed interface LineUiState {
    data object Loading : LineUiState
    data class Error(val message: String) : LineUiState
    data class Data(val line: LineScreenData) : LineUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineScreen(
    modifier: Modifier = Modifier,
    lineNo: String,
    initialStationId: String?,
    sessionState: SessionState,
    repository: ChelaileRepository,
    onBack: () -> Unit
) {
    var selectedStationId by rememberSaveable(lineNo) { mutableStateOf(initialStationId.orEmpty()) }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = lineNo) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when (sessionState) {
            SessionState.Loading -> {
                LoadingPanel(
                    modifier = Modifier.padding(innerPadding),
                    message = "Loading location"
                )
            }

            SessionState.PermissionDenied -> {
                MessagePanel(
                    modifier = Modifier.padding(innerPadding),
                    title = "Location Context Needed",
                    body = "Line details need the current city and stop context."
                )
            }

            is SessionState.Error -> {
                MessagePanel(
                    modifier = Modifier.padding(innerPadding),
                    title = "Line Unavailable",
                    body = sessionState.message
                )
            }

            is SessionState.Ready -> {
                val uiState by produceState<LineUiState>(
                    initialValue = LineUiState.Loading,
                    sessionState.city.id,
                    sessionState.location.lat,
                    sessionState.location.lng,
                    lineNo,
                    selectedStationId
                ) {
                    value = runCatching {
                        LineUiState.Data(
                            repository.getLineScreen(
                                cityId = sessionState.city.id,
                                location = sessionState.location,
                                lineNo = lineNo,
                                stationId = selectedStationId.takeIf { it.isNotBlank() }
                            )
                        )
                    }.getOrElse { throwable ->
                        LineUiState.Error(throwable.message ?: "Line loading failed")
                    }
                }

                when (val state = uiState) {
                    LineUiState.Loading -> {
                        LoadingPanel(
                            modifier = Modifier.padding(innerPadding),
                            message = "Loading both directions"
                        )
                    }

                    is LineUiState.Error -> {
                        MessagePanel(
                            modifier = Modifier.padding(innerPadding),
                            title = "Line Loading Failed",
                            body = state.message
                        )
                    }

                    is LineUiState.Data -> {
                        val directions = state.line.directions
                        if (directions.isEmpty()) {
                            MessagePanel(
                                modifier = Modifier.padding(innerPadding),
                                title = "No Directions",
                                body = "No route directions were returned for this line."
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                directions.forEachIndexed { index, direction ->
                                    DirectionSection(
                                        modifier = Modifier.weight(1f),
                                        panel = direction,
                                        onSelectStop = { stop ->
                                            selectedStationId = stop.id
                                        }
                                    )
                                    if (index != directions.lastIndex) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectionSection(
    modifier: Modifier = Modifier,
    panel: LineDirectionPanel,
    onSelectStop: (RouteStop) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = panel.line.directionLabel,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = panel.tip.ifBlank {
                        panel.buses.firstOrNull()?.etaText ?: "No live tip"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Text(
                    text = "Selected stop: ${panel.selectedStop.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                )
            }
        }

        if (panel.buses.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(panel.buses.take(4), key = { "${it.busId}_${it.order}" }) { bus ->
                    BusChip(bus = bus)
                }
            }
        } else {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = "No live bus positions",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        RouteTimeline(
            modifier = Modifier.fillMaxWidth(),
            stops = panel.stations,
            selectedStopId = panel.selectedStop.id,
            targetOrder = panel.targetOrder,
            onSelectStop = onSelectStop
        )
    }
}

@Composable
private fun BusChip(bus: BusMarker) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = bus.busId,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Near stop ${bus.order}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
            )
            Text(
                text = bus.etaText,
                style = MaterialTheme.typography.titleMedium
            )
            bus.distanceToStationMeters?.let { distance ->
                Text(
                    text = "${distance}m",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                )
            }
        }
    }
}

@Composable
private fun RouteTimeline(
    modifier: Modifier = Modifier,
    stops: List<RouteStop>,
    selectedStopId: String,
    targetOrder: Int,
    onSelectStop: (RouteStop) -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 20.dp)
        ) {
            items(stops, key = { "${it.id}_${it.order}" }) { stop ->
                val selected = stop.id == selectedStopId
                val accent = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                }
                Column(
                    modifier = Modifier
                        .width(92.dp)
                        .clickable { onSelectStop(stop) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(accent)
                        )
                        Box(
                            modifier = Modifier
                                .size(if (selected) 14.dp else 10.dp)
                                .background(
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                        )
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(2.dp)
                                .background(accent)
                        )
                    }
                    Text(
                        text = stop.name,
                        style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (stop.order == targetOrder) "Selected" else "#${stop.order}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
