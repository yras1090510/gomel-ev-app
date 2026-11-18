package com.gomelev.app.network

import android.util.Log
import com.gomelev.app.model.DealerEntity
import com.gomelev.app.model.SyncSourceLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import com.gomelev.app.model.BankListing
import okhttp3.Request
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.IOException
import java.util.concurrent.TimeUnit

data class NbrbSyncResult(
    val usdRate: Double?,
    val dateLabel: String?,
    val log: SyncSourceLog
)

internal data class LiveSyncOutcome(
    val usdRate: Double?,
    val nbrbDateLabel: String?,
    val refinancingRatePct: Double?,
    val belarusbankTerms: Map<String, BelarusbankTerms>,
    val inventories: Map<String, JSONArray>,
    val belvebOnline: BelvebOnlineTerms?,
    val bankListings: List<BankListing>?,
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
        .callTimeout(18, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val userAgent =
        "Mozilla/5.0 (Linux; Android 15; SM-F966B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

    private fun fetchText(url: String, query: JSONObject? = null): Triple<Int?, Long, String?> {
        val start = System.currentTimeMillis()
        return try {
            val req = Request.Builder()
                .url(url)
                .apply { if (query != null) post(query.toString().toRequestBody("application/json".toMediaType())) }
                .header("User-Agent", userAgent)
                .header("Accept-Language", "ru-BY,ru;q=0.9,en;q=0.7")
                .build()
            client.newCall(req).execute().use { resp ->
                val latency = System.currentTimeMillis() - start
                val body = if (resp.isSuccessful) resp.body?.string() else null
                Triple(resp.code, latency, body)
            }
        } catch (t: IOException) {
            val latency = System.currentTimeMillis() - start
            Log.w("LiveWebSync", "Failed $url: ${t.message}")
            Triple(null, latency, t.javaClass.simpleName + ": " + (t.message ?: "error"))
        }
    }

    internal suspend fun runFullLiveSync(
        dealers: List<DealerEntity>,
    ): LiveSyncOutcome = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        coroutineScope {
            val nbrbDeferred = async { syncNbrbRate(now) }
            val refinancingDeferred = async { syncRefinancingRate(now) }
            val apsDeferred = async { syncAps(now) }
            val belvebDeferred = async {
                val url = "https://www.belveb.by/credits/avtokredit-online/"
                val (code, latency, html) = fetchText(url)
                val terms = if (code == 200 && html != null) BelvebOnlineTerms.parse(html) else null
                terms to SyncSourceLog("БелВЭБ · Автокредит онлайн", url, "Банки", terms != null, code, latency,
                    if (terms == null) "Условия не прочитаны" else "Сверены ставки, срок и сумма", now)
            }
            val myfinDeferred = async { syncMyfinRates(now) }
            val keyCarSitesDeferred = async { syncKeyDealerCatalogs(now) }

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
                    val stock = if (dealer.id == "nts_ev" && code == 200 && body != null)
                        runCatching { DealerInventory.faw(body) }.getOrNull() else null
                    val resultLog = if (dealer.id == "nts_ev") log.copy(ok = stock != null,
                        summary = stock?.let { "Электромобилей на витрине: ${it.length()}" } ?: "Склад не обновлён") else log
                    Triple(dealer.id to patch, resultLog, stock)
                }
            }

            // Key bank sites check
            val bankUrls = listOf(
                Triple("Приорбанк · автокредит", "Банки", "https://www.priorbank.by/offers/credits/credit-belorusskie-avto-partnery"),
                Triple("Белгазпромбанк (Авто ДРАЙВ)", "Банки", "https://belgazprombank.by/personal_banking/krediti/kredit_na_priobretenie_avtotransporta/Autodrive-credite-na-priobretenie-auto/"),
                Triple("Дабрабыт (АВТОНАДО)", "Банки", "https://bankdabrabyt.by/personal/credit/avtonado/"),
                Triple("Беларусбанк · электромобили", "Банки", "https://belarusbank.by/fizicheskim_licam/kredit/consumer/kredit-na-priobretenie-avtomobilya-elektromobilya-/"),
                Triple("Беларусбанк (EX2/EX5)", "Банки", "https://belarusbank.by/fizicheskim_licam/kredit/consumer/kredit-na-priobretenie-u-dilerov-szao-beldzhi-elektromobiley-marki-geely-modeli-ex5/")
            )
            val bankDeferreds = bankUrls.map { (name, cat, url) ->
                async {
                    val (code, latency, body) = fetchText(url)
                    val ok = code != null && code in 200..399
                    val terms = if (body == null || !ok) null else when (name) {
                        "Беларусбанк (EX2/EX5)" ->
                            BelarusbankTerms.parse(body)?.let { "belarus_ex2" to it }
                        "Беларусбанк · электромобили" ->
                            BelarusbankTerms.parseGeneralEv(body)?.let { "belarus_ev_general" to it }
                        else -> null
                    }
                    terms to SyncSourceLog(
                        sourceName = name,
                        url = url,
                        category = cat,
                        ok = ok,
                        httpCode = code,
                        latencyMs = latency,
                        summary = when {
                            terms != null -> "Условия программы прочитаны"
                            ok -> "Страница доступна · условия не сверены"
                            else -> "Статус: ${code ?: "ошибка сети"}"
                        },
                        timestampEpochMs = now
                    )
                }
            }

            val nbrbRes = nbrbDeferred.await()
            val (listings, myfinLog) = myfinDeferred.await()
            val (aps, apsLog) = apsDeferred.await()
            val (belveb, belvebLog) = belvebDeferred.await()
            val (refinancingRate, refinancingLog) = refinancingDeferred.await()
            val (carUpdates, extraCarLogs) = keyCarSitesDeferred.await()
            val dealerResults = dealerDeferreds.awaitAll()
            val bankResults = bankDeferreds.awaitAll()
            val bankTerms = bankResults.mapNotNull { it.first }.toMap()
            val bankLogs = bankResults.map { it.second }

            val patchMap = dealerResults.associate { it.first }
            val dealerLogs = dealerResults.map { it.second }

            val allLogs = listOf(nbrbRes.log, refinancingLog, myfinLog, apsLog, belvebLog) + bankLogs + extraCarLogs + dealerLogs
            LiveSyncOutcome(
                usdRate = nbrbRes.usdRate,
                nbrbDateLabel = nbrbRes.dateLabel,
                refinancingRatePct = refinancingRate,
                belarusbankTerms = bankTerms,
                inventories = buildMap {
                    dealerResults.forEach { result -> result.third?.let { put(result.first.first, it) } }
                    aps?.let { put("autopromservis", it) }
                },
                bankListings = listings,
                belvebOnline = belveb,
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

    private fun syncRefinancingRate(now: Long): Pair<Double?, SyncSourceLog> {
        val url = "https://api.nbrb.by/refinancingrate"
        val (code, latency, body) = fetchText(url)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
            timeZone = java.util.TimeZone.getTimeZone("Europe/Minsk")
        }.format(Date(now))
        val latest = runCatching {
            val rows = JSONArray(body ?: "[]")
            (0 until rows.length()).map { rows.getJSONObject(it) }
                .filter { it.getString("Date").take(10) <= today }
                .maxByOrNull { it.getString("Date") }
        }.getOrNull()
        val rate = latest?.optDouble("Value")?.takeIf { code == 200 && it in 0.1..100.0 }
        val summary = if (rate != null) "Ставка $rate% с ${latest?.optString("Date")?.take(10)}"
            else "Ставка не получена"
        return rate to SyncSourceLog("НБ РБ · ставка рефинансирования", url, "Ставки", rate != null, code, latency, summary, now)
    }

    private fun syncMyfinRates(now: Long): Pair<List<BankListing>?, SyncSourceLog> {
        val start = System.currentTimeMillis()
        var code: Int? = null
        val listings = runCatching {
            val collected = linkedMapOf<String, BankListing>()
            var url = BankListings.URL
            val visited = mutableSetOf<String>()
            while (visited.size < 20 && visited.add(url)) {
                val result = fetchText(url)
                code = result.first
                require(code == 200)
                val page = requireNotNull(result.third)
                val parsed = requireNotNull(BankListings.parse(page))
                parsed.forEach { collected[it.url] = it }
                url = BankListings.nextPage(page) ?: return@runCatching collected.values.toList()
            }
            error("Неполная выдача программ")
        }.getOrNull()
        return listings to SyncSourceLog("Myfin · предложения автокредитов", BankListings.URL, "Банки",
            listings != null, code, System.currentTimeMillis() - start,
            listings?.let { "Прочитано предложений: ${it.size}" } ?: "Каталог не прочитан", now)
    }

    private fun syncAps(now: Long): Pair<JSONArray?, SyncSourceLog> {
        val start = System.currentTimeMillis()
        var code: Int? = null
        // The dealer uses a zero-based page number named offset and a fixed 35-row page.
        val rows = runCatching {
            val byId = linkedMapOf<Long, JSONObject>()
            val pageSignatures = mutableSetOf<List<Long>>()
            for (page in 0 until 60) {
                val query = JSONObject().put("offset", JSONArray().put(page)).put("sort", JSONArray().put(1))
                    .put("brands_id", JSONArray().put(2).put(31)).put("car_conditions_id", JSONArray().put(1))
                    .put("car_type_id", JSONArray().put(1))
                val response = fetchText(DealerInventory.APS_API, query)
                code = response.first
                require(code == 200)
                val batch = JSONArray(requireNotNull(response.third))
                val ids = (0 until batch.length()).map { batch.getJSONObject(it).getLong("id") }
                require(pageSignatures.add(ids))
                for ((i, id) in ids.withIndex()) byId[id] = batch.getJSONObject(i)
                if (batch.length() < 35) return@runCatching DealerInventory.apsPage(JSONArray(byId.values))
            }
            error("Неполная выдача")
        }.getOrNull()
        return rows to SyncSourceLog("Склад Dongfeng", "https://dongfeng.by/stock/", "Автомобили",
            rows != null, code, System.currentTimeMillis() - start,
            rows?.let { "Свободных предложений: ${it.length()}" } ?: "Полный склад не получен", now)
    }

    private fun syncKeyDealerCatalogs(
        now: Long
    ): Pair<Map<String, Pair<Double, String>>, List<SyncSourceLog>> {
        val updates = mutableMapOf<String, Pair<Double, String>>()
        val extraLogs = mutableListOf<SyncSourceLog>()

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
}
