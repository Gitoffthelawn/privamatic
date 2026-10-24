# Widget centre change — build round (2026-10-24, Pixel 8, Android 17, Pixel Launcher)

Uncommitted `feat/widget-pack` working tree (the gate failed, so nothing was committed). Oliver's widget 22, score 50 Fair
(44 "Needs Attention" with Private DNS off). The change state was forced with the debug-only `debug_delta` extra,
and the opacity with `debug_opacity`. 1 dp = 2.625 px; the shield's slot is 105 × 105 px, centred in the widget.
Raw shots, UI dumps and the scripts are in the session scratchpad only.

Images:
- `build-font2-sheet.png`, top to bottom:
  - font 2.0: ↓12 with no background, then ↓12 on the card;
  - "Needs Attention" beside ↓12 at 2.0, with no background on the photo, then the dark-text layout on White;
  - ↓6 with no background at font 1.0.
- `shield-outline-before-after.png`, left to right:
  - photo, stroke 1 (old build);
  - photo, stroke 1.5;
  - White, light variant (wrong), 1.5;
  - White, dark-text variant, 1.5.

## Change in the shield's place

"View" is the change's view bounds from `uiautomator`; with no background that is its halo. The gaps are horizontal:
- the start gap runs from the end of the device-name and OS boxes to the change;
- the end gap runs from the change to the start of the score and rating boxes;
- on the card, a second gap is measured between glyph ink.

The light and dark themes give identical bounds in all 16 pairs, so each row below covers both themes.

| Font | Style | State | Size set (px) | View w × h | In slot | Gap start / end, view | Gap start / end, ink |
|---|---|---|---|---|---|---|---|
| 1.0 | card | shield | — | 105 × 105 | yes | 62.9 / 123.4 dp | 69.0 / 129.5 dp |
| 1.0 | card | ↓6, ↑6 | 47.0 (full) | 50 × 65 | yes | 73.1 / 134.1 dp | 76.6 / 135.6 dp |
| 1.0 | card | ↓12 | 47.0 | 76 × 65 | yes | 68.2 / 129.1 dp | 71.6 / 130.7 dp |
| 1.0 | none | shield | — | 105 × 105 | yes | 54.5 / 115.0 dp | — |
| 1.0 | none | ↓6, ↑6 | 47.0 | 72 × 65 | yes | 60.6 / 121.5 dp | — |
| 1.0 | none | ↓12 | 47.0 | 98 × 65 | yes | 55.6 / 116.6 dp | — |
| 2.0 | card | shield | — | 105 × 105 | yes | 12.2 / 107.4 dp | 23.2 / 114.3 dp |
| 2.0 | card | ↓6, ↑6 | 72.4 | 76 × 99 | yes | 17.5 / 113.1 dp | 27.0 / 116.2 dp |
| 2.0 | card | ↓12 | not logged | **116** × 99 | **no** (11 px wide) | 9.9 / 105.5 dp | 19.4 / 108.2 dp |
| 2.0 | none | shield | — | 105 × 105 | yes | 12.2 / 99.0 dp | — |
| 2.0 | none | ↓6, ↑6 | 72.4 | 98 × 99 | yes | 13.3 / 100.6 dp | — |
| 2.0 | none | ↓12 | 60.6 | **120** × 83 | **no** (15 px wide) | 9.1 / 96.4 dp | — |
| 2.0 | dark text (White) | ↓12 | 60.6 | **120** × 83 | **no** | 9.1 / 9.5 dp ("Needs At…") | — |
| 2.0 | card | ↓12, "Needs Attention" | — | **116** × 99 | **no** | 9.9 / 10.3 dp | 19.4 / 13.0 dp |
| 1.0 | card | ↓12, "Needs Attention" | 47.0 | 76 × 65 | yes | 68.2 / 55.6 dp | 71.6 / 57.1 dp |
| 1.0 | none | ↓12, "Needs Attention" | 47.0 | 98 × 65 | yes | 55.6 / 43.0 dp | — |

- **Fails the gate:** at font 2.0, "↓12" is wider than the 40 dp box (116 and 120 px against 105). It is whole and centred,
  and nothing overlaps: the smallest gap is 9.1 dp between halos and 13.0 dp between glyphs on the card. But the box is
  supposed never to grow (spec invariant 7). All single-digit cases fit, and so does every case at font 1.0.
- **Cause, from the numbers (not proven):** the fit measures the layout's own TextView in-process, inflated under
  `Theme.DeviceDefault`. On the launcher, "↓12" is about 1.62 px wide per px of text size: 98 px of text at 60.6 px, 76 px at 47.
  The mock-up's plain `TextPaint` predicted the same 1.62, and it matched the launcher within 1 px.
  The in-process TextView must have measured at most 1.37 (no background) to 1.45 (card), or it wouldn't have accepted these sizes.
  The heights agree (99 px at 72.4 px on both sides), so the difference is in the width alone. One likely cause is a different
  typeface or glyph fallback under that theme; it was not checked.
- **1 px height overrun from the mock-up: gone.** Single digit, no background, font 2.0: 98 × 99 px (was 104 × 106).
  The mock-up's `maxHeight` never took effect, because `maxLines` sets the same TextView field.
- **Truncation:** the change text is never cut. "Stock Android" ellipsizes at 2.0 ("Stock And…" on the card, "Stock An…"
  with no background), and so does "Needs Attention" ("Needs Att…" / "Needs At…"). Both are as before this run: the spec allows the
  ellipsis when the side is too narrow. At font 1.0 everything is whole, "Needs Attention" included.
