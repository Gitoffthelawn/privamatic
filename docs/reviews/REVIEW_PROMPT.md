Review docs/reviews/<branch>.diff at the Privamatic repo root (offline Android
privacy auditing app, Kotlin/Compose, <base>..<branch>: <one line on what the
run did>). You can't run git, Gradle or tests in this workspace. Read only the
files this diff touches, and only the functions around the changed lines —
don't explore the rest of the repo. Skip strings.xml, fastlane metadata, tests
and screenshots unless they are the point of the run. If you need more than
~10 files, stop and say which and why.

Don't fix anything. List findings ranked by severity (High / Medium / Low)
with file:line, a concrete failure scenario, and your confidence.

Context: <what earlier review rounds found / what was fixed / what was
rejected and why — or "first review">.

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
8. <invariant>
9. <invariant>

<Optional: Also judge this rejected finding: "<claim>". Rejection reasoning:
<reasoning>. Find a path where it fails, or confirm it.>
