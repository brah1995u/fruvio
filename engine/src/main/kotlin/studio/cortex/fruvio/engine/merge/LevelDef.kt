package studio.cortex.fruvio.engine.merge

/** Fixed parameters for one campaign level. */
data class LevelDef(
    val index: Int,
    val worldIndex: Int = (index - 1) / 5 + 1,
    val displayName: String = "Level $index",
    val jarWidthUnits: Int,
    val jarHeightUnits: Int,
    val spawnTierMin: FruitTier,
    val spawnTierMax: FruitTier,
    val winCondition: WinCondition,
    /** Target drop count for a three-star clear. */
    val parValue: Int,
    val coinReward: Int,
) {
    init {
        require(index > 0) { "index must be positive" }
        require(worldIndex in 1..4) { "worldIndex must be in 1..4" }
        require(displayName.isNotBlank()) { "displayName must not be blank" }
        require(jarWidthUnits > 0) { "jarWidthUnits must be positive" }
        require(jarHeightUnits > 0) { "jarHeightUnits must be positive" }
        require(spawnTierMin.ordinal <= spawnTierMax.ordinal) {
            "spawnTierMin must not be above spawnTierMax"
        }
        require(parValue > 0) { "parValue must be positive" }
        require(coinReward > 0) { "coinReward must be positive" }

        val wc = winCondition
        require(wc !is WinCondition.ReachTier || wc.target.ordinal > spawnTierMax.ordinal) {
            "ReachTier target must be strictly above spawnTierMax"
        }
    }

    val spawnableTiers: List<FruitTier>
        get() = FruitTier.entries.subList(spawnTierMin.ordinal, spawnTierMax.ordinal + 1)
}