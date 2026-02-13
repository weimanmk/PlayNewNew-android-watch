package niuniu.a.presentation.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class PlayRecord(
    val timestampMillis: Long,
    val spendSeconds: Int,
    val count: Int,
    val frequency: Double,
    val heartRate: Double,
)

data class IntStats(
    val max: Int,
    val min: Int,
    val average: Int,
)

data class DoubleStats(
    val max: Double,
    val min: Double,
    val average: Double,
)

data class DailyAverage(
    val day: LocalDate,
    val times: Int,
    val spendAverage: Double,
    val countAverage: Double,
    val frequencyAverage: Double,
    val heartRateAverage: Double,
)

fun List<PlayRecord>.recordsForLastDays(days: Long, nowMillis: Long = System.currentTimeMillis()): List<PlayRecord> {
    val from = nowMillis - days * 24L * 60L * 60L * 1000L
    return filter { it.timestampMillis in from..nowMillis }
}

fun List<PlayRecord>.recordsForToday(nowMillis: Long = System.currentTimeMillis()): List<PlayRecord> {
    val zoneId = ZoneId.systemDefault()
    val today = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalDate()
    return filter {
        Instant.ofEpochMilli(it.timestampMillis).atZone(zoneId).toLocalDate() == today
    }
}

fun List<PlayRecord>.intStats(selector: (PlayRecord) -> Int): IntStats {
    if (isEmpty()) return IntStats(max = 0, min = 0, average = 0)
    val values = map(selector)
    return IntStats(
        max = values.maxOrNull() ?: 0,
        min = values.minOrNull() ?: 0,
        average = values.sum() / values.size,
    )
}

fun List<PlayRecord>.doubleStats(selector: (PlayRecord) -> Double): DoubleStats {
    if (isEmpty()) return DoubleStats(max = 0.0, min = 0.0, average = 0.0)
    val values = map(selector)
    return DoubleStats(
        max = values.maxOrNull() ?: 0.0,
        min = values.minOrNull() ?: 0.0,
        average = values.sum() / values.size,
    )
}

fun List<PlayRecord>.dailyAverages(): List<DailyAverage> {
    if (isEmpty()) return emptyList()
    val zoneId = ZoneId.systemDefault()
    return groupBy {
        Instant.ofEpochMilli(it.timestampMillis).atZone(zoneId).toLocalDate()
    }
        .toList()
        .sortedBy { it.first }
        .map { (day, records) ->
            DailyAverage(
                day = day,
                times = records.size,
                spendAverage = records.map { it.spendSeconds }.average(),
                countAverage = records.map { it.count }.average(),
                frequencyAverage = records.map { it.frequency }.average(),
                heartRateAverage = records.map { it.heartRate }.average(),
            )
        }
}
