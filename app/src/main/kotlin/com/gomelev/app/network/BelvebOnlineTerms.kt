package com.gomelev.app.network

import org.jsoup.Jsoup

internal data class BelvebOnlineTerms(val first: Double, val next: Double, val months: Int, val cap: Double) {
    companion object {
        fun parse(html: String): BelvebOnlineTerms? {
            val text = Jsoup.parse(html).text().replace('\u00a0', ' ')
            if (!text.contains("равномерными долями") || !text.contains("В приложении UP Online")) return null
            val m = Regex("""Срок кредита От 1 года до (\d+) лет включительно Процентная ставка Первый 1 месяц\s*[–—-]\s*([\d,.]+)\s*%, начиная со второго месяца\s*[–—-]\s*([\d,.]+)\s*%[^;]{0,80}?Максимальная сумма кредита ([\d ]+)\s*ƃ""").find(text) ?: return null
            val years = m.groupValues[1].toIntOrNull() ?: return null
            fun rate(i: Int) = m.groupValues[i].replace(',', '.').toDoubleOrNull()
            val first = rate(2) ?: return null
            val next = rate(3) ?: return null
            val cap = m.groupValues[4].replace(" ", "").toDoubleOrNull() ?: return null
            return if (years in 1..30 && first in 0.0..100.0 && next in 0.0..100.0 && cap > 0)
                BelvebOnlineTerms(first, next, years * 12, cap) else null
        }
    }
}
