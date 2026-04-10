package io.github.afterglowsdev.takebus.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.afterglowsdev.takebus.R
import io.github.afterglowsdev.takebus.data.chelaile.ChelaileRepository
import io.github.afterglowsdev.takebus.data.chelaile.HomeLineGroup
import io.github.afterglowsdev.takebus.data.chelaile.HomeStationCard
import io.github.afterglowsdev.takebus.ui.SessionState
import io.github.afterglowsdev.takebus.ui.components.ActionPill
import io.github.afterglowsdev.takebus.ui.components.LoadingPanel
import io.github.afterglowsdev.takebus.ui.components.MessagePanel

private sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Error(val message: String) : HomeUiState
    data class Data(val cards: List<HomeStationCard>) : HomeUiState
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    sessionState: SessionState,
    repository: ChelaileRepository,
    onRequestLocation: () -> Unit,
    onOpenStation: (String) -> Unit,
    onOpenLine: (lineId: String, displayLineNo: String, stationId: String) -> Unit
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    when (sessionState) {
        SessionState.Loading -> {
            LoadingPanel(
                modifier = modifier.padding(contentPadding),
                message = stringResource(R.string.home_location_resolving)
            )
        }

        SessionState.PermissionDenied -> {
            MessagePanel(
                modifier = modifier.padding(contentPadding),
                title = stringResource(R.string.home_need_location_title),
                body = stringResource(R.string.home_need_location_body),
                actionLabel = stringResource(R.string.action_request_again),
                onAction = onRequestLocation
            )
        }

        is SessionState.Error -> {
            MessagePanel(
                modifier = modifier.padding(contentPadding),
                title = stringResource(R.string.home_location_failed_title),
                body = sessionState.message,
                actionLabel = stringResource(R.string.action_retry),
                onAction = onRequestLocation
            )
        }

        is SessionState.Ready -> {
            val listState = rememberLazyListState()
            val expandedStations = remember { mutableStateMapOf<String, Boolean>() }
            val genericNearbyStopsError = stringResource(R.string.home_nearby_stops_error)
            val uiState by produceState<HomeUiState>(
                initialValue = HomeUiState.Loading,
                key1 = sessionState.city.id,
                key2 = sessionState.location.lat,
                key3 = sessionState.location.lng
            ) {
                value = runCatching {
                    HomeUiState.Data(
                        repository.getHomeStationCards(
                            city = sessionState.city,
                            location = sessionState.location
                        )
                    )
                }.getOrElse { throwable ->
                    HomeUiState.Error(throwable.message ?: genericNearbyStopsError)
                }
            }

            when (val state = uiState) {
                HomeUiState.Loading -> {
                    LoadingPanel(
                        modifier = modifier.padding(contentPadding),
                        message = stringResource(R.string.home_loading_stops)
                    )
                }

                is HomeUiState.Error -> {
                    MessagePanel(
                        modifier = modifier.padding(contentPadding),
                        title = stringResource(R.string.home_stop_loading_failed_title),
                        body = state.message,
                        actionLabel = stringResource(R.string.action_refresh_location),
                        onAction = onRequestLocation
                    )
                }

                is HomeUiState.Data -> {
                    if (state.cards.isEmpty()) {
                        MessagePanel(
                            modifier = modifier.padding(contentPadding),
                            title = stringResource(R.string.home_no_nearby_stops_title),
                            body = stringResource(R.string.home_no_nearby_stops_body),
                            actionLabel = stringResource(R.string.action_refresh),
                            onAction = onRequestLocation
                        )
                    } else {
                        LazyColumn(
                            modifier = modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp),
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(
                                top = maxOf(topInset, contentPadding.calculateTopPadding()) + 24.dp,
                                bottom = contentPadding.calculateBottomPadding() + 32.dp
                            )
                        ) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = stringResource(R.string.home_title),
                                        style = MaterialTheme.typography.displaySmall
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.LocationOn,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = sessionState.city.name ?: sessionState.city.id,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                                        )
                                    }
                                }
                            }

                            itemsIndexed(state.cards, key = { _, card -> card.station.id }) { index, card ->
                                val relativeOffset =
                                    (index - listState.firstVisibleItemIndex) -
                                        (listState.firstVisibleItemScrollOffset / 420f)
                                val normalized = relativeOffset.coerceIn(-0.8f, 4f)
                                val targetScale = when {
                                    normalized < 0f -> 1f + (-normalized * 0.01f)
                                    else -> 1f - (normalized * 0.028f)
                                }.coerceIn(0.9f, 1.01f)

                                val scale by animateFloatAsState(
                                    targetValue = targetScale,
                                    animationSpec = spring(
                                        dampingRatio = 0.88f,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "windowScale"
                                )
                                val expanded = expandedStations[card.station.id] == true

                                StationWindowCard(
                                    modifier = Modifier.scale(scale),
                                    card = card,
                                    expanded = expanded,
                                    onToggle = {
                                        expandedStations[card.station.id] = !expanded
                                    },
                                    onOpenStation = { onOpenStation(card.station.id) },
                                    onOpenLine = { lineId, displayLineNo ->
                                        onOpenLine(lineId, displayLineNo, card.station.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StationWindowCard(
    modifier: Modifier = Modifier,
    card: HomeStationCard,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenStation: () -> Unit,
    onOpenLine: (lineId: String, displayLineNo: String) -> Unit
) {
    val nearLabel = stringResource(R.string.home_near)
    val linesCount = stringResource(R.string.home_lines_count, card.lines.size)
    val distanceLabel = card.station.distanceMeters
        ?.let { stringResource(R.string.common_distance_meters, it) }
        ?: nearLabel

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(30.dp),
        tonalElevation = 0.dp,
        shadowElevation = 16.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onOpenStation),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = card.station.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = buildString {
                            append(distanceLabel)
                            append("  |  ")
                            append(linesCount)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            shape = CircleShape
                        )
                        .clickable(onClick = onToggle)
                        .padding(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(if (expanded) 180f else 0f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val lines = if (expanded) card.lines else card.lines.take(2)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                lines.forEach { line ->
                    HomeLineItem(
                        group = line,
                        onClick = {
                            onOpenLine(
                                line.representativeLineId,
                                line.displayLineNo
                            )
                        }
                    )
                }
            }

            if (!expanded && card.lines.size > 2) {
                Spacer(modifier = Modifier.height(16.dp))
                ActionPill(label = stringResource(R.string.home_show_all), onClick = onToggle)
            }
        }
    }
}

@Composable
private fun HomeLineItem(
    group: HomeLineGroup,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = group.displayLineNo,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = group.bestEntry.etaText,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = group.entries.joinToString("  |  ") { it.line.directionLabel },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
