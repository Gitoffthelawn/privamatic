# Privamatic — STATE

Task list and evidence log. The rules for keeping it are in CLAUDE.md. Keep this file under about 150 lines.
Older runs move to `docs/state-archive/YYYY-MM.md`.

## Current

Released version is v1.5.1 (versionCode 9). F-Droid serves it, and it is installed on the Pixel 9.
The debug build of v1.5.1 is installed on the Pixel 8.
The Claude Code workflow is being set up:
- the inventory is done (`docs/workflow/inventory.md`);
- CLAUDE.md, STATE.md and the narrowed `.claude/settings.local.json` exist on `chore/workflow-setup`;
- none of this is merged yet;
- there is no guard hook and no verify subagent yet, so the deny list in `.claude/settings.local.json` is the only hard block.

## Next

- [ ] **Step 1b — trial fix.** Run a first small implementation run under CLAUDE.md to test the workflow
      end to end. The spec still needs to be written (`docs/specs/<feature>.md`).
- [ ] **Guard hook** (`.claude/hooks/`, registered in a tracked `.claude/settings.json`). It must:
  - reject adb calls without `-s <serial>`;
  - limit `55290DLAQ000MR` to read-only commands;
  - block `git push` / `git tag` / `gh` writes;
  - catch quoted or compound forms that prefix deny rules miss (e.g. `adb -s … shell "pm clear …"`).
- [ ] **Backup-rules fix.** In `backup_rules.xml` and `data_extraction_rules.xml`:
  - the `sharedpref` excludes `ad_id_prefs` and `score_history` lack `.xml`, so they may match nothing;
  - `backup_rules.xml` lacks `-shm` / `-wal`;
  - gate: check it on the Pixel 8 with `bmgr` (inventory §6).

## Open questions for Oliver

- What is the scope of step 1b, and which spec should it get?
- Backup fix: test it on the Pixel 8 with `bmgr` before and after, or just add `.xml`? Default: test before and after on the Pixel 8.
- Stale rulebooks (`.cursorrules`, `docs/QUICK_REFERENCE.md`, `docs/POST_IMPLEMENTATION_CHECKLIST.md`): delete them, or turn them into pointers to CONVENTIONS.md?
- CONVENTIONS still shows `JAVA_HOME=/home/techtrest/.local/share/android-studio/jbr` for the Framework laptop. Is that still valid?
- CONVENTIONS "Git discipline" still says `push to git push origin master`. CLAUDE.md now says Oliver pushes. Should that line change?

## Runs

### 2026-10-20 — workflow setup (`chore/workflow-setup`, from `chore/workflow-inventory`)

- [x] `.claude/settings.local.json` untracked.
      Evidence: `git rm --cached` → `D  .claude/settings.local.json`; the file is still on disk.
- [x] `.gitignore` ignores only `.claude/settings.local.json`.
      Evidence: `git check-ignore -v` matches that file only; `.claude/settings.json`, `agents/` and `hooks/` are not ignored.
- [x] Allow list narrowed to 20 entries, and a deny list of 44 entries added. Oliver approved both.
      Evidence: `jq .` parses; `{allow: 20, deny: 44}`.
- [x] CLAUDE.md written (108 lines). Its Gradle commands match the allow list exactly.
- [x] STATE.md and `docs/state-archive/README.md` written.
- [x] CONVENTIONS.md: keystore line corrected (Framework laptop, not used by F-Droid, never committed). Nothing else changed.
- [ ] Not verified at runtime: whether the allow and deny rules match as intended in a live session
      (no device or Gradle commands were run in this run, by design).

### 2026-10-20 — workflow inventory (`chore/workflow-inventory`, commit `549e47b`)

- [x] Repo, build, signing, data stores, backup rules and devices inventoried.
      Evidence: `docs/workflow/inventory.md`, with the commands quoted.
- [x] JDK verified: `temurin-21` builds, JBR 25 fails.
      Evidence: `compileDebugKotlin` exit 0 vs `BUILD FAILED ("25.0.2")`.
- [x] Unit tests: 16 suites, 146 tests, 0 failures.
      Evidence: `testDebugUnitTest --rerun`, results XML timestamp 2026-10-20T18:26:37.
- [x] Debug APK path and id confirmed.
      Evidence: `aapt2 dump badging` → `com.techtrest.privamatic.debug`, versionCode 9.
- [x] Keystore: not on the MiniPC. Oliver says it is on the Framework laptop. CONVENTIONS is updated in the setup run.
- [ ] Backup `sharedpref` `.xml` suffix: hypothesis only, not tested on a device.
