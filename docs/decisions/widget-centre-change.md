# Widget: the change in the shield's slot — design run (2026-10-24)

Oliver's idea: the sides stay as they are but sit exactly at the widget's start and end edges. While the change timer runs,
the centre shows the change instead of the shield; the shield returns when the timer ends. This run replaces round 3
of `feat/widget-pack`. Mock-up branch `design/widget-centre-change` (never merged). Measurements and images are in
`docs/screens/widget-centre-change/`.

## 1. How the change timer works (code on `feat/widget-pack`, device checks on the Pixel 8)

- **Start:** `ScoreHistoryRepository.recordScore` stores `delta_first_appeared_timestamp` = now. It does this the first time a
  score differs from the day's baseline (`ScoreHistoryRepository.kt:93-97`, called at `:51` and `:68`). `recordScore` runs on every
  widget update (`PrivacyWidgetProvider.kt:99`) and every app scan (`PrivacyViewModel.kt:226`).
- **Length:** nominally 48 h: `CHANGE_EXPIRY_MS` (`PrivacyWidgetProvider.kt:271`), checked at `:102-105`. In practice it is shorter.
  The delta is measured against the day's baseline. The first update after midnight makes the last score the new baseline
  (`ScoreHistoryRepository.kt:65-74`). If the score didn't change overnight, the delta is then 0 and the timestamp resets (`:94`).
  So a change normally shows until the **first update after midnight**, not for 48 h.
- **End:** whichever comes first, at an update: the delta returns to 0, or 48 h have passed since the timestamp.
  Latent edge: if the score changes again across midnight, the old timestamp is kept. The 48 h cap can then hide a fresh change.
- **Scheduling:** nothing of its own. It is a stored timestamp, checked at the next widget update. Updates come from:
  - the existing 6-hour `setInexactRepeating(ELAPSED_REALTIME_WAKEUP)` alarm (`PrivacyWidgetProvider.kt:277-286`);
  - app scans and trusted-list changes (`PrivacyViewModel.kt:120, 174`);
  - saving the widget settings (`WidgetConfigurationActivity.kt:84`);
  - boot (`BootReceiver.kt:35-36`) and app updates;
  - a font-scale change (seen on the device today).

  `updatePeriodMillis` is 0.
- **How late the shield can come back** (Pixel 8 `dumpsys alarm`):
  - The update alarm has a window of +4 h 30 m on its 6 h interval, so without the app being opened updates are 6–10.5 h apart.
  - The app is in standby bucket 10 (ACTIVE, because of its bound widget), so standby quotas don't apply.
  - In deep Doze a wakeup alarm without allow-while-idle waits for the next maintenance window (`max_idle_to` = 6 h here).
  - So the shield returns up to **~10.5 h** after the nominal end, and later in deep Doze.

    Example: a change at 20:00 and an alarm at 23:55 leave the arrow up until the next alarm, between 05:55 and 10:25. Opening the app ends it at once.
- **Reuse needs nothing new that wakes the device:** the centre slot reads the same `showPrivacyChange`. So no stop was needed.
  A punctual end would need a new alarm at midnight or at the expiry: that is Oliver's call, and it is not built.

## 2. What the mock-up does

- Sides at the widget's 16 dp padding on both edges (the released 24 dp card / 8 dp no-background insets removed).
  The score stays centred over the rating, and that group sits at the end edge.
- While the change is live, the shield goes invisible (its anchors stay) and a text view takes its 40 dp slot.
  Its text and size are chosen in code with `StaticLayout` at the update's font scale, so it fits the slot. The slot never grows.
- "pts" only when "↓6 pts" fits at the full 18 sp. Otherwise the number alone, shrunk only as far as it must.
- ↓ red, ↑ green (Oliver, mid-run; replaces "rating colour"). Spoken: "Score dropped / rose N point(s)".
- Halos as approved. The shield-outline thickening was not part of the mock-up.

## 3. Results (details in measurements)

- Nothing truncated or clipped in the centre. At font 1.0, "↓6", "↓12" and "↑6" show at full size, with no background too.
- **"pts" never fits on one line** in 40 dp, not even "↓6 pts" at font 1.0. So the "only when it fits" rule always drops it.
  As a second line at 0.6 size it fits at font 1.0 for ↓6, ↓12 and ↑6 with the number at full size. At 2.0 it doesn't fit.
- Font 2.0: "↓6" at 98 % of full size; "↓12" at 82 % on the card and 63 % with no background (the halo padding takes 22 px).
- Gaps at 2.0 are tightest beside the OS name: 56–69 px on the card, 32 px (12 dp) with no background, between halos.
- Implementation notes for a real build: round to whole pixels as the framework does (the cause of the one wrap, fixed in
  round 1). The height check also needs it: a 1 px overrun remains (106 against 105, single digit, no background, 2.0).

## 4. Oliver's choices

| Question | Options | Recommendation |
|---|---|---|
| Adopt the centre-change layout? | (a) yes, as mocked; (b) back to the round-3 plan (change at the end edge) | (a): nothing collides at 2.0, and score and rating no longer need reserved space |
| "pts" | (a) never: number only, as released, with "points" in the spoken text; (b) "pts" as a small second line when it fits (font 1.0) | (a): at 10.8 sp the unit is the smallest text on the widget, and it appears only at 1.0 |
| Side inset | (a) 16 dp widget padding on both edges (mocked); (b) keep the released 40 dp text inset | (a) is the literal reading of "exactly at the edges" |
| Shield return lag (up to ~10.5 h) | (a) accept: next update, as today; (b) a new alarm at midnight / at expiry (wakes the device) | (a) unless the lag bothers you |
