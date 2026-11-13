package com.gomelev.app.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gomelev.app.BuildConfig
import com.gomelev.app.data.PreloadedCatalog
import com.gomelev.app.domain.CreditCalculator
import com.gomelev.app.model.CarOffer
import com.gomelev.app.model.ForecastScenario
import com.gomelev.app.model.MarketSnapshot
import com.gomelev.app.model.SyncSourceLog
import com.gomelev.app.model.UserFinanceProfile
import com.gomelev.app.ui.components.Formatters
import com.gomelev.app.ui.components.openExternalUrl
import com.gomelev.app.ui.theme.LocalSemanticColors
import kotlin.math.roundToInt

@Composable
fun ForecastAndSyncScreen(
    cars: List<CarOffer>,
    profile: UserFinanceProfile,
    snapshot: MarketSnapshot,
    scenario: ForecastScenario,
    autoSyncEnabled: Boolean,
    isExpandedWidth: Boolean,
    onSelectScenario: (ForecastScenario) -> Unit,
    onUpdateProfile: ((UserFinanceProfile) -> UserFinanceProfile) -> Unit,
    onTriggerSync: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onUpdateManifestUrl: (String) -> Unit,
    onCheckRemoteUpdate: () -> Unit,
    onInstallRemoteApk: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isExpandedWidth) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BoxForecastPane(
                cars = cars,
                profile = profile,
                scenario = scenario,
                onSelectScenario = onSelectScenario,
                onUpdateProfile = onUpdateProfile,
                modifier = Modifier
                    .weight(0.52f)
                    .fillMaxSize()
            )
            BoxSyncPane(
                snapshot = snapshot,
                autoSyncEnabled = autoSyncEnabled,
                onTriggerSync = onTriggerSync,
                onToggleAutoSync = onToggleAutoSync,
                onUpdateManifestUrl = onUpdateManifestUrl,
                onCheckRemoteUpdate = onCheckRemoteUpdate,
                onInstallRemoteApk = onInstallRemoteApk,
                modifier = Modifier
                    .weight(0.48f)
                    .fillMaxSize()
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ForecastSectionContent(
                    cars = cars,
                    profile = profile,
                    scenario = scenario,
                    onSelectScenario = onSelectScenario,
                    onUpdateProfile = onUpdateProfile
                )
            }
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            item {
                SyncControlSection(
                    snapshot = snapshot,
                    autoSyncEnabled = autoSyncEnabled,
                    onTriggerSync = onTriggerSync,
                    onToggleAutoSync = onToggleAutoSync,
                    onUpdateManifestUrl = onUpdateManifestUrl,
                    onCheckRemoteUpdate = onCheckRemoteUpdate,
                    onInstallRemoteApk = onInstallRemoteApk
                )
            }
            items(snapshot.syncLogs) { log ->
                SyncLogCard(log)
            }
        }
    }
}

@Composable
private fun BoxForecastPane(
    cars: List<CarOffer>,
    profile: UserFinanceProfile,
    scenario: ForecastScenario,
    onSelectScenario: (ForecastScenario) -> Unit,
    onUpdateProfile: ((UserFinanceProfile) -> UserFinanceProfile) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        ForecastSectionContent(
            cars = cars,
            profile = profile,
            scenario = scenario,
            onSelectScenario = onSelectScenario,
            onUpdateProfile = onUpdateProfile
        )
    }
}

@Composable
private fun BoxSyncPane(
    snapshot: MarketSnapshot,
    autoSyncEnabled: Boolean,
    onTriggerSync: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onUpdateManifestUrl: (String) -> Unit,
    onCheckRemoteUpdate: () -> Unit,
    onInstallRemoteApk: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SyncControlSection(
                snapshot = snapshot,
                autoSyncEnabled = autoSyncEnabled,
                onTriggerSync = onTriggerSync,
                onToggleAutoSync = onToggleAutoSync,
                onUpdateManifestUrl = onUpdateManifestUrl,
                onCheckRemoteUpdate = onCheckRemoteUpdate,
                onInstallRemoteApk = onInstallRemoteApk
            )
        }
        items(snapshot.syncLogs) { log ->
            SyncLogCard(log)
        }
    }
}

