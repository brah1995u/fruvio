package studio.cortex.fruvio.engine.achievements

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AchievementsTest {
    @Test
    fun releaseCatalogHasThirtyThreeUniqueRewardingGoals() {
        assertEquals(33, Achievements.all.size)
        assertEquals(33, Achievements.all.map { it.id }.toSet().size)
        assertTrue(Achievements.all.all { achievement ->
            achievement.id.isNotBlank() &&
                achievement.title.isNotBlank() &&
                achievement.description.isNotBlank() &&
                achievement.target > 0 &&
                achievement.coinReward > 0
        })
    }
}