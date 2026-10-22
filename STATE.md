# Privamatic — STATE

Task list and evidence log. The rules for keeping it are in CLAUDE.md. Keep this file under about 150 lines.
Older runs move to `docs/state-archive/YYYY-MM.md`.

## Current

Released version is v1.5.1 (versionCode 9). F-Droid serves it, and it is installed on the Pixel 9.
The debug build of v1.5.1 is installed on the Pixel 8.
The Claude Code workflow files are merged into local `master` with `--no-ff`. They are not pushed yet; Oliver pushes. They are:
- CLAUDE.md and STATE.md;
- the narrowed `.claude/settings.local.json` (untracked; it now holds only the allow list);
- the inventory.

CONVENTIONS.md is the single rulebook: `.cursorrules` now points to it, and the duplicate docs are deleted.
The guard hook is merged into local `master` and passed its live smoke test (see Next).
`master` now also has a spec template, a `device-verify` subagent and an external-review step (CLAUDE.md), all merged from `fix/backup-exclusions`.
`device-verify` is registered only in sessions started after it was committed. Until then, a general-purpose agent reads its file.
`.claude/settings.local.json` is untracked since `c837e59`, so a `git pull` on the Framework laptop, or a switch to any
pre-`c837e59` branch, overwrites or deletes it. Back it up first. The guard hook now denies such switches.

## Next

- [x] **Step 1b — trial fix: Default Launcher row debug text.** Done; see Runs.
- [x] **Same leak in three more rows** (browser, SMS, email). Done 2026-10-21; see Runs.
- [x] **`QuickWinsDetector.extractAppName` parses UI copy.** Replaced by `PrivacyIssue.appName`; see Runs.
- [ ] **Default Keyboard row copy is still hardcoded English** ("Using …", "No default keyboard set", "IME: …").
      It was out of scope for `fix/default-app-copy`. It has no unknown branch, so it doesn't leak.
- [x] **Guard hook.** Done; see Runs and Guard hook below.
- [x] **Guard hook live smoke test** (2026-10-20, live session): all 11 checks matched as intended: 5 deny, 3 ask,
      1 silent allow on the Pixel 9. `.claude/hook.log` had 8 lines afterwards, one per deny and ask.
      This also closes "allow and deny rules match as intended in a live session" from the workflow-setup run.
      Side effect: `adb devices` starts the adb daemon if it isn't running. That is harmless, but it means a
      "first" adb call is never fully read-only on the host.
- [x] **Backup-rules fix.** Merged into local `master` with `--no-ff` after external review round 1 (no findings); see Runs. The four suspicions:
  - [x] The missing `.xml` was **not** a bug: on `master`, `ad_id_prefs.xml` and `score_history.xml` stayed out of backup (device, before-run).
  - [x] `-shm` / `-wal`: moot. The whole database domain is now excluded, and Room uses TRUNCATE mode, so the files never exist.
  - [x] Both `<cloud-backup>` and `<device-transfer>` are fixed, mirrored, and enforced by `BackupRulesTest`.
  - [x] Room was already excluded with `domain="database"` on `master` (device, before-run).
- [ ] **Legacy-rules and real-D2D check** (optional): run the same bmgr cycle on an API 30 emulator, and do one phone-to-phone transfer.
- [ ] **Release note for the backup change** (Oliver agrees the text): trusted apps and settings no longer move to a new phone.
- [x] **Widget readability: design run.** Paper `docs/decisions/widget-readability.md`; see Runs.
- [ ] **Widget readability + widget pack: implementation run** (`feat/widget-pack`). **Paused 2026-10-22** on the font-scale-2.0
      layout question (Open questions). Then: regenerate the review diff and prompt, and stop at external review (see Runs).
