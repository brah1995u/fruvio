package studio.cortex.fruvio.engine.merge

/** Pure merge-resolution rule: two touching same-tier fruits merge into the next tier up. */
object MergeRules {
    /**
     * Returns the resulting tier if [a] and [b] merge, or null if they don't — either because
     * they're different tiers, or because they're both the top tier with no tier above it.
     */
    fun resolve(a: FruitTier, b: FruitTier): FruitTier? {
        if (a != b) return null
        return a.next
    }
}
