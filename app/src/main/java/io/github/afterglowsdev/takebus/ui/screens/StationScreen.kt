package io.github.afterglowsdev.takebus.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.afterglowsdev.takebus.data.chelaile.ChelaileRepository
import io.github.afterglowsdev.takebus.data.chelaile.StationDetails
import io.github.afterglowsdev.takebus.ui.SessionState
import io.github.afterglowsdev.takebus.ui.components.LoadingPanel
import io.github.afterglowsdev.takebus.ui.components.MessagePanel

private sealed interface StationUiState {
    data object Loading : StationUiState
    data class Error(val message: String) : StationUiState
    data class Data(val details: StationDetails) : StationUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationScreen(
    modifier: Modifier = Modifier,
    stationId: String,
    sessionState: SessionState,
    repository: ChelaileRepository,
    onBack: () -> Unit,
    onOpenLine: (String) -> Unit
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Stop") },
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
                    title = "Location Needed",
                    body = "Stop details need the current city context."
                )
            }

            is SessionState.Error -> {
                MessagePanel(
                    modifier = Modifier.padding(innerPadding),
                    title = "Stop Unavailable",
                    body = sessionState.message
                )
            }

            is SessionState.Ready -> {
                val uiState by produceState<StationUiState>(
                    initialValue = StationUiState.Loading,
                    sessionState.city.id,
                    sessionState.location.lat,
                    sessionState.location.lng,
                    stationId
                ) {
                    value = runCatching {
                        StationUiState.Data(
                            repository.getStationScreen(
                                cityId = sessionState.city.id,
                                location = sessionState.location,
                                stationId = stationId
                            )
                        )
                    }.getOrElse { throwable ->
                        StationUiState.Error(throwable.message ?: "Stop loading failed")
                    }
                }

                when (val state = uiState) {
                    StationUiState.Loading -> {
                        LoadingPanel(
                            modifier = Modifier.padding(innerPadding),
                            message = "Loading stop routes"
                        )
                    }

                    is StationUiState.Error -> {
                        MessagePanel(
                            modifier = Modifier.padding(innerPadding),
                            title = "Stop Loading Failed",
                            body = state.message
                        )
                    }

                    is StationUiState.Data -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(
                                top = innerPadding.calculateTopPadding() + 10.dp,
                                bottom = 24.dp
                            )
                        ) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = state.details.station.name,
                                        style = MaterialTheme.typography.displaySmall
                                    )
                                    Text(
                                        text = "${state.details.lines.size} directions",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f)
                                    )
                                }
                            }
                            items(state.details.lines, key = { "${it.line.lineId}_${it.targetOrder}" }) { line ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenLine(line.line.lineNo) },
                                    shape = RoundedCornerShape(26.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 0.dp,
                                    shadowElevation = 12.dp
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${line.line.lineNo}  ${line.line.directionLabel}",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "Next stop ${line.nextStationName}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = line.etaText,
                                            style = MaterialTheme.typography.titleLarge
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
