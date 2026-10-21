# Backup exclusions — spec

Run type: implementation run. Branch: `fix/backup-exclusions` (from `master`, 2026-10-21).

## Goal

No Privamatic app data leaves the device through Android backup: neither cloud backup (Google, Seedvault, any transport)
nor device-to-device (D2D) transfer, on every Android version from minSdk 26 to targetSdk 35 and beyond.

## Scope

**In:**
- `AndroidManifest.xml`: `android:allowBackup`.
- `res/xml/data_extraction_rules.xml` (Android 12+) and `res/xml/backup_rules.xml` (Android 11 and lower).
- A JVM test that parses both rule files and checks they exclude every domain, and that `<device-transfer>` mirrors `<cloud-backup>`.
- CONVENTIONS.md "Android Auto Backup exclusions" rule, updated to the decision.

**Out:**
- Any in-app export or import of the trusted list or settings (a possible future replacement for backup; see Found).
- A custom `BackupAgent`.
- `<cross-platform-transfer platform="ios">` (Android 16 QPR2+): it needs an iOS app's `bundleId` and `teamId`, and Privamatic has none.

## Oliver's calls

| Question | Default |
|---|---|
| What exactly is excluded from backup and D2D? | ~~What CONVENTIONS.md intends: Ad ID data and score history, done correctly in both rules files.~~ **Decided by Oliver (2026-10-21): exclude everything.** Score history, trusted list, settings, Ad ID data, caches, all of it. Ideally the app is not backed up at all. |

## Findings before changing code (2026-10-21)

**Manifest** (`app/src/main/AndroidManifest.xml:13-15`): `allowBackup="true"`, `dataExtractionRules="@xml/data_extraction_rules"`,
`fullBackupContent="@xml/backup_rules"`. minSdk 26, targetSdk 35 (`app/build.gradle.kts:14-15`). No `backupAgent`.

**On-disk stores**: the code names them, and the Pixel 8 listing confirms them (`run-as … find . -type f`, debug v1.5.1):

| Store | API | On-disk file | Excluded on `master`? |
|---|---|---|---|
| Score history chart | Room | `databases/privacy_history.db` (+ `-journal`, TRUNCATE mode) | yes (database) |
| Daily baseline / delta | SharedPreferences | `shared_prefs/score_history.xml` | `sharedpref path="score_history"` (no `.xml`) |
| Ad ID confirmation | SharedPreferences | `shared_prefs/ad_id_prefs.xml` | `sharedpref path="ad_id_prefs"` (no `.xml`) |
| Maintenance state | DataStore | `files/datastore/maintenance_prefs.preferences_pb` | yes |
| SDK scan cache | DataStore | `files/datastore/sdk_scan_prefs.preferences_pb` | yes |
| Trusted list | DataStore | `files/datastore/trusted_apps_prefs.preferences_pb` | yes |
| Quick-win dismissals | DataStore | `files/datastore/quick_win_dismissals.preferences_pb` | yes |
| Onboarding done | SharedPreferences | `shared_prefs/onboarding_prefs.xml` | **no** |
| What's new seen | SharedPreferences | `shared_prefs/whats_new_prefs.xml` | **no** |
| Details view choice | SharedPreferences | `shared_prefs/details_view_prefs.xml` | **no** |
| Widget opacity | SharedPreferences | `shared_prefs/widget_preferences.xml` | **no** |
| Tips shown | SharedPreferences | `shared_prefs/privacy_tip_history.xml` | **no** |
| Baseline profile marker | AndroidX | `files/profileInstalled` | **no** |
| Room lock, Skia cache | — | `cache/…`, `user_de/…/cache/…` | auto-excluded (cache) |

**CONVENTIONS.md** (Security & Privacy, "Android Auto Backup exclusions") lists only `ad_id_prefs` and `maintenance_prefs`.
It is behind the rule files, which since `b9e7adb` also exclude the SDK cache, the trusted list, quick-win dismissals and score history.

