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
import org.json.JSONObject

class CatalogRepository(private val appContext: Context) {

    private val prefs = appContext.getSharedPreferences("gomel_ev_prefs", Context.MODE_PRIVATE)
    private val syncEngine = LiveWebSyncEngine()

    private val _cars = MutableStateFlow(restoreCarOverrides(PreloadedCatalog.cars))
    val cars: StateFlow<List<CarOffer>> = _cars.asStateFlow()

    private val _dealers = MutableStateFlow(PreloadedCatalog.dealers)
    val dealers: StateFlow<List<DealerEntity>> = _dealers.asStateFlow()

    private val _programs = MutableStateFlow(PreloadedCatalog.creditPrograms)
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
            salaryByn = prefs.getFloat("salary_byn", 2000f).toDouble(),
            existingHousingCreditByn = prefs.getFloat("housing_byn", 0f).toDouble(),
            downPaymentPct = prefs.getInt("down_pct", 0),
            customDownPaymentByn = prefs.getFloat("custom_pv_byn", 0f).toDouble(),
            useCustomDownPaymentByn = prefs.getBoolean("use_custom_pv", false),
            monthlySavingsFor2027Byn = prefs.getFloat("monthly_save_2027", 600f).toDouble(),
            forceConservativeP0 = prefs.getBoolean("conservative_p0", false)
        )
    }

    private fun restoreSnapshot(): MarketSnapshot {
        val lastSync = prefs.getLong("last_sync_ms", -1L).takeIf { it > 0L }
        return MarketSnapshot(
            usdBynRate = prefs.getFloat("usd_byn_rate", 3.0026f).toDouble(),
            nbrbDateLabel = prefs.getString("nbrb_date_label", "13.11.2026 (база)") ?: "13.11.2026 (база)",
            rvsrPct = prefs.getFloat("rvsr_pct", 16.94f).toDouble(),
            lastFullSyncEpochMs = lastSync
        )
    }

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
        if (_snapshot.value.isSyncing) return
        _snapshot.update { it.copy(isSyncing = true) }
        try {
            val outcome = syncEngine.runFullLiveSync(_dealers.value, _cars.value)

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

            // Apply synced price updates if any site changed price
            if (outcome.updatedCarPrices.isNotEmpty()) {
                val overrides = JSONObject(prefs.getString("car_price_overrides", "{}") ?: "{}")
                _cars.update { list ->
                    list.map { car ->
                        val upd = outcome.updatedCarPrices[car.id]
                        if (upd != null) {
                            overrides.put(
                                car.id,
                                JSONObject().put("price", upd.first).put("note", upd.second)
                            )
                            car.copy(priceByn = upd.first, lastSyncedPriceNote = upd.second)
                        } else {
                            car
                        }
                    }
                }
                prefs.edit().putString("car_price_overrides", overrides.toString()).apply()
            }

            val nextUsd = outcome.usdRate ?: _snapshot.value.usdBynRate
            val nextDate = outcome.nbrbDateLabel ?: _snapshot.value.nbrbDateLabel
            val nextRvsr = outcome.detectedRvsrPct ?: _snapshot.value.rvsrPct

            prefs.edit()
                .putFloat("usd_byn_rate", nextUsd.toFloat())
                .putString("nbrb_date_label", nextDate)
                .putFloat("rvsr_pct", nextRvsr.toFloat())
                .putLong("last_sync_ms", outcome.syncEpochMs)
                .apply()

            _snapshot.update {
                it.copy(
                    usdBynRate = nextUsd,
                    nbrbDateLabel = nextDate,
                    rvsrPct = nextRvsr,
                    lastFullSyncEpochMs = outcome.syncEpochMs,
                    isSyncing = false,
                    syncLogs = outcome.logs
                )
            }
        } catch (t: Throwable) {
            _snapshot.update {
                it.copy(
                    isSyncing = false,
                    updateStatusMessage = "Ошибка синхронизации: ${t.message}"
                )
            }
        }
    }
}
