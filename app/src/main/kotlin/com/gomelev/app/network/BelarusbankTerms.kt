package com.gomelev.app.network

/** The EX2/EX5 online application column; the branch and other models have different rates. */
internal data class BelarusbankTerms(
    val graceMonths: Int,
    val graceOffset: Double,
    val nextOffset: Double,
    val maxMonths: Int,
    val downPayment: Int,
    val principalGraceMonths: Int = 0
) {
    companion object {
        private val schedule = Regex(
            """в течение первых (\d+) месяцев[^;]*?ставка рефинансирования НБ\s*([+-])\s*(\d+[,.]\d+) п\.п\.[^;]*;\s*начиная с (\d+)-го месяца[^;]*?ставка рефинансирования НБ\s*([+-])\s*(\d+[,.]\d+) п\.п\."""
        )

        fun parse(html: String): BelarusbankTerms? {
            val rows = productRows(html, "EX2, EX5") ?: return null
            val heading = rows.indexOfFirst { plain(it).startsWith("Процентная ставка") }
            if (heading < 0 || !plain(rows[heading]).endsWith("через каналы ДБО")) return null
            val cells = cells(rows.getOrNull(heading + 1) ?: return null)
            if (cells.size != 3) return null
            val m = schedule.find(cells.last()) ?: return null
            val months = m.groupValues[1].toIntOrNull() ?: return null
            if (m.groupValues[4].toIntOrNull() != months + 1) return null
            val size = rows.map(::plain).firstOrNull { it.startsWith("Размер кредита") } ?: return null
            val financed = Regex("""до (\d+)\s*% стоимости автомобиля[^;.]*электромобили моделей EX2, EX5""")
                .find(size)?.groupValues?.get(1)?.toIntOrNull() ?: return null
            val term = rows.map(::plain).firstOrNull { it.startsWith("Срок кредита до ") } ?: return null
            val years = Regex("""Срок кредита до (\d+) лет""").find(term)?.groupValues?.get(1)?.toIntOrNull()
                ?: return null
            if (years !in 1..30 || months !in 1 until years * 12 || financed !in 1..100) return null
            if (rows.none { plain(it).contains("основной долг – ежемесячно, равными долями") }) return null
            fun offset(sign: Int, number: Int) = (m.groupValues[sign] + m.groupValues[number].replace(',', '.')).toDouble()
            return BelarusbankTerms(months, offset(2, 3), offset(5, 6), years * 12, 100 - financed)
        }

        /** General EV program, the second column, with a principal-only deferral. */
        fun parseGeneralEv(html: String): BelarusbankTerms? {
            if (!html.contains("Без первоначального взноса")) return null
            val rows = productRows(html, "Счет-фактура онлайн") ?: return null
            val heading = rows.indexOfFirst { plain(it).startsWith("Процентная ставка") }
            if (heading < 0) return null
            val terms = cells(rows.getOrNull(heading + 1) ?: return null)
            if (terms.size != 2) return null
            val offsets = Regex("""ставка рефинансирования НБ\s*([+-])\s*(\d+(?:[,.]\d+)?) п\.п\.""")
                .findAll(terms[1]).map { (it.groupValues[1] + it.groupValues[2].replace(',', '.')).toDouble() }.toList()
            if (offsets.size != 2 || !terms[1].contains("180 календарных дней") ||
                !terms[1].contains("181-го календарного дня")) return null
            val term = rows.firstOrNull { plain(it).startsWith("Срок кредита до ") } ?: return null
            val years = Regex("""^до (\d+) лет$""").matchEntire(cells(term).getOrNull(1) ?: return null)
                ?.groupValues?.get(1)?.toIntOrNull() ?: return null
            val payment = rows.firstOrNull { plain(it).startsWith("Порядок погашения кредита и процентов") }
                ?: return null
            val evPayment = cells(payment).getOrNull(1) ?: return null
            if (years !in 1..30 || !evPayment.contains("основной долг – ежемесячно, равными долями") ||
                !evPayment.contains("по истечении 6 месяцев") || !evPayment.contains("проценты – ежемесячно")) return null
            return BelarusbankTerms(6, offsets[0], offsets[1], years * 12, 0, principalGraceMonths = 6)
        }

        private fun productRows(html: String, productMarker: String): List<String>? {
            val body = Regex("""<tbody\b[^>]*>(.*?)</tbody>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(html).map { it.groupValues[1] }.firstOrNull {
                    it.contains(productMarker) && it.contains("Процентная ставка")
                } ?: return null
            return Regex("""<tr\b[^>]*>(.*?)</tr>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(body).map { it.groupValues[1] }.toList()
        }

        private fun plain(value: String) = value.replace(Regex("""<sup\b[^>]*>.*?</sup>""", RegexOption.DOT_MATCHES_ALL), "")
                .replace(Regex("<[^>]+>"), " ").replace("&nbsp;", " ").replace('\u00a0', ' ')
                .replace(Regex("""\s+"""), " ").trim()

        private fun cells(row: String) =
            Regex("""<td\b[^>]*>(.*?)</td>""", RegexOption.DOT_MATCHES_ALL)
                .findAll(row).map { plain(it.groupValues[1]) }.toList()
    }
}
