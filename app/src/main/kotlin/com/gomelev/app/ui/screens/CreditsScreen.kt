package com.gomelev.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.gomelev.app.model.CreditProgram
import com.gomelev.app.model.CreditScope
import com.gomelev.app.model.MarketSnapshot
import com.gomelev.app.model.ScheduleType
import com.gomelev.app.model.UserFinanceProfile
import com.gomelev.app.ui.components.FoldableTwoPane
import com.gomelev.app.ui.components.Formatters
import com.gomelev.app.ui.components.PdnControlBar
import com.gomelev.app.ui.components.openExternalUrl
import com.gomelev.app.ui.theme.ExpressiveMotion
import com.gomelev.app.ui.theme.LocalSemanticColors
import kotlin.math.roundToInt

private enum class CreditFilter(val label: String) {
    ALL("Все программы"),
    ZERO_PV("С 0% ПВ"),
    ANNUITY("Аннуитет / макс. сумма"),
    EV_GEELY("Электромобили / Geely"),
    USED("Авто с пробегом / Лизинг"),
    DIFF_STATE("Госбанки (дифф.)")
}

@Composable
fun CreditsScreen(
    programs: List<CreditProgram>,
    profile: UserFinanceProfile,
    snapshot: MarketSnapshot,
    isExpandedWidth: Boolean,
    foldingFeature: FoldingFeature?,
    selectedProgramId: String?,
    onSelectProgram: (String?) -> Unit,
    onUpdateProfile: ((UserFinanceProfile) -> UserFinanceProfile) -> Unit,
    onTriggerSync: () -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by remember { mutableStateOf(CreditFilter.ALL) }

    val filtered = remember(programs, filter) {
        programs.filter { p ->
            when (filter) {
                CreditFilter.ALL -> true
                CreditFilter.ZERO_PV -> p.minDownPaymentPct == 0
                CreditFilter.ANNUITY -> p.scheduleType != ScheduleType.DIFFERENTIATED
                CreditFilter.EV_GEELY -> p.scope in listOf(
                    CreditScope.GEELY_EV,
                    CreditScope.EV_ALL,
                    CreditScope.BEL_AUTO
                )
                CreditFilter.USED -> p.scope in listOf(
                    CreditScope.USED_PARTNER,
                    CreditScope.UNIVERSAL_AUTO,
                    CreditScope.PARTNER_ALL,
                    CreditScope.LEASING
                )
                CreditFilter.DIFF_STATE -> p.scheduleType == ScheduleType.DIFFERENTIATED
            }
        }
    }

    val activeProgram = remember(programs, selectedProgramId, filtered) {
        programs.find { it.id == selectedProgramId } ?: if (isExpandedWidth) filtered.firstOrNull() else null
    }

    if (!isExpandedWidth && selectedProgramId != null) {
        BackHandler { onSelectProgram(null) }
    }

    FoldableTwoPane(
        isExpandedWidth = isExpandedWidth,
        foldingFeature = foldingFeature,
        hasDetailSelection = selectedProgramId != null,
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
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PdnControlBar(
                            profile = profile,
                            snapshot = snapshot,
                            onUpdateProfile = onUpdateProfile,
                            onTriggerSync = onTriggerSync
                        )
                        @OptIn(ExperimentalLayoutApi::class)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CreditFilter.entries.forEach { f ->
                                FilterChip(
                                    selected = filter == f,
                                    onClick = { filter = f },
                                    label = { Text(f.label, style = MaterialTheme.typography.labelMedium) }
                                )
                            }
                        }
                    }
                }

                items(filtered, key = { it.id }) { p ->
                    val selected = activeProgram?.id == p.id && isExpandedWidth
                    CreditProgramCard(
                        program = p,
                        profile = profile,
                        selected = selected,
                        onClick = { onSelectProgram(p.id) }
                    )
                }
            }
        },
        detailPane = {
            if (activeProgram != null) {
                CreditProgramDetailPane(
                    program = activeProgram,
                    profile = profile,
                    snapshot = snapshot,
                    showBackButton = !isExpandedWidth,
                    onBack = { onSelectProgram(null) }
                )
            } else {
                BoxEmptyPlaceholder("Выберите кредитную линию для калькулятора платежей и проверки ПДН")
            }
        }
    )
}

