package com.gomelev.app.ui.components

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

object Formatters {
    private val symbols = DecimalFormatSymbols(Locale("ru", "BY")).apply {
        groupingSeparator = ' '
        decimalSeparator = ','
    }
    private val intFormat = DecimalFormat("#,##0", symbols)
    private val twoDecFormat = DecimalFormat("#,##0.00", symbols)
    private val rateFormat = DecimalFormat("0.0000", symbols)

    fun byn(value: Double): String = "${intFormat.format(value.roundToLong())} BYN"

    fun bynShort(value: Double): String = intFormat.format(value.roundToLong())

    fun usdFromByn(byn: Double, usdByn: Double): String {
        val usd = if (usdByn > 0.1) byn / usdByn else byn / 3.0026
        return "~$${intFormat.format(usd.roundToLong())}"
    }

    fun pct(value: Double): String = "${twoDecFormat.format(value).removeSuffix(",00")}%"

    fun rateFx(value: Double): String = rateFormat.format(value)

    fun timestamp(epochMs: Long?): String {
        if (epochMs == null) return "база 13.11.2026 (ожидает сеть)"
        val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return fmt.format(Date(epochMs))
    }
}
