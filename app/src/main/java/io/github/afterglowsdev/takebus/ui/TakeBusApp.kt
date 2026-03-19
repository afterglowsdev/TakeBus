package io.github.afterglowsdev.takebus.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.afterglowsdev.takebus.AppContainer
import io.github.afterglowsdev.takebus.data.chelaile.City
import io.github.afterglowsdev.takebus.data.chelaile.GeoPoint
import io.github.afterglowsdev.takebus.data.settings.AppSettings
import io.github.afterglowsdev.takebus.ui.components.DockItem
import io.github.afterglowsdev.takebus.ui.components.FloatingDock
import io.github.afterglowsdev.takebus.ui.screens.AboutScreen
import io.github.afterglowsdev.takebus.ui.screens.HomeScreen
import io.github.afterglowsdev.takebus.ui.screens.LineScreen
import io.github.afterglowsdev.takebus.ui.screens.SearchScreen
import io.github.afterglowsdev.takebus.ui.screens.SettingsScreen
import io.github.afterglowsdev.takebus.ui.screens.StationScreen
import io.github.afterglowsdev.takebus.ui.theme.TakeBusTheme
import kotlinx.coroutines.launch

private const val HomeRoute = "home"
private const val SearchRoute = "search"
private const val SettingsRoute = "settings"
private const val StationRoute = "station/{stationId}"
private const val AboutRoute = "about"
private const val LineRoute = "line/{lineNo}?stationId={stationId}"

sealed interface SessionState {
    data object Loading : SessionState
    data object PermissionDenied : SessionState
    data class Error(val message: String) : SessionState
    data class Ready(val location: GeoPoint, val city: City) : SessionState
}

@Composable
fun TakeBusApp(container: AppContainer) {
    val context = LocalContext.current
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = AppSettings()
    )

    var sessionState by remember { mutableStateOf<SessionState>(SessionState.Loading) }
    var refreshToken by rememberSaveable { mutableIntStateOf(0) }
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.any { it }
        if (granted) {
            refreshToken++
        } else {
            sessionState = SessionState.PermissionDenied
        }
    }

    val hasLocationPermission = remember {
        {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission()) {
            refreshToken++
        } else if (!hasRequestedPermission) {
            hasRequestedPermission = true
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            sessionState = SessionState.PermissionDenied
        }
    }

    LaunchedEffect(refreshToken) {
        if (refreshToken == 0) return@LaunchedEffect
        sessionState = SessionState.Loading
        sessionState = runCatching {
            val location = container.locationRepository.currentLocation()
            val city = container.chelaileRepository.resolveCity(location)
            SessionState.Ready(location = location, city = city)
        }.getOrElse { throwable ->
            SessionState.Error(throwable.message ?: "Unable to load location")
        }
    }

    TakeBusTheme(
        themeMode = settings.themeMode,
        fontScale = settings.fontScale
    ) {
        val settingsScope = rememberCoroutineScope()
        val navController = rememberNavController()
        val dockItems = remember {
            listOf(
                DockItem(route = HomeRoute, icon = Icons.Rounded.Home),
                DockItem(route = SearchRoute, icon = Icons.Rounded.Search),
                DockItem(route = SettingsRoute, icon = Icons.Rounded.Settings)
            )
        }
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route?.substringBefore("?")
        val showDock = currentRoute in setOf(HomeRoute, SearchRoute, SettingsRoute)

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (showDock) {
                    FloatingDock(
                        modifier = Modifier.navigationBarsPadding(),
                        items = dockItems,
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(HomeRoute) {
                    HomeScreen(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = innerPadding,
                        sessionState = sessionState,
                        repository = container.chelaileRepository,
                        onRequestLocation = {
                            if (hasLocationPermission()) {
                                refreshToken++
                            } else {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        onOpenStation = { stationId ->
                            navController.navigate("station/${Uri.encode(stationId)}")
                        },
                        onOpenLine = { lineNo, stationId ->
                            navController.navigate(
                                "line/${Uri.encode(lineNo)}?stationId=${Uri.encode(stationId)}"
                            )
                        }
                    )
                }

                composable(SearchRoute) {
                    SearchScreen(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = innerPadding,
                        sessionState = sessionState,
                        repository = container.chelaileRepository,
                        onOpenStation = { stationId ->
                            navController.navigate("station/${Uri.encode(stationId)}")
                        },
                        onOpenLine = { lineNo ->
                            navController.navigate("line/${Uri.encode(lineNo)}?stationId=")
                        }
                    )
                }

                composable(SettingsRoute) {
                    SettingsScreen(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = innerPadding,
                        settings = settings,
                        onThemeModeChange = { mode ->
                            settingsScope.launch {
                                container.settingsRepository.setThemeMode(mode)
                            }
                        },
                        onFontScaleChange = { size ->
                            settingsScope.launch {
                                container.settingsRepository.setFontScale(size)
                            }
                        },
                        onOpenAbout = {
                            navController.navigate(AboutRoute)
                        }
                    )
                }

                composable(
                    route = StationRoute,
                    arguments = listOf(navArgument("stationId") { type = NavType.StringType })
                ) { entry ->
                    val stationId = entry.arguments?.getString("stationId").orEmpty()
                    StationScreen(
                        modifier = Modifier.fillMaxSize(),
                        stationId = stationId,
                        sessionState = sessionState,
                        repository = container.chelaileRepository,
                        onBack = { navController.popBackStack() },
                        onOpenLine = { lineNo ->
                            navController.navigate(
                                "line/${Uri.encode(lineNo)}?stationId=${Uri.encode(stationId)}"
                            )
                        }
                    )
                }

                composable(
                    route = LineRoute,
                    arguments = listOf(
                        navArgument("lineNo") { type = NavType.StringType },
                        navArgument("stationId") {
                            type = NavType.StringType
                            defaultValue = ""
                            nullable = true
                        }
                    )
                ) { entry ->
                    val lineNo = entry.arguments?.getString("lineNo").orEmpty()
                    val stationId = entry.arguments?.getString("stationId")?.takeIf { it.isNotBlank() }
                    LineScreen(
                        modifier = Modifier.fillMaxSize(),
                        lineNo = lineNo,
                        initialStationId = stationId,
                        sessionState = sessionState,
                        repository = container.chelaileRepository,
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(AboutRoute) {
                    AboutScreen(
                        modifier = Modifier.fillMaxSize(),
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
