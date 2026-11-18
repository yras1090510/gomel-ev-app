package com.gomelev.app.data

import android.content.Context
import com.gomelev.app.model.AppThemeMode
import com.gomelev.app.model.CarOffer
import com.gomelev.app.model.CreditProgram
import com.gomelev.app.model.DealerEntity
import com.gomelev.app.model.ForecastScenario
import com.gomelev.app.model.MarketSnapshot
import com.gomelev.app.model.UserFinanceProfile
import com.gomelev.app.network.LiveWebSyncEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.gomelev.app.network.DealerInventory
import com.gomelev.app.model.BankListing
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import com.gomelev.app.ui.components.Formatters

class CatalogRepository(private val appContext: Context) {

    private val prefs = appContext.getSharedPreferences("gomel_ev_prefs", Context.MODE_PRIVATE)
    private val syncEngine = LiveWebSyncEngine()
    private val syncMutex = Mutex()

    private val _cars = MutableStateFlow(restoreCars())
    val cars: StateFlow<List<CarOffer>> = _cars.asStateFlow()

    private val _dealers = MutableStateFlow(PreloadedCatalog.dealers)
    val dealers: StateFlow<List<DealerEntity>> = _dealers.asStateFlow()

    private val _programs = MutableStateFlow(restoreCreditTerms())
    val programs: StateFlow<List<CreditProgram>> = _programs.asStateFlow()

    private val _profile = MutableStateFlow(restoreProfile())
    val profile: StateFlow<UserFinanceProfile> = _profile.asStateFlow()

    private val _snapshot = MutableStateFlow(restoreSnapshot())
    val snapshot: StateFlow<MarketSnapshot> = _snapshot.asStateFlow()

    private val _scenario = MutableStateFlow(ForecastScenario.BASE)
    val scenario: StateFlow<ForecastScenario> = _scenario.asStateFlow()

    private val _autoSyncEnabled = MutableStateFlow(prefs.getBoolean("auto_sync_enabled", true))
    val autoSyncEnabled: StateFlow<Boolean> = _autoSyncEnabled.asStateFlow()