@Composable
private fun ForecastSectionContent(
    cars: List<CarOffer>,
    profile: UserFinanceProfile,
    scenario: ForecastScenario,
    onSelectScenario: (ForecastScenario) -> Unit,
    onUpdateProfile: ((UserFinanceProfile) -> UserFinanceProfile) -> Unit
) {
    val sem = LocalSemanticColors.current
    val savedBy2027 = remember(profile.monthlySavingsFor2027Byn) {
        CreditCalculator.simulateDepositSavings(profile.monthlySavingsFor2027Byn, months = 10, annualRatePct = 12.0)
    }

    Text("Прогноз цен: сентябрь 2027", style = MaterialTheme.typography.headlineMedium)

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ForecastScenario.entries.forEach { sc ->
            FilterChip(
                selected = scenario == sc,
                onClick = { onSelectScenario(sc) },
                label = { Text(sc.labelRu, style = MaterialTheme.typography.labelLarge) }
            )
        }
    }
    Text(
        scenario.subtitleRu,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    // Wealth accumulation simulator for Sept 2027
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Стратегия: брать сейчас или копить до сентября 2027?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Откладывать в месяц (10 мес под ~12%): ${Formatters.byn(profile.monthlySavingsFor2027Byn)} → Накоплено: ${Formatters.byn(savedBy2027)}",
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                color = sem.passAccent,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = profile.monthlySavingsFor2027Byn.toFloat(),
                onValueChange = { v ->
                    onUpdateProfile { it.copy(monthlySavingsFor2027Byn = ((v / 50f).roundToInt() * 50.0)) }
                },
                valueRange = 200f..1000f
            )
            Text(
                "Вывод: с 0% ПВ прямо сейчас уверенно проходит только FAW Xiaoma (24 990) и ДВС/Peugeot iOn. " +
                    "Накопив ${Formatters.bynShort(savedBy2027)} BYN к сентябрю 2027, вы закроете 12–15% ПВ для Geely EX2 (Приорбанк 4%→12,5% / БГПБ 12 лет) или подешевевшего б/у BYD Seagull.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Forecast comparison table per EV
    val keyModels = remember(cars) {
        cars.filter {
            it.id in setOf(
                "faw_xiaoma",
                "byd_seagull",
                "dongfeng_box",
                "kia_niro_ev",
                "geely_ex2_pro",
                "arcfox_t1_pro",
                "jac_e30x_new"
            )
        }
    }

    keyModels.forEach { car ->
        val projected = CreditCalculator.projectedPrice2027(car, scenario)
        val pct = when (scenario) {
            ForecastScenario.OPTIMISTIC_BULL -> car.forecast2027BullPct
            ForecastScenario.BASE -> car.forecast2027BasePct
            ForecastScenario.PESSIMISTIC_BEAR -> car.forecast2027BearPct
        }
        val netLoan2027 = (projected - savedBy2027).coerceAtLeast(0.0)
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(car.brandModel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(
                        "${if (pct >= 0) "+" else ""}${(pct * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
                        color = if (pct <= 0) sem.passAccent else MaterialTheme.colorScheme.error
                    )
                }
                Text(
                    "Сейчас: ${Formatters.byn(car.priceByn)}  →  Сент. 2027: ${Formatters.byn(projected)}  ·  Кредит с вашим ПВ: ${Formatters.byn(netLoan2027)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    car.forecastDriverRu,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    Text("Макродрайверы 2026–2027", style = MaterialTheme.typography.titleLarge)
    PreloadedCatalog.macroDrivers2027.forEach { driver ->
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(driver.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Surface(
                        shape = CircleShape,
                        color = if (driver.isPositiveForBuyer) sem.passContainer else sem.warnContainer
                    ) {
                        Text(
                            driver.tag,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (driver.isPositiveForBuyer) sem.onPassContainer else sem.onWarnContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(driver.detailRu, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SyncControlSection(
    snapshot: MarketSnapshot,
    autoSyncEnabled: Boolean,
    onTriggerSync: () -> Unit,
    onToggleAutoSync: (Boolean) -> Unit,
    onUpdateManifestUrl: (String) -> Unit,
    onCheckRemoteUpdate: () -> Unit,
    onInstallRemoteApk: (String) -> Unit
) {
    val sem = LocalSemanticColors.current
    var manifestInput by remember(snapshot.appUpdateManifestUrl) {
        mutableStateOf(snapshot.appUpdateManifestUrl)
    }

    Text("Живая связь с сайтами и обновления", style = MaterialTheme.typography.headlineMedium)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Курс НБ РБ: 1 USD = ${Formatters.rateFx(snapshot.usdBynRate)} BYN",
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold)
                    )
                    Text(
                        "${snapshot.nbrbDateLabel} · РВСР ${Formatters.pct(snapshot.rvsrPct)} · СР ${Formatters.pct(snapshot.refinancingRatePct)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Последняя синхронизация: ${Formatters.timestamp(snapshot.lastFullSyncEpochMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Button(
                onClick = onTriggerSync,
                enabled = !snapshot.isSyncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (snapshot.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Опрос НБ РБ, дилеров и Myfin.by...")
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Обновить цены, курсы и дилеров с сайтов")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Фоновое автообновление (WorkManager)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Text("Раз в 12 часов при подключении к интернету", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = autoSyncEnabled, onCheckedChange = onToggleAutoSync)
            }
        }
    }

    // OTA App Update Checker
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text("Обновление приложения (OTA APK + база)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Текущая версия: v${BuildConfig.VERSION_NAME} (код ${BuildConfig.VERSION_CODE})", style = MaterialTheme.typography.labelMedium)
                }
            }

            OutlinedTextField(
                value = manifestInput,
                onValueChange = {
                    manifestInput = it
                    onUpdateManifestUrl(it)
                },
                label = { Text("URL манифеста (manifest.json)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCheckRemoteUpdate, modifier = Modifier.weight(1f)) {
                    Text("Проверить APK / базу")
                }
                val apkUrl = snapshot.remoteApkUrl
                val hasNewerApk = apkUrl != null && (snapshot.remoteVersionCode ?: 0) > BuildConfig.VERSION_CODE
                if (hasNewerApk && apkUrl != null) {
                    Button(onClick = { onInstallRemoteApk(apkUrl) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Установить APK")
                    }
                }
            }

            if (snapshot.updateStatusMessage != null) {
                Text(
                    snapshot.updateStatusMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = sem.passAccent
                )
            }
        }
    }

    if (snapshot.syncLogs.isNotEmpty()) {
        val okCount = snapshot.syncLogs.count { it.ok }
        Text(
            "Журнал живого опроса сайтов ($okCount/${snapshot.syncLogs.size} онлайн)",
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
private fun SyncLogCard(log: SyncSourceLog) {
    val context = LocalContext.current
    val sem = LocalSemanticColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant),
        onClick = { openExternalUrl(context, log.url) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (log.ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = if (log.ok) sem.passAccent else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(log.sourceName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(log.summary, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = "${log.httpCode ?: "—"} · ${log.latencyMs}мс",
                style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}