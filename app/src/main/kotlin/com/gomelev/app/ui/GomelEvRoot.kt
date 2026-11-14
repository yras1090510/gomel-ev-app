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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.gomelev.app.model.UserFinanceProfile
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
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val selectedCarId by viewModel.selectedCarId.collectAsStateWithLifecycle()
    val selectedDealerId by viewModel.selectedDealerId.collectAsStateWithLifecycle()
    val selectedProgramId by viewModel.selectedProgramId.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onAppLaunchSyncIfNeeded()
    }

    val isExpandedWidth = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    val mainArea: @Composable (Modifier) -> Unit = { areaModifier ->
        Column(modifier = areaModifier.fillMaxSize()) {
            GomelEvHeader(
                profile = profile,
                snapshot = snapshot,
                themeMode = themeMode,
                isExpandedWidth = isExpandedWidth,
                onCycleTheme = viewModel::cycleThemeMode,
                onSyncClick = viewModel::triggerLiveSync
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
                        onToggleAutoSync = viewModel::toggleAutoSync
                    )
                }
            }
        }
    }

    if (isExpandedWidth) {
        // Unfolded / tablet layout: left NavigationRail with items VERTICALLY CENTERED ("слева посередине")
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
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
            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0)
            ) { innerPadding ->
                mainArea(Modifier.padding(innerPadding))
            }
        }
    } else {
        // Compact / Folded cover screen layout: ergonomic bottom NavigationBar ("внизу")
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                CompactBottomBar(
                    destination = destination,
                    onSelectDestination = viewModel::selectDestination
                )
            }
        ) { innerPadding ->
            mainArea(Modifier.padding(innerPadding))
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GomelEvHeader(
    profile: UserFinanceProfile,
    snapshot: MarketSnapshot,
    themeMode: AppThemeMode,
    isExpandedWidth: Boolean,
    onCycleTheme: () -> Unit,
    onSyncClick: () -> Unit
) {
    val sem = LocalSemanticColors.current
    val syncLabel = snapshot.lastFullSyncEpochMs?.let {
        "Сеть " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
    } ?: "База 13.11"

    val headerGradient = Brush.linearGradient(
        colors = listOf(
            sem.brandGradientStart.copy(alpha = if (sem.isDark) 0.22f else 0.12f),
            sem.brandGradientEnd.copy(alpha = if (sem.isDark) 0.14f else 0.06f),
            Color.Transparent
        )
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerGradient)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isExpandedWidth) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(sem.brandGradientStart, sem.brandGradientEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ElectricBolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Гомель EV · Авто & Кредит",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "НБ РБ · дилеры · прогноз 2027",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Compact Theme Switcher Pill
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onCycleTheme)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = themeIcon(themeMode),
                            contentDescription = "Тема: ${themeMode.labelRu}",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = themeMode.labelRu,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Live Sync Pill
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(enabled = !snapshot.isSyncing, onClick = onSyncClick)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (snapshot.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(13.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Sync,
                                contentDescription = "Обновить курсы и наличие",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = if (snapshot.isSyncing) "Поиск" else "Синхр.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HeaderBadge(
                    text = "1 USD = ${String.format(Locale.US, "%.4f", snapshot.usdBynRate)} BYN",
                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                    content = MaterialTheme.colorScheme.onSurface
                )
                HeaderBadge(
                    text = "ПДН ≤ ${profile.freePdnRoomByn.toInt()} BYN",
                    container = sem.passContainer,
                    content = sem.onPassContainer
                )
                HeaderBadge(
                    text = syncLabel,
                    container = MaterialTheme.colorScheme.surfaceContainerHigh,
                    content = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(
                thickness = 0.7.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
            )
        }
    }
}

@Composable
private fun HeaderBadge(
    text: String,
    container: Color,
    content: Color
) {
    Surface(
        shape = CircleShape,
        color = container
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
