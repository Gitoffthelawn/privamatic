**Blocked on me**
- None. Static review only, as requested.

**Changed**
- Nothing.

**Found**
No High findings.

Medium
- `app/src/main/java/com/techtrest/privamatic/PrivacyWidgetProvider.kt:241` / `app/src/main/res/layout/widget_privacy.xml:17` — On API 26–30, `setImageAlpha` changes an `ImageView`’s image drawable, but the card is assigned as its background. A stored 76–99% opacity therefore still renders a fully opaque card. Already documented as out of scope. Confidence: high.

Low
- `app/src/main/java/com/techtrest/privamatic/WidgetStyle.kt:38` — Every negative value maps to no background, although the spec says only values rounding to zero do so and all other sub-minimum values become 76%. For example, `-5f` unexpectedly selects the wallpaper style; `WidgetStyleTest.kt:30` codifies this contradiction. Confidence: high.

The other reviewed paths uphold the stated RemoteViews, release-stub, colour-pair, visibility, fitting, and storage invariants.

Roadmap:
- Make opacity affect legacy Android widgets.
- Align negative-opacity behavior between the implementation, tests, and design.

---

## Resolution (Claude Code, 2026-10-24)

Round 1 had no High finding, so per CLAUDE.md there is no round 2. Both findings were checked against the code before acting.

**L1 (Low) — negative opacity selects no background. Accepted (Oliver), fixed.**
- Confirmed: `WidgetStyle.effectiveOpacity` had `stored < 0.5f -> NO_BACKGROUND`, so -5 gave no background, while the spec
  says only a value that rounds to 0 % does; `WidgetStyleTest.kt:30` listed `-5f` as no background.
- Every caller goes through `effectiveOpacity` (provider and, via `settingsFor`, the configuration screen), so one change covers both.
- Failing test first: `effectiveOpacity_negative_drawsAsMinimum` (-5, -1 → 76) failed on the old code with
  `stored -5.0 expected:<76.0> but was:<0.0>`.
- Fix: `stored.roundToInt() == 0 -> NO_BACKGROUND` (no-background range is now -0.5 ≤ v < 0.5). The old test now checks
  `-0.49f` instead of `-5f`. The open-and-save round trip still holds for -1 (now 76 → stores 76).
- Full suite `testDebugUnitTest --rerun`: 185 tests, 0 failures. No device gate: a pure mapping change, and the settings screen
  can only store 0 or 76–100, so a negative value comes only from old or damaged data.
- Settled by commit `e2093fc` (fix: negative widget opacity draws the 76 % card, not no background).

**M1 (Medium) — the opacity slider has no effect on API 26–30. Rejected for this release (Oliver).**
- Confirmed as described: on API 26–30 the provider calls `setImageAlpha` on `widget_background`, but `setImageAlpha` acts on the
  ImageView's image drawable, and the card is the view's `android:background` (no `src`). So 76–99 % draws a fully opaque card.
- Not a regression: `master` has the same layout and the same call (provider line 194 there).
- Reasons: the card renders fully opaque there, which is readable, so nothing breaks except a slider without effect. A fix changes
  how the card is drawn on older Android and needs verification on an API 30 emulator, which is a separate run.
- Recorded in STATE.md "Next" as a follow-up run.
