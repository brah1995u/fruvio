package studio.cortex.fruvio

/** Platform-specific vibration. Desktop/capture builds get [NoHaptics]; Android supplies a real one. */
fun interface Haptics {
    fun vibrate(ms: Int)
}

val NoHaptics = Haptics { }
