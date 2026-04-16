package io.github.afterglowsdev.takebus.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.afterglowsdev.takebus.R
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
    lineId: String,
    displayLineNo: String,
    initialStationId: String?,
    sessionState: SessionState,
    repository: ChelaileRepository,
    onBack: () -> Unit
) {
    var selectedStationId by rememberSaveable(lineId, displayLineNo) { mutableStateOf(initialStationId.orEmpty()) }
    val loadingLocation = stringResource(R.string.line_loading_location)
    val locationContextNeededTitle = stringResource(R.string.line_location_context_needed_title)
    val locationContextNeededBody = stringResource(R.string.line_location_context_needed_body)
    val lineUnavailableTitle = stringResource(R.string.line_unavailable_title)
    val lineLoadingFailedTitle = stringResource(R.string.line_loading_failed_title)
    val lineLoadingBoth = stringResource(R.string.line_loading_both)
    val lineLoadingFailed = stringResource(R.string.line_loading_failed)
    val noDirectionsTitle = stringResource(R.string.line_no_directions_title)
    val noDirectionsBody = stringResource(R.string.line_no_directions_body)
    val titleText = displayLineNo.ifBlank { lineId }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = titleText,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
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
                    message = loadingLocation
                )
            }

            SessionState.PermissionDenied -> {
                MessagePanel(
                    modifier = Modifier.padding(innerPadding),
                    title = locationContextNeededTitle,
                    body = locationContextNeededBody
                )
            }

            is SessionState.Error -> {
                MessagePanel(
                    modifier = Modifier.padding(innerPadding),
                    title = lineUnavailableTitle,
                    body = sessionState.message
                )
            }

            is SessionState.Ready -> {
                val uiState by produceState<LineUiState>(
                    initialValue = LineUiState.Loading,
                    sessionState.city.id,
                    sessionState.location.lat,
                    sessionState.location.lng,
                    lineId,
                    displayLineNo,
                    selectedStationId
                ) {
                    value = runCatching {
                        LineUiState.Data(
                            repository.getLineScreen(
                                cityId = sessionState.city.id,
                                location = sessionState.location,
                                lineId = lineId,
                                displayLineNo = displayLineNo,
                                stationId = selectedStationId.takeIf { it.isNotBlank() }
                            )
                        )
                    }.getOrElse { throwable ->
                        LineUiState.Error(throwable.message ?: lineLoadingFailed)
                    }
                }

                when (val state = uiState) {
                    LineUiState.Loading -> {
                        LoadingPanel(
                            modifier = Modifier.padding(innerPadding),
                            message = lineLoadingBoth
                        )
                    }

                    is LineUiState.Error -> {
                        MessagePanel(
                            modifier = Modifier.padding(innerPadding),
                            title = lineLoadingFailedTitle,
                            body = state.message
                        )
                    }

                    is LineUiState.Data -> {
                        val directions = state.line.directions
                        if (directions.isEmpty()) {
                            MessagePanel(
                                modifier = Modifier.padding(innerPadding),
                                title = noDirectionsTitle,
                                body = noDirectionsBody
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
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
                                            thickness = 1.dp
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
    val noLiveTip = stringResource(R.string.line_no_live_tip)
    val noLivePositions = stringResource(R.string.line_no_live_positions)

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Header + bus chips ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Direction info card with primary colour background
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = panel.line.directionLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Text(
                        text = panel.tip.ifBlank {
                            panel.buses.firstOrNull()?.etaText ?: noLiveTip
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f)
                    )
                    // Selected stop badge
                    val selectedStopName = panel.selectedStop.name
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f),
                                    CircleShape
                                )
                        )
                        Text(
                            text = selectedStopName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Bus arrival chips row
            if (panel.buses.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(panel.buses.take(4), key = { "${it.busId}_${it.order}" }) { bus ->
                        BusChip(bus = bus)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = noLivePositions,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // ── Route timeline fills remaining space ────────────────────────────
        RouteTimeline(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            stops = panel.stations,
            selectedStopId = panel.selectedStop.id,
            targetOrder = panel.targetOrder,
            onSelectStop = onSelectStop
        )
    }
}

@Composable
private fun BusChip(bus: BusMarker) {
    // Treat ≤ 2 min as "arriving" urgency
    val isUrgent = (bus.etaMinutes ?: Int.MAX_VALUE) <= 2
    val chipBg by animateColorAsState(
        targetValue = if (isUrgent) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surface,
        animationSpec = tween(300),
        label = "busChipBg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isUrgent) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(300),
        label = "busChipContent"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = chipBg,
        tonalElevation = 0.dp,
        shadowElevation = if (isUrgent) 2.dp else 6.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ETA — most prominent info
            Text(
                text = bus.etaText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            // Bus ID
            Text(
                text = bus.busId,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.7f)
            )
            // Distance (optional)
            bus.distanceToStationMeters?.let { distance ->
                Text(
                    text = stringResource(R.string.common_distance_meters, distance),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.55f)
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
    val selectedLabel = stringResource(R.string.line_selected_label)

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 8.dp
            )
        ) {
            itemsIndexed(
                items = stops,
                key = { _, stop -> "${stop.id}_${stop.order}" }
            ) { index, stop ->
                val isSelected = stop.id == selectedStopId
                val isFirst = index == 0
                val isLast = index == stops.lastIndex
                val isTarget = stop.order == targetOrder

                val dotColor by animateColorAsState(
                    targetValue = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isTarget -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.40f)
                    },
                    animationSpec = tween(220),
                    label = "dot_${stop.id}"
                )
                val lineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)

                // Row: timeline indicator | stop info | order badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .clickable { onSelectStop(stop) }
                        .then(
                            if (isSelected) Modifier.background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
                                shape = RoundedCornerShape(14.dp)
                            ) else Modifier
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    // ── Timeline column ───────────────────────────────────
                    Column(
                        modifier = Modifier
                            .width(26.dp)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top connector
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(14.dp)
                                .background(if (isFirst) Color.Transparent else lineColor)
                        )
                        // Station dot
                        Box(
                            modifier = Modifier
                                .size(if (isSelected || isTarget) 13.dp else 8.dp)
                                .then(
                                    if (isSelected) Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        shape = CircleShape
                                    ) else Modifier
                                )
                                .background(dotColor, CircleShape)
                        )
                        // Bottom connector fills remaining height
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .weight(1f)
                                .background(if (isLast) Color.Transparent else lineColor)
                        )
                    }

                    // ── Stop name & label ─────────────────────────────────
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp, top = 8.dp, bottom = 14.dp)
                    ) {
                        Text(
                            text = stop.name,
                            style = if (isSelected) {
                                MaterialTheme.typography.bodyLarge
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onBackground
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isTarget) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                            )
                        }
                    }

                    // ── Order badge ───────────────────────────────────────
                    Text(
                        text = "#${stop.order}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = if (isSelected) 0.55f else 0.32f
                        ),
                        modifier = Modifier
                            .padding(top = 10.dp, end = 6.dp)
                            .align(Alignment.Top)
                    )
                }
            }
        }
    }
}
