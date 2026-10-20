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

- [ ] **Step 1b — trial fix: Default Launcher row debug text.** The row shows
      "Using Pixel Launcher (unknown, 0 pts)". Oliver starts this in a fresh session.
- [ ] **Guard hook** (`.claude/hooks/`, registered in a tracked `.claude/settings.json`). It must:
  - reject adb calls without `-s <serial>`;
  - limit `55290DLAQ000MR` to read-only commands;
  - block `git push` / `git tag` / `gh` writes;
  - catch forms that prefix deny rules miss: `git -C <dir> push`, `adb -s … shell "pm clear …"`;
  - consider moving the deny list into the tracked `.claude/settings.json`, so the Framework laptop gets it too.
- [ ] **Backup-rules fix.** Gate: run `bmgr` on the Pixel 8 before and after the change. Check that:
  - [ ] the `sharedpref` excludes `ad_id_prefs` and `score_history` lack `.xml` and may match nothing;
  - [ ] `backup_rules.xml` excludes `-shm` / `-wal` (it doesn't yet);
  - [ ] `data_extraction_rules.xml` (Android 12+, both `<cloud-backup>` and `<device-transfer>`) gets the same fix;
  - [ ] the score-history chart lives in Room (`privacy_history.db`), so its exclude must use `domain="database"`, not `sharedpref`.
        The `score_history` sharedpref holds only the daily baseline and delta.

## Open questions for Oliver

None open.

## Runs

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
- Note: after the push, the Framework laptop's next `git pull` will **delete** its local
  `.claude/settings.local.json`, because the file is untracked now. Back it up before pulling.

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
