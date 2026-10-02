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
     * Privacy-friendly (allowlist): OpenBoard, FlorisBoard, AnySoftKeyboard, HeliBoard, Simple Keyboard, FUTO, Unexpected Keyboard (0 pts)
     * Everything else: Insecure (-3 pts)
     *
     * This catches ALL non-privacy keyboards including: Gboard, SwiftKey, Samsung, Xiaomi, Huawei, OnePlus, Oppo, Vivo, and any OEM keyboard
     */
    fun checkDefaultKeyboard(): PrivacyIssue {
        return try {
            val currentKeyboard = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD) ?: "none"

            // Privacy-friendly keyboards (allowlist)
            val friendly = when {
                currentKeyboard.contains("florisboard", ignoreCase = true) || currentKeyboard.contains("dev.patrickgold.florisboard", ignoreCase = true) -> "FlorisBoard"
                currentKeyboard.contains("heliboard", ignoreCase = true) || currentKeyboard.contains("helium314.keyboard", ignoreCase = true) -> "HeliBoard"
                currentKeyboard.contains("anysoftkeyboard", ignoreCase = true) -> "AnySoftKeyboard"
                currentKeyboard.contains("simplekeyboard", ignoreCase = true) || currentKeyboard.contains("rkr.simplekeyboard.inputmethod", ignoreCase = true) -> "Simple Keyboard"
                currentKeyboard.contains("futo", ignoreCase = true) || currentKeyboard.contains("org.futo.inputmethod.latin", ignoreCase = true) -> "FUTO Keyboard"
                currentKeyboard.contains("unexpected", ignoreCase = true) && currentKeyboard.contains("keyboard") || currentKeyboard.contains("juloo.keyboard2", ignoreCase = true) -> "Unexpected Keyboard"
                currentKeyboard.contains("openboard", ignoreCase = true) || currentKeyboard.contains("org.dslul.openboard.inputmethod.latin", ignoreCase = true) -> "OpenBoard"
                currentKeyboard.contains("android.inputmethod.latin", ignoreCase = true) || currentKeyboard.contains("com.android.inputmethod", ignoreCase = true) -> "AOSP Keyboard"
                else -> null
            }

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

        internal fun invasiveBrowser(packageName: String): Triple<Int, String, Boolean>? = when {
            packageName.contains("chrome", ignoreCase = true) -> Triple(3, "Chrome", false)
            packageName.contains("edge", ignoreCase = true) && packageName.contains("microsoft") -> Triple(3, "Microsoft Edge", false)
            // Edge ships as com.microsoft.emmx (plus .beta/.dev/.canary), which contains no "edge"
            packageName.startsWith("com.microsoft.emmx") -> Triple(3, "Microsoft Edge", false)
            packageName.contains("opera", ignoreCase = true) -> Triple(3, "Opera", false)
            packageName.contains("ucbrowser", ignoreCase = true) || packageName.contains("uc.browser", ignoreCase = true) -> Triple(3, "UC Browser", false)
            packageName.contains("sec.android.app.sbrowser", ignoreCase = true) -> Triple(2, "Samsung Internet", false)
            else -> null
        }

        internal fun friendlyBrowser(packageName: String): String? = when {
            packageName.contains("brave", ignoreCase = true) -> "Brave"
            packageName.contains("firefox", ignoreCase = true) -> "Firefox"
            packageName.contains("focus", ignoreCase = true) -> "Firefox Focus"
            packageName.contains("duckduckgo", ignoreCase = true) -> "DuckDuckGo Browser"
            packageName.contains("vanadium", ignoreCase = true) -> "Vanadium"
            packageName.contains("cromite", ignoreCase = true) -> "Cromite"
            packageName.contains("mull", ignoreCase = true) -> "Mull"
            packageName.contains("tor", ignoreCase = true) && packageName.contains("browser") -> "Tor Browser"
            else -> null
        }

        internal fun invasiveSms(packageName: String): Triple<Int, String, Boolean>? = when {
            packageName.contains("google.android.apps.messaging", ignoreCase = true) -> Triple(2, "Google Messages", false)
            packageName.contains("facebook.orca", ignoreCase = true) -> Triple(3, "Facebook Messenger", false)
            packageName.contains("whatsapp", ignoreCase = true) -> Triple(3, "WhatsApp", false)
            packageName.contains("sec.android.messaging", ignoreCase = true) -> Triple(2, "Samsung Messages", false)
            packageName == "com.samsung.android.messaging" -> Triple(2, "Samsung Messages", false)
            else -> null
        }

        internal fun friendlySms(packageName: String): String? = when {
            packageName.contains("fossify", ignoreCase = true) && packageName.contains("messages", ignoreCase = true) -> "Fossify Messages"
            packageName.contains("signal", ignoreCase = true) || packageName.contains("securesms", ignoreCase = true) -> "Signal"
            packageName.contains("molly", ignoreCase = true) -> "Molly"
            packageName.contains("asms", ignoreCase = true) -> "aSMS"
            packageName.contains("qksms", ignoreCase = true) -> "QKSMS"
            packageName.contains("simplex", ignoreCase = true) -> "SimpleX Chat"
            packageName.contains("partisan", ignoreCase = true) -> "Partisan SMS"
            packageName.contains("silence", ignoreCase = true) -> "Silence"
            else -> null
        }

        internal fun invasiveEmail(packageName: String): Triple<Int, String, Boolean>? = when {
            packageName.contains("google.android.gm", ignoreCase = true) -> Triple(2, "Gmail", false)
            packageName.contains("microsoft.office.outlook", ignoreCase = true) -> Triple(2, "Outlook", false)
            packageName.contains("yahoo.mobile", ignoreCase = true) -> Triple(2, "Yahoo Mail", false)
            packageName.contains("sec.android.email", ignoreCase = true) -> Triple(1, "Samsung Email", false)
            packageName == "com.samsung.android.email.provider" -> Triple(1, "Samsung Email", false)
            else -> null
        }

        internal fun friendlyEmail(packageName: String): String? = when {
            packageName.contains("fsck.k9", ignoreCase = true) -> "K-9 Mail"
            packageName.contains("faircode.email", ignoreCase = true) -> "FairEmail"
            packageName.contains("protonmail", ignoreCase = true) -> "ProtonMail"
            packageName.contains("tutanota", ignoreCase = true) -> "Tutanota"
            packageName.contains("net.thunderbird.android", ignoreCase = true) -> "Thunderbird"
            packageName.contains("simple.mail", ignoreCase = true) -> "Simple Mail"
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
