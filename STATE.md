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
`.claude/settings.local.json` is untracked since `c837e59`, so a `git pull` on the Framework laptop, or a switch to any
pre-`c837e59` branch, overwrites or deletes it. Back it up first. The guard hook now denies such switches.

## Next

- [x] **Step 1b — trial fix: Default Launcher row debug text.** Done; see Runs.
- [ ] **Same leak in three more rows.** `DefaultAppsChecker.kt` lines 99 (browser), 169 (SMS) and 304 (email) still set
      `"Using $appName (unknown, 0 pts)"` as hardcoded English. Same fix as the launcher row.
- [ ] **`QuickWinsDetector.extractAppName` parses UI copy** (`removePrefix("Using ")`). This breaks once the
      `Using …` statuses are localised. Pass the app name as a field instead.
- [x] **Guard hook.** Done; see Runs and Guard hook below.
- [x] **Guard hook live smoke test** (2026-10-20, live session): all 11 checks matched as intended: 5 deny, 3 ask,
      1 silent allow on the Pixel 9. `.claude/hook.log` had 8 lines afterwards, one per deny and ask.
      This also closes "allow and deny rules match as intended in a live session" from the workflow-setup run.
      Side effect: `adb devices` starts the adb daemon if it isn't running. That is harmless, but it means a
      "first" adb call is never fully read-only on the host.
- [ ] **Backup-rules fix.** Gate: run `bmgr` on the Pixel 8 before and after the change. Check that:
  - [ ] the `sharedpref` excludes `ad_id_prefs` and `score_history` lack `.xml` and may match nothing;
  - [ ] `backup_rules.xml` excludes `-shm` / `-wal` (it doesn't yet);
  - [ ] `data_extraction_rules.xml` (Android 12+, both `<cloud-backup>` and `<device-transfer>`) gets the same fix;
  - [ ] the score-history chart lives in Room (`privacy_history.db`), so its exclude must use `domain="database"`, not `sharedpref`.
        The `score_history` sharedpref holds only the daily baseline and delta.

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

None open.

## Runs

### 2026-10-20 — guard hook (`chore/guard-hook` → `master`)

- [x] Hook contract confirmed from the official docs; see Guard hook.
- [x] `.claude/hooks/guard.sh` (bash + jq) and `scripts/test-hook.sh`.
      Evidence: `SUMMARY: 224 passed, 0 failed, 224 cases`. That covers every rule in plain and evasive forms, Edit/Write, fail-closed (no `jq`, bad JSON, missing script) and logging.
- [x] `.claude/settings.json` (tracked) written after Oliver's OK. It registers the hook, sets `"disableAllHooks": false` and holds the deny list (41 entries).
  - `git tag`, `git branch -D` and `git commit --amend` were dropped from the list, because the hook is the precise version of each.
  - The tests prove they are still covered.
- [x] `.claude/settings.local.json`: the allow list only (20 entries). Both files parse with `jq empty`. `.gitignore` adds `.claude/hook.log`.
- [x] STATE.md: the three oldest runs moved to `docs/state-archive/2026-10.md`.
- [ ] Not verified live: the hook in a real Claude Code session. Hooks load on a new session, and Oliver runs the smoke test (Next).
      No device commands were run; the adb rules are tested only through `test-hook.sh`.

### 2026-10-20 — Default Launcher row copy (`fix/launcher-row-copy` → `master`)

- [x] Investigation: the exact reported text, "Using **Pixel Launcher** (unknown, 0 pts)", has not occurred since v1.5.0.
  - `f8221e1` classifies `com.google.android.apps.nexuslauncher` as invasive, so the row shows "Using Pixel Launcher" with −2.
  - Evidence: `git tag --contains f8221e1` → v1.5.0, v1.5.1.
  - The leak was still live on `master` for any launcher on neither list (`DefaultAppsChecker.kt:378`), so I fixed it.
- [x] Fix: the unknown branch now uses `fmt_launcher_not_rated` ("Using %s — not rated yet") via `context.getString`.
      The row's Info icon and missing chip already say "unknown" and "no deduction".
- [x] Gate: `compileDebugKotlin` OK; `testDebugUnitTest --rerun`: 16 suites, 146 tests, 0 failures; `assembleDebug` OK.
- [x] Device, Pixel 8: a throwaway resource-only HOME stub (`org.example.teststub.home`, label "Test Home", built in the scratchpad) was set as the HOME role holder.
  - Cold start → Details → Default Apps shows "Default Launcher / Using Test Home — not rated yet", with the Info icon and no chip.
  - Evidence: `docs/screens/2026-10-20-launcher-row-not-rated.png` and a `uiautomator` dump.
  - Afterwards the HOME role was set back to Pixel Launcher and the stub uninstalled; `resolve-activity` confirms.
  - Settings `FallbackHome` was tried first and doesn't work: at priority −1000, `resolveActivity` ignores it.
- [x] Diff reviewed against `master`: no blockers. The launcher Quick Win reads `currentStatus` only when `!isSecure`, and the unknown branch is secure.
- [ ] Not verified: the stock Pixel Launcher row with this build. That branch is unchanged.

### 2026-10-20 — STATE and CONVENTIONS fixes (`chore/state-and-conventions` → `master`)

- [x] STATE settings-file note corrected: a pull *and* a switch to any pre-`c837e59` branch overwrite or delete the untracked file.
      The guard-hook item gains a switch check.
- [x] CONVENTIONS.md Design Constraints gains two Compose rules from the deleted `docs/QUICK_REFERENCE.md`:
  - `Modifier` is the last parameter, with a default;
  - composables are stateless, with state hoisted.

  Evidence: `git show d1fb5ce^:docs/QUICK_REFERENCE.md`, lines 15, 26 and 28.
- [x] Docs only, so there is no build gate. Diff reviewed against `master`: no blockers. Merged with `--no-ff`.
