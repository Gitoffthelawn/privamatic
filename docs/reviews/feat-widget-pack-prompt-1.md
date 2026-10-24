Review docs/reviews/feat-widget-pack.diff at the Privamatic repo root (offline Android
privacy auditing app, Kotlin/Compose, master..feat/widget-pack: the home-screen widget gets a
measured minimum card opacity (76 %) or a no-background style at 0 with the text colour from the
wallpaper's dark-text hint and a halo behind each text, the score tinted by rating, and the score
change shown as "↓6"/"↑6" (red/green) in the centred shield's place, fitted to its 40 dp box in code;
both sides sit at a 28 dp padding, each side's two lines centred as one block so the score shares
the device name's baseline; debug-only broadcast extras live in src/debug with a release no-op in
src/release; the same diff adds the spec, STATE.md and device evidence under docs/screens).
You can't run git, Gradle or tests in this workspace. Read only the
files this diff touches, and only the functions around the changed lines —
don't explore the rest of the repo. Skip strings.xml, fastlane metadata, tests
and screenshots unless they are the point of the run. If you need more than
~10 files, stop and say which and why.

The point of this run is PrivacyWidgetProvider.kt (updateWidget, centreChangeTextSize),
WidgetStyle.kt, WidgetConfigurationActivity.kt, both WidgetDebugOverride.kt files
(app/src/debug and app/src/release), .gitignore and the widget layouts. The five layouts are
near-identical: read layout-v31/widget_privacy.xml and layout/widget_privacy_no_background.xml in
full, and compare the other three (layout/, layout-night-v31/ widget_privacy.xml and
layout/widget_privacy_no_background_dark_text.xml) against them in the diff. Read WidgetStyleTest.kt
too. docs/specs/widget-pack.md explains the design and the device measurements.

Don't fix anything. List findings ranked by severity (High / Medium / Low)
with file:line, a concrete failure scenario, and your confidence.

Context: first review. A round-1 prompt was written on 2026-10-22 for an earlier state of this
branch, but it was never run and there are no findings. Since then the layout changed three times;
the Pixel 8 gate of the final state (Android 17, Pixel Launcher) is in
docs/screens/widget-centre-change/build-round.md, round 3. Not device-checked: API 26–30.

Standing invariants — check that each holds on every path this diff touches:
1. Offline: no INTERNET permission, no network calls, no new dependency that
   can reach the network.
2. User data never leaves the device: score history (Room), the trusted list
   and settings are excluded from cloud backup and device transfer, in both
   data_extraction_rules.xml (Android 12+) and the legacy backup rules.
3. Room: every schema change bumps the version and ships a migration; no
   destructive fallback; existing score history survives an upgrade.
4. Scoring: the PrivacyCheck enum is the single source of truth for
   deductions; trust math goes through PrivacyCheck.cappedDeductionFor();
   no score logic elsewhere.
5. Detection: a disabled app still returns ApplicationInfo — check
   appInfo.enabled before treating a package as active.
6. All user-facing text comes from strings.xml.
7. Release builds stay unsigned (signingConfig = null) for F-Droid.

Run-specific invariants:
8. Stored widget data is never rewritten by the clamp: the prefs file `widget_preferences`, the key
   `widget_opacity_<id>` and its float values keep their meaning; only an explicit Save in the
   configuration screen writes a value. Any stored value (negative, 0, fractional, 1–75, NaN, >100)
   maps to exactly one of "no background" or a card at 76–100 %.
9. Every RemoteViews call in updateWidget targets a view that exists in the layout it is applied to,
   on every API level and in all five layouts (no "Problem loading widget"); nothing touches
   `widget_background` in the no-background layouts. Each update sets both the change and the shield
   visibility, so the shield and the change are never shown together, also when the launcher
   reapplies an update onto views it already shows.
10. Debug-only hooks never change a release build: a release build from git compiles (the
    `app/src/release` no-op is tracked despite the generic `release/` ignore rule), and with the
    no-op every widget update behaves exactly as with no override (real change, real device name,
    stored opacity untouched).
11. centreChangeTextSize always terminates, never returns a size below its minimum, measures the same
    layout resource that the RemoteViews use, and cannot throw inside the widget update on any
    API level (inflating the widget layout from a receiver context with no theme). On API 31+ the
    card colours still follow the launcher's light/dark mode without a new update (XML system
    tokens or `setColorInt` day/night pairs).
