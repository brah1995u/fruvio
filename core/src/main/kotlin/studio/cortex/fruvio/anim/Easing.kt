package studio.cortex.fruvio.anim

/** Standard ease-out-back: overshoots past 1 near the end then settles — a landing bounce. */
fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f; val c3 = c1 + 1f
    val t = x - 1f
    return 1f + c3 * t * t * t + c1 * t * t
}

fun easeOutCubic(x: Float): Float {
    val t = 1f - x
    return 1f - t * t * t
}

/** Stronger deceleration than cubic. */
fun easeOutQuint(x: Float): Float {
    val t = 1f - x
    return 1f - t * t * t * t * t
}