**Platform behaviour.** Docs were fetched 2026-10-21; the AOSP source is a hypothesis until a device shows it.
- developer.android.com, Android 12 behaviour changes: for apps targeting 31+, the `fullBackupContent` rules do not affect D2D.
  The same goes for `allowBackup="false"`, which "on devices from some device manufacturers … doesn't disable D2D transfers".
  D2D needs `<device-transfer>` in `dataExtractionRules`. Once `dataExtractionRules` is set, `fullBackupContent` is ignored on 12+,
  but it is "still required for devices running Android 11 or lower".
- Auto Backup guide: a mode with no section (for example, `<device-transfer>` missing) "is fully enabled for all content
  except for `no-backup` and `cache` directories". Paths take no wildcards; `"."` is the domain directory. There are nine domains:
  `root file database sharedpref external device_root device_file device_database device_sharedpref`.
- AOSP `FullBackup.parseRules` (main): a `sharedpref` path without `.xml` also gets the `.xml` variant added, for exclude as well as
  include. A `database` path also gets `-journal` and `-wal`. **So the suspected missing-`.xml` bug may not exist.** The before-run decides.
- AOSP `BackupAgent.onFullBackup`: each domain is walked as its own tree; `root` skips the `files`, `databases` and `shared_prefs` subtrees.
  An exclude prunes a tree only on an exact canonical-path match. **So `exclude root "."` alone would not cover the `files`,
  `databases` and `shared_prefs` domains: each domain needs its own `"."` exclude.**
- The Pixel 8 offers `com.android.localtransport/.LocalTransport`. The LocalTransport parameter `is_device_transfer=true`
  (`Settings.Secure.BACKUP_LOCAL_TRANSPORT_PARAMETERS`, AOSP `LocalTransportParameters`) makes it report the D2D transport flag.
  That simulates a D2D transfer on one device, with no data leaving it.

**Before-run on `master`'s build** (device-verify, Pixel 8, LocalTransport; evidence in `docs/screens/2026-10-21-backup-before/`):
- 20 files seeded: the real app data plus markers in root, `files`, and the four device-protected domains. `backupnow` → package `Success`.
  `pm clear` → 0 files. `bmgr restore 1` → **12 files came back** with identical md5.
- Back on restore: `onboarding_prefs`, `whats_new_prefs`, `details_view_prefs`, `widget_preferences`, `privacy_tip_history`,
  `files/profileInstalled`, `files/marker_file.txt`, `marker_root.txt`, and all four device-protected markers.
- Not carried: `privacy_history.db` (+`-journal`), all four DataStore files (trusted list included), `score_history.xml`, `ad_id_prefs.xml`.
- D2D mode (`is_device_transfer=true`): an identical result.
- **So the suspected missing-`.xml` bug does not exist.** The excludes without `.xml` work, as the AOSP parser predicted.
  Measured against Oliver's decision, the bug is the 12 files above: settings, plus anything outside the listed stores, and all device-protected storage.
- Attempt 1 broke: `backupnow` said "Backup is not allowed" because `pm clear` leaves the package stopped (`dumpsys`: `stopped=true`).
  The fix was `pm unstop` after seeding. Evidence: `docs/screens/2026-10-21-backup-before-attempt1/`.

## Design

1. `android:allowBackup="false"`. This disables cloud backup on all versions, and D2D on Android 11 and lower.
2. `data_extraction_rules.xml`: `<cloud-backup>` and `<device-transfer>` each exclude all nine domains with `path="."`, and nothing is included.
   This covers D2D on 12+, where `allowBackup` is ignored for apps targeting 31+.
   It also covers any 12+ transport that ignores `allowBackup`.
3. `backup_rules.xml`: the same nine excludes. On Android 11 and lower, `allowBackup="false"` already blocks everything;
   the legacy rules are a second layer in case an OEM or transport ignores it.

## Invariants (run-specific; review items 8+)

