Review docs/reviews/feat-widget-pack.diff at the Privamatic repo root (offline Android
privacy auditing app, Kotlin/Compose, master..feat/widget-pack: the home-screen widget gets a
card from a measured minimum opacity (76 %) or a no-background style at 0, stored opacities below
the minimum are clamped on read, stronger text/icon colours, the score tinted by rating, the change
shown as "↑N pts"/"↓N pts" in green/red, and a layout where score and change can't overlap at large
font sizes; the same diff adds the spec, STATE.md updates and device evidence under docs/screens).
You can't run git, Gradle or tests in this workspace. Read only the
files this diff touches, and only the functions around the changed lines —
don't explore the rest of the repo. Skip strings.xml, fastlane metadata, tests
and screenshots unless they are the point of the run. If you need more than
~10 files, stop and say which and why.

The point of this run is PrivacyWidgetProvider.kt (updateWidget), WidgetStyle.kt,
WidgetConfigurationActivity.kt, the four widget layouts (layout/, layout-v31/, layout-night-v31/
widget_privacy.xml and layout/widget_privacy_no_background.xml) and WidgetStyleTest.kt; read the
test too. docs/specs/widget-pack.md explains the design and the device measurements.

Don't fix anything. List findings ranked by severity (High / Medium / Low)
with file:line, a concrete failure scenario, and your confidence.

Context: first review.

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
   on every API level and in both styles. In particular nothing touches `widget_background` in the
   no-background layout (a missing view shows "Problem loading widget").
10. On API 31+ the card style follows the launcher's light/dark mode without a new widget update:
    colours come from XML system tokens or `setColorInt` day/night pairs, never a single-colour
    `setTextColor`. The no-background style and API 26–30 may use `setTextColor`.
11. The colours in WidgetStyle, the layouts and the spec's colour table agree, and each card colour
    reaches its target (text 4.5:1, shield 3:1) at MIN_CARD_OPACITY on any wallpaper, as
    WidgetStyleTest models it.
