package studio.cortex.fruvio.engine.merge

/** Stars are based consistently on drops used for every mission type. */
object StarRating {
    fun stars(levelDef: LevelDef, performance: Int): Int {
        val par = levelDef.parValue
        return when {
            performance <= par -> 3
            performance <= (par * 3) / 2 -> 2
            else -> 1
        }
    }
}