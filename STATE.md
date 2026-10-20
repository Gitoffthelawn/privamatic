# Privamatic — STATE

Task list and evidence log. The rules for keeping it are in CLAUDE.md. Keep this file under about 150 lines.
Older runs move to `docs/state-archive/YYYY-MM.md`.

## Current

Released version is v1.5.1 (versionCode 9). F-Droid serves it, and it is installed on the Pixel 9.
The debug build of v1.5.1 is installed on the Pixel 8.
The Claude Code workflow files are merged into local `master` with `--no-ff`. They are not pushed yet; Oliver pushes. They are:
- CLAUDE.md and STATE.md;
- the narrowed `.claude/settings.local.json`, which is untracked and has a deny list;
- the inventory.

CONVENTIONS.md is the single rulebook: `.cursorrules` now points to it, and the duplicate docs are deleted.
There is no guard hook or verify subagent yet. The deny list is the only hard block.

## Next

- [x] **Step 1b — trial fix: Default Launcher row debug text.** Done; see Runs.
- [ ] **Same leak in three more rows.** `DefaultAppsChecker.kt` lines 99 (browser), 169 (SMS) and 304 (email) still set
      `"Using $appName (unknown, 0 pts)"` as hardcoded English. Same fix as the launcher row.
- [ ] **`QuickWinsDetector.extractAppName` parses UI copy** (`removePrefix("Using ")`). This breaks once the
      `Using …` statuses are localised. Pass the app name as a field instead.
- [ ] **Guard hook** (`.claude/hooks/`, registered in a tracked `.claude/settings.json`). It must:
  - reject adb calls without `-s <serial>`;
  - limit `55290DLAQ000MR` to read-only commands;
  - block `git push` / `git tag` / `gh` writes;
  - catch forms that prefix deny rules miss: `git -C <dir> push`, `adb -s … shell "pm clear …"`;
  - consider moving the deny list into the tracked `.claude/settings.json`, so the Framework laptop gets it too;
  - reject `git switch` / `git checkout` to a branch that lacks `c837e59` (it would overwrite the untracked `settings.local.json`).
- [ ] **Backup-rules fix.** Gate: run `bmgr` on the Pixel 8 before and after the change. Check that:
  - [ ] the `sharedpref` excludes `ad_id_prefs` and `score_history` lack `.xml` and may match nothing;
  - [ ] `backup_rules.xml` excludes `-shm` / `-wal` (it doesn't yet);
  - [ ] `data_extraction_rules.xml` (Android 12+, both `<cloud-backup>` and `<device-transfer>`) gets the same fix;
  - [ ] the score-history chart lives in Room (`privacy_history.db`), so its exclude must use `domain="database"`, not `sharedpref`.
        The `score_history` sharedpref holds only the daily baseline and delta.

## Open questions for Oliver

None open.

## Runs

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

### 2026-10-20 — workflow setup, round 2: rulebook cleanup and merge (`chore/workflow-setup` → `master`)

- [x] `.cursorrules` replaced with a 3-line pointer to CONVENTIONS.md: read it before any edit, never push.
- [x] Deleted `docs/QUICK_REFERENCE.md` and `docs/POST_IMPLEMENTATION_CHECKLIST.md` (they were identical).
      Evidence: `git grep` finds no other references to them.
- [x] Compared the "Add New Check" checklist with CONVENTIONS.md:
  - step 3 "Add to relevant PrivacyCategory" was missing, so I added it under the `PrivacyCheck` rule;
  - step 4 "Update Scoring System page" needs no separate step.

  Evidence: `ScoringSystemScreen.kt:56` builds from `PrivacyCategory.entries`.
- [x] CONVENTIONS.md push line: Oliver pushes; Claude Code and Cursor never push.
- [x] CONVENTIONS.md Framework laptop JBR path marked "unverified since Oct 2026, Framework laptop only".
- [x] Reviewed the branch diff against `master`: no merge blockers. No app code, Gradle or resources changed, so there is no build gate.
- [x] Merged into `master` locally with `--no-ff`. Not pushed.
- [ ] Not verified: whether the allow and deny rules match as intended in a live session.
- Note: `.claude/settings.local.json` is untracked since `c837e59`, so git can overwrite or
  delete it on any machine. This happens:
  - on the Framework laptop's next `git pull`;
  - on `git switch` / `git checkout` to any branch created before `c837e59`, because those branches still track the file.

  Back it up before either. The guard hook (Next) will block switches to such branches.
  Check with `git merge-base --is-ancestor c837e59 <branch>`.

### 2026-10-20 — workflow setup (`chore/workflow-setup`, from `chore/workflow-inventory`)

- [x] `.claude/settings.local.json` untracked.
      Evidence: `git rm --cached` → `D  .claude/settings.local.json`; the file is still on disk.
- [x] `.gitignore` ignores only `.claude/settings.local.json`.
      Evidence: `git check-ignore -v`.
- [x] Allow list narrowed to 20 entries, and a deny list of 44 entries added. Oliver approved both.
      Evidence: `jq .` parses.
- [x] CLAUDE.md written (108 lines). Its Gradle commands match the allow list exactly.
- [x] STATE.md and `docs/state-archive/README.md` written.
- [x] CONVENTIONS.md keystore line: the keystore is on the Framework laptop, is not used by F-Droid, and is never committed.

### 2026-10-20 — workflow inventory (`chore/workflow-inventory`, commit `549e47b`)

- [x] Repo, build, signing, data stores, backup rules and devices inventoried.
      Evidence: `docs/workflow/inventory.md`, with the commands quoted.
- [x] JDK: `temurin-21` builds, JBR 25 fails.
      Evidence: `compileDebugKotlin` exit 0 vs `BUILD FAILED ("25.0.2")`.
- [x] Unit tests: 16 suites, 146 tests, 0 failures.
      Evidence: `testDebugUnitTest --rerun`, results timestamp 2026-10-20T18:26:37.
- [x] Debug APK confirmed.
      Evidence: `aapt2 dump badging` → `com.techtrest.privamatic.debug`, versionCode 9.
- [ ] Backup `sharedpref` `.xml` suffix: hypothesis only. Moved to Next.
