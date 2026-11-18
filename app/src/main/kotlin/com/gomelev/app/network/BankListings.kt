package com.gomelev.app.network

import com.gomelev.app.model.BankListing
import org.jsoup.Jsoup

internal object BankListings {
    const val URL = "https://myfin.by/kredity/avto"
    fun nextPage(html: String): String? {
        val next = Jsoup.parse(html, URL).selectFirst(".pagination-new li.next:not(.hidden):not(.disabled) a[href]")
            ?: return null
        return next.absUrl("href").also { require(it.matches(Regex("""https://myfin\.by/kredity/avto\?page=\d+"""))) }
    }

    fun parse(html: String): List<BankListing>? {
        val doc = Jsoup.parse(html, URL)
        val cards = doc.select("#list_products .products__product")
        if (cards.isEmpty()) return null
        return cards.mapNotNull { card ->
            val link = card.selectFirst(".products__product-product-name a[href]") ?: return@mapNotNull null
            val url = link.absUrl("href").substringBefore('?')
            if (!url.startsWith("https://myfin.by/bank/") || "/kredity/avto/" !in url) return@mapNotNull null
            val terms = card.select(".products__product-data").take(4).joinToString(" · ") { it.text() }
            BankListing(link.text(), card.select(".products__product-bank-name").text(), terms, url)
        }.distinctBy { it.url }.takeIf { it.isNotEmpty() }
    }
}
