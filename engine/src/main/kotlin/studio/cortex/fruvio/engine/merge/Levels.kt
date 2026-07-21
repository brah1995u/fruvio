package studio.cortex.fruvio.engine.merge

data class WorldDef(val index: Int, val displayName: String, val firstLevel: Int, val lastLevel: Int)

/** Twenty-level campaign: four worlds with five progressively harder missions each. */
object Levels {
    val worlds: List<WorldDef> = listOf(
        WorldDef(1, "CITRUS BAY", 1, 5),
        WorldDef(2, "TROPICAL RUSH", 6, 10),
        WorldDef(3, "COCKTAIL LAB", 11, 15),
        WorldDef(4, "MIDNIGHT MIX", 16, 20),
    )

    val all: List<LevelDef> = listOf(
        level(1, 1, "First Splash", 6, 9, FruitTier.CHERRY, FruitTier.LEMON, WinCondition.ScoreThreshold(160), 10, 30),
        level(2, 1, "Peach Please", 6, 9, FruitTier.CHERRY, FruitTier.LEMON, WinCondition.ReachTier(FruitTier.PEACH), 8, 35),
        level(3, 1, "Fresh Budget", 6, 9, FruitTier.CHERRY, FruitTier.LEMON, WinCondition.DropLimit(16, 260), 14, 40),
        level(4, 1, "Three in a Row", 6, 9, FruitTier.CHERRY, FruitTier.LEMON, WinCondition.ComboChallenge(3, 240), 14, 50),
        level(5, 1, "Bay Watermelon", 7, 10, FruitTier.RASPBERRY, FruitTier.PEACH, WinCondition.ReachTier(FruitTier.WATERMELON), 18, 70),

        level(6, 2, "Juice Rush", 6, 9, FruitTier.CHERRY, FruitTier.PEACH, WinCondition.ScoreThreshold(540), 16, 75),
        level(7, 2, "Melon Current", 6, 9, FruitTier.RASPBERRY, FruitTier.PEACH, WinCondition.ReachTier(FruitTier.WATERMELON), 14, 85),
        level(8, 2, "Tight Squeeze", 6, 9, FruitTier.CHERRY, FruitTier.PEACH, WinCondition.DropLimit(20, 700), 18, 95),
        level(9, 2, "Combo Wave", 6, 9, FruitTier.CHERRY, FruitTier.LEMON, WinCondition.ComboChallenge(4, 600), 17, 105),
        level(10, 2, "Tropical Finale", 6, 9, FruitTier.RASPBERRY, FruitTier.PEACH, WinCondition.ComboChallenge(5, 1050), 20, 130),

        level(11, 3, "Lab Pressure", 5, 9, FruitTier.CHERRY, FruitTier.PEACH, WinCondition.ScoreThreshold(1050), 18, 135),
        level(12, 3, "Perfect Mix", 5, 9, FruitTier.RASPBERRY, FruitTier.PEACH, WinCondition.ReachTier(FruitTier.WATERMELON), 12, 145),
        level(13, 3, "Measured Pour", 5, 9, FruitTier.CHERRY, FruitTier.PEACH, WinCondition.DropLimit(18, 1150), 16, 155),
        level(14, 3, "Frozen Streak", 5, 9, FruitTier.CHERRY, FruitTier.LEMON, WinCondition.ComboChallenge(5, 1100), 18, 165),
        level(15, 3, "Lab Finale", 5, 9, FruitTier.RASPBERRY, FruitTier.PEACH, WinCondition.ComboChallenge(6, 1600), 22, 190),

        level(16, 4, "Midnight Score", 5, 8, FruitTier.CHERRY, FruitTier.PEACH, WinCondition.ScoreThreshold(1650), 20, 195),
        level(17, 4, "Dark Watermelon", 5, 8, FruitTier.RASPBERRY, FruitTier.PEACH, WinCondition.ReachTier(FruitTier.WATERMELON), 10, 210),
        level(18, 4, "Last Drops", 5, 8, FruitTier.CHERRY, FruitTier.PEACH, WinCondition.DropLimit(16, 1800), 14, 225),
        level(19, 4, "Night Combo", 5, 8, FruitTier.CHERRY, FruitTier.LEMON, WinCondition.ComboChallenge(6, 1750), 18, 245),
        level(20, 4, "Master Cocktail", 5, 8, FruitTier.RASPBERRY, FruitTier.PEACH, WinCondition.ComboChallenge(8, 2500), 24, 300),
    )

    fun world(index: Int): WorldDef = worlds.first { it.index == index }

    private fun level(
        index: Int,
        world: Int,
        name: String,
        width: Int,
        height: Int,
        min: FruitTier,
        max: FruitTier,
        condition: WinCondition,
        parDrops: Int,
        reward: Int,
    ) = LevelDef(index, world, name, width, height, min, max, condition, parDrops, reward)
}