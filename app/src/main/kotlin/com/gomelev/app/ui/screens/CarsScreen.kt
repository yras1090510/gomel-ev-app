package com.gomelev.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.gomelev.app.domain.CreditCalculator
import com.gomelev.app.model.CarOffer
import com.gomelev.app.model.CreditProgram
import com.gomelev.app.model.DealerEntity
import com.gomelev.app.model.ForecastScenario
import com.gomelev.app.model.MarketSnapshot
import com.gomelev.app.model.PaymentEvaluation
import com.gomelev.app.model.PdnVerdict
import com.gomelev.app.model.StockStatus
import com.gomelev.app.model.UserFinanceProfile
import com.gomelev.app.ui.components.FoldableTwoPane
import com.gomelev.app.ui.components.Formatters
import com.gomelev.app.ui.components.MinimalistCarStage
import com.gomelev.app.ui.components.PdnControlBar
import com.gomelev.app.ui.components.PdnStatusPill
import com.gomelev.app.ui.components.dialPhone
import com.gomelev.app.ui.components.openExternalUrl
import com.gomelev.app.ui.components.openGeoLocation
import com.gomelev.app.ui.theme.LocalSemanticColors

private enum class CarFilter(val label: String) {
    ALL("Все авто"),
    PASS_PDN("Проходят ПДН"),
    EV_ONLY("Электро EV"),
    IN_GOMEL("В Гомеле"),
    UNDER_40K("До 40 000")
}

@Composable
fun CarsScreen(
    cars: List<CarOffer>,
    dealers: List<DealerEntity>,
    programs: List<CreditProgram>,
    profile: UserFinanceProfile,
    snapshot: MarketSnapshot,
    scenario: ForecastScenario,
    isExpandedWidth: Boolean,
    foldingFeature: FoldingFeature?,
    selectedCarId: String?,
    onSelectCar: (String?) -> Unit,
    onUpdateProfile: ((UserFinanceProfile) -> UserFinanceProfile) -> Unit,
    onTriggerSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeFilter by remember { mutableStateOf(CarFilter.ALL) }

    val evaluatedPairs = remember(cars, programs, profile) {
        cars.map { car ->
            car to CreditCalculator.bestEvaluationForCar(car, programs, profile)
        }
    }

    val filtered = remember(evaluatedPairs, activeFilter) {
        evaluatedPairs.filter { (car, bestEval) ->
            when (activeFilter) {
                CarFilter.ALL -> true
                CarFilter.PASS_PDN -> bestEval != null && bestEval.verdict != PdnVerdict.FAIL
                CarFilter.EV_ONLY -> car.isEv
                CarFilter.IN_GOMEL -> car.stockStatus == StockStatus.IN_GOMEL
                CarFilter.UNDER_40K -> car.priceByn <= 40_000.0
            }
        }
    }

    val activeCar = remember(cars, selectedCarId, filtered) {
        cars.find { it.id == selectedCarId } ?: if (isExpandedWidth) filtered.firstOrNull()?.first else null
    }

    if (!isExpandedWidth && selectedCarId != null) {
        BackHandler { onSelectCar(null) }
    }

    FoldableTwoPane(
        isExpandedWidth = isExpandedWidth,
        foldingFeature = foldingFeature,
        hasDetailSelection = selectedCarId != null,
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    PdnControlBar(
                        profile = profile,
                        snapshot = snapshot,
                        onUpdateProfile = onUpdateProfile,
                        onTriggerSync = onTriggerSync
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CarFilter.entries.forEach { f ->
                            FilterChip(
                                selected = activeFilter == f,
                                onClick = { activeFilter = f },
                                label = { Text(f.label, style = MaterialTheme.typography.labelLarge) }
                            )
                        }
                    }
                }

                items(filtered, key = { it.first.id }) { (car, bestEval) ->
                    val isSelected = activeCar?.id == car.id && isExpandedWidth
                    CarListCard(
                        car = car,
                        bestEval = bestEval,
                        usdBynRate = snapshot.usdBynRate,
                        selected = isSelected,
                        onClick = { onSelectCar(car.id) }
                    )
                }
            }
        },
        detailPane = {
            if (activeCar != null) {
                CarDetailPane(
                    car = activeCar,
                    dealer = dealers.find { it.id == activeCar.dealerId },
                    programs = programs,
                    profile = profile,
                    snapshot = snapshot,
                    scenario = scenario,
                    showBackButton = !isExpandedWidth,
                    onBack = { onSelectCar(null) },
                    onUpdateProfile = onUpdateProfile
                )
            } else {
                BoxEmptyPlaceholder("Выберите автомобиль из каталога, чтобы увидеть расчёт по всем банкам и прогноз 2027")
            }
        }
    )
}

