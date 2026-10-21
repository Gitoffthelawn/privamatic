Review docs/reviews/fix-backup-exclusions.diff at the Privamatic repo root (offline Android
privacy auditing app, Kotlin/Compose, master..fix/backup-exclusions: keeps all app data out of
cloud backup and device-to-device transfer — allowBackup="false", every domain excluded in
data_extraction_rules.xml and backup_rules.xml, plus a JVM test that enforces it; the same diff
also adds workflow docs (CLAUDE.md, a device-verify agent, a spec template), STATE.md updates and
device evidence under docs/screens). You can't run git, Gradle or tests in this workspace. Read only the
files this diff touches, and only the functions around the changed lines —
don't explore the rest of the repo. Skip strings.xml, fastlane metadata, tests
and screenshots unless they are the point of the run. If you need more than
~10 files, stop and say which and why.

The point of this run is AndroidManifest.xml, res/xml/data_extraction_rules.xml,
res/xml/backup_rules.xml and app/src/test/.../BackupRulesTest.kt; read the test too.
docs/specs/backup-exclusions.md explains the design and the device evidence.

Don't fix anything. List findings ranked by severity (High / Medium / Low)
with file:line, a concrete failure scenario, and your confidence.

Context: first review.

Standing invariants — check that each holds on every path this diff touches:
1. Offline: no INTERNET permission, no network calls, no new dependency that
   can reach the network.
2. User data never leaves the device: score history (Room), the trusted list
   and settings are excluded from cloud backup and device transfer, in both
   data_extraction_rules.xml (Android 12+) and the legacy backup rules.
3. Room: every schema change bumps the version and ships a migration; no
   destructive fallback; existing score history survives an upgrade.
4. Scoring: the PrivacyCheck enum is the single source of truth for
   deductions; trust math goes through PrivacyCheck.cappedDeductionFor();
   no score logic elsewhere.
5. Detection: a disabled app still returns ApplicationInfo — check
   appInfo.enabled before treating a package as active.
6. All user-facing text comes from strings.xml.
7. Release builds stay unsigned (signingConfig = null) for F-Droid.

Run-specific invariants:
8. No file the app writes can reach a backup or device-transfer transport on any
   Android version from minSdk 26 to targetSdk 35 and later: each of the nine domains
   (root, file, database, sharedpref, external, device_root, device_file,
   device_database, device_sharedpref) has its own path="." exclude in <cloud-backup>,
   in <device-transfer> and in the legacy <full-backup-content>; nothing is included.
   The platform walks each domain separately, so a root exclude does not cover the others.
9. <device-transfer> excludes exactly what <cloud-backup> excludes, and BackupRulesTest
   would fail if either section, the legacy file or the manifest drifted (check the test
   can actually fail: e.g. a duplicate, a missing domain, an added <include>, a third section).
10. The change touches no code, no store or file names and no Room schema; existing
    on-device data is untouched by the upgrade.
