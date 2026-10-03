# Privamatic — Development Conventions

## Project Identity
- **Package**: `com.techtrest.privamatic` (NOT `com.techtrest.privacywidget` — grep before prompting)
- **Source root**: `app/src/main/java/com/techtrest/privamatic/`
- **Project path**: `~/Local App Projects/Privamatic` — always quote in shell commands (spaces)
- **Build command (MiniPC)**: `JAVA_HOME=/home/techtresthome/.local/share/jdk/temurin-21 ./gradlew compileDebugKotlin`
  — pinned Temurin 21.0.7 (extracted from Gradle's downloaded tarball, Sept 2026). The Android Studio JBR at
  `/opt/android-studio/jbr` was upgraded to Java 25, which Gradle 8.10 rejects (`IllegalArgumentException: 25.0.2`);
  `~/.gradle/jdks/` is an auto-managed toolchain cache, so don't depend on it.
- **Build command (Framework laptop)**: `JAVA_HOME=/home/techtrest/.local/share/android-studio/jbr ./gradlew compileDebugKotlin`
  — if that JBR has also moved to 25, replicate the MiniPC fix (extract a JDK 21 to `~/.local/share/jdk/temurin-21`)
- System JDK 25 breaks Kotlin parser on both machines; never omit JAVA_HOME prefix
- **Default branch**: `master` (not `main`)

## File Structure
app/src/main/java/com/techtrest/privamatic/

├── data/

│   ├── model/              # Data classes (PrivacyScore, PrivacyIssue, PrivacyCheck)

│   ├── scanner/

│   │   └── checks/         # Individual checker classes

│   ├── util/               # PackageManagerUtil and shared helpers

│   └── QuickWinsDetector.kt

├── ui/

│   ├── components/

│   ├── screens/

│   ├── navigation/         # AppNavigationState

│   └── theme/

└── MainActivity.kt

## Architecture Rules

**No DI framework** — no Hilt. Shared components are object singletons passed explicitly:
- `TrustedAppsAdjuster` — singleton shared between `PrivacyViewModel` and `PrivacyWidgetProvider`
- `QuickWinsDetector` — singleton
  — a Quick Win's `impact` is its check's points plus `alsoFixes`: other checks the same fix
    clears, included only while they currently cost points (Developer options off also turns
    USB debugging off). Tiles, detail page and sorting all read `impact`
- Never instantiate these per-caller — widget and ViewModel must produce identical scores

**Widget runs independently** — `PrivacyWidgetProvider` runs its own `PrivacyScanner` scan.
It has no access to ViewModel StateFlow. The scan → TrustedAppsRepository → TrustedAppsAdjuster
sequence must mirror ViewModel logic exactly.

**`getTotalPoints()` rule** — must read directly from DataStore, never delegate to a flow
that applies UI visibility filters. Visibility filters silently drop completed-but-not-overdue
items, losing those points.

**Shared utilities** — package helpers (`getAppName`, `isSystemApp`) belong in
`data/util/PackageManagerUtil.kt`. Never duplicate across Checker files.
`isSystemApp` must default to `true` on errors (conservative for security).

**`PrivacyCheck` enum is single source of truth** — all scoring logic lives in the enum.
ViewModel and widget both derive from the same enum values. No dead code drift.

**Details tabs** — Checks / Trusted / SDKs (`DetailsTab`). Score Breakdown is not a tab: it is
the alternate view of the Checks tab (`ChecksView.BREAKDOWN`, `BreakdownContent.kt`).
Layout: a ledger, one card with one line per check (name left, `DeductionChip` right — the
same chip as the Checks rows) and no footer; the total lives in the tab header
("−N pts · Score S"). Filter: `pointDeduction > 0`. A "Manual checks not done" row (hidden at 0) makes the rows
add up to 100 − score; all rows, manual included, sort descending by points (manual first on ties); its value and the header total come
from `PrivacyScoreCalculator.manualCheckDeduction()` / `totalDeduction()`, never a re-derived
formula. Only a score clamped at 0 doesn't reconcile.
Tapping a check row washes it in `primaryContainer` (~200 ms), then switches to the list view,
collapses every category except the target's, expands the target row (its recommendation;
every other row collapses), scrolls the expanded row into view and highlights it (same colour,
fades over ~1.4 s). A plain header toggle keeps expansion and scroll and never highlights
(`ChecksTab` owns category and row expansion, `LazyListState` and the highlight). The manual row
opens the Actions tab immediately.

