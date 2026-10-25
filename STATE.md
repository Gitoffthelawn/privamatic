# Privamatic — STATE

Task list and evidence log. The rules for keeping it are in CLAUDE.md. Keep this file under about 150 lines.
Older runs move to `docs/state-archive/YYYY-MM.md`.

## Current

Released version is v1.5.1 (versionCode 9). F-Droid serves it, and it is installed on the Pixel 9.
v1.5.2 (versionCode 10) is prepared on `release/v1.5.2` and merged into local `master` (2026-10-25); not pushed, not tagged.
The Pixel 8 runs the 1.5.2 debug build (versionCode 10), What's new already dismissed.
The Claude Code workflow files are merged into `master` with `--no-ff` and pushed. They are:
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

- [ ] [S] Widget update action is open to other apps: `PrivacyWidgetProvider` is exported with `ACTION_UPDATE_WIDGET` (full scan, battery only).
- [ ] [S] Re-arm the 6-hour widget alarm on app start and `MY_PACKAGE_REPLACED`. A force-stop clears both alarms; only the snapshot is
  re-armed (`PackageReplacedReceiver.kt:22`, `MainActivity.kt:26`). Pixel 8: 0 alarms after two force-stops, both back after a reboot;
  three `install -r` in a row kept both, so an update alone doesn't clear them. Whether an F-Droid update force-stops: not checked.
- [ ] [M] 48 h change cap edge: a score change again across midnight keeps the first change's timestamp, so the cap can hide a fresh
  change (`ScoreHistoryRepository.kt:93-97`). Code reading only; failing test first. Risky code (score history).
- [ ] [M] Snapshot alarm time: comments say 23:50, code sets 23:00 (`SnapshotReceiver.kt:69`); the inexact daily alarm has an 18 h
  window on the Pixel 8, so a "midnight" snapshot can land at 17:00 next day. Read-only finding. Risky code (score history).
- [ ] [M] Widget opacity on Android 8–11 does nothing (review round 1 M1, rejected for this release by Oliver). `setImageAlpha` acts on
  the ImageView's image, but the card is its background (also on `master`). Card draws opaque, so readable; fix needs an API 30 emulator.
- [ ] [M] Legacy-rules and real-D2D check (optional): same bmgr cycle on an API 30 emulator, plus one phone-to-phone transfer.
- [ ] [S] device-verify ran its sweep from a scratchpad script, which hides each adb call from the guard hook (commands matched, Pixel 8
  only). Consider adding "run commands inline, never from a script file" to the agent file.
- [ ] [S] device-verify can't restore a named wallpaper: the picker has no recents list. Reapplying the lock screen's photo to Home works
  while the lock screen still has it.

## Blocked on me

- Push `master` and tag v1.5.2 on the release merge commit (commands in the 2026-10-25 release run report).

## Done (moved from Next, 2026-10-25)

- [x] [S] Default Keyboard row strings moved to `strings.xml`; `.gitignore` covers `docs/store/`, `docs/store-graphics/`, `docs/audits/` (2026-10-25, run below).
- [x] [S] Step 1b — trial fix: Default Launcher row debug text. See archive.
- [x] [S] Same leak in browser, SMS, email rows (2026-10-21, `666a434`).
- [x] [S] `QuickWinsDetector.extractAppName` parsed UI copy; replaced by `PrivacyIssue.appName` (`666a434`).
- [x] [M] Guard hook (`015f699`); see Guard hook below.
- [x] [S] Guard hook live smoke test (2026-10-20): all 11 checks as intended (5 deny, 3 ask, 1 silent allow on the Pixel 9);
  `.claude/hook.log` had 8 lines, one per deny and ask. Closes the workflow-setup "rules match in a live session" item.
  Side effect: `adb devices` starts the adb daemon, so a "first" adb call is never fully read-only on the host.
- [x] [M] Backup-rules fix (`87b6f78`, external review round 1: no findings). The four suspicions:
  - Missing `.xml` was not a bug: `ad_id_prefs.xml` and `score_history.xml` already stayed out of backup (device, before-run).
  - `-shm` / `-wal` moot: the database domain is excluded and Room uses TRUNCATE mode.
  - `<cloud-backup>` and `<device-transfer>` fixed, mirrored, enforced by `BackupRulesTest`.
  - Room was already excluded with `domain="database"` (device, before-run).
- [x] [S] Widget readability design run: `docs/decisions/widget-readability.md`; see archive.
- [x] [L] Widget readability + widget pack (`feat/widget-pack`, merged `3f5a553` 2026-10-24). Rounds 1–2 parked; round 3 gate clean.
  "↓12" at font 2.0 fits 40 dp; `.gitignore` tracks `app/src/release/`; 28 dp sides, 18 sp score, rows aligned at 1.0 and 2.0.
  External review round 1: no High; L1 fixed (`e2093fc`), M1 rejected for this release.
