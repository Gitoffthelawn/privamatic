package com.techtrest.privamatic.data.scanner.checks

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import com.techtrest.privamatic.BuildConfig
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.util.PackageManagerUtil

class DefaultAppsChecker(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Check default browser app with three-tier detection
     * Privacy-invasive: Chrome, Edge, Opera, UC Browser (-3), Samsung Internet (-2)
     * Privacy-friendly: Brave, Firefox, DuckDuckGo, etc. (0)
     * Unknown: Everything else (0, displayed as unknown)
     */
    fun checkDefaultBrowser(): PrivacyIssue {
        return try {
            // URI passed only to PackageManager.resolveActivity() — no network request is made
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))

            // Try MATCH_DEFAULT_ONLY first
            var resolveInfo = packageManager.resolveActivity(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
            var packageName = resolveInfo?.activityInfo?.packageName

            // If no default, try without the flag to see what's available
            if (packageName == null || packageName == "android") {
                resolveInfo = packageManager.resolveActivity(browserIntent, 0)
                val fallbackPackage = resolveInfo?.activityInfo?.packageName

                // If we got a real app (not the chooser), use it
                if (fallbackPackage != null && fallbackPackage != "android") {
                    packageName = fallbackPackage
                }
            }

            val finalPackage = packageName ?: "none"

            // Privacy-invasive browsers
            val invasive = invasiveBrowser(finalPackage)

            if (invasive != null) {
                val (points, name, _) = invasive
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_BROWSER,
                    isSecure = false,
                    currentStatus = "Using $name",
                    technicalDetails = "Package: $finalPackage",
                    customPointDeduction = points
                )
            }

            // Privacy-friendly browsers
            val friendly = friendlyBrowser(finalPackage)

            if (friendly != null) {
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_BROWSER,
                    isSecure = true,
                    currentStatus = "Using $friendly",
                    technicalDetails = "Package: $finalPackage",
                    customPointDeduction = 0
                )
            }

            // No default or unknown browser
            if (finalPackage == "android" || finalPackage == "none") {
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_BROWSER,
                    isSecure = true,
                    currentStatus = "No default browser set",
                    technicalDetails = "Package: $finalPackage",
                    customPointDeduction = 0
                )
            } else {
                // Unknown browser - don't penalize
                val appName = PackageManagerUtil.getAppName(packageManager, finalPackage)
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_BROWSER,
                    isSecure = true,
                    isUnknown = true,
                    currentStatus = "Using $appName (unknown, 0 pts)",
                    technicalDetails = "Package: $finalPackage",
                    customPointDeduction = 0
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking default browser", e)
            PrivacyIssue(
                check = PrivacyCheck.DEFAULT_BROWSER,
                isSecure = true,
                isUnknown = true,
                currentStatus = "Unable to determine default browser",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    /**
     * Check default SMS/Messaging app with three-tier detection
     * Privacy-invasive: Google Messages (-2), Facebook Messenger (-3), WhatsApp (-3), Samsung Messages (-2)
     * Privacy-friendly: Signal, QKSMS, etc. (0)
     * Unknown: Everything else (0, displayed as unknown)
     */
    fun checkDefaultSms(): PrivacyIssue {
        return try {
            val defaultSmsPackage = Telephony.Sms.getDefaultSmsPackage(context) ?: "none"

            // Privacy-invasive messaging apps
            val invasive = invasiveSms(defaultSmsPackage)

            if (invasive != null) {
                val (points, name, _) = invasive
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_SMS,
                    isSecure = false,
                    currentStatus = "Using $name",
                    technicalDetails = "Package: $defaultSmsPackage",
                    customPointDeduction = points
                )
            }

            // Privacy-friendly messaging apps
            val friendly = friendlySms(defaultSmsPackage)

            if (friendly != null) {
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_SMS,
                    isSecure = true,
                    currentStatus = "Using $friendly",
                    technicalDetails = "Package: $defaultSmsPackage",
                    customPointDeduction = 0
                )
            }

            // No default or unknown
            if (defaultSmsPackage == "none") {
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_SMS,
                    isSecure = true,
                    currentStatus = "No default SMS app set",
                    technicalDetails = "Package: $defaultSmsPackage",
                    customPointDeduction = 0
                )
            } else {
                val appName = PackageManagerUtil.getAppName(packageManager, defaultSmsPackage)
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_SMS,
                    isSecure = true,
                    isUnknown = true,
                    currentStatus = "Using $appName (unknown, 0 pts)",
                    technicalDetails = "Package: $defaultSmsPackage",
                    customPointDeduction = 0
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking default SMS", e)
            PrivacyIssue(
                check = PrivacyCheck.DEFAULT_SMS,
                isSecure = true,
                isUnknown = true,
                currentStatus = "Unable to determine default SMS app",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    /**
     * Check default keyboard with allowlist approach
     * Privacy-friendly (allowlist, see friendlyKeyboard): AOSP, OpenBoard, FlorisBoard, AnySoftKeyboard, HeliBoard,
     * Simple Keyboard, FUTO, Unexpected Keyboard, Thumb-Key, Fossify Keyboard, Fcitx5, Trime, Indic Keyboard (0 pts)
     * Everything else: Insecure (-3 pts)
     *
     * This catches ALL non-privacy keyboards including: Gboard, SwiftKey, Samsung, Xiaomi, Huawei, OnePlus, Oppo, Vivo, and any OEM keyboard
     */
    fun checkDefaultKeyboard(): PrivacyIssue {
        return try {
            val currentKeyboard = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: "none"

            // Privacy-friendly keyboards (allowlist)
            val friendly = friendlyKeyboard(currentKeyboard)

            if (friendly != null) {
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_KEYBOARD,
                    isSecure = true,
                    currentStatus = "Using $friendly",
                    technicalDetails = "IME: $currentKeyboard",
                    customPointDeduction = 0
                )
            }

            // No default keyboard
            if (currentKeyboard == "none") {
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_KEYBOARD,
                    isSecure = true,
                    currentStatus = "No default keyboard set",
                    technicalDetails = "IME: $currentKeyboard",
                    customPointDeduction = 0
                )
            }

            // Everything else is non-privacy-friendly (including Gboard, SwiftKey, Samsung, Xiaomi, Huawei, etc.)
            val keyboardPackage = currentKeyboard.substringBefore('/')
            val appName = PackageManagerUtil.getAppName(packageManager, keyboardPackage)
            return PrivacyIssue(
                check = PrivacyCheck.DEFAULT_KEYBOARD,
                isSecure = false,
                currentStatus = "Using $appName",
                technicalDetails = "IME: $currentKeyboard",
                customPointDeduction = 3,
                flaggedPackages = listOf(keyboardPackage)
            )
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking default keyboard", e)
            PrivacyIssue(
                check = PrivacyCheck.DEFAULT_KEYBOARD,
                isSecure = true,
                isUnknown = true,
                currentStatus = "Unable to determine default keyboard",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    /**
     * Check default email app with three-tier detection
     * Privacy-invasive: Gmail (-2), Outlook (-2), Yahoo Mail (-2), Samsung Email (-1)
     * Privacy-friendly: K-9 Mail, FairEmail, ProtonMail, etc. (0)
     * Unknown: Everything else (0, displayed as unknown)
     */
    fun checkDefaultEmail(): PrivacyIssue {
        return try {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
            }

            val resolveInfo = packageManager.resolveActivity(emailIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val packageName = resolveInfo?.activityInfo?.packageName ?: "none"

            // Privacy-invasive email apps
            val invasive = invasiveEmail(packageName)

            if (invasive != null) {
                val (points, name, _) = invasive
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_EMAIL,
                    isSecure = false,
                    currentStatus = "Using $name",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = points
                )
            }

            // Privacy-friendly email apps
            val friendly = friendlyEmail(packageName)

            if (friendly != null) {
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_EMAIL,
                    isSecure = true,
                    currentStatus = "Using $friendly",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = 0
                )
            }

            // No default or unknown
            if (packageName == "android" || packageName == "none") {
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_EMAIL,
                    isSecure = true,
                    currentStatus = "No default email app set",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = 0
                )
            } else {
                val appName = PackageManagerUtil.getAppName(packageManager, packageName)
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_EMAIL,
                    isSecure = true,
                    isUnknown = true,
                    currentStatus = "Using $appName (unknown, 0 pts)",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = 0
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking default email", e)
            PrivacyIssue(
                check = PrivacyCheck.DEFAULT_EMAIL,
                isSecure = true,
                isUnknown = true,
                currentStatus = "Unable to determine default email app",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    /**
     * Check default launcher with three-tier detection
     * Privacy-invasive: Nova Launcher (-2), Microsoft Launcher (-2), Samsung/Xiaomi with ads (-2), Pixel Launcher (-2)
     * Privacy-friendly: Lawnchair, KISS Launcher, etc. (0)
     * Unknown: Everything else (0, displayed as unknown)
     */
    fun checkDefaultLauncher(): PrivacyIssue {
        return try {
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }

            val resolveInfo = packageManager.resolveActivity(launcherIntent, PackageManager.MATCH_DEFAULT_ONLY)
            val packageName = resolveInfo?.activityInfo?.packageName ?: "none"

            // Privacy-invasive launchers
            val invasive = invasiveLauncher(packageName)

            if (invasive != null) {
                val (points, name, _) = invasive
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_LAUNCHER,
                    isSecure = false,
                    currentStatus = "Using $name",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = points
                )
            }

            // Privacy-friendly launchers
            val friendly = friendlyLauncher(packageName)

            if (friendly != null) {
                return PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_LAUNCHER,
                    isSecure = true,
                    currentStatus = "Using $friendly",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = 0
                )
            }

            // No default or unknown
            if (packageName == "android" || packageName == "none") {
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_LAUNCHER,
                    isSecure = true,
                    currentStatus = "No default launcher set",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = 0
                )
            } else {
                val appName = PackageManagerUtil.getAppName(packageManager, packageName)
                PrivacyIssue(
                    check = PrivacyCheck.DEFAULT_LAUNCHER,
                    isSecure = true,
                    isUnknown = true,
                    currentStatus = "Using $appName (unknown, 0 pts)",
                    technicalDetails = "Package: $packageName",
                    customPointDeduction = 0
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Error checking default launcher", e)
            PrivacyIssue(
                check = PrivacyCheck.DEFAULT_LAUNCHER,
                isSecure = true,
                isUnknown = true,
                currentStatus = "Unable to determine default launcher",
                technicalDetails = "Error: ${e.message}",
                customPointDeduction = 0
            )
        }
    }

    companion object {
        private const val TAG = "DefaultAppsChecker"

        // Package classifiers, kept pure so they can be unit-tested without PackageManager.
        // invasive*() returns (points, display name, unused); friendly*() returns the display name.

        // Package-name rules: exact names, or a documented prefix where one app ships under
        // several packages. Generic words ("tor", "focus", "edge", "chrome") are never matched
        // as substrings, since they collide with unrelated packages.
        internal fun invasiveBrowser(packageName: String): Triple<Int, String, Boolean>? = when {
            // Stable is com.android.chrome; Beta/Dev/Canary are com.chrome.<channel>
            packageName == "com.android.chrome" || packageName.startsWith("com.chrome.") -> Triple(3, "Chrome", false)
            // Edge ships as com.microsoft.emmx (plus .beta/.dev/.canary)
            packageName.startsWith("com.microsoft.emmx") -> Triple(3, "Microsoft Edge", false)
            // Opera publishes several browsers under com.opera.* (browser, mini.native, gx, ...)
            packageName.startsWith("com.opera.") -> Triple(3, "Opera", false)
            // UC Browser is com.UCMobile.intl; UC Mini and regional builds are com.uc.browser.*
            packageName == "com.UCMobile.intl" || packageName.startsWith("com.uc.browser.") -> Triple(3, "UC Browser", false)
            // Samsung Internet stable and .beta
            packageName.startsWith("com.sec.android.app.sbrowser") -> Triple(2, "Samsung Internet", false)
            else -> null
        }

        internal fun friendlyBrowser(packageName: String): String? = when {
            // Brave stable, _beta and _nightly
            packageName.startsWith("com.brave.browser") -> "Brave"
            // Firefox stable and _beta
            packageName.startsWith("org.mozilla.firefox") -> "Firefox"
            // Focus ships as Klar in German-speaking markets
            packageName == "org.mozilla.focus" || packageName == "org.mozilla.klar" -> "Firefox Focus"
            packageName == "com.duckduckgo.mobile.android" -> "DuckDuckGo Browser"
            packageName == "app.vanadium.browser" -> "Vanadium"
            packageName == "org.cromite.cromite" -> "Cromite"
            packageName == "us.spotco.fennec_dos" -> "Mull"
            // Tor Browser stable and _alpha
            packageName.startsWith("org.torproject.torbrowser") -> "Tor Browser"
            else -> null
        }

        internal fun invasiveSms(packageName: String): Triple<Int, String, Boolean>? = when {
            packageName == "com.google.android.apps.messaging" -> Triple(2, "Google Messages", false)
            packageName == "com.facebook.orca" -> Triple(3, "Facebook Messenger", false)
            packageName == "com.whatsapp" || packageName == "com.whatsapp.w4b" -> Triple(3, "WhatsApp", false)
            packageName == "com.samsung.android.messaging" -> Triple(2, "Samsung Messages", false)
            // Legacy Samsung token kept from the original list; its exact package is unverified
            packageName.contains("sec.android.messaging", ignoreCase = true) -> Triple(2, "Samsung Messages", false)
            else -> null
        }

        internal fun friendlySms(packageName: String): String? = when {
            packageName == "org.fossify.messages" -> "Fossify Messages"
            packageName == "org.thoughtcrime.securesms" -> "Signal"
            packageName == "im.molly.app" -> "Molly"
            packageName == "com.moez.QKSMS" -> "QKSMS"
            packageName == "chat.simplex.app" -> "SimpleX Chat"
            packageName == "org.smssecure.smssecure" -> "Silence"
            // Exact packages for these two are unverified, so they keep the original tokens
            packageName.contains("asms", ignoreCase = true) -> "aSMS"
            packageName.contains("partisan", ignoreCase = true) -> "Partisan SMS"
            else -> null
        }

        internal fun invasiveEmail(packageName: String): Triple<Int, String, Boolean>? = when {
            // Exact: "google.android.gm" is also a substring of Play Services (com.google.android.gms)
            packageName == "com.google.android.gm" || packageName == "com.google.android.gm.lite" -> Triple(2, "Gmail", false)
            packageName == "com.microsoft.office.outlook" -> Triple(2, "Outlook", false)
            packageName == "com.yahoo.mobile.client.android.mail" -> Triple(2, "Yahoo Mail", false)
            packageName == "com.samsung.android.email.provider" -> Triple(1, "Samsung Email", false)
            // Legacy Samsung token kept from the original list; its exact package is unverified
            packageName.contains("sec.android.email", ignoreCase = true) -> Triple(1, "Samsung Email", false)
            else -> null
        }

        internal fun friendlyEmail(packageName: String): String? = when {
            packageName == "com.fsck.k9" -> "K-9 Mail"
            packageName == "eu.faircode.email" -> "FairEmail"
            packageName == "ch.protonmail.android" -> "ProtonMail"
            packageName == "de.tutao.tutanota" -> "Tutanota"
            // Thunderbird stable and .beta
            packageName.startsWith("net.thunderbird.android") -> "Thunderbird"
            // Exact package unverified, so it keeps the original token
            packageName.contains("simple.mail", ignoreCase = true) -> "Simple Mail"
            else -> null
        }

        /**
         * [imeId] is Settings.Secure.DEFAULT_INPUT_METHOD ("package/class"). Only the package
         * is matched: Gboard (com.google.android.inputmethod.latin) inherits AOSP's
         * com.android.inputmethod.latin.LatinIME class, so class names can't identify a keyboard.
         */
        internal fun friendlyKeyboard(imeId: String): String? = when (imeId.substringBefore('/')) {
            "com.android.inputmethod.latin" -> "AOSP Keyboard"
            // FlorisBoard publishes a stable track and a .beta preview track
            "dev.patrickgold.florisboard", "dev.patrickgold.florisboard.beta" -> "FlorisBoard"
            // HeliBoard attaches its .debug build to every GitHub release alongside release/nouserlib
            "helium314.keyboard", "helium314.keyboard.debug" -> "HeliBoard"
            "com.menny.android.anysoftkeyboard" -> "AnySoftKeyboard"
            "rkr.simplekeyboard.inputmethod" -> "Simple Keyboard"
            // FUTO ships its direct download and its Play Store build under different packages
            "org.futo.inputmethod.latin", "org.futo.inputmethod.latin.playstore" -> "FUTO Keyboard"
            "juloo.keyboard2" -> "Unexpected Keyboard"
            "org.dslul.openboard.inputmethod.latin" -> "OpenBoard"
            "com.dessalines.thumbkey" -> "Thumb-Key"
            "org.fossify.keyboard" -> "Fossify Keyboard"
            "org.fcitx.fcitx5.android" -> "Fcitx5"
            "com.osfans.trime" -> "Trime"
            "org.smc.inputmethod.indic" -> "Indic Keyboard"
            else -> null
        }

        // Launchers match on exact package names so the result never depends on branch
        // order ("olauncher" is a substring of other launcher packages).
        internal fun invasiveLauncher(packageName: String): Triple<Int, String, Boolean>? = when (packageName) {
            "com.teslacoilsw.launcher" -> Triple(2, "Nova Launcher", false)
            "com.microsoft.launcher" -> Triple(2, "Microsoft Launcher", false)
            "com.sec.android.app.launcher" -> Triple(2, "Samsung Launcher", false)
            "com.miui.home" -> Triple(2, "Xiaomi Launcher", false)
            "com.google.android.apps.nexuslauncher" -> Triple(2, "Pixel Launcher", false)
            else -> null
        }

        internal fun friendlyLauncher(packageName: String): String? = when {
            packageName == "fr.neamar.kiss" -> "KISS Launcher"
            packageName == "com.saggitt.omega" -> "Neo Launcher"
            packageName == "app.olauncher" || packageName == "app.olaunchercf" -> "Olauncher"
            packageName == "com.android.launcher3" -> "AOSP Launcher"
            // Lawnchair has shipped as app.lawnchair, app.lawnchair.play and
            // ch.deletescape.lawnchair.* (v2 / CI builds), so it keeps a substring match
            packageName.contains("lawnchair", ignoreCase = true) -> "Lawnchair"
            // Kvaesitso publishes release and nightly builds as de.mm20.launcher2.<channel>
            packageName.startsWith("de.mm20.launcher2.") -> "Kvaesitso"
            else -> null
        }
    }
}
