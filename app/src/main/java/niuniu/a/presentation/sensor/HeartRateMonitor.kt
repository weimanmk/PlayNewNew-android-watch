package niuniu.a.presentation.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.roundToInt

class HeartRateMonitor(
    context: Context,
    private val onHeartRateChanged: (Int) -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val heartRateSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE, true)
            ?: sensorManager.getSensorList(Sensor.TYPE_HEART_RATE).firstOrNull()

    private var started = false
    private var lastHeartRate: Int? = null

    fun isSupported(): Boolean = heartRateSensor != null

    fun start(): Boolean {
        if (started) return true
        val sensor = heartRateSensor ?: return false
        started = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        return started
    }

    fun stop() {
        if (!started) return
        sensorManager.unregisterListener(this)
        started = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_HEART_RATE || event.values.isEmpty()) return
        val heartRate = extractHeartRate(event.values) ?: return
        if (heartRate <= 0) return
        lastHeartRate = heartRate
        onHeartRateChanged(heartRate)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun extractHeartRate(values: FloatArray): Int? {
        if (values.size >= 3) {
            val third = values[2]
            if (third in 25f..240f) return third.roundToInt()
        }
        val candidates = values.filter { it in 25f..240f }
        if (candidates.isEmpty()) return null

        val preferred = candidates.filter { it in 40f..200f }
        val pool = if (preferred.isNotEmpty()) preferred else candidates
        val target = (lastHeartRate ?: 90).toFloat()
        return pool.minByOrNull { kotlin.math.abs(it - target) }?.roundToInt()
    }
}