    private val _themeMode = MutableStateFlow(restoreThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private fun restoreThemeMode(): AppThemeMode {
        val raw = prefs.getString("theme_mode", AppThemeMode.SYSTEM.name)
        return runCatching { AppThemeMode.valueOf(raw ?: AppThemeMode.SYSTEM.name) }
            .getOrDefault(AppThemeMode.SYSTEM)
    }

    private fun restoreProfile(): UserFinanceProfile {
        return UserFinanceProfile(
            salaryByn = prefs.getFloat("salary_byn", 2400f).toDouble(),
            existingHousingCreditByn = prefs.getFloat("housing_byn", 320f).toDouble(),
            downPaymentPct = prefs.getInt("down_pct", 10),
            customDownPaymentByn = prefs.getFloat("custom_pv_byn", 0f).toDouble(),
            useCustomDownPaymentByn = prefs.getBoolean("use_custom_pv", false),
            monthlySavingsFor2027Byn = prefs.getFloat("monthly_save_2027", 600f).toDouble(),
            forceConservativeP0 = prefs.getBoolean("conservative_p0", false),
            otherMonthlyDebtByn = prefs.getFloat("other_monthly_debt_byn", 0f).toDouble()
        )
    }

    private fun restoreSnapshot(): MarketSnapshot {
        val lastSync = prefs.getLong("last_sync_ms", -1L).takeIf { it > 0L }
        return MarketSnapshot(
            bankListings = readBankListings(),
            bankListChecked = prefs.getLong("bank_list_checked", 0).takeIf { it > 0 },
            usdBynRate = prefs.getFloat("usd_byn_rate", 3.0026f).toDouble(),
            nbrbDateLabel = prefs.getString("nbrb_date_label", "13.11.2026 (база)") ?: "13.11.2026 (база)",
            refinancingRatePct = prefs.getFloat("refinancing_rate_pct", 9.25f).toDouble(),
            lastFullSyncEpochMs = lastSync
        )
    }

    private fun restoreCreditTerms(): List<CreditProgram> {
        return PreloadedCatalog.creditPrograms.map { program ->
            val saved = prefs.getString("${program.id}_terms", null) ?: return@map program
            runCatching {
                val fields = JSONObject(saved)
                if (program.id == "belveb_online") return@map program.copy(
                    programName = "Автокредит онлайн", maxMonths = fields.getInt("maxMonths"),
                    maxAmountByn = fields.getDouble("cap"), graceMonths = 1,
                    graceRatePct = fields.getDouble("graceRate"), postRatePct = fields.getDouble("postRate"),
                    notesRu = "Равные доли основного долга. Перечисление продавцу. От 21 года, до 60 лет на дату погашения.",
                    liveRateNote = "Ставки, срок и сумма сверены · ${Formatters.timestamp(fields.getLong("checkedAt"))}")
                val isGeely = program.id == "belarus_ex2"
                program.copy(
                    programName = if (isGeely) "На электромобили Geely EX2 / EX5 (ДБО)" else "Кредит на электромобиль",
                    graceRatePct = fields.getDouble("graceRate"),
                    graceMonths = fields.getInt("graceMonths"),
                    principalGraceMonths = fields.optInt("principalGraceMonths"),
                    postRatePct = fields.getDouble("postRate"),
                    maxMonths = fields.getInt("maxMonths"),
                    minDownPaymentPct = fields.getInt("downPayment"),
                    notesRu = if (isGeely) {
                        "Основной долг выплачивается равными долями. Ставки зависят от ставки рефинансирования НБ РБ. Условия обеспечения уточняйте в банке."
                    } else {
                        "Электромобиль младше 10 лет. Первые шесть месяцев выплачиваются проценты, затем основной долг равными долями плюс проценты. Обеспечение — поручительство."
                    },
                    liveRateNote = "Ставки, срок и взнос сверены: ${Formatters.timestamp(fields.getLong("checkedAt"))}"
                )
            }.getOrDefault(program)
        }
    }

    private fun restoreCars(): List<CarOffer> {
        val base = restoreCarOverrides(PreloadedCatalog.cars)
        val snapshots = prefs.getString("inventories", null) ?: return base
        return runCatching { DealerInventory.merge(base, JSONObject(snapshots)) }.getOrDefault(base)
    }

    private fun readBankListings(): List<BankListing> = runCatching {
        val rows = JSONArray(prefs.getString("bank_list", "[]"))
        (0 until rows.length()).map {
            val row = rows.getJSONObject(it)
            BankListing(row.getString("name"), row.getString("provider"), row.getString("terms"), row.getString("url"))
        }
    }.getOrDefault(emptyList())

    private fun restoreCarOverrides(base: List<CarOffer>): List<CarOffer> {
        val raw = prefs.getString("car_price_overrides", null) ?: return base
        return try {
            val json = JSONObject(raw)
            base.map { c ->
                val o = json.optJSONObject(c.id)
                if (o != null) {
                    c.copy(
                        priceByn = o.optDouble("price", c.priceByn),
                        lastSyncedPriceNote = o.optString("note", "").takeIf { it.isNotBlank() }
                    )
                } else {
                    c
                }
            }
        } catch (_: Throwable) {
            base
        }
    }

    fun updateProfile(transform: (UserFinanceProfile) -> UserFinanceProfile) {
        _profile.update { prev ->
            val next = transform(prev)
            prefs.edit()
                .putFloat("salary_byn", next.salaryByn.toFloat())
                .putFloat("housing_byn", next.existingHousingCreditByn.toFloat())
                .putInt("down_pct", next.downPaymentPct)
                .putFloat("custom_pv_byn", next.customDownPaymentByn.toFloat())
                .putBoolean("use_custom_pv", next.useCustomDownPaymentByn)
                .putFloat("monthly_save_2027", next.monthlySavingsFor2027Byn.toFloat())
                .putBoolean("conservative_p0", next.forceConservativeP0)
                .putFloat("other_monthly_debt_byn", next.otherMonthlyDebtByn.toFloat())
                .apply()
            next
        }
    }

    fun selectScenario(newScenario: ForecastScenario) {
        _scenario.value = newScenario
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        _autoSyncEnabled.value = enabled
        prefs.edit().putBoolean("auto_sync_enabled", enabled).apply()
    }

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    suspend fun syncLiveWebData() {
        if (!syncMutex.tryLock()) return
        _snapshot.update { it.copy(isSyncing = true, updateStatusMessage = null) }
        try {
            val outcome = syncEngine.runFullLiveSync(_dealers.value)

            // Apply updated dealer diagnostics
            _dealers.update { list ->
                list.map { dealer ->
                    val patch = outcome.dealerStatusUpdates[dealer.id]
                    if (patch != null) {
                        dealer.copy(
                            liveHttpStatus = patch.httpCode,
                            liveLatencyMs = patch.latencyMs,
                            liveStockHint = patch.hint,
                            lastCheckedEpochMs = patch.epochMs
                        )
                    } else {
                        dealer
                    }
                }
            }

            val previous = _cars.value.associateBy { it.id }
            val nextUsd = outcome.usdRate ?: _snapshot.value.usdBynRate
            val nextDate = outcome.nbrbDateLabel ?: _snapshot.value.nbrbDateLabel
            val refinancing = outcome.refinancingRatePct
            val editor = prefs.edit()
                .putFloat("usd_byn_rate", nextUsd.toFloat())
                .putString("nbrb_date_label", nextDate)
                .putFloat("refinancing_rate_pct", (refinancing ?: _snapshot.value.refinancingRatePct).toFloat())
                .putLong("last_sync_ms", outcome.syncEpochMs)
            val inventories = JSONObject(prefs.getString("inventories", "{}") ?: "{}")
            for ((id, rows) in outcome.inventories) {
                inventories.put(id, JSONObject().put("rows", rows).put("checked", outcome.syncEpochMs))
            }
            editor.putString("inventories", inventories.toString())
            val overrides = JSONObject(prefs.getString("car_price_overrides", "{}") ?: "{}")
            for ((id, update) in outcome.updatedCarPrices) {
                overrides.put(id, JSONObject().put("price", update.first).put("note", update.second))
            }
            editor.putString("car_price_overrides", overrides.toString())
            outcome.bankListings?.let { listings ->
                val rows = JSONArray()
                listings.forEach { rows.put(JSONObject().put("name", it.name).put("provider", it.provider)
                    .put("url", it.url).put("terms", it.terms)) }
                editor.putString("bank_list", rows.toString()).putLong("bank_list_checked", outcome.syncEpochMs)
            }
            var updatedCredits = 0
            if (refinancing != null) {
                for ((id, terms) in outcome.belarusbankTerms) {
                    val grace = refinancing + terms.graceOffset
                    val post = refinancing + terms.nextOffset
                    if (grace !in 0.0..100.0 || post !in 0.0..100.0) continue
                    editor.putString("${id}_terms", JSONObject()
                        .put("graceRate", grace).put("postRate", post).put("graceMonths", terms.graceMonths)
                        .put("principalGraceMonths", terms.principalGraceMonths)
                        .put("maxMonths", terms.maxMonths).put("downPayment", terms.downPayment)
                        .put("checkedAt", outcome.syncEpochMs).toString())
                    updatedCredits++
                }
            }
            outcome.belvebOnline?.let { terms ->
                editor.putString("belveb_online_terms", JSONObject().put("graceRate", terms.first)
                    .put("postRate", terms.next).put("maxMonths", terms.months).put("cap", terms.cap)
                    .put("checkedAt", outcome.syncEpochMs).toString())
                updatedCredits++
            }
            editor.apply()
            val refreshed = restoreCars()
            _cars.value = refreshed
            if (updatedCredits > 0) _programs.value = restoreCreditTerms()

            val failed = outcome.logs.count { !it.ok }
            val status = buildString {
                if (failed > 0) append("Источников недоступно: $failed. ")
                val added = refreshed.count { it.id !in previous }
                val changed = refreshed.count { previous[it.id]?.priceByn?.let { old -> old != it.priceByn } == true }
                append("Склады: ${outcome.inventories.size}/2 · новых авто: $added · изменённых цен: $changed. ")
                append("Сверено банковских программ: $updatedCredits/3.")
            }

            _snapshot.update {
                it.copy(
                    bankListings = outcome.bankListings ?: it.bankListings,
                    bankListChecked = if (outcome.bankListings != null) outcome.syncEpochMs else it.bankListChecked,
                    usdBynRate = nextUsd,
                    nbrbDateLabel = nextDate,
                    refinancingRatePct = refinancing ?: it.refinancingRatePct,
                    updateStatusMessage = status,
                    lastFullSyncEpochMs = outcome.syncEpochMs,
                    isSyncing = false,
                    syncLogs = outcome.logs
                )
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (t: Exception) {
            _snapshot.update {
                it.copy(
                    isSyncing = false,
                    updateStatusMessage = "Ошибка синхронизации: ${t.message}"
                )
            }
        } finally {
            _snapshot.update { it.copy(isSyncing = false) }
            syncMutex.unlock()
        }
    }
}
