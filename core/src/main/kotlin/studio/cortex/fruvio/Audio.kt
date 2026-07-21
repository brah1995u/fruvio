package studio.cortex.fruvio

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.Disposable

/** Only CLICK exists so far — extend as new screens need their own cues (matches Flame
 *  Jester's Sfx set shape; add entries here, not a parallel enum, when that need arrives). */
enum class Sfx(val file: String) {
    CLICK("audio/click.ogg"),
    DROP("audio/drop.ogg"),
    MERGE("audio/merge.ogg"),
    WIN("audio/win.ogg"),
    LOSE("audio/lose.ogg"),
}

/**
 * SFX + looping background music, gated by [FruvioGame.soundOn]/`musicOn` and scaled by
 * `musicVolume`. No sound files exist anywhere in Fruvio yet (not part of the Figma export) —
 * both [play] and [updateMusic] check [com.badlogic.gdx.files.FileHandle.exists] before ever
 * touching a [Sound]/[Music] object.
 */
class AudioManager(private val game: FruvioGame) : Disposable {
    private val sounds = HashMap<Sfx, Sound>()
    private var music: Music? = null
    private var musicTried = false

    fun play(sfx: Sfx, volume: Float = 1f) {
        if (!game.soundOn || game.sfxVolume <= 0f) return
        val sound = sounds[sfx] ?: run {
            val handle = Gdx.files.internal(sfx.file)
            if (!handle.exists()) return
            Gdx.audio.newSound(handle).also { sounds[sfx] = it }
        }
        sound.play((volume * game.sfxVolume).coerceIn(0f, 1f))
    }

    /** (Re)apply music on/volume state; call after any music setting change. */
    fun updateMusic() {
        if (game.musicOn) {
            if (music == null && !musicTried) {
                musicTried = true
                val handle = Gdx.files.internal(MUSIC_FILE)
                if (handle.exists()) music = Gdx.audio.newMusic(handle).apply { isLooping = true }
            }
            music?.let {
                it.volume = game.musicVolume
                if (!it.isPlaying) it.play()
            }
        } else {
            music?.pause()
        }
    }

    override fun dispose() {
        sounds.values.forEach { it.dispose() }
        sounds.clear()
        music?.dispose()
        music = null
    }

    private companion object {
        const val MUSIC_FILE = "audio/music.ogg"
    }
}