- [ ] **Widget opacity on Android 8–11 probably does nothing** (`setImageAlpha` on a background-only `ImageView`). Hypothesis; needs an API 30 emulator.
- [ ] **device-verify ran its sweep from a scratchpad script**, which hides each adb call from the guard hook. The commands matched the prompt, Pixel 8 only.
      Consider a line in the agent file: "run commands inline, never from a script file".

## Guard hook

`.claude/hooks/guard.sh` is a PreToolUse hook, registered in the tracked `.claude/settings.json` for `Bash` and for `Edit|Write|MultiEdit|NotebookEdit`.
It is the real guard. The allow and deny lists are a convenience.
- **Silent by default.** It outputs a decision only for the patterns below; anything else (heredocs, long scripts) gets none.
- **It sees through:** `&&`, `||`, `;`, `|`, subshells, `$(...)` and backticks, `bash -c` / `sh -c` / `eval`, quotes, env prefixes,
  `command`/`env`/`exec`/`nohup`/`timeout`/`xargs`, absolute paths, `git -C`/`-c`, git aliases, `find -exec`, heredoc lines and `adb shell "..."` strings.
- **Deny:**
  - git: `push` (any form), tag create or delete, `clean -f`, and `reset --hard` / `commit` / `merge --continue` / `cherry-pick` / `revert` / `am` while `master` is checked out;
  - a switch or checkout to anything without `c837e59` (resolved locally, then as `origin/<x>`), and `--orphan`;
  - gh writes, `ssh`/`scp`/`rsync`/`sftp`, `sudo`/`su`;
  - adb without `-s`, except `devices [-l]`, `start-server`, `kill-server`, `version`;
  - on the Pixel 9, a variable serial or an unknown serial: data/package/settings changes (`pm`, `cmd`, `rm`, `run-as`, `settings`, `input`, `am` writes, `push`, `uninstall`, install without `--user 0`, `logcat -c`, device-side redirects);
  - Gradle `install*`/`uninstall*`/`connected*` without `ANDROID_SERIAL=38041FDJH001MA`;
  - keystore reads, `keytool`, and printing `*PASSWORD*`/`*KEY*`/`*SECRET*`/`*TOKEN*` variables or the whole environment.
- **Allow silently:** Pixel 9 `exec-out screencap -p > *.png`, `shell getprop [prop]`, `shell dumpsys package <debug>`,
  `shell pidof <debug>`, `logcat --pid=…`, optionally piped into `grep`/`head`/`tail`/`wc`. This applies only when every part of the command is on that list; otherwise ask.
- **Ask:**
  - any other Pixel 9 or unknown-serial command;
  - `git branch -D`, or a switch target it can't resolve;
  - unknown `gh` subcommands;
  - Bash writes to `.claude/settings*.json` or `.claude/hooks/**`;
  - Edit/Write on those paths, or on Gradle files whose new text contains `signingConfig`.
- **Fail closed:** missing `jq`, bad JSON or a script error exit 2, which blocks. The hook command ends in `|| exit 2`, so a missing `guard.sh` blocks too.
- **Log:** every deny and ask goes to `.claude/hook.log` (git-ignored) as timestamp, decision, rule id and the first 200 characters of the command.
- **Test:** `bash scripts/test-hook.sh` prints one line per case and exits non-zero on any failure. It needs no device.
- **Hook contract** (code.claude.com/docs/en/hooks, via Context7, 2026-10-20):
  - **Input:** JSON on stdin with `tool_name`, `tool_input` (`command`, `file_path`, …) and `cwd`.
  - **Output:** `hookSpecificOutput.permissionDecision` is `allow`, `deny`, `ask` or `defer`, with a `permissionDecisionReason`. Precedence is deny > defer > ask > allow.
    Settings deny and ask rules still apply over a hook `allow`.
  - **Exit codes:** JSON is read only on exit 0. Exit 2 blocks, and its stderr goes to Claude. Any other exit code is a non-blocking error, and the call goes ahead; hence the trap and `|| exit 2`.
  - **Loading:** settings files are watched, so hook edits normally apply mid-session. `/hooks` is a read-only browser.
    Hooks run only after the workspace trust dialog is accepted. To be sure, start a new session after changing them.
  - **Auto mode:** a hook `ask` still shows the prompt. The classifier can deny the call but cannot approve it silently.
    Before v2.1.211 it could approve an unsandboxed Bash command. Installed is 2.1.304, so no ask rule needs to become deny.

