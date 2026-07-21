package studio.cortex.fruvio.engine.merge

/**
 * Ordered small → large, matching real-world fruit size. These 5 tiers correspond to the real
 * shipped Figma art (`sym_cherry`, `sym_raspberry`, `sym_lemon`, `sym_peach`, `sym_watermelon`) —
 * the names are final, not placeholders. [next] is null for the top tier (no merge target above it).
 */
enum class FruitTier {
    CHERRY, RASPBERRY, LEMON, PEACH, WATERMELON;

    val next: FruitTier? get() = entries.getOrNull(ordinal + 1)
}
