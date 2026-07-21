package studio.cortex.fruvio.engine.merge

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LevelsTest {
    @Test fun twentyLevelsAreSequentialAcrossFourWorlds() {
        assertEquals(20, Levels.all.size)
        assertEquals((1..20).toList(), Levels.all.map { it.index })
        assertEquals((1..4).toList(), Levels.worlds.map { it.index })
        assertEquals(listOf(5, 5, 5, 5), Levels.worlds.map { world -> Levels.all.count { it.worldIndex == world.index } })
    }

    @Test fun everyCampaignLevelHasValidEconomyAndAtLeastThreeSpawnTiers() {
        Levels.all.forEach { level ->
            assertTrue(level.spawnableTiers.size >= 3, "level ${level.index} needs 3+ spawn tiers")
            assertTrue(level.parValue > 0)
            assertTrue(level.coinReward > 0)
            assertTrue(level.displayName.isNotBlank())
        }
    }

    @Test fun everyWorldStartsWithScoreThenReachDropAndComboMissions() {
        Levels.worlds.forEach { world ->
            val levels = Levels.all.filter { it.worldIndex == world.index }
            assertTrue(levels[0].winCondition is WinCondition.ScoreThreshold)
            assertTrue(levels[1].winCondition is WinCondition.ReachTier)
            assertTrue(levels[2].winCondition is WinCondition.DropLimit)
            assertTrue(levels[3].winCondition is WinCondition.ComboChallenge)
        }
    }

    @Test fun laterCampaignUsesNarrowerJarsAndHigherTargets() {
        assertTrue(Levels.all.last().jarWidthUnits < Levels.all.first().jarWidthUnits)
        val firstScore = (Levels.all.first().winCondition as WinCondition.ScoreThreshold).target
        val lastScore = (Levels.all.last().winCondition as WinCondition.ComboChallenge).scoreTarget
        assertTrue(lastScore > firstScore)
    }
}