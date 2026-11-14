package com.gomelev.app.network

import android.util.Log
import com.gomelev.app.model.CarOffer
import com.gomelev.app.model.DealerEntity
import com.gomelev.app.model.SyncSourceLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class NbrbSyncResult(
    val usdRate: Double?,
    val dateLabel: String?,
    val log: SyncSourceLog
)

data class LiveSyncOutcome(
    val usdRate: Double?,
    val nbrbDateLabel: String?,
    val detectedRvsrPct: Double?,
    val updatedCarPrices: Map<String, Pair<Double, String>>,
    val dealerStatusUpdates: Map<String, DealerSyncPatch>,
    val logs: List<SyncSourceLog>,
    val syncEpochMs: Long
)

data class DealerSyncPatch(
    val httpCode: Int?,
    val latencyMs: Long,
    val hint: String?,
    val epochMs: Long
)

class LiveWebSyncEngine {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(9, TimeUnit.SECONDS)
        .readTimeout(11, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val userAgent =
        "Mozilla/5.0 (Linux; Android 15; SM-F966B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

    private fun fetchText(url: String): Triple<Int?, Long, String?> {
        val start = System.currentTimeMillis()
        return try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", userAgent)
                .header("Accept-Language", "ru-BY,ru;q=0.9,en;q=0.7")
                .build()
            client.newCall(req).execute().use { resp ->
                val latency = System.currentTimeMillis() - start
                val body = if (resp.isSuccessful) resp.body?.string() else null
                Triple(resp.code, latency, body)
            }
        } catch (t: Throwable) {
            val latency = System.currentTimeMillis() - start
            Log.w("LiveWebSync", "Failed $url: ${t.message}")
            Triple(null, latency, t.javaClass.simpleName + ": " + (t.message ?: "error"))
        }
    }

