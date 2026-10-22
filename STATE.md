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
- [ ] **Widget readability + widget pack: implementation run** (`feat/widget-pack`, option B, Oliver 2026-10-22). Code and unit tests done;
      device gate waiting for the Pixel 8 (see Runs). External review applies.
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

- Widget pack: the defaults under "Oliver's calls" in `docs/specs/widget-pack.md` (no-background tints, slider snapping).

## Runs

### 2026-10-22 — widget readability + widget pack (`feat/widget-pack`, in progress)

- [x] Spec `docs/specs/widget-pack.md` (option B; minimum measured, not guessed).
- [x] Model (paper's tone model, all text 4.5:1, icon 3:1, photo/White/Black): the binding colours are the dark-theme tints
      (`#93D5AA`/`#A3CDDB`/`#FFB4AB`: 4.43 at 75 %, 4.58 at 76 %), then light teal/red (4.53 at 75 %). Model minimum: **76 %**.
- [x] Code: `WidgetStyle` (clamp on read, slider snap, tints), provider picks the card or no-background layout,
      `setColorInt` day/night pairs for score and change, stronger tokens, plurals "↑1 pt"/"↓6 pts" at 20sp bold, two-column layout
      with an auto-sizing change, `resizeMode` + vertical. `values-night-v31/colors.xml` deleted: all six entries unused
      (the v31 layouts referenced `system_*` tokens directly; the two change colours are now set in code).
- [x] `compileDebugKotlin` OK; `testDebugUnitTest --rerun`: 19 suites, 172 tests, 0 failures (`WidgetStyleTest`: 12 new).
- [ ] Device gate (Pixel 8): not run yet. `adb devices -l` listed no device on 2026-10-22.
      Plan: measurement build with the minimum temporarily at 70 (widgets at 70/75/76/100/0), then the final build.

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
