# Widget centre change — mock-up measurements (2026-10-24, Pixel 8, Android 17, Pixel Launcher)

Branch `design/widget-centre-change` (`86cb2db`, never merged). Oliver's widget 22, photo wallpaper, light theme,
score 53 Fair. The change was forced with the debug-only broadcast extra `debug_delta`. The opacity was switched
between 100 % and 0 with `debug_opacity`. 1 dp = 2.625 px; the shield's slot is 105 × 105 px (40 dp).
Raw shots and UI dumps are in the scratchpad only. Contact sheet: `contact-sheet.png` (rows: shield, ↓6, ↓12, ↑6,
then the stacked variant ↓6/↑6/↓12 with "pts" on a second line).

Method: the view bounds come from `uiautomator dump`. "Visible" is the glyph ink on the card (pixels > 60 RGB from the
card colour) and the halo box with no background. "Gap" is horizontal, from the start-side text (device name / OS)
and to the end-side text (score / rating). Sizes come from the provider's debug log.

| Font | Background | State | Text, size | Visible w × h px | In slot | Gap start / end px | "pts" |
|---|---|---|---|---|---|---|---|
| 1.0 | card 100 % | shield | — | 79 × 97 | yes | 181 / 340 | — |
| 1.0 | card 100 % | ↓6 / ↑6 | "↓6", 47.25 px (full 18 sp) | 42–43 × 36 | yes | 201 / 356 | no: "↓6 pts" doesn't fit 40 dp |
| 1.0 | card 100 % | ↓12 | "↓12", full | 69 × 36 | yes | 188 / 343 | no |
| 1.0 | card 100 % | stacked ↓6 / ↓12 / ↑6 | number full, "pts" at 0.6 below | 42–69 × 80 | yes | 188–201 / 343–357 | **fits** (second line) |
| 1.0 | none | shield | — | 105 × 105 | yes | 143 / 302 | — |
| 1.0 | none | ↓6 / ↑6 | "↓6", full | 72 × 65 | yes | 159 / 319 | no |
| 1.0 | none | ↓12 | "↓12", full | 98 × 65 | yes | 146 / 306 | no |
| 1.0 | none | stacked | number full, "pts" below | 72–98 × 98 | yes | 146–159 / 306–319 | **fits** |
| 2.0 | card 100 % | shield | — | 79 × 97 | yes | 61 / 300 | — |
| 2.0 | card 100 % | ↓6 / ↑6 | 77.4 px (98 % of full 78.75) | 69 × 58 | yes | 69 / 302 | no |
| 2.0 | card 100 % | ↓12 | 64.3 px (82 %) | 94 × 48 | yes | 56 / 290 | no |
| 2.0 | none | shield | — | 105 × 105 | yes | 32 / 260 | — |
| 2.0 | none | ↓6 / ↑6 | 77.4 px (98 %) | 104 × **106** | 1 px over in height | 32 / 261 | no |
| 2.0 | none | ↓12 | 49.9 px (63 %) | 103 × 68 | yes | 33 / 261 | no |

At 2.0 the stacked variant falls back to the number alone (same rows as inline).

- **Truncation:** the change is never cut or wrapped (after the fix below). Score "53", rating "Fair" and "Pixel 8" are whole at both scales.
  At 2.0 the OS name ellipsizes: "Stock And…" on the card, "Stock An…" with no background (round 3: "Stock A…").
  A long rating ("Needs Attention") next to the centre change was not produced.
- **Edges:** text ink is 16.4 / 16.4 dp from the start / end edges on the card at 1.0 (16.8 / 16.0 at 2.0); with no background,
  the halo edges are 16.0 / 15.6 dp. The change's centre is within 3.5 px of the widget's centre.
- **Colours:** ↓ red, ↑ green (`WidgetStyle.deltaColors`, as released), per Oliver mid-run. Not re-measured; the same tints were measured in round B.
- **Content description:** "Score dropped 6 points", "Score dropped 12 points", "Score rose 6 points" in the dumps; in the shield state the change view is gone.
- **Fix round 1 (of this run):** the first sweep wrapped "↓12" (no background, font 2.0) into "↓1 / 2". The fit check used 4 dp = 10.5 px of halo padding,
  but the framework rounds it to 11 px, and the check compared unrounded widths. After rounding to whole pixels, it chose 49.9 px and stayed on one line.
  The height check still misses by 1 px (106 against 105) for a single digit with no background at 2.0. Not patched in the mock-up.
- **Font-scale change:** setting 2.0 or back to 1.0 triggered two widget updates within 1.1 s (logged twice). So the fit follows a new font scale.
