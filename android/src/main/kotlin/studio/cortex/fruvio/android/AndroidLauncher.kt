package studio.cortex.fruvio.android

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import studio.cortex.fruvio.FruvioGame

/** Android entry point → produces the APK. Same [FruvioGame] as desktop. */
class AndroidLauncher : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AndroidApplicationConfiguration().apply {
            useImmersiveMode = true
        }
        initialize(FruvioGame(haptics = AndroidHaptics(this)), config)
    }
}
