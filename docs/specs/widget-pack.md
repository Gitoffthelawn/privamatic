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
| Minimum card opacity | Not guessed: the lowest step from 70 to 76 % at which all text reaches 4.5:1 and the icon 3:1 on all six wallpaper/theme combinations (decided 2026-10-22). **Measured: 76 %** (75 % fails at 4.43; see Measurements). |
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

**Chosen minimum: 76 %** (measured 2026-10-22, Pixel 8, Android 17, Pixel Launcher).

Method (round A): a measurement build with the minimum temporarily at 70 %, five widgets (ids 17–21) at 70 / 75 / 76 / 100 % and
no background on one page. Photo (the phone's own mountain photo), plain White and plain Black home wallpaper × light and dark theme.
No widget update after a theme or wallpaper change: the launcher switched every colour by itself.
`widget_contrast.py` uses the same method as the paper's `contrast.py`. Run on the paper's screenshots, it gives back their numbers exactly.
Weakest text per combination (worst 5 % of pixels; text must reach 4.5, the shield 3.0). **Bold = fails.**

| Wallpaper | Theme | 70 % | 75 % | 76 % | 100 % | Shield at 76 % / 100 % |
|---|---|---|---|---|---|---|
| photo | light | 6.66 (score) | 6.60 (score) | 7.17 (score) | 8.27 (score) | 7.27 / 8.29 |
| photo | dark | 5.24 (score) | 6.61 (score) | 5.64 (score) | 10.03 (score) | 5.64 / 10.02 |
| White | light | 8.58 (score) | 8.52 (score) | 8.52 (score) | 8.27 (score) | 8.54 / 8.29 |
| White | dark | **3.69 (score)** | **4.43 (score)** | 4.57 (score) | 10.03 (score) | 4.57 / 10.02 |
| Black | light | **3.95 (score)** | 4.54 (score) | 4.64 (score) | 8.27 (score) | 4.65 / 8.29 |
| Black | dark | 10.87 (score) | 10.76 (score) | 10.76 (score) | 10.05 (score) | 10.81 / 10.10 |

- 75 % fails: the dark-theme Fair score `#A3CDDB` on White reaches 4.43. Contrast only rises with opacity here, so 70–74 % fail too.
  76 % passes everything. The tightest margin is that same score at 4.57 (model 4.58), then light Fair `#224C58` on Black at 4.64.
- The shield, which failed everywhere before (2.0 / 2.9 at 100 %), now reaches at least 4.57.
- On the device the score was Fair (53), so the teal was measured. The green and red tints are covered by the model and
  `WidgetStyleTest` (dark green 4.59, dark red 4.60 at 76 % on White; both fail at 75 %).
- On the photo, each widget sits over a different patch, so the rows aren't directly comparable with each other.
- Full table: `docs/screens/2026-10-22-widget-pack/measurements.md`. Contact sheet: `sweep-contact-sheet.png` there.

**No background** (same shots; text white `#FFFFFF`, score `#A3CDDB`; worst-5 % in brackets):

| Wallpaper | Text vs its shadow ring, median | Text vs plain wallpaper, median | Shield (fill vs outline ring) |
|---|---|---|---|
| Black | 21.0 (21.0) | 21.0 | 20.1 (8.6) |
| photo (light brown hills, `#B3A28E`) | 3.0–4.0 (2.0–2.9); score 2.1 | 2.5; score 1.5 | 10.3 (3.1) |
| White | 1.4–1.6 (≈1.1); score 1.3 | 1.0; score 1.7 | 8.6 (1.3) |

Weakest: **White**, in both themes; the style ignores the theme. The text is readable only through its grey shadow, and it is far below 4.5:1.
On a light-to-mid photo it reads, but is still below 4.5:1. It works well only on dark wallpapers.
This is the cost of Oliver's choice of light text only. The launcher's own fix, dark text when the wallpaper reports
`HINT_SUPPORTS_DARK_TEXT`, is the follow-up (see Found in STATE).

**Round B (final build, minimum 76 %)**, Pixel 8, Black wallpaper. Contact sheet: `tint-change-font2-sheet.png`;
configuration screens: `config-clamp.png`.

| Gate | Result |
|---|---|
| Clamp on device | Widgets stored at 40, 75 and 76 draw the identical card pixel (180,182,193) = 76 % of the card (238,240,255) over black. The settings screen shows "Opacity: 76%" for the widget stored at 40 and "No background" for 0. Leaving with Back saved nothing (the prefs file was unchanged). |
| Score tint, two ratings | Private DNS `off` → 47 "Needs Attention" (Poor) in `#93000A` light / `#FFB4AB` dark. Back to unset → 53 Fair in `#224C58` / `#A3CDDB`. |
| Change indicator | "↓6 pts" red (`#93000A` / `#FFB4AB`) and "↑6 pts" green (`#004225` / `#93D5AA`), both themes, switched by the launcher without an update. At 76 % in light theme on Black: red 4.63, green 5.76, teal score 4.64. |
| Font scale 2.0 | Score "53" ends at x=745 and "↑6 pts" starts at x=766: no overlap. All text sits inside the widget; "Stock Android" ellipsizes to "Stock And…". Same in dark theme. |
| Restore | Five test widgets removed (only the old invisible ids 14 and 15 remain), page 3 gone. Wallpaper back to the photo: home page 1 pixel-identical to the baseline, page 2 identical except a 59×16 px spot at the page indicator. Theme light, font scale 1.0, Private DNS unset. The debug app's `widget_preferences` and `score_history` files were written back from their pre-run copies. Crash buffer empty in both rounds. |

Not verified on a device: API 26–30 (legacy card and its tint colours; no API 30 emulator), and Good/Excellent green as a *score* tint (the green was measured as the ↑ colour).

## STOP points

| When | What Oliver does |
|---|---|
| After the diff review, before merge | Runs the external review (see CLAUDE.md, External review) and saves the findings file. |
