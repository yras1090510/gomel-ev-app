package com.gomelev.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import com.gomelev.app.ui.screens.CarsScreen
import com.gomelev.app.ui.screens.CreditsScreen
import com.gomelev.app.ui.screens.DealersScreen
import com.gomelev.app.ui.screens.ForecastAndSyncScreen

@Composable
fun GomelEvRoot(
    viewModel: MainViewModel,
    windowSizeClass: WindowSizeClass,
    foldingFeature: FoldingFeature?
) {
    val cars by viewModel.cars.collectAsStateWithLifecycle()
    val dealers by viewModel.dealers.collectAsStateWithLifecycle()
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val scenario by viewModel.scenario.collectAsStateWithLifecycle()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsStateWithLifecycle()
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val selectedCarId by viewModel.selectedCarId.collectAsStateWithLifecycle()
    val selectedDealerId by viewModel.selectedDealerId.collectAsStateWithLifecycle()
    val selectedProgramId by viewModel.selectedProgramId.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onAppLaunchSyncIfNeeded()
    }

    val isExpandedWidth = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
    val layoutType = if (isExpandedWidth) {
        NavigationSuiteType.NavigationRail
    } else {
        NavigationSuiteType.NavigationBar
    }

    NavigationSuiteScaffold(
        layoutType = layoutType,
        containerColor = MaterialTheme.colorScheme.background,
        navigationSuiteItems = {
            AppDestination.entries.forEach { dest ->
                val icon = when (dest) {
                    AppDestination.CARS -> Icons.Rounded.DirectionsCar
                    AppDestination.DEALERS -> Icons.Rounded.Storefront
                    AppDestination.CREDITS -> Icons.Rounded.AccountBalance
                    AppDestination.FORECAST_SYNC -> Icons.Rounded.TrendingUp
                }
                item(
                    icon = { Icon(icon, contentDescription = dest.labelRu) },
                    label = { Text(dest.labelRu, style = MaterialTheme.typography.labelMedium) },
                    selected = destination == dest,
                    onClick = { viewModel.selectDestination(dest) }
                )
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Minimalist Editorial Header Bar (adapts nicely to Fold 8 cover vs unfolded!)
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Gomel EV",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isExpandedWidth) {
                                    "Fold режим · Все дилеры Гомеля, кредитный скоринг ПДН и прогноз 2027"
                                } else {
                                    "Дилеры Гомеля · ПДН ${profile.freePdnRoomByn.toInt()} BYN/м"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    when (destination) {
                        AppDestination.CARS -> CarsScreen(
                            cars = cars,
                            dealers = dealers,
                            programs = programs,
                            profile = profile,
                            snapshot = snapshot,
                            scenario = scenario,
                            isExpandedWidth = isExpandedWidth,
                            foldingFeature = foldingFeature,
                            selectedCarId = selectedCarId,
                            onSelectCar = viewModel::selectCar,
                            onUpdateProfile = viewModel::updateProfile,
                            onTriggerSync = viewModel::triggerLiveSync
                        )

                        AppDestination.DEALERS -> DealersScreen(
                            dealers = dealers,
                            cars = cars,
                            isExpandedWidth = isExpandedWidth,
                            foldingFeature = foldingFeature,
                            selectedDealerId = selectedDealerId,
                            onSelectDealer = viewModel::selectDealer,
                            onOpenCar = viewModel::openCarFromDealer
                        )

                        AppDestination.CREDITS -> CreditsScreen(
                            programs = programs,
                            profile = profile,
                            snapshot = snapshot,
                            isExpandedWidth = isExpandedWidth,
                            foldingFeature = foldingFeature,
                            selectedProgramId = selectedProgramId,
                            onSelectProgram = viewModel::selectProgram,
                            onUpdateProfile = viewModel::updateProfile,
                            onTriggerSync = viewModel::triggerLiveSync
                        )

                        AppDestination.FORECAST_SYNC -> ForecastAndSyncScreen(
                            cars = cars,
                            profile = profile,
                            snapshot = snapshot,
                            scenario = scenario,
                            autoSyncEnabled = autoSyncEnabled,
                            isExpandedWidth = isExpandedWidth,
                            onSelectScenario = viewModel::selectScenario,
                            onUpdateProfile = viewModel::updateProfile,
                            onTriggerSync = viewModel::triggerLiveSync,
                            onToggleAutoSync = viewModel::toggleAutoSync,
                            onUpdateManifestUrl = viewModel::updateManifestUrl,
                            onCheckRemoteUpdate = viewModel::checkRemoteUpdate,
                            onInstallRemoteApk = viewModel::installRemoteApk
                        )
                    }
                }
            }
        }
    }
}