@Composable
private fun CreditProgramCard(
    program: CreditProgram,
    profile: UserFinanceProfile,
    selected: Boolean,
    onClick: () -> Unit
) {
    val sem = LocalSemanticColors.current
    val (maxLoan, conservativeLoan, diffFallback) = remember(program, profile) {
        CreditCalculator.maxProgramLimits(program, profile)
    }
    val pvFactor = (1.0 - program.minDownPaymentPct / 100.0).coerceAtLeast(0.10)
    val maxCarPrice = maxLoan / pvFactor

    val borderColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        animationSpec = ExpressiveMotion.colorTween,
        label = "creditBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 1.8.dp else 0.8.dp,
        animationSpec = ExpressiveMotion.fastSpatial(),
        label = "creditBorderW"
    )
    val cardBg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainerLowest,
        animationSpec = ExpressiveMotion.colorTween,
        label = "creditBg"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(width = borderWidth, color = borderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    program.bankName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    shape = CircleShape,
                    color = if (program.scheduleType == ScheduleType.DIFFERENTIATED) sem.warnContainer else sem.passContainer
                ) {
                    Text(
                        program.scheduleType.labelRu,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (program.scheduleType == ScheduleType.DIFFERENTIATED) sem.onWarnContainer else sem.onPassContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                program.programName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val rateStr = if (program.graceMonths > 0) {
                "${Formatters.pct(program.graceRatePct)} (${program.graceMonths}м) → ${Formatters.pct(program.postRatePct)} · до ${program.maxMonths} мес."
            } else {
                "${Formatters.pct(program.postRatePct)} · до ${program.maxMonths} мес."
            }
            Text(
                text = "$rateStr · Мин. ПВ ${program.minDownPaymentPct}%",
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Потолок кредита при ≤ ${Formatters.bynShort(profile.freePdnRoomByn)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        Formatters.byn(maxLoan),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFeatureSettings = "tnum",
                            fontWeight = FontWeight.Bold
                        ),
                        color = sem.passAccent
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Макс. авто (ПВ ${program.minDownPaymentPct}%)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        Formatters.byn(maxCarPrice),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFeatureSettings = "tnum",
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            if (diffFallback != null) {
                Text(
                    "Конс. от P0: ${Formatters.bynShort(conservativeLoan)} · если банк применит дифф.: ${Formatters.bynShort(diffFallback)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CreditProgramDetailPane(
    program: CreditProgram,
    profile: UserFinanceProfile,
    snapshot: MarketSnapshot,
    showBackButton: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sem = LocalSemanticColors.current
    var testCarPrice by remember(program.id) { mutableFloatStateOf(39900f) }
    var testPvPct by remember(program.id) { mutableFloatStateOf(program.minDownPaymentPct.toFloat()) }

    val (maxLoan, conservativeLoan, diffFallback) = remember(program, profile) {
        CreditCalculator.maxProgramLimits(program, profile)
    }

    val appliedPv = maxOf(testPvPct.roundToInt(), program.minDownPaymentPct)
    val pvByn = testCarPrice * (appliedPv / 100.0)
    val loan = (testCarPrice - pvByn).coerceAtLeast(0.0)

    val stepped = remember(loan, program) {
        CreditCalculator.steppedAnnuity(loan, program.graceRatePct, program.graceMonths, program.postRatePct, program.maxMonths)
    }
    val diff = remember(loan, program) {
        CreditCalculator.differentiatedDetails(loan, program.graceRatePct, program.graceMonths, program.postRatePct, program.maxMonths)
    }
    val currentPeak = if (program.scheduleType == ScheduleType.DIFFERENTIATED) {
        maxOf(diff.month1Grace, diff.peakPostGrace)
    } else {
        maxOf(stepped.graceMonthly, stepped.postGraceMonthly)
    }
    val passes = currentPeak <= profile.freePdnRoomByn && loan <= program.maxAmountByn

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
                Text("Кредитные линии", style = MaterialTheme.typography.titleMedium)
            }
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(program.bankName, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(program.programName, style = MaterialTheme.typography.headlineMedium)
                Text(program.notesRu, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Text(
                    "График: ${program.scheduleType.labelRu} · Срок: до ${program.maxMonths} мес.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Без залога до: ${Formatters.byn(program.noCollateralCapByn)} · Справка: ${if (program.requiresIncomeProof) "обязательна" else "не нужна (до лимита)"} · КАСКО: ${if (program.requiresCasco) "обязательно" else "нет"}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "РВСР база по стране: ${Formatters.pct(snapshot.rvsrPct)} · СР НБ РБ: ${Formatters.pct(snapshot.refinancingRatePct)}",
                    style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(shape = RoundedCornerShape(14.dp), color = sem.passContainer) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            "Ваш расчётный лимит (≤ ${Formatters.bynShort(profile.freePdnRoomByn)}/мес):",
                            style = MaterialTheme.typography.labelLarge,
                            color = sem.onPassContainer
                        )
                        Text(
                            "Макс. сумма кредита: ${Formatters.byn(maxLoan)} (конс. ${Formatters.byn(conservativeLoan)})",
                            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold),
                            color = sem.onPassContainer
                        )
                        if (diffFallback != null) {
                            Text(
                                "Если менеджер оформит дифференцированный график: потолок снизится до ${Formatters.byn(diffFallback)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                color = sem.onPassContainer
                            )
                        }
                    }
                }

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { openExternalUrl(context, program.officialUrl) }) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Сайт банка")
                    }
                    OutlinedButton(onClick = { openExternalUrl(context, program.myfinUrl) }) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Myfin.by")
                    }
                }
            }
        }

        // Interactive Custom Simulator
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Калькулятор под любую цену авто", style = MaterialTheme.typography.titleLarge)

                Text(
                    "Цена авто: ${Formatters.byn(testCarPrice.toDouble())} (${Formatters.usdFromByn(testCarPrice.toDouble(), snapshot.usdBynRate)})",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
                )
                Slider(
                    value = testCarPrice,
                    onValueChange = { testCarPrice = ((it / 500f).roundToInt() * 500f) },
                    valueRange = 12000f..80000f
                )

                Text(
                    "Первоначальный взнос: $appliedPv% (${Formatters.byn(pvByn)}) → Кредит ${Formatters.byn(loan)}",
                    style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum")
                )
                Slider(
                    value = testPvPct,
                    onValueChange = { testPvPct = it.roundToInt().toFloat() },
                    valueRange = program.minDownPaymentPct.toFloat()..40f
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (passes) sem.passContainer else sem.failContainer
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        val tc = if (passes) sem.onPassContainer else sem.onFailContainer
                        Text(
                            text = if (passes) "Одобрение по ПДН: ПРОХОДИТ (≤ ${Formatters.bynShort(profile.freePdnRoomByn)})" else "Одобрение по ПДН: ОТКАЗ (> ${Formatters.bynShort(profile.freePdnRoomByn)})",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = tc
                        )
                        if (program.scheduleType == ScheduleType.DIFFERENTIATED) {
                            Text(
                                "1-й мес (льгот.): ${Formatters.byn(diff.month1Grace)} · Пик после льготы: ${Formatters.byn(diff.peakPostGrace)} · Последний мес: ${Formatters.byn(diff.lastMonth)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = tc
                            )
                        } else {
                            Text(
                                "Аннуитет в грейс (${program.graceMonths}м): ${Formatters.byn(stepped.graceMonthly)} · После грейса: ${Formatters.byn(stepped.postGraceMonthly)} · Конс. P0: ${Formatters.byn(stepped.fullRateFromP0Monthly)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                                color = tc
                            )
                            Text(
                                "(Для сравнения пик по дифф. графику был бы ${Formatters.byn(maxOf(diff.month1Grace, diff.peakPostGrace))})",
                                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                                color = tc.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}
