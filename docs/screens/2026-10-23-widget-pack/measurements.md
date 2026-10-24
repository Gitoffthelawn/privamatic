# Widget pack — measurements (2026-10-23, Pixel 8, Android 17, Pixel Launcher)

Raw shots and ui dumps stay local (`raw/`, git-ignored). 1 dp = 2.625 px.

## No background: text against its own halo (build 2, halos as committed)

`python3 -I ../2026-10-22-widget-pack/widget_contrast.py raw/n-nb-<case>.png raw/n-nb-<case>-ui.xml 76,76,100,0 <case> light raw/n-nb-black-light.png`.
"Halo" = the 1–4 px ring around each glyph (halo plus shadow); "wallpaper" = pixels at least 12 px from any glyph.
"Wrong" = the colour variant that doesn't match the wallpaper, as seen between a wallpaper change and the next widget update
(forced by changing the wallpaper with no update). Score was 53 Fair (teal); the red/green tints are covered by `WidgetStyleTest` (model ≥ 3.8).

| Wallpaper | Variant | Name | OS | Score (teal) | Rating | Shield (median / worst 5 %) | Text vs bare wallpaper |
|---|---|---|---|---|---|---|---|
| White | dark (right) | 17.13 | 17.13 | 9.36 | 17.13 | 16.06 / 8.93 | same |
| White | light (**wrong**) | 9.29 / 7.34 | 8.86 / 7.34 | 5.20 / 4.37 | 8.59 / 7.34 | 8.59 / 1.27 | 1.00–1.71 |
| Black | light (right) | 21.00 | 21.00 | 12.31 | 21.00 | 20.09 / 8.59 | same |
| Black | dark (**wrong**) | 8.54 / 7.55 | 8.26 / 7.37 | 4.62 / 4.12 | 8.17 / 7.37 | 8.17 / 1.23 | 1.23–2.24 |
| photo | light (hint) | 11.98 / 10.05 | 11.58 / 9.94 | 6.61 / 5.82 | 11.07 / 9.74 | 9.43 / 2.12 | 1.00–1.69 |
| photo | dark (**wrong**) | 14.84 / 14.46 | 14.70 / 14.36 | 8.18 / 8.05 | 14.86 / 14.61 | 11.53 / 8.93 | 5.2–10.2 |

Values: median / worst 5 %. Every text reaches 3:1 against its halo in every case, worst 5 % included (lowest: teal score, dark variant on Black, 4.12).
The shield's outline ring keeps a median ≥ 8.2 but its worst 5 % drops to 1.2–2.1 in the wrong variant (thin 1 px outline).
Contact sheet: `no-background-halo-sheet.png`. Note: this build still stretched the device-name and OS halos to the shield (fixed in source afterwards, not yet seen on a device).

## Layout (round 3: released geometry, score and rating as one centred group)

From `raw/r3-*` (widget 28 = card 100 %, widget 29 = no background; Oliver's widget 22 matches the card).

| Shot | Name ink from edge | Change ink from end edge | Score / rating centre (px) | Rating → change gap | "…" |
|---|---|---|---|---|---|
| card, 47 Needs Attention ↓6, font 1.0 | 41.1 dp | 40.8 dp | 731.5 / 732.5 | 20 px | none |
| no background, same | 29.3 dp | 29.0 dp | 741.5 / 742.5 | 41 px | none |
| card, 44 ↓9, font 1.0 | 41.1 dp | 40.8 dp | 732.0 / 732.5 | 20 px | none |
| card, 53 Fair, font 1.0 (no change) | — | — | 790.5 / 791.0 (end-half centre, as released) | — | none |
| card, 47 ↓6, font 2.0 | — | — | 710.5 / **695.0** | 60 px | OS, rating |
| no background, 47 ↓6, font 2.0 | — | — | 720.5 / **706.0** | 80 px | OS, rating |

- Font 1.0: Oliver's screenshot geometry (text 40 dp in on both sides, shield centred, score over rating, ↓N centred on both rows); "Needs Attention" moves towards the shield to clear the change, whole.
- Font 2.0: score and arrow whole, nothing overlaps; the group moved fully to the shield before "Need…". **Open:** the ellipsized rating is drawn from the start of its box (15 px off the score's centre), and in the no-background widget the score's halo is clipped flat by the widget's 8 dp top padding.
- Gboard as default keyboard: score 53 → 44, "↓9" in red on the card (`#93000A`) and pink on the no-background halo (`#FFB4AB`); app shows "Privacy Change ↓9 pts".
- A two-digit change was not produced on the device.