**What's new card** — one-time Dashboard card below the Score card after an update, never on a
fresh install (`WhatsNewCard`, `WhatsNew`, `WhatsNewPreferences`). Notes live in `WhatsNew`:
`NOTES_VERSION_CODE` plus a list of `copy_whats_new_*` strings. For a release with news, set it to
that release's versionCode and replace the items; a release that leaves it alone shows nothing,
so old notes never repeat. Seen state is `last_seen_version_code` in SharedPreferences
(`whats_new_prefs`), written on dismiss; a fresh install (`firstInstallTime == lastUpdateTime`)
is marked seen silently. The card shows only when `BuildConfig.VERSION_CODE ==
NOTES_VERSION_CODE`, so the versionCode bump at release is what turns it on.

**Rescan after a fix deep link** — Settings intents that let the user fix a check (Quick Win
detail, check-row "Open settings") call `IntentHelper.launchActionIntent(…, rescanOnReturn = true)`;
`MainScreen` rescans once on the next resume. Manual-check links don't (they change no scan
result). The flag is in memory only.

**View toggles vs tabs** — a tab is for different data; a toggle is for the same data viewed
differently. Toggles are an `IconButton` in the tab's fixed header row (never a segmented
row under the tab bar), wrapped in a `PlainTooltip`, with one string used as both tooltip and
content description, naming the view the button switches *to*. The header's left side carries
information about the current view (e.g. Checks: status counts via `statusCounts()`;
Breakdown: total deduction), not a repeat of the tab name. The chosen view is persisted in
SharedPreferences (`DetailsViewPreferences` pattern: store the enum `name`, fall back to the
default on unknown values) and read synchronously so the first frame shows the saved view.

**`effectivelySecure`** — `issue.isSecure || allPackagesTrusted` — pure UI derivation,
no data model changes. Row icons and category chip counts go through
`PrivacyIssue.displayStatus()` (`IssueItem.kt`), which applies `isUnknown` first, then trust.

## Detection & Scoring Rules

