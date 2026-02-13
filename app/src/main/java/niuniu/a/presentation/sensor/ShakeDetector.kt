package niuniu.a.presentation.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlin.math.sqrt
import kotlin.math.roundToLong

class ShakeDetector(
    context: Context,
    private val onShake: () -> Unit,
    private val onSensorUpdate: (x: Float, y: Float, z: Float) -> Unit = { _, _, _ -> },
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    var threshold: Float = 1.4f
    var minIntervalSeconds: Float = 0.45f

    private var lastShakeTimeMillis = 0L
    private var started = false

    fun start() {
        if (started) return
        val currentSensor = sensor ?: return
        sensorManager.registerListener(this, currentSensor, SensorManager.SENSOR_DELAY_GAME)
        started = true
    }

    fun stop() {
        if (!started) return
        sensorManager.unregisterListener(this)
        lastShakeTimeMillis = 0L
        started = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.values.size < 3) return
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        onSensorUpdate(x, y, z)

        val accelerationMagnitude = sqrt((x * x + y * y + z * z).toDouble()).toFloat()
        val isThresholdExceeded = accelerationMagnitude > threshold
        if (!isThresholdExceeded) return

        val now = SystemClock.elapsedRealtime()
        val intervalMillis = (minIntervalSeconds * 1000f).roundToLong().coerceAtLeast(1L)
        if (now - lastShakeTimeMillis < intervalMillis) return

        lastShakeTimeMillis = now
        onShake()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
