package com.gomelev.app.model

import androidx.annotation.DrawableRes

enum class DealerCategory(val labelRu: String) {
    OFFICIAL("Оф. дилер"),
    AUTOHAUS("Автохаус б/у"),
    IMPORTER("Под заказ")
}

enum class AppThemeMode(val labelRu: String) {
    SYSTEM("Авто"),
    LIGHT("Светлая"),
    DARK("Тёмная")
}

enum class StockStatus(val labelRu: String) {
    IN_GOMEL("В наличии в Гомеле"),
    RB_WAREHOUSE("Склад сети / РБ"),
    ORDER("Под заказ")
}

enum class ScheduleType(val labelRu: String) {
    ANNUITY("Аннуитет (равные)"),
    DIFFERENTIATED("Дифференцированный"),
    ANNUITY_UNSPECIFIED("Аннуитет (уточнить в банке)")
}

enum class CreditScope(val labelRu: String) {
    GEELY_EV("Geely EX2 / EX5"),
    BEL_AUTO("Авто пр-ва РБ / Партнёрка"),
    EV_ALL("Электромобили (общий)"),
    EV_UNDER_TEN_YEARS("Электромобили младше 10 лет"),
    PARTNER_ALL("Салоны-партнёры"),
    USED_PARTNER("Б/у у партнёров"),
    UNIVERSAL_AUTO("Любой новый / б/у"),
    CONSUMER("На любые цели / Онлайн"),
    BEL_GOODS("Белорусские товары"),
    LEASING("Лизинг")
}

enum class PdnVerdict(val labelRu: String) {
    PASS("В расчётном лимите"),
    BORDERLINE("Чуть выше лимита"),
    FAIL("Выше лимита")
}

enum class ForecastScenario(val labelRu: String, val subtitleRu: String) {
    OPTIMISTIC_BULL("Оптимистичный", "Продление квоты ЕЭК + скидки со складов"),
    BASE("Базовый", "Пошлина 15% без квоты, 0% НДС (Указ 428), USD ~3.15"),
    PESSIMISTIC_BEAR("Негативный", "Жёсткая пошлина 15% + курс ~3.25–3.30 BYN")
}

data class UserFinanceProfile(
    val salaryByn: Double = 2400.0,
    val existingHousingCreditByn: Double = 320.0,
    val pdnRatioCap: Double = 0.40,
    val downPaymentPct: Int = 10,
    val customDownPaymentByn: Double = 0.0,
    val useCustomDownPaymentByn: Boolean = false,
    val monthlySavingsFor2027Byn: Double = 600.0,
    val forceConservativeP0: Boolean = false,
    val otherMonthlyDebtByn: Double = 0.0
) {
    val totalPdnMaxByn: Double get() = salaryByn * pdnRatioCap
    val freePdnRoomByn: Double get() = (totalPdnMaxByn - existingHousingCreditByn - otherMonthlyDebtByn).coerceAtLeast(0.0)
}

data class CarOffer(
    val id: String,
    val brandModel: String,
    val trimAndYear: String,
    val bodyType: String,
    val batteryOrEngine: String,
    val rangeOrSpec: String,
    val priceByn: Double,
    val priceNote: String,
    val isEv: Boolean,
    val isUsed: Boolean,
    val isGeelyEx: Boolean,
    val isBelPartner: Boolean,
    val isDongfengIceOrForthing: Boolean,
    val isDealerLegalEntity: Boolean,
    val stockStatus: StockStatus,
    val cityNote: String,
    val dealerId: String,
    @DrawableRes val imageRes: Int?,
    val imageSource: String? = null,
    val imageCredit: String? = null,
    val listingUrl: String,
    val forecast2027BullPct: Double,
    val forecast2027BasePct: Double,
    val forecast2027BearPct: Double,
    val forecastDriverRu: String,
    val lastSyncedPriceNote: String? = null,
    val modelYear: Int? = null,
    val remotePhoto: String? = null
)

data class DealerEntity(
    val id: String,
    val name: String,
    val brandLine: String,
    val category: DealerCategory,
    val address: String,
    val geoQuery: String,
    val schedule: String,
    val phones: List<String>,
    val websiteUrl: String,
    val syncCheckUrl: String,
    val stockSummaryRu: String,
    val hasLiveEvInGomel: Boolean,
    val liveHttpStatus: Int? = null,
    val liveLatencyMs: Long? = null,
    val liveStockHint: String? = null,
    val lastCheckedEpochMs: Long? = null
)

data class CreditProgram(
    val id: String,
    val bankName: String,
    val programName: String,
    val scope: CreditScope,
    val scheduleType: ScheduleType,
    val graceRatePct: Double,
    val graceMonths: Int,
    val postRatePct: Double,
    val maxMonths: Int,
    val minDownPaymentPct: Int,
    val maxAmountByn: Double,
    val noCollateralCapByn: Double,
    val requiresIncomeProof: Boolean,
    val requiresCasco: Boolean,
    val officialUrl: String,
    val myfinUrl: String,
    val notesRu: String,
    val liveRateNote: String? = null,
    val principalGraceMonths: Int = 0
)

data class PaymentEvaluation(
    val program: CreditProgram,
    val carPriceByn: Double,
    val appliedDownPaymentPct: Int,
    val downPaymentByn: Double,
    val loanPrincipalByn: Double,
    val graceMonthlyByn: Double,
    val peakPostGraceMonthlyByn: Double,
    val conservativeP0MonthlyByn: Double,
    val strictDiffMonth1MonthlyByn: Double,
    val decidingMonthlyByn: Double,
    val maxLoanByPdnByn: Double,
    val maxCarPriceAtMinPvByn: Double,
    val maxCarPriceAt10PvByn: Double,
    val diffFallbackMaxLoanByn: Double?,
    val verdict: PdnVerdict,
    val marginByn: Double,
    val reasonRu: String
)

data class SyncSourceLog(
    val sourceName: String,
    val url: String,
    val category: String,
    val ok: Boolean,
    val httpCode: Int?,
    val latencyMs: Long,
    val summary: String,
    val timestampEpochMs: Long
)

data class BankListing(val name: String, val provider: String, val terms: String, val url: String)

data class MarketSnapshot(
    val bankListings: List<BankListing> = emptyList(),
    val bankListChecked: Long? = null,
    val usdBynRate: Double = 3.0026,
    val nbrbDateLabel: String = "13.11.2026 (база)",
    val refinancingRatePct: Double = 9.25,
    val lastFullSyncEpochMs: Long? = null,
    val isSyncing: Boolean = false,
    val syncLogs: List<SyncSourceLog> = emptyList(),
    val updateStatusMessage: String? = null
)

data class MacroDriver2027(
    val title: String,
    val tag: String,
    val impactLabel: String,
    val isPositiveForBuyer: Boolean,
    val detailRu: String
)