- **Edges:** text sits at the 16 dp padding on both sides; the change's centre is within 0.5 px of the widget's centre.
- **Spoken text:** in all 36 change shots the change view reads "Score dropped 6 points", "Score dropped 12 points" or
  "Score rose 6 points", matching its text. In all 11 shield shots there is no change node. The provider log kept only the
  last phase (the main buffer wrapped).
- **Dark-text layout** (plain White Home wallpaper, after the next update): dark text and a dark shield in all 8 states and
  in the dark theme. Before that update, the old light variant shows on White (as known).

## Score halo at the top (no background, font 2.0)

Halo-coloured span across the score's box, row by row (y = 443 is the widget's top edge, y = 464 the end of the 8 dp padding):

| Build | y 443 | 445 | 448 | 452 | 458 | 464+ |
|---|---|---|---|---|---|---|
| mock-up (clipped to the padding) | 0 | 0 | 0 | 0 | 0 | 132 px |
| this build (`clipToPadding="false"`) | 106 px | 118 | 122 | 128 | 130 | 132 |

The halo is no longer cut flat at the padding. It now starts at the widget's top edge with rounded corners. At most a pixel or two
of the corner curve may fall outside the widget's bounds (an estimate from the first row's width).

## Shield outline, no background (font 1.0, ring = 2–4 px around the fill; `widget_contrast.py`)

| Wallpaper | Variant | Stroke 1 (before) median / worst 5 % | Stroke 1.5 (after) median / worst 5 % |
|---|---|---|---|
| photo (grey rock behind the widget) | light (hint) | 11.15 / 4.01 (this run, old build) | 8.59 / 8.59 |
| White | light (**wrong**) | 8.59 / 1.27 (2026-10-23) | 8.59 / 8.59 |
| White | dark (right) | 16.06 / 8.93 (2026-10-23) | 8.93 / 8.93 |

At 1.5 the outline covers the whole ring, so the worst 5 % equals the median. The change is one attribute in each of the two
no-background shield drawables. A wider stroke would clip the shield's top point at the edge of its 24-unit viewport.

## Round 2 (2026-10-24): 28 dp sides, 18 sp score, unthemed fit

Same widget 22 and wallpaper; 48 shots by device-verify (raw in `raw/gate2/`, git-ignored). Image for Oliver:
`inset-score-f1-bg100-d6.png` (font 1.0, card 100 %, ↓6).

**Fit method, "↓12" at font 2.0** (logged in-process at the chosen size, against the launcher's bounds):

| Style | Size | Launcher | TextView, `Theme.DeviceDefault` | TextView, plain context | `TextPaint` |
|---|---|---|---|---|---|
| card | 72.4 px | 116 × 99 | 98 × 105 | 116 × 99 | 116 |
| no background | 60.6 px | 120 × 83 | 104 × 89 | 120 × 83 | 120 (+ 22 padding) |

The themed wrapper picks a narrower, taller typeface. The plain-context TextView is now used: at font 2.0, ↓12 is 104 × 88 (card) and
103 × 69 (no background), ↓6/↑6 81 × 105 and 103 × 105; all inside 105 × 105, and the logged size equals the launcher's in all 24 change shots.

**Insets** (font 1.0, card, ink to the card edge): before 16.4 dp sides, 22.5 dp top (score) / 27.8 dp (device name), 31.2 dp bottom.
After (28 dp padding, 18 sp score): left 29.0, right 28.6, top 28.2, bottom 31.6 dp.

**Rows** (vertical offset, glyph ink; negative = score/rating higher):

| Font | Score vs device name, before → after | Rating vs OS name, before → after |
|---|---|---|
| 1.0 | −4 px → 0 px | 0 → 0 |
| 2.0 | not measured → **−4 px** (bounds: top −4, bottom 0; ink −4/−3) | 0 → 0 |

At 2.0 the device name's view starts at y 464, exactly the card's top (443) plus the 8 dp padding: it is cut to 102 px tall where the
score gets 106, so its text sits 4 px lower. **Gate not clean**; options are in STATE.md.

**Other measurements:** smallest centre–side gap 12.2 dp (start side, font 2.0; 9.1 dp in round 1). "Stock Android" ellipsizes at 2.0
("Stock An…" card, "Stock A…" no background), and "Needs Attention" beside ↓12 at 2.0 becomes "Needs At…" / "Needs A…". Nothing is clipped,
and every halo is rounded at the top. Dark-text layout on White: dark text and dark shield in all 9 states.

## Round 3 (2026-10-24): each side's two lines centred as one block — gate clean

Raw in `raw/gate3/` (git-ignored). Images for Oliver: `centred-block-f1-bg100-d6.png`, `centred-block-f2-bg100-d6.png`.

| Font | Device name / score (px) | OS name / rating (px) | Flat glyph edges | Ink from card top / bottom | Ink from card sides |
|---|---|---|---|---|---|
| 1.0 | 506–569 / 506–569 | 575–626 / 575–626 | within 0.1 px | 30.5 / 29.3 dp | 29.0 / 28.6 dp |
| 2.0 | 468–574 / 468–574 | 580–671 / 580–671 | within 0.1 px | 19.0 / 16.4 dp | 29.3 / 28.2 dp |

Same in light and dark, card and no background, and the dark-text layout. Round glyphs overshoot by ≤ 0.8 px. At 2.0 the device name
is no longer cut short (106 px, was 102). All 24 changes fit 105 × 105, and the logged size equals the launcher's in all 24.
Smallest centre–side gap 12.2 dp by bounds, 15.6 dp by ink ("Needs At…" beside ↓12 at 2.0). "Galaxy S8" (debug override) at 2.0:
"Galaxy …" on the card, "Galaxy…" with no background, "y" whole with 5.3 px below it; whole at 1.0.