    suspend fun runFullLiveSync(
        dealers: List<DealerEntity>,
        currentCars: List<CarOffer>
    ): LiveSyncOutcome = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        coroutineScope {
            val nbrbDeferred = async { syncNbrbRate(now) }
            val myfinDeferred = async { syncMyfinRates(now) }
            val keyCarSitesDeferred = async { syncKeyDealerCatalogs(currentCars, now) }

            // Check dealer sites concurrently in small clean pool
            val dealerDeferreds = dealers.map { dealer ->
                async {
                    val (code, latency, body) = fetchText(dealer.syncCheckUrl)
                    val ok = code != null && code in 200..399
                    val hint = when {
                        ok && body != null -> extractDealerSnippetHint(dealer.id, body)
                        code != null -> "HTTP $code (${latency}мс)"
                        else -> "Оффлайн / недоступен по сети (${latency}мс)"
                    }
                    val patch = DealerSyncPatch(code, latency, hint, now)
                    val log = SyncSourceLog(
                        sourceName = dealer.name,
                        url = dealer.syncCheckUrl,
                        category = dealer.category.labelRu,
                        ok = ok,
                        httpCode = code,
                        latencyMs = latency,
                        summary = hint ?: if (ok) "Доступен (${latency}мс)" else "Ошибка сети",
                        timestampEpochMs = now
                    )
                    (dealer.id to patch) to log
                }
            }

            // Key bank sites check
            val bankUrls = listOf(
                Triple("Приорбанк (4%→12,5%)", "Банки", "https://www.priorbank.by/offers/credits/credit-belorusskie-avto-partnery"),
                Triple("Белгазпромбанк (Авто ДРАЙВ)", "Банки", "https://belgazprombank.by/personal_banking/krediti/kredit_na_priobretenie_avtotransporta/Autodrive-credite-na-priobretenie-auto/"),
                Triple("Дабрабыт (АВТОНАДО)", "Банки", "https://bankdabrabyt.by/personal/credit/avtonado/"),
                Triple("Беларусбанк (EX2/EX5)", "Банки", "https://belarusbank.by/fizicheskim_licam/kredit/consumer/kredit-na-priobretenie-u-dilerov-szao-beldzhi-elektromobiley-marki-geely-modeli-ex5/")
            )
            val bankDeferreds = bankUrls.map { (name, cat, url) ->
                async {
                    val (code, latency, body) = fetchText(url)
                    val ok = code != null && code in 200..399
                    val rateHint = if (ok && body != null) extractFirstRateHint(body) else "Статус: ${code ?: "ошибка"}"
                    SyncSourceLog(
                        sourceName = name,
                        url = url,
                        category = cat,
                        ok = ok,
                        httpCode = code,
                        latencyMs = latency,
                        summary = rateHint,
                        timestampEpochMs = now
                    )
                }
            }

            val nbrbRes = nbrbDeferred.await()
            val (detectedRvsr, myfinLogs) = myfinDeferred.await()
            val (carUpdates, extraCarLogs) = keyCarSitesDeferred.await()
            val dealerResults = dealerDeferreds.awaitAll()
            val bankLogs = bankDeferreds.awaitAll()

            val patchMap = dealerResults.associate { it.first }
            val dealerLogs = dealerResults.map { it.second }

            val allLogs = listOf(nbrbRes.log) + myfinLogs + bankLogs + extraCarLogs + dealerLogs
            LiveSyncOutcome(
                usdRate = nbrbRes.usdRate,
                nbrbDateLabel = nbrbRes.dateLabel,
                detectedRvsrPct = detectedRvsr,
                updatedCarPrices = carUpdates,
                dealerStatusUpdates = patchMap,
                logs = allLogs,
                syncEpochMs = now
            )
        }
    }

    private fun syncNbrbRate(now: Long): NbrbSyncResult {
        val url = "https://api.nbrb.by/exrates/rates/431"
        val (code, latency, body) = fetchText(url)
        if (code == 200 && body != null) {
            try {
                val json = JSONObject(body)
                val rate = json.optDouble("Cur_OfficialRate", -1.0)
                val rawDate = json.optString("Date", "")
                val cleanDate = rawDate.take(10)
                if (rate > 0.5 && rate < 20.0) {
                    return NbrbSyncResult(
                        usdRate = rate,
                        dateLabel = "$cleanDate (API НБ РБ)",
                        log = SyncSourceLog(
                            sourceName = "НБ РБ Официальный курс USD/BYN",
                            url = url,
                            category = "Курс НБ РБ",
                            ok = true,
                            httpCode = 200,
                            latencyMs = latency,
                            summary = "1 USD = ${"%.4f".format(rate)} BYN ($cleanDate)",
                            timestampEpochMs = now
                        )
                    )
                }
            } catch (_: Throwable) {
            }
        }
        return NbrbSyncResult(
            usdRate = null,
            dateLabel = null,
            log = SyncSourceLog(
                sourceName = "НБ РБ Официальный курс USD/BYN",
                url = url,
                category = "Курс НБ РБ",
                ok = false,
                httpCode = code,
                latencyMs = latency,
                summary = "Не удалось получить курс (${code ?: "ошибка сети"})",
                timestampEpochMs = now
            )
        )
    }

    private fun syncMyfinRates(now: Long): Pair<Double?, List<SyncSourceLog>> {
        val logs = mutableListOf<SyncSourceLog>()
        var detectedRvsr: Double? = null
        val urls = listOf(
            "Myfin.by: Автокредиты РБ" to "https://myfin.by/kredity/avto",
            "Myfin.by: На электромобили" to "https://myfin.by/kredity/avto/elektromobil",
            "Myfin.by: На бел. товары" to "https://myfin.by/kredity/na-belorusskie-tovary-i-uslugi"
        )
        for ((title, url) in urls) {
            val (code, latency, body) = fetchText(url)
            val ok = code != null && code in 200..399
            var note = if (ok) "Страница программ обновлена (${latency}мс)" else "HTTP ${code ?: "сеть"} (${latency}мс)"
            if (ok && body != null) {
                val rvsrMatch = Regex("""(1[5-9][.,]\d{2})\s*%""").find(body)
                val parsed = rvsrMatch?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull()
                if (parsed != null && parsed in 14.0..20.0) {
                    if (detectedRvsr == null) detectedRvsr = parsed
                    note = "Найдена базовая ставка ~${"%.2f".format(parsed)}% (${latency}мс)"
                }
            }
            logs.add(SyncSourceLog(title, url, "Myfin.by", ok, code, latency, note, now))
        }
        return detectedRvsr to logs
    }

    private fun syncKeyDealerCatalogs(
        cars: List<CarOffer>,
        now: Long
    ): Pair<Map<String, Pair<Double, String>>, List<SyncSourceLog>> {
        val updates = mutableMapOf<String, Pair<Double, String>>()
        val extraLogs = mutableListOf<SyncSourceLog>()

        // 1. Check NTS Xiaoma promo price
        val xiaomaCar = cars.find { it.id == "faw_xiaoma" }
        if (xiaomaCar != null) {
            val (code, latency, body) = fetchText("https://faw-bestune.by/in-stock/gomel/")
            if (code == 200 && body != null) {
                val plain = body.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ")
                val pMatch = Regex("""(?:Xiaoma|сяома)[^0-9]{0,120}(\d{2}\s?\d{3})\s*(?:BYN|руб)""", RegexOption.IGNORE_CASE)
                    .find(plain)
                val foundPrice = pMatch?.groupValues?.get(1)?.replace(" ", "")?.toDoubleOrNull()
                if (foundPrice != null && foundPrice in 15_000.0..45_000.0) {
                    updates["faw_xiaoma"] = foundPrice to "Сверено с faw-bestune.by"
                    extraLogs.add(
                        SyncSourceLog("Парсер цен: FAW Xiaoma Гомель", "https://faw-bestune.by/in-stock/gomel/", "Цены", true, 200, latency, "Цена на сайте: ${foundPrice.toInt()} BYN", now)
                    )
                }
            }
        }

        // 2. Check Red Dragon ARCFOX T1 price
        val (_, latRd, bodyRd) = fetchText("https://red-dragon-service.by/")
        if (bodyRd != null) {
            val plain = bodyRd.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ")
            val pMatch = Regex("""(?:ARCFOX\s*T1|T1\s*PRO)[^0-9]{0,120}(\d{2}\s?\d{3})""", RegexOption.IGNORE_CASE)
                .find(plain)
            val foundPrice = pMatch?.groupValues?.get(1)?.replace(" ", "")?.toDoubleOrNull()
            if (foundPrice != null && foundPrice in 35_000.0..85_000.0) {
                updates["arcfox_t1_pro"] = foundPrice to "Сверено с red-dragon-service.by"
                extraLogs.add(
                    SyncSourceLog("Парсер цен: ARCFOX T1", "https://red-dragon-service.by/", "Цены", true, 200, latRd, "Стартовая цена: ${foundPrice.toInt()} BYN", now)
                )
            }
        }

        return updates to extraLogs
    }

    private fun extractDealerSnippetHint(dealerId: String, html: String): String {
        val plain = html.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ")
        return when (dealerId) {
            "butikavto" -> {
                val countMatch = Regex("""(\d{2,3})\s*(?:авто|предложен|объяв)""", RegexOption.IGNORE_CASE).find(plain)
                if (countMatch != null) "Каталог онлайн: ${countMatch.value}" else "Каталог Гомель EV онлайн (200 OK)"
            }
            "geely_gomel", "autonova" -> {
                if (plain.contains("EX2", ignoreCase = true)) "Модель EX2 активна на сайте" else "Сайт дилера доступен"
            }
            "nts_ev" -> {
                if (plain.contains("Xiaoma", ignoreCase = true)) "Xiaoma присутствует в выдаче Гомель" else "Склад Гомель проверен"
            }
            "autopromservis" -> {
                if (plain.contains("BOX", ignoreCase = true)) "Dongfeng BOX активен в каталоге" else "Каталог Dongfeng доступен"
            }
            else -> "Сайт проверен и отвечает"
        }
    }

    private fun extractFirstRateHint(html: String): String {
        val plain = html.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ")
        val rates = Regex("""(\d{1,2}[.,]\d{1,2})\s*%""").findAll(plain).take(2).map { it.value }.toList()
        return if (rates.isNotEmpty()) "Упомянуты ставки: ${rates.joinToString(", ")}" else "Страница банка доступна"
    }
}