8. With this change, no file the app writes reaches a backup or D2D transport: no domain is left out in either section,
   and the legacy file covers the same domains.
9. `<device-transfer>` excludes exactly the same domains as `<cloud-backup>`; a unit test enforces it.
10. Nothing else changes: no code, no store names, no Room schema. Existing on-device data is untouched by the upgrade.

## Milestones

| # | Milestone | Gate |
|---|---|---|
| 1 | Bug shown on `master`'s build | device-verify, Pixel 8, LocalTransport: seed → `backupnow` → `pm clear` → `restore`. Files that should be excluded come back. Repeat in D2D mode (`is_device_transfer=true`). |
| 2 | Fix | Manifest + both rule files per Design; `compileDebugKotlin` OK. |
| 3 | Rules test | `BackupRulesTest` (JVM, parses both XML files): every domain excluded with `"."`, `<device-transfer>` == `<cloud-backup>`, legacy == cloud. `testDebugUnitTest --rerun` green. |
| 4 | Fix proven | device-verify on the branch build: cloud mode, `bmgr backupnow` reports the package not eligible or backs up nothing, and after `pm clear` + `restore` no seeded file comes back. D2D mode: nothing seeded comes back. Launch and Details screen unaffected (screenshot). |
| 5 | Legacy rules (API ≤ 30) | COULD NOT VERIFY on a device: the Pixel 8 is Android 17 and ignores `fullBackupContent`. Covered by the milestone 3 test. |
| 6 | Real D2D (second phone) | COULD NOT VERIFY: needs two devices. Covered by the milestone 4 D2D-mode run and the milestone 3 test. |

## Results (2026-10-21)

- [x] 1: see "Before-run" above. 12 of 20 seeded files came back in both cloud and D2D mode.
- [x] 2: `compileDebugKotlin` OK, `assembleRelease` OK, and `lintVitalRelease` raised nothing on the rule files.
- [x] 3: `BackupRulesTest` failed 4 of 6 on `master`'s rules, before the fix: cloud, device-transfer, legacy, manifest.
      After the fix: all of `testDebugUnitTest --rerun` passes, 17 suites, 152 tests, 0 failures.
- [x] 4: after-run on the branch build. device-verify, Pixel 8, fresh APK 19:32:38. Evidence in `docs/screens/2026-10-21-backup-after/`.
  - Package flags `[ DEBUGGABLE HAS_CODE ALLOW_CLEAR_USER_DATA ]`: no `ALLOW_BACKUP`.
  - Cloud: `bmgr backupnow` → "Backup is not allowed", even though seeding printed `stopped=false`; 0 `file_backup_helper` lines.
    `bmgr restore 1` → `restoreStarting: 0 packages`.
  - D2D: the backup agent is bound (D2D ignores `allowBackup`) but writes **0 files**. The transport then rejects the empty stream
    ("Transport rejected package…", PFTBT −1002). The restore covers only `@pm@`.
  - Both modes: **none of the 20 seeded files came back** (`grep -Fxf` seeded vs restored: no match).
  - The restored lists hold 2 files that the app wrote itself: `profileInstalled` and `score_history.xml`, both with md5 different from the seed.
    A `PrivacyWidgetProvider` broadcast restarted the app after `pm clear` (logcat `Start proc … for broadcast`).
    They are not backup data.
  - Dashboard and Details render normally with real data (`dashboard-real-data.png`, `details.png`); the crash buffer is empty.
- [ ] 5 and 6: COULD NOT VERIFY, as planned. The legacy rules on Android 11 and lower, and a real two-phone D2D transfer.
      Both are covered by `BackupRulesTest`; D2D is also covered by the LocalTransport D2D-mode run.

## STOP points

| When | What Oliver does |
|---|---|
| After the diff review, before merge | Runs the external review round 1 in Cursor and saves `docs/reviews/fix-backup-exclusions-findings-1.md`. |
| Before release | Agrees the F-Droid changelog line (trusted apps and settings no longer move to a new phone). |
