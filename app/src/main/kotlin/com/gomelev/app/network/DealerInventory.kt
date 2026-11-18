package com.gomelev.app.network

import com.gomelev.app.model.CarOffer
import com.gomelev.app.model.StockStatus
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import java.time.Instant
import java.time.ZoneId

/** A complete response is the replacement unit. A missing or malformed page keeps the previous snapshot. */
internal object DealerInventory {
    const val FAW_URL = "https://faw-bestune.by/in-stock/gomel/"
    const val APS_API = "https://apistock.aps.by/cars"
    private const val APS_URL = "https://dongfeng.by/stock/"

    fun faw(html: String): JSONArray {
        val doc = Jsoup.parse(html, FAW_URL)
        require(doc.title().contains("Гомель") && doc.title().contains("FAW"))
        require(doc.select("a.next.page-numbers, link[rel=next]").isEmpty())
        val cards = doc.select("a.car-stock-item")
        require(cards.isNotEmpty() || doc.text().contains("Автомобилей в наличии нет", ignoreCase = true))
        val rows = JSONArray()
        for (card in cards) {
            val assets = card.select(".car-assets-item").map { it.text() }
            require(assets.isNotEmpty())
            if ("электро" !in assets || "Гомель" !in assets) continue
            val url = card.absUrl("href")
            require(url.startsWith("https://faw-bestune.by/in-stock/"))
            val name = card.selectFirst(".cat-list-title strong")!!.text()
            val amount = card.selectFirst(".cat-list-title em.black .dgts-space, .cat-list-title em.black")
                ?.text()?.filter { it.isDigit() }?.toDoubleOrNull()
            require(name.isNotBlank() && amount != null && amount > 0)
            val photo = card.selectFirst("img[src]")?.absUrl("src")
            rows.put(JSONObject().put("id", "nts_" + url.trimEnd('/').substringAfterLast('/'))
                .put("name", name).put("price", amount).put("url", url).put("location", "Гомель")
                .put("stock", StockStatus.IN_GOMEL.name).put("photo", photo)
                .put("year", assets.firstOrNull { it.matches(Regex("""20\d\d""")) }?.toInt()))
        }
        return rows
    }

    fun apsPage(raw: JSONArray): JSONArray {
        val rows = JSONArray()
        for (i in 0 until raw.length()) {
            val item = raw.getJSONObject(i)
            if (item.getJSONObject("car_type").getString("name") != "электро" ||
                item.getJSONObject("car_conditions").getInt("id") != 1 || item.getBoolean("reserved")) continue
            val model = item.getJSONObject("models")
            val city = item.getJSONObject("cities").getString("name")
            val local = item.getJSONObject("car_status").getInt("id") in setOf(2, 5)
            val stock = when {
                !local -> StockStatus.ORDER
                city == "Гомель" -> StockStatus.IN_GOMEL
                else -> StockStatus.RB_WAREHOUSE
            }
            val price = item.getDouble("price")
            require(price.isFinite() && price > 0)
            val photo = item.optString("images").takeIf { it.startsWith("/images/") && !it.contains("no_photo") }
            rows.put(JSONObject().put("id", "aps_" + item.getLong("id"))
                .put("name", item.getJSONObject("brands").getString("name") + " " + model.getString("name"))
                .put("trim", item.optJSONObject("car_packages")?.optString("name"))
                .put("body", item.optJSONObject("car_body")?.optString("name"))
                .put("range", item.optString("power_reserve").replace(" / ", " ").trim())
                .put("location", item.optJSONObject("addresses")?.optString("name") ?: city)
                .put("stock", stock.name).put("price", price).put("year", item.getInt("years"))
                .put("url", APS_URL + "?models=" + model.getInt("id"))
                .put("photo", photo?.let { "https://stock.aps.by$it" }))
        }
        return rows
    }

    fun merge(base: List<CarOffer>, snapshots: JSONObject): List<CarOffer> {
        val result = base.toMutableList()
        for (dealer in listOf("nts_ev", "autopromservis")) {
            val snapshot = snapshots.optJSONObject(dealer) ?: continue
            val rows = snapshot.getJSONArray("rows")
            val date = Instant.ofEpochMilli(snapshot.getLong("checked")).atZone(ZoneId.of("Europe/Minsk")).toLocalDate()
            val seedId = if (dealer == "nts_ev") "faw_xiaoma" else "dongfeng_box"
            result.removeAll { it.id == seedId }
            for (i in 0 until rows.length()) {
                val row = rows.getJSONObject(i)
                val url = row.getString("url")
                val seed = base.firstOrNull { it.id == seedId && it.imageSource == url && dealer == "nts_ev" }
                val year = row.optInt("year").takeIf { it > 0 }
                result += CarOffer(id = seed?.id ?: row.getString("id"), brandModel = row.getString("name"),
                    trimAndYear = listOfNotNull(row.optString("trim").ifBlank { null }, year?.toString()).joinToString(" · "),
                    bodyType = row.optString("body", seed?.bodyType ?: "Электромобиль"),
                    batteryOrEngine = seed?.batteryOrEngine ?: "Электропривод",
                    rangeOrSpec = row.optString("range", seed?.rangeOrSpec ?: "Запас хода уточните у дилера"),
                    priceByn = row.getDouble("price"), priceNote = "Цена витрины дилера", isEv = true, isUsed = false,
                    isGeelyEx = false, isBelPartner = false, isDongfengIceOrForthing = false, isDealerLegalEntity = true,
                    stockStatus = StockStatus.valueOf(row.getString("stock")), cityNote = row.getString("location"),
                    dealerId = dealer, imageRes = seed?.imageRes, imageSource = url, imageCredit = "Фото дилера",
                    listingUrl = url, forecast2027BullPct = seed?.forecast2027BullPct ?: 0.0,
                    forecast2027BasePct = seed?.forecast2027BasePct ?: 0.0, forecast2027BearPct = seed?.forecast2027BearPct ?: 0.0,
                    forecastDriverRu = seed?.forecastDriverRu.orEmpty(), lastSyncedPriceNote = "Витрина · $date",
                    modelYear = year, remotePhoto = row.optString("photo").takeIf { it.startsWith("https://") })
            }
        }
        return result
    }
}