- [x] [S] Release note for the backup change, agreed by Oliver 2026-10-21; use exactly:
  "Privamatic no longer copies any of its data into backups or to a new phone. On a new phone, set up your trusted apps and settings again."
- [x] [S] Koloe17 report closed: resolved by #19 in 1.4.1; no newer report exists (Oliver, 2026-10-25).
- [x] [S] Spec "Out" line corrected: a font-scale change does trigger widget updates (2026-10-24).

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

- None open. (Device name at large text: Oliver accepted the ellipsis; added to the spec 2026-10-24.)

## Runs

### 2026-10-25 — release run v1.5.2 (`release/v1.5.2`, merged into local `master`)

- [x] Version 1.5.1 / 9 → 1.5.2 / 10. Changelog `changelogs/10.txt`: Oliver's four lines verbatim, "-" bullets as in `9.txt`; 456 characters (limit 500).
- [x] What's new: one release's notes at a time, keyed by `WhatsNew.NOTES_INTRODUCED_IN` (was 8, so 1.5.0/1.5.1 dismissers would see nothing).
      Now 10 with the same four lines as strings (card draws the bullet). 4 new tests on the shipped values: update from 8, 9 or 1.4.1 shows; fresh install doesn't.
- [x] Gates: `compileDebugKotlin` OK; `testDebugUnitTest --rerun` 189 tests, 0 failures; `assembleRelease` OK, `app-release-unsigned.apk`,
      badging versionCode 10 / 1.5.2, `signingConfig = null`, no signature files. Release dex has none of `debug_delta`, `debug_opacity`,
      `debug_device_name`, `debug_clear`, `widget_debug_override` (debug dex has them, so the check works).
- [x] Pixel 8, `install -r` over debug 1.5.1 (last seen 9): card "What's new in 1.5.2" showed with all four lines; "Got it" stored 10;
      Back + `am start` (new activity, no force-stop) showed no card. Widget rendered "Pixel 8 / Stock Android, ↑3, 53 Fair". Crash buffer empty.
      Restored: app drawer open, font scale 1.0 (drawer scroll position differs). Note: `install -r` itself ends the old process.
- [x] "Switch it off completely" holds on Android 8–11: no background uses its own layout (`PrivacyWidgetProvider.kt:172-176`). Code reading only.
- [x] Open issues #6, #7, #11: none addressed by 1.5.2. No external review: version, copy and notes only.
- [x] Store texts (Oliver accepted both proposals): `full_description.txt` widget line 25 and backups line 30 replaced as written; merged into `master` again.

### 2026-10-25 — keyboard strings and .gitignore (`chore/keyboard-strings-gitignore`)

- [x] Keyboard row: "Using %s" reuses `fmt_default_app_using`; new `status_no_default_keyboard`, `status_default_keyboard_unknown`, `fmt_technical_ime`; error detail reuses `fmt_technical_error`. Wording unchanged, copy voice OK.
- [x] Scoring: `compileDebugKotlin` OK; `testDebugUnitTest --rerun` 185 tests, 0 failures (includes `DefaultAppsCheckerScoringTest`).
- [x] Device (Pixel 8, device-verify, no force-stop): row text identical before and after ("Using HeliBoard", "IME: helium314.keyboard/.latin.LatinIME"); crash buffer empty. Image: `docs/screens/2026-10-25-keyboard-row/before-after.png`. Not verified: the "No default keyboard set" and error paths (no way to set them on the device).
- [x] `git check-ignore`: all three folders and `ROADMAP.md` ignored. `git log --all -- docs/audits ROADMAP.md` empty; nothing tracked there.

### 2026-10-25 — project-tracker contract (`chore/tracker-contract`, merged into local `master`)

- [x] `ROADMAP.md` added from the tracker draft, reconciled against STATE, git log, GitHub and code; gitignored (`git check-ignore` → `.gitignore:66:/ROADMAP.md`).
- [x] STATE `## Next` in contract format, `## Blocked on me` added, done items moved to `## Done`; "not pushed" lines corrected (`origin/master` = `3f5a553`).
- [x] CLAUDE.md end-of-run rule points at ROADMAP.md and the contract. No code changes, so no build gate.

Earlier runs of this feature (2026-10-24 review round 1 and merge, build rounds 2–3, 2026-10-23, 2026-10-22 rounds A–D, 2026-10-21 design run): `docs/state-archive/2026-10.md`.
