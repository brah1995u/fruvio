package studio.cortex.fruvio.android

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import studio.cortex.fruvio.Haptics

/** Real vibration via the platform [Vibrator] service, branching on the API-26 [VibrationEffect] cutover. */
class AndroidHaptics(context: Context) : Haptics {
    private val vibrator: Vibrator? = context.getSystemService(Vibrator::class.java)

    override fun vibrate(ms: Int) {
        val v = vibrator ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(ms.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION") v.vibrate(ms.toLong())
        }
    }
}
