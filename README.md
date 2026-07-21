# Fruvio

A fruit-merge physics puzzle where you drop fruit into a jar and combine matching tiers into ever-bigger fruit, backed by a hub of three fruit-themed mini-games.

Fruvio's core loop is a Box2D-driven "drop and merge" puzzle in the Suika/Watermelon Game tradition: fruit falls into a jar, same-tier fruit touching merges into the next tier up, and each level defines its own jar size, spawnable fruit range, win condition and par-based star rating. Around that core sits a mini-games hub (Fruit Plinko, Fruit Box, Fruit Guess), a persistent coin shop for gameplay boosters, and an achievements screen. It's a fun-coins-only demo with no real-money mechanics, built as the second title in a small game series sharing UI and engine conventions with an earlier project.

## Features

- Box2D physics-driven fruit-merge core loop (`MergeWorld`) with real contact-based merging
- Level catalog with per-level jar dimensions, spawn ranges, win conditions and star ratings (`LevelDef`, `Levels`, `StarRating`)
- Three fruit-themed mini-games: Fruit Plinko (physics drop board), Fruit Box (bonus pick-a-box), Fruit Guess (higher/lower)
- Persistent coin shop with gameplay boosters (extra preview, remove fruit, shake jar) usable in Merge levels
- Achievements system with progress tracking and snapshots
- Cocktail combo system layered on top of standard fruit merging (`CocktailCombo`)
- Shared UI toolkit (buttons, dialogs, scroll views) and sound/haptic feedback across all screens

## Tech Stack

- **Language:** Kotlin
- **Platform:** Android (minSdk 24, targetSdk 36), plus an LWJGL3 desktop target for development
- **Engine / framework:** libGDX with Box2D physics
- **Build:** Gradle (Kotlin DSL), multi-module (`engine`, `core`, `android`, `lwjgl3`)

## Project Structure

```
engine/       # Pure-Kotlin rules: merge/level logic, plinko board, bonus box, higher-lower, achievements
core/         # libGDX screens, UI toolkit, rendering, audio, assets, Box2D world glue
android/      # Android launcher module (APK packaging)
lwjgl3/       # Desktop launcher for fast local iteration
design/       # Visual reference material
docs/         # Design doc
```

## Building

```bash
git clone https://github.com/brah1995u/fruvio.git
cd fruvio
./gradlew :android:assembleDebug
```

The APK lands in `android/build/outputs/apk/debug/`.

## Status

Playable build covering the full merge campaign, all three mini-games, shop and achievements. It is a fun-coins demo with no real-money mechanics.
