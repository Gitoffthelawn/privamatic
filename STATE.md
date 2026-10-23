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
- [ ] **Widget readability + widget pack: implementation run** (`feat/widget-pack`). **Parked 2026-10-23** after the second
      layout fix round. Next = **round 3**, decisions already made (Oliver, 2026-10-23):
  - [ ] Centre the truncated rating in its box (score and "Need…" share a centre at font 2.0).
  - [ ] Let the no-background score's halo draw past the widget padding (no flat top at font 2.0).
  - [ ] Halos approved as they are. Thicken the no-background shield outline if it's a small change, and measure it (now worst 5 % 1.2–2.1 in the wrong variant).
  - [ ] "↓6 pts" at font 1.0 when it fits whole next to the rating; the unit drops only when the end half is too narrow. If it can't fit
        whole at 1.0, show Oliver the measurement instead. Update the spec's "Change text" call and the changelog line to match.
  - [ ] Device: a two-digit change and an ↑ change; the device-name and OS halos hugging their text (wrapped in source, never seen on a device).
  - [ ] Clean → regenerate `docs/reviews/feat-widget-pack.diff` and the round-1 prompt (invariants: no-background text colour from the
        wallpaper hint, never from pixels near the widget; score number and arrow never truncated) and stop at "external review".
        Not clean → park again. **No round 4 without Oliver.**
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

- None open; the widget pack's next steps are decided (Next, round 3).

## Runs

### 2026-10-23 — widget pack resumed (`feat/widget-pack`, parked)

- [x] Oliver's decisions (2026-10-23): no-background text stays readable in the "wrong" colour variant (≥ 3:1 against its halo);
      the layout is the released v1.5.1 one from his screenshot (positions and sizes); the change shows "↓6" without "pts" (chosen
      over keeping "pts", which collides with a centred score); score and arrow are never cut. Spec updated (calls, scope, invariant 7, changelog line).
- [x] Halo: a model of TextView shadows (calibrated: today's shadow gives round A's 1.4–1.6) shows no shadow, even 3 stacked, reaches 3:1
      for the tints. So each no-background text sits on a rounded halo of the opposite colour at 65 % (`widget_text_halo*`).
      Device: every text ≥ 4.12 against its halo (worst 5 %) on White, Black and the photo in both variants; table in
      `docs/screens/2026-10-23-widget-pack/measurements.md`. The shield's thin outline: worst 5 % 1.2–2.1 in the wrong variant (median ≥ 8.2).
- [x] Refresh after a wallpaper change: nothing cheap exists. Providers get no wallpaper callback (`javap` android-37.2), `ACTION_WALLPAPER_CHANGED`
      can't reach a manifest receiver, and no framework theme attribute carries the wallpaper's dark-text hint for `setColorAttr`. Not built.
- [x] Layout: released RelativeLayout geometry; score and rating form one group between two invisible copies of the change
      (`widget_delta_space_*`) in a row laid out in the reversed direction (`@integer/widget_reversed_layout_direction`, ltr under `-ldrtl`),
      so the end-side copy is measured first and the shield-side copy shrinks first. Arrow strings back to `fmt_widget_delta_*` ("↑%d"); plurals removed.
- [x] Device round 1 (layout I built first, then rows per text): Gboard as keyboard → 53 → 44 and "↓9" red on the card, pink on the halo; keyboard restored.
      Score and rating drifted apart, the arrow's halo was a tall box, and "Needs Attent…" was cut with no background: fixed in round 2.
- [x] Device round 3 (one group, wrapped arrow, 8 dp no-background margin): font 1.0 as the screenshot (text 41 dp in both sides, centres within 1 px,
      "Needs Attention" whole beside "↓6"/"↓9"); font 2.0: score and arrow whole, no overlap, "Stock A…"/"Need…". Two open details (Open questions).
- [x] After round 3, in source only: device-name and OS halos were stretched to the shield (pinned at both edges); now wrapped (test added). **Not device-checked.**
- [x] `compileDebugKotlin` OK; `testDebugUnitTest --rerun`: 19 suites, 182 tests, 0 failures.
- [x] Restored (both rounds): test widgets removed, photo wallpaper (home only), font 1.0, light theme, Private DNS unset, keyboard HeliBoard;
      prefs written back (score_history's `last_update_timestamp` then moved on with the final update). Oliver's widget 22 now shows the new layout
      (the debug build changed). Crash buffer empty.
- [ ] Not verified: a two-digit change on the device; ↑ in the new layout; API 26–30.

Earlier runs of this feature (2026-10-22 rounds A–D, 2026-10-21 design run): `docs/state-archive/2026-10.md`.
