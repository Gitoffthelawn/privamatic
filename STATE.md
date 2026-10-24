# Privamatic — STATE

Task list and evidence log. The rules for keeping it are in CLAUDE.md. Keep this file under about 150 lines.
Older runs move to `docs/state-archive/YYYY-MM.md`.

## Current

Released version is v1.5.1 (versionCode 9). F-Droid serves it, and it is installed on the Pixel 9.
The Pixel 8 runs a debug build of `feat/widget-pack` (centre change, build round 3, committed 2026-10-24; waiting for external review).
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
- [x] **Widget readability: design run.** Paper `docs/decisions/widget-readability.md`; see the archive.
- [ ] **Widget readability + widget pack: implementation run** (`feat/widget-pack`). **Parked 2026-10-23** after the second
      layout fix round.
  Round 3 was replaced by the centre-change design run. Build rounds 1 and 2 parked; **build round 3 gate clean** (2026-10-24; see Runs).
  - [x] "↓12" at font 2.0 fits the 40 dp box. `.gitignore` tracks `app/src/release/`. 28 dp sides, 18 sp score, rows aligned at 1.0 and 2.0.
  - [ ] **External review round 1** (STOP): Oliver runs `docs/reviews/feat-widget-pack-prompt-1.md` in Cursor and saves the findings.
- [ ] **Snapshot alarm time:** comments say 23:50, the code sets 23:00 (`SnapshotReceiver.kt:69`), and the inexact daily alarm has an
      18 h window on the Pixel 8, so a "midnight" snapshot can land as late as 17:00 the next day. Read-only finding; not changed.
- [ ] **48 h change cap edge:** if the score changes again across midnight, the first change's timestamp is kept, so the cap can hide
      a fresh change (`ScoreHistoryRepository.kt:93-97`). From reading the code only; not reproduced.
- [x] **Spec "Out" line** corrected: a font-scale change does trigger widget updates (2026-10-24).
- [ ] **Widget opacity on Android 8–11 probably does nothing** (`setImageAlpha` on a background-only `ImageView`). Hypothesis; needs an API 30 emulator.
- [ ] **device-verify ran its sweep from a scratchpad script**, which hides each adb call from the guard hook. The commands matched the prompt, Pixel 8 only.
      Consider a line in the agent file: "run commands inline, never from a script file".
- [ ] **The 6-hour widget alarm only comes back on boot or a new widget.** A force-stop clears both alarms; opening the app and
      `MY_PACKAGE_REPLACED` re-arm only the midnight snapshot (`PackageReplacedReceiver.kt:22`, `MainActivity.kt:26`). Seen on the Pixel 8
      (0 alarms after two force-stops and an install, both back after a reboot). 2026-10-24: three `install -r` in a row kept both alarms,
      so on the Pixel 8 an update by itself does **not** clear them; the force-stops did. Whether an F-Droid update force-stops: not checked.
- [ ] **device-verify can't restore a named wallpaper**: the picker has no recents list. Reapplying the lock screen's photo to Home works
      while the lock screen still has it.

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

- **Device name at font 2.0:** a long name ellipsizes ("Galaxy S8" → "Galaxy …" on the card, "Galaxy…" with no background). It always
  could (`ellipsize="end"`); the spec names the ellipsis only for the OS name and rating. Accept and add it to the spec, or not?

## Runs

### 2026-10-24 — widget centre change, build round 3 (`feat/widget-pack`, **gate clean**)

- [x] Oliver approved the 28 dp sides and the 18 sp bold score, and chose option i: each side's two lines centred as one block
      (device over OS in one column; score over rating aligned to its baseline). The centre-line anchor is gone.
- [x] Debug-only `debug_device_name` extra (debug source set + release no-op + one provider line), for the descender check.
- [x] `compileDebugKotlin` OK; `testDebugUnitTest --rerun`: 184 tests, 0 failures (new: the start column test).
- [x] **Gate (device-verify, Pixel 8, 32 sweep shots + C, C2, D):** both baseline pairs have identical node bounds in every shot at
      font 1.0 and 2.0 (f1 506–569 / 575–626, f2 468–574 / 580–671 px); flat glyph edges within 0.1 px, round overshoot ≤ 0.8 px.
      All 24 changes inside 105 × 105, log = launcher 24/24. Nothing clipped; only the OS name (and "Needs Attention") ellipsizes at 2.0.
      Smallest centre–side gap 12.2 dp (bounds) / 15.6 dp (ink, "Needs At…" beside ↓12).
- [x] Distance from the card edges to text ink: font 1.0 top 30.5 / bottom 29.3 dp (sides 29.0 / 28.6); font 2.0 top 19.0 / bottom 16.4 dp
      (the blocks sit 25 px from both edges; the ink difference is glyph shape).
- [x] "Galaxy S8" at 2.0: "y" whole (5.3 px of card or halo below it), but the name ellipsizes (Open questions). My prompt's unquoted
      `--es` value reached the device as "Galaxy"; the agent's quoted repeat (`"'Galaxy S8'"`) delivered the full name.
- [x] Dark-text layout on White: dark text and shield in all 9 states. Restored: font 1.0, light, Private DNS unset, widget 22
      `88.328766` (`cmp` identical), override `<map />`, both alarms; wallpaper crop hints as before, only the clock differs outside the widget.
      `score_history`: only `last_update_timestamp` moved. Crash buffer empty, same pid. Pixel 9 not used.
- [x] For Oliver: `centred-block-f1-bg100-d6.png`, `centred-block-f2-bg100-d6.png` in `docs/screens/widget-centre-change/`.

Earlier runs of this feature (2026-10-23, 2026-10-22 rounds A–D, 2026-10-21 design run): `docs/state-archive/2026-10.md`.
