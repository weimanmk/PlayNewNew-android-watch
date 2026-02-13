package niuniu.a.presentation.feedback

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object DeviceFeedback {
    fun playStartTone(enabled: Boolean) {
        if (!enabled) return
        playTone(ToneGenerator.TONE_PROP_ACK, 120)
    }

    fun playEndTone(enabled: Boolean) {
        if (!enabled) return
        playTone(ToneGenerator.TONE_PROP_NACK, 180)
    }

    fun vibratePulse(context: Context, durationMillis: Long = 40L) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE),
            )
            return
        }

        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(durationMillis)
        }
    }

    private fun playTone(tone: Int, durationMillis: Int) {
        val toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 80)
        toneGenerator.startTone(tone, durationMillis)
        Handler(Looper.getMainLooper()).postDelayed(
            { toneGenerator.release() },
            durationMillis.toLong() + 150L,
        )
    }
}
