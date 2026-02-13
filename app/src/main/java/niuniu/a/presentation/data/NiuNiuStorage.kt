package niuniu.a.presentation.data

import android.content.Context
import niuniu.a.presentation.model.PlayRecord
import org.json.JSONArray
import org.json.JSONObject

object NiuNiuStorage {
    private const val PREFS_NAME = "niuniu_watch_storage"

    private const val KEY_RECORDS = "records"
    private const val KEY_SOUND_ENABLED = "isSoundOpen"
    private const val KEY_ACCELERATION_THRESHOLD = "accelerationThreshold"
    private const val KEY_TIME_THRESHOLD = "timeThreshold"

    private const val DEFAULT_SOUND_ENABLED = true
    private const val DEFAULT_ACCELERATION_THRESHOLD = 1.4f
    private const val DEFAULT_TIME_THRESHOLD = 0.45f

    fun loadRecords(context: Context): List<PlayRecord> {
        val raw = prefs(context).getString(KEY_RECORDS, "[]") ?: "[]"
        return runCatching {
            val jsonArray = JSONArray(raw)
            buildList {
                for (index in 0 until jsonArray.length()) {
                    val item = jsonArray.optJSONObject(index) ?: continue
                    add(
                        PlayRecord(
                            timestampMillis = item.optLong("timestampMillis", 0L),
                            spendSeconds = item.optInt("spendSeconds", 0),
                            count = item.optInt("count", 0),
                            frequency = item.optDouble("frequency", 0.0),
                            heartRate = item.optDouble("heartRate", 0.0),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun appendRecord(context: Context, record: PlayRecord) {
        val records = loadRecords(context).toMutableList()
        records.add(record)
        saveRecords(context, records)
    }

    fun clearRecords(context: Context) {
        prefs(context).edit().remove(KEY_RECORDS).apply()
    }

    fun isSoundEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_SOUND_ENABLED, DEFAULT_SOUND_ENABLED)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun accelerationThreshold(context: Context): Float {
        return prefs(context).getFloat(KEY_ACCELERATION_THRESHOLD, DEFAULT_ACCELERATION_THRESHOLD)
    }

    fun setAccelerationThreshold(context: Context, threshold: Float) {
        prefs(context).edit().putFloat(KEY_ACCELERATION_THRESHOLD, threshold).apply()
    }

    fun timeThreshold(context: Context): Float {
        return prefs(context).getFloat(KEY_TIME_THRESHOLD, DEFAULT_TIME_THRESHOLD)
    }

    fun setTimeThreshold(context: Context, threshold: Float) {
        prefs(context).edit().putFloat(KEY_TIME_THRESHOLD, threshold).apply()
    }

    fun resetSensitivity(context: Context) {
        prefs(context).edit()
            .putFloat(KEY_ACCELERATION_THRESHOLD, DEFAULT_ACCELERATION_THRESHOLD)
            .putFloat(KEY_TIME_THRESHOLD, DEFAULT_TIME_THRESHOLD)
            .apply()
    }

    private fun saveRecords(context: Context, records: List<PlayRecord>) {
        val jsonArray = JSONArray()
        records.forEach { record ->
            jsonArray.put(
                JSONObject()
                    .put("timestampMillis", record.timestampMillis)
                    .put("spendSeconds", record.spendSeconds)
                    .put("count", record.count)
                    .put("frequency", record.frequency)
                    .put("heartRate", record.heartRate),
            )
        }
        prefs(context).edit().putString(KEY_RECORDS, jsonArray.toString()).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
