package com.loaderapp.ui.common

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateLabelFormatter {
    fun dateLabel(orderDate: LocalDate, now: LocalDate, locale: Locale): String = when (orderDate) {
        now -> "Сегодня"
        now.plusDays(1) -> "Завтра"
        else -> orderDate.format(DateTimeFormatter.ofPattern("dd MMM", locale))
    }

    fun dateLabel(
        timestampMillis: Long,
        now: LocalDate = LocalDate.now(ZoneId.systemDefault()),
        zoneId: ZoneId = ZoneId.systemDefault(),
        locale: Locale = Locale("ru")
    ): String {
        val orderDate = Instant.ofEpochMilli(timestampMillis).atZone(zoneId).toLocalDate()
        return dateLabel(orderDate = orderDate, now = now, locale = locale)
    }
}
