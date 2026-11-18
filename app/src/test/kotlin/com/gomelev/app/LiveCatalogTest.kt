package com.gomelev.app

import com.gomelev.app.data.PreloadedCatalog
import com.gomelev.app.network.BankListings
import com.gomelev.app.network.BelvebOnlineTerms
import com.gomelev.app.network.DealerInventory
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LiveCatalogTest {
    private fun faw(vararg cars: Pair<String, Int>): String = "<title>Гомель FAW</title>" + cars.joinToString("") { (id, price) ->
        """<a class="car-stock-item" href="/in-stock/$id/"><div class="cat-list-title"><strong>FAW $id</strong>
            <em class="old-price">59900</em><em class="black"><em class="dgts-space">$price</em> руб.</em></div>
            <div class="car-assets-item">Гомель</div><div class="car-assets-item">электро</div></a>"""
    }

    @Test fun completeDealerSnapshotAddsRepricesAndRemovesAcrossRestart() {
        val cache = JSONObject()
        fun refresh(page: String) {
            val rows = runCatching { DealerInventory.faw(page) }.getOrNull() ?: return
            cache.put("nts_ev", JSONObject().put("rows", rows).put("checked", 1L))
        }
        fun stock() = DealerInventory.merge(PreloadedCatalog.cars, JSONObject(cache.toString())).filter { it.dealerId == "nts_ev" }
        refresh(faw("one" to 28000, "old" to 30100))
        refresh(faw("one" to 26500, "new" to 24000))
        refresh("<h1>Maintenance</h1>")
        assertEquals(mapOf("nts_one" to 26500.0, "nts_new" to 24000.0), stock().associate { it.id to it.priceByn })
        refresh("<title>Гомель FAW</title><p>Автомобилей в наличии нет</p>")
        assertTrue(stock().isEmpty())
    }

    @Test fun apsOnlyImportsUnreservedElectricCarsAndKeepsFactorySeparateFromCityStock() {
        val raw = JSONObject("""{"id":1,"models":{"id":92,"name":"BOX"},"brands":{"name":"Dongfeng"},
            "car_type":{"name":"электро"},"car_conditions":{"id":1},"reserved":false,"cities":{"name":"Гомель"},
            "car_status":{"id":8},"price":28000,"years":2026}""")
        val rows = JSONArray().put(raw).put(JSONObject(raw.toString()).put("reserved", true))
        assertEquals(1, DealerInventory.apsPage(rows).length())
        assertEquals("ORDER", DealerInventory.apsPage(rows).getJSONObject(0).getString("stock"))
        raw.getJSONObject("car_status").put("id", 2)
        assertEquals("IN_GOMEL", DealerInventory.apsPage(rows).getJSONObject(0).getString("stock"))
    }

    @Test fun belvebRateChangeAndTermAreParsedTogether() {
        val html = """В приложении UP Online Срок кредита От 1 года до 8 лет включительно Процентная ставка
            Первый 1 месяц – 9,90 %, начиная со второго месяца – 17,5 % (СР+8,25 п.п)
            Максимальная сумма кредита 100 000 ƃ равномерными долями"""
        assertEquals(BelvebOnlineTerms(9.9, 17.5, 96, 100000.0), BelvebOnlineTerms.parse(html))
        assertNull(BelvebOnlineTerms.parse(html.replace("равномерными долями", "аннуитет")))
    }

    @Test fun discoversNewBankLineWithoutInventingRepaymentSchedule() {
        val page = """<div id="list_products"><div class="products__product"><div class="products__product-product-name">
            <a href="/bank/example/kredity/avto/123-ev">Электро</a></div><div class="products__product-bank-name">Банк</div>
            <div class="products__product-data">Ставка от 12%</div></div></div>"""
        assertEquals("Ставка от 12%", BankListings.parse(page)!!.single().terms)
        assertNull(BankListings.parse("<html/>"))
    }
}
