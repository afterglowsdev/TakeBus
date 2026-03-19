package io.github.afterglowsdev.takebus.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.afterglowsdev.takebus.data.chelaile.ChelaileRepository
import io.github.afterglowsdev.takebus.data.chelaile.SearchLineHit
import io.github.afterglowsdev.takebus.data.chelaile.SearchResults
import io.github.afterglowsdev.takebus.data.chelaile.SearchStationHit
import io.github.afterglowsdev.takebus.ui.SessionState
import io.github.afterglowsdev.takebus.ui.components.LoadingPanel
import io.github.afterglowsdev.takebus.ui.components.MessagePanel
import kotlinx.coroutines.delay

private sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Error(val message: String) : SearchUiState
    data class Data(val results: SearchResults) : SearchUiState
}

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    sessionState: SessionState,
    repository: ChelaileRepository,
    onOpenStation: (String) -> Unit,
    onOpenLine: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }

    when (sessionState) {
        SessionState.Loading -> {
            LoadingPanel(
                modifier = modifier.padding(contentPadding),
                message = "Preparing search scope"
            )
        }

        SessionState.PermissionDenied -> {
            MessagePanel(
                modifier = modifier.padding(contentPadding),
                title = "Location Context Needed",
                body = "Allow location access so search can stay inside the current city."
            )
        }

        is SessionState.Error -> {
            MessagePanel(
                modifier = modifier.padding(contentPadding),
                title = "Search Unavailable",
                body = sessionState.message
            )
        }

        is SessionState.Ready -> {
            val searchState by produceState<SearchUiState>(
                initialValue = SearchUiState.Idle,
                query,
                sessionState.city.id,
                sessionState.location.lat,
                sessionState.location.lng
            ) {
                if (query.isBlank()) {
                    value = SearchUiState.Idle
                    return@produceState
                }
                delay(280)
                value = SearchUiState.Loading
                value = runCatching {
                    SearchUiState.Data(
                        repository.search(
                            cityId = sessionState.city.id,
                            location = sessionState.location,
                            keyword = query
                        )
                    )
                }.getOrElse { throwable ->
                    SearchUiState.Error(throwable.message ?: "Search failed")
                }
            }

            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(
                    top = 28.dp,
                    bottom = contentPadding.calculateBottomPadding() + 28.dp
                )
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Search",
                            style = MaterialTheme.typography.displaySmall
                        )
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(text = "Search route or stop")
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Search,
                                    contentDescription = null
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(26.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                when (val state = searchState) {
                    SearchUiState.Idle -> {
                        item {
                            SearchStatusCard(
                                title = "Search Instantly",
                                body = "Routes and stops open directly into their detail pages."
                            )
                        }
                    }

                    SearchUiState.Loading -> {
                        item {
                            SearchStatusCard(
                                title = "Searching",
                                body = "Results refresh automatically."
                            )
                        }
                    }

                    is SearchUiState.Error -> {
                        item {
                            SearchStatusCard(
                                title = "Search Failed",
                                body = state.message
                            )
                        }
                    }

                    is SearchUiState.Data -> {
                        val hasResults = state.results.lines.isNotEmpty() || state.results.stations.isNotEmpty()
                        if (!hasResults) {
                            item {
                                SearchStatusCard(
                                    title = "No Results",
                                    body = "Try another keyword."
                                )
                            }
                        } else {
                            if (state.results.lines.isNotEmpty()) {
                                item {
                                    SearchSectionTitle(text = "Routes")
                                }
                                items(state.results.lines, key = { "${it.lineId}_${it.direction}" }) { line ->
                                    SearchLineRow(line = line, onClick = { onOpenLine(line.lineNo) })
                                }
                            }
                            if (state.results.stations.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    SearchSectionTitle(text = "Stops")
                                }
                                items(state.results.stations, key = { it.stationId }) { station ->
                                    SearchStationRow(
                                        station = station,
                                        onClick = { onOpenStation(station.stationId) }
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

@Composable
private fun SearchSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SearchLineRow(line: SearchLineHit, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = line.lineNo,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = line.directionLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun SearchStationRow(station: SearchStationHit, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "Stop",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
        }
    }
}

@Composable
private fun SearchStatusCard(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )
        }
    }
}
