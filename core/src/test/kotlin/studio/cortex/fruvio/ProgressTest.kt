package studio.cortex.fruvio

import com.badlogic.gdx.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgressTest {
    @Test fun legacyBoostersMigrateExactlyOnce() {
        val prefs = MemoryPreferences().apply {
            putInteger("booster_undo_drop", 2)
            putInteger("booster_slowmo", 3)
            putInteger(Booster.REMOVE_FRUIT.prefKey, 1)
        }
        val progress = Progress(prefs)
        assertEquals(3, progress.boosterCount(Booster.REMOVE_FRUIT))
        assertEquals(3, progress.boosterCount(Booster.SHAKE_JAR))
        assertFalse(prefs.contains("booster_undo_drop"))
        assertFalse(prefs.contains("booster_slowmo"))
        Progress(prefs)
        assertEquals(3, progress.boosterCount(Booster.REMOVE_FRUIT))
    }

    @Test fun boosterCountsPersistAndConsumptionCannotGoBelowZero() {
        val progress = Progress(MemoryPreferences())
        progress.addBooster(Booster.COMBO_FREEZE, 2)
        assertTrue(progress.consumeBooster(Booster.COMBO_FREEZE))
        assertTrue(progress.consumeBooster(Booster.COMBO_FREEZE))
        assertFalse(progress.consumeBooster(Booster.COMBO_FREEZE))
        assertEquals(0, progress.boosterCount(Booster.COMBO_FREEZE))
    }

    @Test fun everyShopBoosterPersistsIndependently() {
        val prefs = MemoryPreferences()
        val progress = Progress(prefs)
        Booster.entries.forEachIndexed { index, booster -> progress.addBooster(booster, index + 1) }
        val reloaded = Progress(prefs)
        Booster.entries.forEachIndexed { index, booster ->
            assertEquals(index + 1, reloaded.boosterCount(booster), booster.name)
        }
    }
    @Test fun miniGameRoundsIncrementAndPersist() {
        val prefs = MemoryPreferences()
        val progress = Progress(prefs)
        progress.markMiniGamePlayed(MiniGame.PLINKO)
        progress.markMiniGamePlayed(MiniGame.PLINKO)
        progress.markMiniGamePlayed(MiniGame.BONUS_BOX)
        assertEquals(3, Progress(prefs).miniGameRoundsCount)
        assertEquals(2, progress.miniGamesPlayedCount())
    }
    private class MemoryPreferences : Preferences {
        private val values = LinkedHashMap<String, Any>()
        override fun putBoolean(key: String, value: Boolean) = apply { values[key] = value }
        override fun putInteger(key: String, value: Int) = apply { values[key] = value }
        override fun putLong(key: String, value: Long) = apply { values[key] = value }
        override fun putFloat(key: String, value: Float) = apply { values[key] = value }
        override fun putString(key: String, value: String) = apply { values[key] = value }
        override fun put(vals: MutableMap<String, *>?) = apply { vals?.forEach { (key, value) -> if (value != null) values[key] = value } }
        override fun getBoolean(key: String) = getBoolean(key, false)
        override fun getInteger(key: String) = getInteger(key, 0)
        override fun getLong(key: String) = getLong(key, 0L)
        override fun getFloat(key: String) = getFloat(key, 0f)
        override fun getString(key: String) = getString(key, "")
        override fun getBoolean(key: String, defValue: Boolean) = values[key] as? Boolean ?: defValue
        override fun getInteger(key: String, defValue: Int) = values[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long) = values[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float) = values[key] as? Float ?: defValue
        override fun getString(key: String, defValue: String) = values[key] as? String ?: defValue
        override fun get(): MutableMap<String, *> = LinkedHashMap(values)
        override fun contains(key: String) = values.containsKey(key)
        override fun clear() = values.clear()
        override fun remove(key: String) { values.remove(key) }
        override fun flush() = Unit
    }
}