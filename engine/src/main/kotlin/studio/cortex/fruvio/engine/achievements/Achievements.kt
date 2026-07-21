package studio.cortex.fruvio.engine.achievements

/** Thirty-three persistent goals paced across the full campaign and all three mini-games. */
object Achievements {
    val all: List<AchievementDef> = listOf(
        AchievementDef("first_clear", "First Splash", "Clear your first campaign level", AchievementDef.Metric.LEVELS_CLEARED, 1, 30, "levels"),
        AchievementDef("citrus_explorer", "Citrus Explorer", "Clear five campaign levels", AchievementDef.Metric.LEVELS_CLEARED, 5, 60, "levels"),
        AchievementDef("tropical_traveler", "Tropical Traveler", "Clear ten campaign levels", AchievementDef.Metric.LEVELS_CLEARED, 10, 100, "levels"),
        AchievementDef("cocktail_pro", "Cocktail Pro", "Clear fifteen campaign levels", AchievementDef.Metric.LEVELS_CLEARED, 15, 150, "levels"),
        AchievementDef("all_cleared", "Level Master", "Clear every campaign level", AchievementDef.Metric.ALL_LEVELS_CLEARED, 20, 300, "levels"),

        AchievementDef("triple_star", "Triple Star", "Earn all 3 stars on any one level", AchievementDef.Metric.ANY_LEVEL_THREE_STARS, 1, 40, "levels"),
        AchievementDef("star_basket", "Star Basket", "Earn 3 stars on five levels", AchievementDef.Metric.THREE_STAR_LEVELS, 5, 90, "levels"),
        AchievementDef("star_cellar", "Star Cellar", "Earn 3 stars on ten levels", AchievementDef.Metric.THREE_STAR_LEVELS, 10, 160, "levels"),
        AchievementDef("star_orchard", "Star Orchard", "Earn 3 stars on fifteen levels", AchievementDef.Metric.THREE_STAR_LEVELS, 15, 240, "levels"),
        AchievementDef("perfectionist", "Perfectionist", "Earn 3 stars on every level", AchievementDef.Metric.ALL_LEVELS_THREE_STARS, 20, 400, "levels"),

        AchievementDef("raspberry_route", "Raspberry Route", "Create a Raspberry through a merge", AchievementDef.Metric.HIGHEST_TIER_ORDINAL, 1, 20, "tier"),
        AchievementDef("lemon_ladder", "Lemon Ladder", "Merge upward until you create a Lemon", AchievementDef.Metric.HIGHEST_TIER_ORDINAL, 2, 50, "tier"),
        AchievementDef("peach_peak", "Peach Peak", "Merge upward until you create a Peach", AchievementDef.Metric.HIGHEST_TIER_ORDINAL, 3, 75, "tier"),
        AchievementDef("big_melon", "Big Melon", "Merge your way to a Watermelon", AchievementDef.Metric.HIGHEST_TIER_ORDINAL, 4, 100, "tier"),

        AchievementDef("first_mix", "First Mix", "Perform 25 fruit merges", AchievementDef.Metric.TOTAL_MERGES, 25, 40, "merges"),
        AchievementDef("merge_apprentice", "Merge Apprentice", "Perform 75 fruit merges", AchievementDef.Metric.TOTAL_MERGES, 75, 75, "merges"),
        AchievementDef("merge_machine", "Merge Machine", "Perform 200 fruit merges", AchievementDef.Metric.TOTAL_MERGES, 200, 120, "merges"),
        AchievementDef("merge_master", "Merge Master", "Perform 400 fruit merges", AchievementDef.Metric.TOTAL_MERGES, 400, 200, "merges"),
        AchievementDef("merge_legend", "Merge Legend", "Perform 750 fruit merges", AchievementDef.Metric.TOTAL_MERGES, 750, 300, "merges"),
        AchievementDef("merge_marathon", "Merge Marathon", "Perform 1,500 fruit merges", AchievementDef.Metric.TOTAL_MERGES, 1500, 450, "merges"),

        AchievementDef("coin_sprout", "Coin Sprout", "Earn 500 lifetime coins", AchievementDef.Metric.LIFETIME_COINS_EARNED, 500, 30, "coins"),
        AchievementDef("coin_collector", "Coin Collector", "Earn 2,000 lifetime coins", AchievementDef.Metric.LIFETIME_COINS_EARNED, 2000, 100, "coins"),
        AchievementDef("coin_grove", "Coin Grove", "Earn 5,000 lifetime coins", AchievementDef.Metric.LIFETIME_COINS_EARNED, 5000, 200, "coins"),
        AchievementDef("fruit_fortune", "Fruit Fortune", "Earn 10,000 lifetime coins", AchievementDef.Metric.LIFETIME_COINS_EARNED, 10000, 300, "coins"),
        AchievementDef("golden_harvest", "Golden Harvest", "Earn 25,000 lifetime coins", AchievementDef.Metric.LIFETIME_COINS_EARNED, 25000, 600, "coins"),

        AchievementDef("side_sip", "Side Sip", "Finish one round in any mini-game", AchievementDef.Metric.MINI_GAMES_PLAYED, 1, 25, "games"),
        AchievementDef("mini_game_tourist", "Mini-Game Tourist", "Finish one round in every mini-game", AchievementDef.Metric.MINI_GAMES_PLAYED, 3, 60, "games"),
        AchievementDef("arcade_warmup", "Arcade Warm-up", "Finish five mini-game rounds", AchievementDef.Metric.MINI_GAME_ROUNDS, 5, 60, "rounds"),
        AchievementDef("arcade_regular", "Arcade Regular", "Finish twenty mini-game rounds", AchievementDef.Metric.MINI_GAME_ROUNDS, 20, 150, "rounds"),
        AchievementDef("arcade_marathon", "Arcade Marathon", "Finish fifty mini-game rounds", AchievementDef.Metric.MINI_GAME_ROUNDS, 50, 350, "rounds"),
        AchievementDef("warm_streak", "Warm Streak", "Reach a 3-card Higher-Lower streak", AchievementDef.Metric.HIGHER_LOWER_STREAK, 3, 60, "streak"),
        AchievementDef("lucky_streak", "Lucky Streak", "Reach a 5-card Higher-Lower streak", AchievementDef.Metric.HIGHER_LOWER_STREAK, 5, 120, "streak"),
        AchievementDef("cocktail_champion", "Cocktail Champion", "Reach a 10-card Higher-Lower streak", AchievementDef.Metric.HIGHER_LOWER_STREAK, 10, 300, "streak"),
    )
}