## Open questions for Oliver

- **Widget layout at font scale 2.0** (blocks the widget pack). Oliver wants the shield centred in the card (2026-10-22). Round D on the
  Pixel 8 (4×1 widget, 980 px wide):
  - Normal size: shield centre 0.2 dp from the card centre; start and end inner margins 24.4 / 24.4 dp; nothing cut.
  - Font 2.0: each half is about 135 dp. Shown: "53 ↑6 …", "47 ↓6 …", "Needs Att…", "Stock And…" (end margin 44 / 29 dp, because the
    group fills its half). Nothing clipped vertically, no overlap. TalkBack still reads the full text.
  - Options: (1) keep the centred shield and accept "…" at 2.0 (recommended); (2) shield 16dp before the score group (round C layout:
    at 2.0 the score group fits, the device name takes the "…", e.g. "Pix…" with Needs Attention); (3) centred shield, score row and
    rating shrink to fit their half: the group then starts right after the shield at every size, and the change colour needs another
    technique (one more device round).

## Runs

### 2026-10-22 — widget readability + widget pack (`feat/widget-pack`, paused)

- [x] Spec `docs/specs/widget-pack.md` (option B; minimum measured, not guessed).
- [x] Model (paper's tone model, all text 4.5:1, icon 3:1, photo/White/Black): the binding colours are the dark-theme tints
      (`#93D5AA`/`#A3CDDB`/`#FFB4AB`: 4.43 at 75 %, 4.58 at 76 %), then light teal/red (4.53 at 75 %). Model minimum: **76 %**.
- [x] Code: `WidgetStyle` (clamp on read, tints), card or no-background layout, `setColorInt` day/night pairs for score and change,
      stronger tokens, plurals "↑1 pt"/"↓6 pts" at 20sp bold, `resizeMode` + vertical. `values-night-v31/colors.xml` deleted
      (all six entries unused).
- [x] Device, Pixel 8, round A (device-verify, fresh APK, a measurement build with the minimum temporarily at 70, never committed):
      widgets 17–21 at 70/75/76/100/0 on photo/White/Black × light/dark.
  - 75 % fails: the dark Fair score `#A3CDDB` on White reaches 4.43. At 76 % everything passes; the weakest is that score at 4.57 (model 4.58).
    The shield is now ≥ 4.57 everywhere. The launcher switched every colour on theme and wallpaper changes with no widget update.
  - No background: Black 21:1; photo 3.0–4.0 against the shadow ring; **White 1.4–1.6 (fails)**.
- [x] Device, round B (final build): stored 40/75/76 all draw the identical 76 % card, and the settings screen shows "Opacity: 76%".
      47 Needs Attention in red with "↓6 pts", 53 Fair in teal with "↑6 pts" in green, in both themes (Private DNS `off`, then unset).
      Font scale 2.0: score and change 21 px apart, nothing clipped. Crash buffer empty.
- [x] Restored: test widgets removed (old invisible ids 14 and 15 left alone) and wallpaper back to the photo
      (page 1 pixel-identical; page 2 differs only in a 59×16 px spot at the page indicator).
      Light theme, font 1.0, Private DNS unset. The debug app's widget and score-history prefs were rewritten from their pre-run copies.
      The lock-screen wallpaper was never changed.
- [x] Evidence: spec Measurements, `docs/screens/2026-10-22-widget-pack/` (measurements.md, 3 cropped images, 224 KB; raw shots stay local).
- [x] Oliver's decisions 2026-10-22 (in the spec): no background must pass on light wallpapers via the wallpaper hint; a "Show background"
      switch + 76–100 % slider; agreed changelog; layout with the score group at the end, then the shield centred in the card.
- [x] Platform facts (SDK `api-versions.xml`, `javap`): the dark-text hint is public from API 31 (= 1); `ACTION_WALLPAPER_CHANGED`
      can't reach a manifest receiver, so a new wallpaper shows at the next widget update. Below API 31: light text with shadow.
- [x] Code: dark-text no-background layout (`#1C1B1F`, light glow, light-theme tints) chosen from `HINT_SUPPORTS_DARK_TEXT`;
      `WidgetStyle.Settings` / `settingsFor` / `storedOpacity` behind the switch + slider; centred-shield layout (applied after round C).
- [x] `compileDebugKotlin` OK; `testDebugUnitTest --rerun`: 19 suites, 177 tests, 0 failures (final source).
- [x] Device round C (dark text, settings): **White now passes** (dark text 17.1, teal score 9.4, shield ≥ 8.9); Black 21. The photo stays weak
      (no hint; white text 1.7 on a pale patch, 2.3–2.8 against the shadow). The glow changes nothing on White (17.13 either way), never
      measured worse: kept. The text colour follows a new wallpaper only after the next update (seen on White and Black).
      Switch off/on, slider 76–100, both Save paths and the 40 → 76 % clamp all pass. Round C ran with the interim layout.
- [x] Device round D (centred layout): measurements in Open questions. Font scale 2.0 cuts text (open question).
- [x] Pixel 8 at the end (2026-10-22, read-only check): font scale 1.0, light theme, Private DNS unset, home wallpaper the mountain photo,
      test widgets removed. Oliver's own widget 22 (page 1) kept, its setting rewritten as 100 % and refreshed. The stale ids 14 and 15
      disappeared during round C on their own. Round C's agent once joined two read-only `cat` calls with `;`.
- [ ] Open: the font-2.0 decision; then regenerate `docs/reviews/feat-widget-pack.diff` and the round-1 prompt (add the invariant
      "no-background text comes from the system wallpaper hint, never from widget-local pixels"), and stop at external review.
- [ ] Not verified on a device: API 26–30 (no API 30 emulator) and green as a *score* tint (it was measured as the ↑ colour).

### 2026-10-21 — widget readability design run (`docs/widget-readability` → `master`)

- [x] Code: on API 31+ the text and icon use fixed Material You tones picked for the card at 100 %. The slider changes only the card's alpha.
      So the blended background moves through the text's own tone, and contrast dips in the middle of the range.
- [x] Device, Pixel 8 (device-verify, fresh APK): widget id 16, 3 wallpapers (the phone's photo, White, Black) × light/dark × 0/25/50/75/100 %.
      Opacity was written via `run-as`, then `ACTION_UPDATE_WIDGET`. Crash buffer empty.
  - Only 100 % passes for all text. The shield fails everywhere (2.0:1 light, 2.9:1 dark at 100 %).
  - Mismatched theme and wallpaper fail from 0 % up to 50–75 %. The dark-theme "↓" is olive/lavender, not red.
  - The measured 100 % values match the tone model to within 0.1, and `floors.py` gives the floors.
  - Evidence: `docs/screens/2026-10-21-widget-readability/` (cropped sweeps, `contact-sheet.png`, `measurements.md`, `floors.md`, both scripts).
- [x] Restored: night mode "no". The wallpaper is Floating Himalayas again: crop hint identical, wallpaper area pixel-identical to the baseline (`c1`/`c7`).
      The test widget was removed. Two older invisible instances (ids 14, 15) were left alone. The debug app's stale `widget_opacity_12` key was overwritten.
- [ ] Not verified: the ↑ (green) delta (the score only fell), and the Android 8–11 path (no API 30 emulator).