@Composable
private fun CarListCard(
    car: CarOffer,
    bestEval: PaymentEvaluation?,
    usdBynRate: Double,
    selected: Boolean,
    onClick: () -> Unit
) {
    val border = if (selected) {
        BorderStroke(1.6.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = if (selected) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = border
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            MinimalistCarStage(car = car, usdBynRate = usdBynRate, compact = true)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = car.brandModel,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = car.trimAndYear,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = Formatters.byn(car.priceByn),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFeatureSettings = "tnum",
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = car.stockStatus.labelRu,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = car.cityNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (bestEval != null) {
                    PdnStatusPill(
                        verdict = bestEval.verdict,
                        monthlyByn = bestEval.decidingMonthlyByn
                    )
                }
            }

            if (bestEval != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Лучшая линия: ${bestEval.program.bankName} · ПВ ${bestEval.appliedDownPaymentPct}%",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CarDetailPane(
    car: CarOffer,
    dealer: DealerEntity?,
    programs: List<CreditProgram>,
    profile: UserFinanceProfile,
    snapshot: MarketSnapshot,
    scenario: ForecastScenario,
    showBackButton: Boolean,
    onBack: () -> Unit,
    onUpdateProfile: ((UserFinanceProfile) -> UserFinanceProfile) -> Unit
) {
    val context = LocalContext.current
    val sem = LocalSemanticColors.current
    val scrollState = rememberScrollState()
    val evaluations = remember(car, programs, profile) {
        CreditCalculator.evaluateAllProgramsForCar(car, programs, profile)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = if (showBackButton) 16.dp else 0.dp,
                end = if (showBackButton) 16.dp else 0.dp,
                top = 8.dp,
                bottom = 28.dp
            ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (showBackButton) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Назад")
                }
                Text("Назад к каталогу", style = MaterialTheme.typography.titleMedium)
            }
        }

        MinimalistCarStage(car = car, usdBynRate = snapshot.usdBynRate, compact = false)

        // Header block
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(car.brandModel, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "${car.trimAndYear} · ${car.bodyType}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            Formatters.byn(car.priceByn),
                            style = MaterialTheme.typography.headlineMedium.copy(fontFeatureSettings = "tnum")
                        )
                        Text(
                            Formatters.usdFromByn(car.priceByn, snapshot.usdBynRate),
                            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    "${car.batteryOrEngine} · ${car.rangeOrSpec}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    car.priceNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (car.lastSyncedPriceNote != null) {
                    Surface(shape = CircleShape, color = sem.passContainer) {
                        Text(
                            text = car.lastSyncedPriceNote,
                            style = MaterialTheme.typography.labelMedium,
                            color = sem.onPassContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                if (dealer != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${dealer.name} · ${dealer.address}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (dealer.phones.isNotEmpty()) {
                            Button(
                                onClick = { dialPhone(context, dealer.phones.first()) },
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Rounded.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Позвонить")
                            }
                        }
                        OutlinedButton(
                            onClick = { openExternalUrl(context, car.listingUrl) },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Сайт дилера")
                        }
                        OutlinedButton(
                            onClick = { openGeoLocation(context, dealer.geoQuery) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Down payment selector right in detail pane
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Первоначальный взнос (ПВ): ${profile.downPaymentPct}% (${Formatters.byn(car.priceByn * profile.downPaymentPct / 100.0)})",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 10, 15, 20, 25, 30).forEach { pct ->
                        FilterChip(
                            selected = !profile.useCustomDownPaymentByn && profile.downPaymentPct == pct,
                            onClick = {
                                onUpdateProfile {
                                    it.copy(downPaymentPct = pct, useCustomDownPaymentByn = false)
                                }
                            },
                            label = { Text("$pct%", style = MaterialTheme.typography.labelMedium) }
                        )
                    }
                }
            }
        }

        // Forecast 2027 summary for this model
        val projected = CreditCalculator.projectedPrice2027(car, scenario)
        val pctVal = when (scenario) {
            ForecastScenario.OPTIMISTIC_BULL -> car.forecast2027BullPct
            ForecastScenario.BASE -> car.forecast2027BasePct
            ForecastScenario.PESSIMISTIC_BEAR -> car.forecast2027BearPct
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.TrendingUp, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "Прогноз цены на сентябрь 2027 (${scenario.labelRu})",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    text = "${Formatters.byn(projected)} (${if (pctVal >= 0) "+" else ""}${(pctVal * 100).toInt()}% от текущей)",
                    style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = car.forecastDriverRu,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Full bank ranking for THIS car
        Text(
            "Все подходящие кредитные линии (${evaluations.size})",
            style = MaterialTheme.typography.titleLarge
        )

        evaluations.forEach { ev ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest,
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ev.program.bankName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                ev.program.programName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        PdnStatusPill(verdict = ev.verdict, monthlyByn = ev.decidingMonthlyByn)
                    }

                    Text(
                        text = if (ev.program.graceMonths > 0) {
                            "Платёж: грейс ${ev.program.graceMonths}м = ${Formatters.byn(ev.graceMonthlyByn)} · затем = ${Formatters.byn(ev.peakPostGraceMonthlyByn)} (от P0: ${Formatters.byn(ev.conservativeP0MonthlyByn)})"
                        } else {
                            "Ежемесячный платёж: ${Formatters.byn(ev.decidingMonthlyByn)} · Срок ${ev.program.maxMonths}м"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum")
                    )

                    Text(
                        text = "Аванс ${ev.appliedDownPaymentPct}% (${Formatters.byn(ev.downPaymentByn)}) → Кредит ${Formatters.byn(ev.loanPrincipalByn)} · ${ev.program.scheduleType.labelRu}",
                        style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = ev.reasonRu,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ev.verdict == PdnVerdict.PASS) sem.passAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { openExternalUrl(context, ev.program.officialUrl) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Сайт банка", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = { openExternalUrl(context, ev.program.myfinUrl) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Myfin.by", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BoxEmptyPlaceholder(text: String) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}