package com.gomelev.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import com.gomelev.app.model.AppThemeMode
import com.gomelev.app.model.MarketSnapshot
import com.gomelev.app.ui.screens.CarsScreen
import com.gomelev.app.ui.screens.CreditsScreen
import com.gomelev.app.ui.screens.DealersScreen
import com.gomelev.app.ui.screens.ForecastAndSyncScreen
import com.gomelev.app.ui.theme.ExpressiveMotion
import com.gomelev.app.ui.theme.LocalSemanticColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun destinationIcon(dest: AppDestination): ImageVector = when (dest) {
    AppDestination.CARS -> Icons.Rounded.DirectionsCar
    AppDestination.DEALERS -> Icons.Rounded.Storefront
    AppDestination.CREDITS -> Icons.Rounded.AccountBalance
    AppDestination.FORECAST_SYNC -> Icons.Rounded.Timeline
}

private fun themeIcon(mode: AppThemeMode): ImageVector = when (mode) {
    AppThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
    AppThemeMode.LIGHT -> Icons.Rounded.LightMode
    AppThemeMode.DARK -> Icons.Rounded.DarkMode
}

@Composable
fun GomelEvRoot(
    viewModel: MainViewModel,
    foldingFeature: FoldingFeature?
) {
    val cars by viewModel.cars.collectAsStateWithLifecycle()
    val dealers by viewModel.dealers.collectAsStateWithLifecycle()
    val programs by viewModel.programs.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val scenario by viewModel.scenario.collectAsStateWithLifecycle()
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val selectedCarId by viewModel.selectedCarId.collectAsStateWithLifecycle()
    val selectedDealerId by viewModel.selectedDealerId.collectAsStateWithLifecycle()
    val selectedProgramId by viewModel.selectedProgramId.collectAsStateWithLifecycle()

    val snackbars = remember { SnackbarHostState() }
    var manualRefresh by remember { mutableStateOf(false) }
    val refresh = {
        manualRefresh = true
        viewModel.triggerLiveSync()
    }
    LaunchedEffect(snapshot.isSyncing, snapshot.updateStatusMessage) {
        val message = snapshot.updateStatusMessage
        if (manualRefresh && !snapshot.isSyncing && message != null) {
            manualRefresh = false
            snackbars.showSnackbar(message)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onAppLaunchSyncIfNeeded()
    }

    val isExpandedWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 680

    val mainArea: @Composable (Modifier) -> Unit = { areaModifier ->
        BoxWithConstraints(modifier = areaModifier.fillMaxSize()) {
            val showTwoPanes = maxWidth >= 600.dp || (foldingFeature?.isSeparating == true &&
                foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL && maxHeight >= 480.dp)
            Column(Modifier.fillMaxSize()) {
                GomelEvHeader(
                    snapshot = snapshot,
                    themeMode = themeMode,
                    isExpandedWidth = isExpandedWidth,
                    onCycleTheme = viewModel::cycleThemeMode,
                    onSyncClick = refresh
                )

                AnimatedContent(
                    targetState = destination,
                    modifier = Modifier.weight(1f),
                    transitionSpec = {
                        val forward = targetState.ordinal >= initialState.ordinal
                        val enterSlide = slideInHorizontally(
                            animationSpec = ExpressiveMotion.defaultSpatial()
                        ) { fullWidth -> if (forward) fullWidth / 10 else -fullWidth / 10 }
                        val exitSlide = slideOutHorizontally(
                            animationSpec = ExpressiveMotion.defaultSpatial()
                        ) { fullWidth -> if (forward) -fullWidth / 14 else fullWidth / 14 }
                        (enterSlide + fadeIn(animationSpec = ExpressiveMotion.defaultSpatial()) +
                            scaleIn(initialScale = 0.985f, animationSpec = ExpressiveMotion.defaultSpatial()))
                            .togetherWith(exitSlide + fadeOut(animationSpec = ExpressiveMotion.fastSpatial()))
                    },
                    label = "destinationTransition"
                ) { currentDest ->
                    when (currentDest) {
                        AppDestination.CARS -> CarsScreen(
                            cars = cars,
                            dealers = dealers,
                            programs = programs,
                            profile = profile,
                            isExpandedWidth = showTwoPanes,
                            foldingFeature = foldingFeature,
                            selectedCarId = selectedCarId,
                            onSelectCar = viewModel::selectCar,
                            onUpdateProfile = viewModel::updateProfile
                        )

                        AppDestination.DEALERS -> DealersScreen(
                            dealers = dealers,
                            cars = cars,
                            isExpandedWidth = showTwoPanes,
                            foldingFeature = foldingFeature,
                            selectedDealerId = selectedDealerId,
                            onSelectDealer = viewModel::selectDealer,
                            onOpenCar = viewModel::openCarFromDealer
                        )

                        AppDestination.CREDITS -> CreditsScreen(
                            programs = programs,
                            profile = profile,
                            snapshot = snapshot,
                            isExpandedWidth = showTwoPanes,
                            foldingFeature = foldingFeature,
                            selectedProgramId = selectedProgramId,
                            onSelectProgram = viewModel::selectProgram,
                            onUpdateProfile = viewModel::updateProfile
                        )

                        AppDestination.FORECAST_SYNC -> ForecastAndSyncScreen(
                            cars = cars,
                            profile = profile,
                            snapshot = snapshot,
                            scenario = scenario,
                            autoSyncEnabled = autoSyncEnabled,
                            isExpandedWidth = showTwoPanes,
                            onSelectScenario = viewModel::selectScenario,
                            onUpdateProfile = viewModel::updateProfile,
                            onTriggerSync = refresh,
                            onToggleAutoSync = viewModel::toggleAutoSync
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbars) },
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isExpandedWidth) {
                CompactBottomBar(
                    destination = destination,
                    onSelectDestination = viewModel::selectDestination
                )
            }
        }
    ) { innerPadding ->
        Row(Modifier.fillMaxSize().padding(innerPadding)) {
            if (isExpandedWidth) {
                CenteredNavigationRail(
                    destination = destination,
                    themeMode = themeMode,
                    onSelectDestination = viewModel::selectDestination,
                    onCycleTheme = viewModel::cycleThemeMode
                )
                VerticalDivider(
                    thickness = 0.8.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                )
            }
            mainArea(Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun CenteredNavigationRail(
    destination: AppDestination,
    themeMode: AppThemeMode,
    onSelectDestination: (AppDestination) -> Unit,
    onCycleTheme: () -> Unit
) {
    val sem = LocalSemanticColors.current
    NavigationRail(
        modifier = Modifier.fillMaxHeight(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        windowInsets = WindowInsets(0, 0, 0, 0),
        header = {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(top = 12.dp)
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(sem.brandGradientStart, sem.brandGradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ElectricBolt,
                    contentDescription = "Гомель EV",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    ) {
        Spacer(Modifier.weight(1f))

        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppDestination.entries.forEach { dest ->
                val selected = destination == dest
                val iconScale by animateFloatAsState(
                    targetValue = if (selected) 1.12f else 1f,
                    animationSpec = ExpressiveMotion.fastSpatial(),
                    label = "railIconScale"
                )
                NavigationRailItem(
                    selected = selected,
                    onClick = { onSelectDestination(dest) },
                    icon = {
                        Icon(
                            imageVector = destinationIcon(dest),
                            contentDescription = dest.labelRu,
                            modifier = Modifier.scale(iconScale)
                        )
                    },
                    label = {
                        Text(
                            text = dest.labelRu,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconButton(onClick = onCycleTheme) {
                Icon(
                    imageVector = themeIcon(themeMode),
                    contentDescription = "Тема: ${themeMode.labelRu}",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = themeMode.labelRu,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CompactBottomBar(
    destination: AppDestination,
    onSelectDestination: (AppDestination) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 4.dp
    ) {
        Column {
            HorizontalDivider(
                thickness = 0.7.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                AppDestination.entries.forEach { dest ->
                    val selected = destination == dest
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.12f else 1f,
                        animationSpec = ExpressiveMotion.fastSpatial(),
                        label = "barIconScale"
                    )
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onSelectDestination(dest) },
                        icon = {
                            Icon(
                                imageVector = destinationIcon(dest),
                                contentDescription = dest.labelRu,
                                modifier = Modifier.scale(iconScale)
                            )
                        },
                        label = {
                            Text(
                                text = dest.labelRu,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun GomelEvHeader(
    snapshot: MarketSnapshot,
    themeMode: AppThemeMode,
    isExpandedWidth: Boolean,
    onCycleTheme: () -> Unit,
    onSyncClick: () -> Unit
) {
    val syncLabel = when {
        snapshot.isSyncing -> "Обновляем данные"
        snapshot.lastFullSyncEpochMs == null -> "Данные из базы"
        else -> "Проверка " + SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
            .format(Date(snapshot.lastFullSyncEpochMs))
    }

    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.fillMaxWidth().statusBarsPadding()
        ) {
            androidx.compose.material3.TopAppBar(
                title = {
                    Column {
                        Text("Гомель EV", style = MaterialTheme.typography.titleLarge, maxLines = 1)
                        Text(syncLabel, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0),
                actions = {
                    if (!isExpandedWidth) {
                        IconButton(onClick = onCycleTheme) {
                            Icon(themeIcon(themeMode), "Тема: ${themeMode.labelRu}")
                        }
                    }
                    IconButton(onClick = onSyncClick, enabled = !snapshot.isSyncing) {
                        if (snapshot.isSyncing) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Sync, "Проверить источники")
                        }
                    }
                }
            )

            HorizontalDivider(
                thickness = 0.7.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )
        }
    }
}