**Package detection** — match exact package names. Prefix or `contains()` matching only
where an app genuinely ships under several package variants (e.g. Lawnchair, Kvaesitso,
Chrome/Edge channels), always with a code comment saying why. Variants of an installed-app
check go in `PackageNames.VARIANT_PACKAGES`.
Never identify an app by its activity, service or IME class name: split component strings
(`package/class`, e.g. `default_input_method`) on `/` and match the package part only —
Gboard inherits AOSP's `com.android.inputmethod.latin.LatinIME` class.
Class/component names are the signal only where that is the design: microG detection
(microG spoofs Google's package name) and `SdkScanner` tracker signatures.
Documented exception: the data-broker blacklist (`FlaggedApp.BLACKLIST_PREFIXES`) matches by
vendor prefix by design — it blocks whole vendors, not specific apps. A package matches when it
is the vendor namespace itself or sits under it (`com.whatsapp`, `com.whatsapp.w4b`), never a
lookalike (`com.whatsappfoo`).
Every addition to a detection list gets a unit test using the real package (or
`package/class`) string, verified against F-Droid, IzzyOnDroid or vendor sources
(the APK manifest when the class matters). Cite the source in the commit message.

**Allowlist checks** — where an unmatched app costs points (default keyboard: anything not
in `friendlyKeyboard()` is −3), a privacy-friendly app missing from the list is penalised.
Any change to matching must check which previously-passing apps would now fail — run the
old and new matcher against real package/class strings — and report them.

**Unknown results** — if a check cannot determine its result, it sets `isUnknown = true`
and costs 0 points (`customPointDeduction = 0` when `isSecure = false`); its status text
tells the user to verify manually. `isUnknown` controls display and counting everywhere:
info icon, excluded from pass AND issue counts (category chips, Dashboard tiles, Breakdown).
`isSecure` only controls whether quick wins and tips appear.
Documented exception: Advertising ID keeps its −5 until the user confirms — a deliberate
nudge, since the state can't be read without Google's AD_ID permission. It stays
`isUnknown = false` with "Not verified — confirm in Actions"; see the comment at
`NetworkSecurityChecker.checkAdvertisingId()`.

**Advice for preinstalled apps** — never tell the user to uninstall a system app (it can't be
done) or to disable one whose loss breaks the phone. A flagged app with `issue.isSystemApp`
shows `PrivacyIssue.systemAppRecommendation` ("came with your phone… disable if you don't use
it", formatted with the app name) instead of its "Consider uninstalling" text; a check whose
disabling breaks something specific sets `PrivacyCheck.systemAppRecommendation` (Camera: no
camera app left). Uninstall Quick Wins already skip system apps (`QuickWinsDetector`).
Google Play Services on stock Android is an informational trade-off with no settings action:
point to GrapheneOS (sandboxed Play) or microG, never to disabling it (audit H3).

**Play Services presence** — absent / disabled / microG / privileged / sandboxed comes only
from `GoogleServicesChecker.playServicesState()` (`PlayServicesState`). Never add a separate
`com.google.android.gms` package lookup. Whether a Google Advertising ID exists is
`PlayServicesState.servesAdvertisingId` (used by the Ad ID scan row and the Ad ID manual check).
- Stock vs sandboxed: `MATCH_SYSTEM_ONLY` resolves only genuine system-partition GMS
  (GrapheneOS sets `FLAG_SYSTEM` on sandboxed GMS, so flags/UID are not trustworthy)
- microG: `PackageManagerUtil.isMicroGInstalled()` — microG installs as
  `com.google.android.gms`, but its components live in the `org.microg.` namespace.
  Companion packages (`org.microg.gms.droidguard`, `org.microg.nlp`) are not a signal:
  modern microG compiles them into GmsCore (#11, #19)

## Strings & Localisation

All user-facing strings in `strings.xml`. Never hardcode English text in Kotlin or XML.

**Enum strings** — use `@StringRes Int` fields, never raw string literals.
In Composables: `stringResource(rating.displayNameRes)`
In non-Composable (widget, checker): `context.getString(rating.displayNameRes)`

**Widget strings** — `RemoteViews` cannot use `stringResource()`. Always use
`context.getString()` in `PrivacyWidgetProvider`.

**Naming convention:**
- UI labels: `label_<screen>_<element>`
- UI copy: `copy_<screen>_<element>`
- Enum display names: `<enum>_<entry>_name`
- Plurals: `plural_<element>` using `<plurals>` tag
- Format strings: `fmt_<element>`

**Minus sign** — every point deduction shown to the user uses the real minus sign U+2212
(`−`), never a hyphen. Format it through `fmt_deduction_chip` (`−%d`) rather than building
`"-$n"` in Kotlin.

**Do NOT extract to strings.xml:** package names, log tags, DataStore keys,
OS brand detection strings (GrapheneOS, CalyxOS), format placeholders.

## Security & Privacy Rules

**Zero network permissions** — no library that makes network calls may be added.

**No third-party SDKs** — no Firebase, Crashlytics, analytics. Ever.

**No GMS dependency** — removed pre-F-Droid. Never re-add `com.google.android.gms`.

**Debug logging** — all `Log.*` calls wrapped in `if (BuildConfig.DEBUG)`.
Sensitive data (package names, device fingerprints) never in logs, even debug.

**Android Auto Backup exclusions** — exclude from cloud backup:
- `ad_id_prefs` (SharedPreferences)
- `maintenance_prefs` (DataStore)
Declared in both `data_extraction_rules.xml` and `backup_rules.xml`.

## Build & Release Rules

**F-Droid requirements:**
- `signingConfig = null` in release build type — remove entire `signingConfigs` block
- Never use `jvmToolchain` — use `kotlinOptions { jvmTarget = "11" }`
- Never add `foojay-resolver-convention` or `gradle-daemon-jvm.properties`
- `isMinifyEnabled = true` and `isShrinkResources = true` on release builds
- All response/model DTOs explicitly listed in `proguard-rules.pro`
  (R8 silently breaks Gson/Retrofit in release; symptoms never appear in debug)
- Test `JAVA_HOME=/home/techtresthome/.local/share/jdk/temurin-21 ./gradlew assembleRelease` before every tag

**Versioning:**
- Always bump `versionCode` before tagging — F-Droid uses versionCode to detect releases
- A tag without a versionCode bump is silently skipped by F-Droid
- Never tag before bumping versionCode

**Keystore:**
- Release keystore at `~/privamatic-release.jks`
- Passwords via env vars `PRIVAMATIC_STORE_PASSWORD` / `PRIVAMATIC_KEY_PASSWORD`

**On-device comparison against the F-Droid release:**
- Debug builds use `applicationIdSuffix = ".debug"` (`com.techtrest.privamatic.debug`,
  label "Privamatic Debug") and install alongside the F-Droid release
- Never uninstall the F-Droid install or clear its data for testing. On multi-user
  devices (GrapheneOS profiles) install with `adb install --user 0`
- Compare row by row (Checks rows, Breakdown, manual checks in Actions) and attribute
  every difference: this branch (cite the commit), fresh debug-app state (trusted apps,
  manual checks, Ad ID confirmation), or unexpected
- With several devices attached, identify each by serial and pass `-s <serial>` to every
  adb command

**microG test emulator (MiniPC):**
- AVD `microg_test_api35`: AOSP x86_64 API 35 (`system-images;android-35;default;x86_64`),
  **no Google APIs**, with microG GmsCore 0.3.16.252432 sideloaded from the official
  microG F-Droid repo (`https://repo.microg.org/fdroid/repo`, signer `O=NOGAPPS Project`)
- Snapshot `microg_installed_baseline` = clean boot with microG installed; load it with
  `-snapshot microg_installed_baseline` (or restore via `adb emu avd snapshot load`)
- Purpose: microG-related detection/behaviour tests only (e.g. `MicroGDetectionDeviceTest`).
  It is NOT a general Google-APIs test device — AOSP was chosen precisely because Google
  APIs images ship a real `com.google.android.gms`, which blocks installing microG over it
- Snapshot `gcam_photos_shim_installed` = `microg_installed_baseline` plus Gcam Services
  Provider v1.6.1 *photosonly* (`app-photosonly-release.apk` from the project's GitHub
  releases, signer `CN=Lukas Pieper`, SHA-256 `357c243c…`) installed as
  `com.google.android.apps.photos`. Purpose: `GooglePhotosShimDeviceTest` positive path (#20).
  Gotcha: the *photos* flavor cannot be installed here — its `com.google.android.gsf.gservices`
  provider conflicts with microG's (`INSTALL_FAILED_CONFLICTING_PROVIDER`); *photosonly*
  is also the only flavor GrapheneOS users run
- Gotcha: the default renderer (SwiftShader, also `-gpu off`/`auto` headless) segfaults on
  this machine — always launch with `-gpu host`:
  `~/Android/Sdk/emulator/emulator -avd microg_test_api35 -no-window -gpu host -no-audio -no-boot-anim`
- Scope Gradle to it with `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest`
  when the Pixel 8 is also attached

**Git discipline:**
- Feature branches always — never commit directly to `master`
- `git diff → git status → git add → git commit` — always in this order
- Prefixes: `feat:` / `fix:` / `chore:` / `release:`
- Never commit `.env`, secrets, or the release keystore
- Default branch is `master` — push to `git push origin master`

**F-Droid fastlane metadata:**
fastlane/metadata/android/en-US/

├── changelogs/<versionCode>.txt   # e.g. 3.txt for versionCode 3

└── images/phoneScreenshots/       # exact name — wrong = silent failure
Locale folder: `en-US` (hyphen not underscore).

## Design Constraints
- `MaterialTheme.typography` only — no arbitrary `fontSize`
- `MaterialTheme.colorScheme` only — no hardcoded hex colors
- Small brand-green text or icons on a surface use `MaterialTheme.accentOnSurface` (Theme.kt),
  not `colorScheme.primary`: dark `primary` is pinned to #00854A, only 3.1:1 on
  `surfaceContainerHigh`. The accent is `primary` in light (9.5:1) and the tone-80 primary
  (`surfaceTint`) in dark (8.5:1); both meet WCAG AA 4.5:1
  TextButton / OutlinedButton default their content to `primary` too: pass
  `colors = accentTextButtonColors()` / `accentOutlinedButtonColors()` unless the button sets its
  own colour (e.g. error, or Cream on the onboarding green). Filled buttons and bars (white on
  `primary`, 4.7:1) are fine
- Widget ARGB colors as named constants, never inline hex
- Material Design spacing: 4dp, 8dp, 12dp, 16dp, 24dp, 32dp — no arbitrary values
- Corner radius: 8dp small, 12dp standard
- Switch rows: the whole row is `Modifier.toggleable(value, enabled, role = Role.Switch)` and the
  `Switch` gets `onCheckedChange = null`, so accessibility services see one checkable node with
  its checked and enabled state (a `clickable` row exposes neither)
- Material icons only — `Icons.Default.*` or `Icons.Outlined.*` (plus `Icons.AutoMirrored.*`
  for directional arrows); no emoji in production UI. Status and category icons
  (row status, category chips, category headers) use `Icons.Outlined.*` for consistency
  with the chips

*Last updated: 2026-10-03*
