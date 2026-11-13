package com.gomelev.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import com.gomelev.app.model.CarOffer
import com.gomelev.app.model.DealerCategory
import com.gomelev.app.model.DealerEntity
import com.gomelev.app.ui.components.FoldableTwoPane
import com.gomelev.app.ui.components.Formatters
import com.gomelev.app.ui.components.dialPhone
import com.gomelev.app.ui.components.openExternalUrl
import com.gomelev.app.ui.components.openGeoLocation
import com.gomelev.app.ui.theme.LocalSemanticColors

private enum class DealerFilter(val label: String) {
    ALL("Все (21)"),
    OFFICIAL("Оф. дилеры (8)"),
    AUTOHAUS("Автохаусы (11)"),
    IMPORTER("Под заказ (2)"),
    EV_IN_GOMEL("EV в Гомеле")
}

@Composable
fun DealersScreen(
    dealers: List<DealerEntity>,
    cars: List<CarOffer>,
    isExpandedWidth: Boolean,
    foldingFeature: FoldingFeature?,
    selectedDealerId: String?,
    onSelectDealer: (String?) -> Unit,
    onOpenCar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(DealerFilter.ALL) }

    val filtered = remember(dealers, filter) {
        dealers.filter { d ->
            when (filter) {
                DealerFilter.ALL -> true
                DealerFilter.OFFICIAL -> d.category == DealerCategory.OFFICIAL
                DealerFilter.AUTOHAUS -> d.category == DealerCategory.AUTOHAUS
                DealerFilter.IMPORTER -> d.category == DealerCategory.IMPORTER
                DealerFilter.EV_IN_GOMEL -> d.hasLiveEvInGomel
            }
        }
    }

    val activeDealer = remember(dealers, selectedDealerId, filtered) {
        dealers.find { it.id == selectedDealerId } ?: if (isExpandedWidth) filtered.firstOrNull() else null
    }

    if (!isExpandedWidth && selectedDealerId != null) {
        BackHandler { onSelectDealer(null) }
    }

    FoldableTwoPane(
        isExpandedWidth = isExpandedWidth,
        foldingFeature = foldingFeature,
        hasDetailSelection = selectedDealerId != null,
        modifier = modifier,
        listPane = {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = if (isExpandedWidth) 0.dp else 16.dp,
                    end = if (isExpandedWidth) 0.dp else 16.dp,
                    top = 8.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Все дилеры и автохаусы Гомеля", style = MaterialTheme.typography.headlineMedium)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DealerFilter.entries.forEach { f ->
                                FilterChip(
                                    selected = filter == f,
                                    onClick = { filter = f },
                                    label = { Text(f.label, style = MaterialTheme.typography.labelLarge) }
                                )
                            }
                        }
                    }
                }

                items(filtered, key = { it.id }) { dealer ->
                    val selected = activeDealer?.id == dealer.id && isExpandedWidth
                    DealerRowCard(
                        dealer = dealer,
                        selected = selected,
                        onClick = { onSelectDealer(dealer.id) }
                    )
                }
            }
        },
        detailPane = {
            if (activeDealer != null) {
                DealerDetailPane(
                    dealer = activeDealer,
                    dealerCars = cars.filter { it.dealerId == activeDealer.id },
                    showBackButton = !isExpandedWidth,
                    onBack = { onSelectDealer(null) },
                    onOpenCar = onOpenCar
                )
            } else {
                BoxEmptyPlaceholder("Выберите салон или автохаус для просмотра адреса, телефонов и наличия EV")
            }
        }
    )
}

@Composable
private fun DealerRowCard(
    dealer: DealerEntity,
    selected: Boolean,
    onClick: () -> Unit
) {
    val sem = LocalSemanticColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 0.8.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        dealer.category.labelRu,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val liveOk = dealer.liveHttpStatus != null && dealer.liveHttpStatus in 200..399
                    val dotColor = when {
                        dealer.liveHttpStatus == null -> MaterialTheme.colorScheme.outlineVariant
                        liveOk -> sem.passAccent
                        else -> MaterialTheme.colorScheme.error
                    }
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                    Text(
                        text = when {
                            dealer.liveHttpStatus == null -> if (dealer.hasLiveEvInGomel) "EV в Гомеле" else "В базе"
                            liveOk -> "Онлайн · ${dealer.liveLatencyMs ?: 0}мс"
                            else -> "Оффлайн / DNS"
                        },
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Text(dealer.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                dealer.brandLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                dealer.address,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                dealer.stockSummaryRu,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DealerDetailPane(
    dealer: DealerEntity,
    dealerCars: List<CarOffer>,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onOpenCar: (String) -> Unit
) {
    val context = LocalContext.current
    val sem = LocalSemanticColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = if (showBackButton) 16.dp else 0.dp,
                end = if (showBackButton) 16.dp else 0.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showBackButton) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
                Text("Все дилеры Гомеля", style = MaterialTheme.typography.titleMedium)
            }
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(dealer.name, style = MaterialTheme.typography.headlineMedium)
                Text(dealer.brandLine, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(dealer.address, style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(dealer.schedule, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Text(dealer.stockSummaryRu, style = MaterialTheme.typography.bodyMedium)

                if (dealer.liveStockHint != null) {
                    Surface(shape = RoundedCornerShape(12.dp), color = sem.passContainer) {
                        Text(
                            text = "Синхронизация: ${dealer.liveStockHint} (${Formatters.timestamp(dealer.lastCheckedEpochMs)})",
                            style = MaterialTheme.typography.labelMedium,
                            color = sem.onPassContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dealer.phones.forEach { phone ->
                        Button(
                            onClick = { dialPhone(context, phone) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(phone, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { openExternalUrl(context, dealer.websiteUrl) }) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Открыть сайт")
                    }
                    OutlinedButton(onClick = { openGeoLocation(context, dealer.geoQuery) }) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("На карте")
                    }
                }
            }
        }

        if (dealerCars.isNotEmpty()) {
            Text("Предложения этого дилера в каталоге", style = MaterialTheme.typography.titleLarge)
            dealerCars.forEach { car ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenCar(car.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(car.brandModel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(car.trimAndYear, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            Formatters.byn(car.priceByn),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFeatureSettings = "tnum",
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}