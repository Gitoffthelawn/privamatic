# Widget readability + widget pack — spec

Run type: implementation run. Branch: `feat/widget-pack` (from `master`, 2026-10-22).
Based on `docs/decisions/widget-readability.md` (option B).

## Goal

The home-screen widget is readable at every opacity the user can pick, on any wallpaper and in both themes.
The score is tinted by its rating, as in the app. The change indicator reads "↓6 pts" and is clearly red or green.
At font scale 2.0 the score and the indicator never overlap.

## Scope

**In:**
- Two styles, chosen by the stored opacity (key `widget_opacity_<id>` in `widget_preferences`, unchanged):
  - **Card**, from the minimum (see Measurements) to 100 %;
  - **No background** at 0: text and icon straight on the wallpaper, light text with a shadow, like launcher labels.
- Clamp on read: a stored value that rounds to 0 is "no background"; anything else below the minimum is drawn
  (and shown on the configuration screen) as the minimum. Stored data is never rewritten.
- Configuration slider: 0, or minimum to 100. Values between snap to 0 (lower half) or the minimum (upper half).
- Stronger card text and icon tokens: light primary T10, secondary T20, shield T30 on a T95 card;
  dark primary T95, secondary T90, shield T80 on a T10 card (`system_accent2_900`, was T20).
- Score tint by rating (API 31+ through `RemoteViews.setColorInt` day/night pairs; fixed hex as named constants).
- Change indicator: plurals "↑1 pt" / "↓6 pts", 20sp bold, green/red by direction in both themes.
- Layout: two columns plus the shield; score and indicator share one row, the indicator auto-sizes, so nothing overlaps
  at font scale 2.0. `resizeMode` gains `vertical`.
- Remove the unused widget colours in `values-night-v31/colors.xml`.
- Fix the "orange-ish" comment at `ScoreCard.kt:119` (it is teal).

**Out:**
- The Android 8–11 slider issue (`setImageAlpha` on a background-only `ImageView`). Report only.
- The widget update trigger (exported receiver; no update on wallpaper or font-scale change). Report only.
- Dark text in the no-background style on light wallpapers (`HINT_SUPPORTS_DARK_TEXT`): Oliver chose light text with shadow.

## Oliver's calls

| Question | Default |
|---|---|
| Option | **B** — decided by Oliver, 2026-10-22. |
| Minimum card opacity | Not guessed: the lowest step from 70 to 76 % at which all text reaches 4.5:1 and the icon 3:1 on all six wallpaper/theme combinations (decided 2026-10-22). Result in Measurements. |
| Fair tint | App-matching teal, darkened to `#224C58` in light theme (decided 2026-10-22). |
| No-background score and indicator colour | Default: the dark-theme tints (light green/teal/red), with the same shadow as the text. |
| Slider snapping below the minimum | Default: lower half snaps to 0 ("No background"), upper half to the minimum. |

## Colours

| Element | Light (card T95) | Dark (card T10) | No background |
|---|---|---|---|
| Device name | `system_accent1_900` (T10) | `system_accent1_50` (T95) | white + shadow |
| OS name, rating | `system_accent1_800` (T20) | `system_accent1_100` (T90) | white + shadow |
| Shield | `system_neutral1_700` (T30) | `system_neutral1_200` (T80) | white, dark outline |
| Score Excellent/Good, ↑ | `#004225` | `#93D5AA` | `#93D5AA` + shadow |
| Score Fair | `#224C58` | `#A3CDDB` | `#A3CDDB` + shadow |
| Score Poor/Critical, ↓ | `#93000A` | `#FFB4AB` | `#FFB4AB` + shadow |

API 26–30 keep their Cream-on-dark card; the score and indicator use the dark-theme tints there (not device-checked).

## Invariants

1. The stored opacity key, its file and its values are unchanged; nothing rewrites them. Clamping happens only when reading.
2. Opacity 0 (or anything that rounds to 0) is the no-background style; it never draws a card.
3. Every RemoteViews call targets a view that exists in the layout it is applied to (no "Problem loading widget"):
   the no-background layout has no `widget_background`, so no alpha call goes to it.
4. API 31+ colours follow the launcher's light/dark mode without a new update (XML tokens or `setColorInt` pairs; no single-colour `setTextColor` on the card style).

## Milestones

| # | Milestone | Gate (evidence that proves it) |
|---|---|---|
| 1 | Clamp, slider snap, tints | Unit tests: clamp and snap edges; every tint and token reaches its target at the minimum and at 100 % on white and black wallpaper (tone model). |
| 2 | Minimum measured | Pixel 8: photo/White/Black × light/dark at 70, 75, 76 and 100 %, `contrast.py` from screenshot pixels. Table below. |
| 3 | No-background style | Same six combinations at 0: screenshots and contrast including the shadow; weakest case named. |
| 4 | Tint and indicator | Score tint for two ratings; ↑ and ↓ indicator (Private DNS toggled on the Pixel 8). |
| 5 | Font scale 2.0 | Screenshot, no overlap. |
| 6 | Clamp on device | A widget with stored 40 % draws the same card as one at the minimum. |
| 7 | Restore | Wallpaper, theme, font scale, Private DNS and widgets as before. |

## Measurements

Filled in from the device run.

## STOP points

| When | What Oliver does |
|---|---|
| After the diff review, before merge | Runs the external review (see CLAUDE.md, External review) and saves the findings file. |
