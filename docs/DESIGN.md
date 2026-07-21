# Fruvio — Design Doc

Second game in Андрій's game series (first: **Flame Jester**, `F:/Claude/projects/games/Flame Jester/`).
Flame Jester is the reference/template project — its architecture, module split, and UI/nav
conventions are reused wholesale; only the core game mechanic, mini-games, and art are new.

Status: design draft, pending review before scaffolding.

## 1. What this is

A fruit-themed, level-based mobile game — native Android/Kotlin + libGDX, fun-coins only (no real
money, same demo stance as Flame Jester). Core loop: a physics fruit-merge puzzle ("Suika/Watermelon
Game"-style — drop fruit, same-tier fruit touching merges into the next tier up) across many levels,
plus a hub of 3 mini-games (Plinko drop, Bonus Pick-a-Box, Higher-Lower cards), a coin shop, and an
achievements screen.

Working title: **Fruvio**. `applicationId` candidate: `studio.cortex.fruvio` (new, no collision with
Flame Jester's `studio.cortex.flamejester`).

**Naming note:** the Figma reference file is named "fruit cocktail," which collides with a real,
well-known commercial slot machine (Igrosoft). The art itself is original (AI-generated, stylized
cartoon fruit characters — not a re-skin of the real game's assets), so there's no asset-rip issue,
but the name doesn't ship. Same precedent as Fire Joker → Flame Jester. **Fruvio** is the replacement.

## 2. Why a new project, not a Flame Jester reskin

Andrii's stated intent (see project memory `game-dev-direction`): build a **series** of games, each
with a different design. Flame Jester's `slot-game-pipeline` skill covers *reskins* — same mechanic,
new theme/art. This request is explicitly the opposite: **new mechanic, new genre, same UI/engine
conventions where it makes sense, avoid duplicating any of Flame Jester's game modes** (not match-3,
not the 3×3 slot, not Fortune Wheel, not Lucky Box, not Fast Click).

Flame Jester is forked as a **pattern reference**, not a code copy for gameplay logic. The reusable
substrate (`core/ui`, `core/render`, `core/anim`, navigation/settings/haptics, the `Progress.kt`
facade shape, Gradle/version matrix, desktop capture harness) carries over structurally; the
gameplay-specific code (`engine/match/*`, `MatchGameScreen`, slot `GameConfig`/`Evaluator`, Wheel)
does not — Fruvio gets its own `engine/merge/` and new screens instead.

## 3. What forks from Flame Jester unchanged (pattern, not literal copy)

- `core/ui/` — `Widgets.kt` (Button/Ui, auto-shrinking `fitText()`), `Controls.kt` (Toggle/Slider,
  the +/- stepper atom — repurposed here for numeric adjusters like shop quantity, no longer tied to
  a slot bet), `Scroll.kt`, `Dialog.kt`.
- `core/render/Draw.kt` — draw helpers, clip/scissor.
- `core/anim/` — particle/juice helpers (`CoinFountain` equivalent).
- Root `Game` class pattern (`FlameJesterGame.kt` → `FruvioGame.kt`): fade-transition `go{}` nav,
  achievement-toast overlay, coin wallet `award()`/`spend()`/`persistBalance()`.
- `Progress.kt` facade shape: single `Preferences` wrapper — coins, per-level star progress, booster
  inventory, permanent upgrades, achievement counters+claims. New keys/content for Fruvio's own
  economy, same shape.
- Screen *patterns* (same structure, new content): Splash, Menu, Settings (Sound/Music/Vibration +
  About/Privacy/Terms), `InfoScreen`, Shop, Achievements, MiniGames hub.
- `Haptics` interface + `AndroidHaptics`/`NoHaptics`, `Audio.kt` (`Sfx` enum + looping-music manager).
- Gradle/version matrix: Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20, libGDX 1.13.1, JDK 17, compileSdk
  35 / minSdk 24 / targetSdk 34. Module split: `engine` (pure Kotlin/JVM) / `core` (libGDX views) /
  `android` / `lwjgl3` (desktop + `--capture=` verification harness).
- The environment-trap checklist (`ANDROID_SDK_HOME` conflict, AndroidManifest namespace mismatch,
  MSYS path mangling, tap-coordinate scaling on `tt_pixel`) — copied verbatim into this project's own
  `docs/MEMORY-MAP.md` once scaffolding starts.

## 4. What's new

- `engine/merge/` — pure-Kotlin merge-puzzle rules and level logic (testable without gdx).
- Box2D physics in `core/` — new dependency (`gdx-box2d` + platform natives), not present in Flame
  Jester at all. This is the one genuinely new technical risk area; budget real tuning time (gravity
  scale, restitution, merge-trigger radius) via desktop capture, not guesswork.
- `MergeGameScreen`, `PlinkoScreen`, `BonusBoxScreen`, `HigherLowerScreen` — all new.
- New `Theme.kt` palette and `Assets.kt` symbol set, sourced from the actual Figma file (pending —
  see §10).

## 5. Core game: Merge-Drop mechanic

- Player drops a fruit from a spout at the top of a jar; gravity + collision (Box2D) settle it.
- Two touching fruits of the **same tier** merge into the **next tier up**, awarding score. This is a
  plain per-tick contact rule, not a special "chain" state machine: every physics step checks all
  live same-tier contacts and merges them, so cascades (a merge producing a fruit that immediately
  touches another same tier) fall out naturally from re-running the same check next tick — same
  cascade *result* as Flame Jester's match engine, but no bespoke cascade logic to write.
- Fruit tiers are ordered small → large, mapped 1:1 onto the ~11 fruit-character symbols in the Figma
  file (exact roster pending asset pull — see §10). Each level exposes a capped spawn range (early
  levels only spawn the smallest 2-3 tiers; later levels start higher, which is what makes them
  harder, mirroring how Flame Jester's `Levels.kt` scales board size/kind-count instead).
- Lose condition: a fruit stack crosses the jar's overflow line and stays there past a short grace
  timer (standard Suika-style rule — prevents instant death from a single bounce).
- Win condition per level (one of, set per `LevelDef`):
  - **REACH_TIER** — produce a specific fruit tier at least once (classic "reach the watermelon").
  - **SCORE_THRESHOLD** — hit a score target before running out of drops.
  - **DROP_LIMIT** — clear/score without exceeding N drops (efficiency-graded).
- Star rating (1-3, same UX pattern as Flame Jester's per-level stars): thresholds on
  drops-used-vs-par or score-vs-par depending on the level's win condition.
- `engine/merge/` owns: `FruitTier` (ordered list), merge-resolution rules (tier[i] + tier[i] →
  tier[i+1], top tier has no merge target), score formula, `LevelDef`/`Levels.kt` (jar
  width/height in grid units, spawn-tier cap, win condition + target value, drop limit, par
  thresholds for stars), spawn-queue RNG (current + next-up-preview). The physical simulation itself
  (body positions, collision detection, actual merge trigger) lives in `core/` since it needs Box2D —
  this split mirrors Flame Jester's engine/core boundary (pure logic vs. gdx-dependent view), just
  with the boundary drawn slightly differently because physics isn't optional here the way rendering
  is elsewhere.

## 6. Level structure

Proposed: **5 "worlds" × 10 levels = 50 levels**, single continuous track (not 5 parallel modes like
Flame Jester's MATCH2/MATCH3/etc., since merge-drop doesn't split naturally along that axis). Each
world changes jar shape/width and raises the spawn-tier floor; difficulty ramps within a world via
drop limits and win-condition mix. This count/split is a **default proposal, not confirmed** — cheap
to adjust once jar-size math is prototyped and a few levels are played by hand (same caveat Flame
Jester's own `MEMORY-MAP.md` notes about its generator-based difficulty curve never being hand-vetted
past level 10).

## 7. Mini-games (hub, 3 total — confirmed with Андрій, all distinct from Flame Jester's 4)

1. **Plinko** — drop a token through a peg field (Box2D again — shares a small physics-world helper
   with `MergeGameScreen` rather than duplicating setup), lands in a bin with a reward multiplier.
2. **Bonus Pick-a-Box** — pick N of M face-down fruit cards, one reveal each, no re-pick. Non-physics;
   reuses `Dialog`/`Button` press-juice. Distinct from Flame Jester's `LuckyBoxScreen` in reveal
   mechanic/animation, not just reskinned.
3. **Higher-Lower** — flip a card, guess whether the next fruit's value is higher or lower; streak
   multiplier, cash out anytime. Non-physics, cheapest of the three to build.

## 8. Screens & navigation

```
Splash → Menu (Play / MiniGames / Shop / Achievements / Settings)
  Play → LevelSelect → MergeGameScreen (pause/victory/defeat dialogs)
  MiniGames → Plinko | BonusBox | HigherLower
  Shop
  Achievements
  Settings → InfoScreen (About/Privacy/Terms)
```

Same shell as Flame Jester's nav (fade-transition `go{}`, shared header coin-pill, unified back
button) — no new navigation pattern needed.

## 9. Data & persistence

`Progress.kt` facade, same shape as Flame Jester's: coins, per-level stars, booster inventory,
permanent upgrades, achievement counters+claims, mini-game state. New `SharedPreferences`/
`Gdx.app.getPreferences` namespace (`fruvio`, not `flamejester`).

Boosters (3, mapped from Flame Jester's hint/reshuffle/freeze set onto merge-drop equivalents):
- **Extra Preview** — see the next 2 upcoming fruits instead of 1 (hint equivalent).
- **Undo Drop** — undo the last drop (reshuffle equivalent — escape a bad placement).
- **Slow-Mo** — brief slow-motion window right after a drop, for precise placement (freeze
  equivalent — buys thinking time instead of skipping a hazard).

## 10. Visual design — Figma

Figma file: "fruit-cocktail (Copia)" (fileKey `1By4j4jM2pmHFOHfyLXQiI`). Structure confirmed via
`get_metadata`: not a UI wireframe — an **asset pack** (~11 fruit-character symbol frames, ~8 UI icon
frames, 3 casino-style background frames, an app-icon frame, 8 "screen" preview frames in light/dark
variants). Same shape as Flame Jester's own Figma reference (asset pack, not mockup) — expected per
the `slot-game-pipeline` skill's intake note.

**Unblocked (2026-07-21, app-shell-and-assets plan):** the Figma MCP rate-limit blocker noted below
no longer fully gates this project — real art has been extracted and integrated via the
screenshot-crop technique instead of the MCP asset-download workflow. Extracted and wired in:
- 5 fruit symbols (`sym_cherry`/`sym_lemon`/`sym_peach`/`sym_raspberry`/`sym_watermelon`, matching
  `engine/merge/FruitTier`'s final 5-tier roster).
- 6 UI-chrome pieces (`ui_btn_square_blue`, `ui_btn_rect_red`, `ui_btn_rect_blue`,
  `ui_badge_circle_gold`, `ui_orb_green`, `ui_panel_square_gold`).
- 8 functional icons (`icon_arrow`/`icon_star`/`icon_cart`/`icon_home`/`icon_menu`/`icon_mute`/
  `icon_close`/`icon_sound`).
- 3 full-screen backgrounds (`bg_water`/`bg_tropical`/`bg_panel`, copied verbatim, not packed into
  the atlas — too large).

All 19 non-background pieces are packed into one atlas (`assets/atlas/fruvio.atlas`, via
`gradlew :core:packTextures`). `Theme.kt`'s palette is now real, measured (PIL
median-of-opaque-pixels) values sampled from these exact PNGs, not invented. `Assets.kt` and
`UiSkin.kt` wire only the surfaces this art actually supports (two button chrome variants + one
panel background, insets measured via pixel-difference scan) — there is no settings-toggle-track/
slider-track art and no coin-front/coin-side art yet, so `Controls.kt`'s Settings widgets stand in
on the generic panel/badge until Settings gets its own chrome in a later plan. `ui_btn_rect_red` and
`ui_orb_green` are packed but not yet bound to any code path — no screen shipped so far needs a
red/danger button or a green orb icon; wire them when one does. The app launcher icon
(`appname_Icon_Final*.png`) remains a separate, deliberately out-of-scope concern.

Figma MCP itself remains rate-limited on the Starter plan (see the original blocker note below,
kept for history) — if any *additional* asset beyond this 19-region set is ever needed (more fruit
tiers, Settings-specific chrome, mini-game art), that pull is still blocked the same way and needs
the same workaround (rate limit reset, Андрій pasting frames directly, or a plan upgrade), or another
screenshot-crop pass like this one.

**Original blocker (resolved via the workaround above, kept for history):** Figma MCP hit the
Starter-plan tool-call rate limit this session after ~3 calls (2 `get_screenshot` + 1 retry all
failed with `rate_limit_paywall`). One screenshot did succeed (a strawberry-character symbol —
confirms original AI-generated art, no direct-rip concern).

## 11. Economy — first pass

No RTP concept (not gambling) — but still needs balancing: coin reward per level/star, shop prices
(consumable boosters + permanent upgrades), achievement targets. First pass will be balanced by feel,
same as Flame Jester's own shop/achievement pricing — revisit if playtesting shows it's too
fast/slow, not simulated up front.

## 12. Module & build architecture

Same 4-module split as Flame Jester: `engine` / `core` / `android` / `lwjgl3`. New Gradle dependency
on top of the existing `gdx`/`gdx-freetype` set: `com.badlogicgames.gdx:gdx-box2d:1.13.1` (core) plus
its platform-natives artifact for each backend (desktop classifier for `lwjgl3`, the android ABI
classifiers for `android`) — exact artifact coordinates and classifiers to confirm against the
current libGDX 1.13.1 docs at scaffolding time, not guessed here.
Desktop `--capture=<screen>:<path.png>` harness pattern carries over unchanged; add
`--frames=N`-driven capture calls specifically for the merge/Plinko physics settle, since those need
a few simulated ticks to look right, not just a static first frame.

## 13. Testing & verification plan

Same discipline as Flame Jester, in order:
1. `engine/merge` unit tests (merge-resolution, level target-check, star thresholds) — pure Kotlin,
   fast, no gdx needed.
2. Desktop `--capture=` for every screen after any view change — catches layout bugs before ever
   touching an emulator. For physics screens, capture at a few `--frames=N` values to check settle
   behavior, not just t=0.
3. Only after desktop capture looks right: `:android:assembleDebug` → emulator → `adb install` → tap
   through the real flow → `adb logcat -d | grep FATAL`.
4. Physically launch on an emulator/device at least once — a green Gradle build only proves it
   compiles (same manifest-namespace trap risk as Flame Jester's `android:name` gotcha).

## 14. Open questions / explicit assumptions (flag before scaffolding)

- Level count (50, 5 worlds × 10) and win-condition mix are my proposed default, not yet confirmed —
  cheap to revise once jar-size math is prototyped.
- Demo/fun-coins only, no real-money path — same default as Flame Jester unless told otherwise.
- Figma asset pull is blocked (see §10) — Theme/Assets work can't start until unblocked one way or
  another.
- Booster set (hint/extra-preview/undo-last-drop) is a first guess adapted from Flame Jester's set —
  not yet confirmed as the right fit for a merge game.

**2026-07-21, added after Plan 1 (scaffold + `engine/merge`) shipped and passed final review** — 3
forward-looking findings to resolve in the level-authoring / `MergeGameScreen` plan, not blockers for
Plan 1 itself:
- **`parValue` vs. `WinCondition` target/threshold relationship is underspecified.** `Levels.kt`'s
  Level 3 sets `parValue == ScoreThreshold.target` (both 500) — under `StarRating`'s current
  semantics, this makes the 1- and 2-star bands unreachable on a normal win (any win already clears
  the 3-star bar). Decide: should `parValue` always be a *stretch* goal strictly beyond the win
  threshold (as Level 4's `DropLimit` example already does — `parValue=20` vs. `maxDrops=25`), or
  should the two concepts merge? Resolve before authoring the real ~50 levels.
  **RESOLVED (2026-07-21, fruit-tier resync):** `parValue` is now always a stretch goal strictly
  beyond the win threshold. `Levels.kt` Level 3 (`ScoreThreshold(target = 300)`) sets
  `parValue = 400`, matching the pattern Level 4's `DropLimit` already used.
- **`ReachTier` has no invariant preventing an instant win.** `MergeGame` seeds
  `highestTierReached = level.spawnableTiers.first()` (the spawn floor), and `WinCondition.ReachTier`
  checks `highestTierReached >= target`. A level whose `target` isn't strictly above its spawn range
  would be marked won on the very first merge, regardless of what merged — no shipped example level
  triggers this, and `LevelDef.init` has no guard against it. Add either an invariant
  (`target.ordinal > spawnTierMax.ordinal`) or seed `highestTierReached` to a sentinel below the
  floor so "reached" strictly means "produced by a merge."
  **RESOLVED (2026-07-21, fruit-tier resync):** `LevelDef.init` now requires
  `wc.target.ordinal > spawnTierMax.ordinal` whenever `winCondition` is a `ReachTier`, throwing
  `IllegalArgumentException` at construction time otherwise. Covered by `LevelDefTest`'s
  `reachTierTarget*` cases.
- **No integration test drives a real `Levels.all` entry through `MergeGame`/`StarRating` end to
  end** — every test so far uses bespoke fixtures. One such test would have caught the `parValue`
  issue above automatically; worth adding once real level content exists.
  **RESOLVED (2026-07-21, fruit-tier resync):** `MergeGameTest.realLevel1FromLevelsAllWinsAndEarnsThreeStars`
  drives `Levels.all[0]` through a full `ReachTier` win via `MergeGame` and checks the resulting
  `StarRating.stars` count.

## 15. Follow-up: capture this as a reusable pipeline

Андрій has said this project should also be recorded as a *learning pass* for future games in the
series (the way Flame Jester's build produced the `slot-game-pipeline` skill). Once Fruvio ships,
write a second skill (or extend `slot-game-pipeline`'s scope) covering "new mechanic in this series"
projects specifically — what forks from the reference project unconditionally (§3 above generalizes
beyond slots), what's genuinely new per game, and the Box2D-specific traps discovered during this
build. Do this as an explicit last step, not deferred indefinitely.
