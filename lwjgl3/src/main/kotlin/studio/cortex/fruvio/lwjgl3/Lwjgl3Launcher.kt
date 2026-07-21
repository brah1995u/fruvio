package studio.cortex.fruvio.lwjgl3

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import studio.cortex.fruvio.Capture
import studio.cortex.fruvio.FruvioGame

/**
 * Desktop entry point. Portrait window at half the 1080x2400 reference resolution.
 * Verification aid: `--capture=<screen>:<outPath.png>` boots straight to that screen,
 * renders a few frames, saves a PNG, and exits (see docs/DESIGN.md §13). Path may contain a
 * Windows drive-letter colon, so split on the FIRST colon only; frame count is a separate
 * `--frames=N` arg (kept out of the colon-delimited pair to avoid ambiguity).
 */
fun main(args: Array<String>) {
    val captureArg = args.firstOrNull { it.startsWith("--capture=") }?.removePrefix("--capture=")
    val framesArg = args.firstOrNull { it.startsWith("--frames=") }?.removePrefix("--frames=")?.toIntOrNull()
    val capture = captureArg?.let {
        val idx = it.indexOf(':')
        Capture(it.substring(0, idx), it.substring(idx + 1), framesArg ?: 14)
    }
    val config = Lwjgl3ApplicationConfiguration().apply {
        setTitle("Fruvio")
        setWindowedMode(540, 1200)
        setForegroundFPS(60)
        useVsync(true)
    }
    Lwjgl3Application(FruvioGame(capture), config)
}
