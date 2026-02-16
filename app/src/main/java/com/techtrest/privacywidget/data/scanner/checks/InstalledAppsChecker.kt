package com.techtrest.privacywidget.data.scanner.checks

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.techtrest.privacywidget.data.model.PackageNames
import com.techtrest.privacywidget.data.model.PrivacyCheck
import com.techtrest.privacywidget.data.model.PrivacyIssue

class InstalledAppsChecker(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager

    // ===== GOOGLE APPS (MAJOR) =====

    fun checkGoogleChrome() = checkAppInstalled(PrivacyCheck.GOOGLE_CHROME)
    fun checkGmailInstalled() = checkAppInstalled(PrivacyCheck.GMAIL_APP)
    fun checkGoogleMapsInstalled() = checkAppInstalled(PrivacyCheck.GOOGLE_MAPS)
    fun checkGooglePhotosInstalled() = checkAppInstalled(PrivacyCheck.GOOGLE_PHOTOS)
    fun checkGoogleDriveInstalled() = checkAppInstalled(PrivacyCheck.GOOGLE_DRIVE)

    // ===== GOOGLE APPS (MINOR) =====

    fun checkYouTubeInstalled() = checkAppInstalled(PrivacyCheck.YOUTUBE)
    fun checkGoogleCalendarInstalled() = checkAppInstalled(PrivacyCheck.GOOGLE_CALENDAR)
    fun checkGoogleKeepInstalled() = checkAppInstalled(PrivacyCheck.GOOGLE_KEEP)
    fun checkGoogleCameraInstalled() = checkAppInstalled(PrivacyCheck.GOOGLE_CAMERA)
    fun checkGoogleDocsInstalled() = checkAppInstalled(PrivacyCheck.GOOGLE_DOCS)

    // ===== META/FACEBOOK APPS =====

    fun checkFacebookInstalled() = checkAppInstalled(PrivacyCheck.FACEBOOK_APP)
    fun checkInstagramInstalled() = checkAppInstalled(PrivacyCheck.INSTAGRAM_APP)
    fun checkWhatsAppInstalled() = checkAppInstalled(PrivacyCheck.WHATSAPP_APP)
    fun checkMessengerInstalled() = checkAppInstalled(PrivacyCheck.MESSENGER_APP)

    // ===== MICROSOFT APPS =====

    fun checkEdgeInstalled() = checkAppInstalled(PrivacyCheck.EDGE_APP)
    fun checkOutlookInstalled() = checkAppInstalled(PrivacyCheck.OUTLOOK_APP)
    fun checkOneDriveInstalled() = checkAppInstalled(PrivacyCheck.ONEDRIVE_APP)

    // ===== AMAZON APPS =====

    fun checkAmazonShoppingInstalled() = checkAppInstalled(PrivacyCheck.AMAZON_SHOPPING)
    fun checkPrimeVideoInstalled() = checkAppInstalled(PrivacyCheck.PRIME_VIDEO)

    // ===== AI/LLM APPS =====

    fun checkChatGPTInstalled() = checkAppInstalled(PrivacyCheck.CHATGPT_APP)

    fun checkGoogleGeminiInstalled(): PrivacyIssue {
        // Google Gemini/Bard - check multiple possible package names
        val possiblePackages = listOf(
            PackageNames.GEMINI,
            PackageNames.GEMINI_ALT
        )

        for (pkg in possiblePackages) {
            if (isAppInstalled(pkg)) {
                return PrivacyIssue(
                    check = PrivacyCheck.GOOGLE_GEMINI,
                    isSecure = false,
                    currentStatus = "Installed and enabled",
                    technicalDetails = "Package: $pkg"
                )
            }
        }

        return PrivacyIssue(
            check = PrivacyCheck.GOOGLE_GEMINI,
            isSecure = true,
            currentStatus = "Not installed or disabled",
            technicalDetails = "Checked: ${possiblePackages.joinToString(", ")}"
        )
    }

    fun checkMicrosoftCopilotInstalled() = checkAppInstalled(PrivacyCheck.MICROSOFT_COPILOT)
    fun checkClaudeInstalled() = checkAppInstalled(PrivacyCheck.CLAUDE_APP)
    fun checkPerplexityInstalled() = checkAppInstalled(PrivacyCheck.PERPLEXITY_APP)
    fun checkMetaAIInstalled() = checkAppInstalled(PrivacyCheck.META_AI)

    // ===== SOCIAL MEDIA =====

    fun checkTikTokInstalled() = checkAppInstalled(PrivacyCheck.TIKTOK_APP)
    fun checkTwitterInstalled() = checkAppInstalled(PrivacyCheck.TWITTER_APP)
    fun checkRedditInstalled() = checkAppInstalled(PrivacyCheck.REDDIT_APP)

    // ===== HELPER METHODS =====

    /**
     * Check if an app is installed using package name and point deduction from the PrivacyCheck enum.
     */
    private fun checkAppInstalled(check: PrivacyCheck): PrivacyIssue {
        val packageName = check.packageName
            ?: return PrivacyIssue(
                check = check,
                isSecure = true,
                currentStatus = "Unable to determine",
                technicalDetails = "No package name configured"
            )

        return try {
            val isInstalled = isAppInstalled(packageName)

            PrivacyIssue(
                check = check,
                isSecure = !isInstalled,
                currentStatus = if (isInstalled) "Installed" else "Not installed",
                technicalDetails = "Package: $packageName"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking ${check.displayName}", e)
            PrivacyIssue(
                check = check,
                isSecure = true,
                currentStatus = "Unable to determine",
                technicalDetails = "Error: ${e.message}"
            )
        }
    }

    /**
     * Helper function to check if an app is installed AND enabled.
     * Only returns true if the app is both installed and enabled.
     */
    private fun isAppInstalled(packageName: String): Boolean {
        Log.d(TAG, "Checking for package: $packageName")

        // Method 1: Try getPackageInfo
        try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA)
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val isEnabled = appInfo.enabled

            Log.d(TAG, "✓ Package found: $packageName (Enabled: $isEnabled)")

            if (!isEnabled) {
                Log.d(TAG, "⚠ Package is DISABLED - not counting as installed")
                return false
            }
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            // Package not found, continue to next method
        } catch (e: Exception) {
            Log.w(TAG, "Error checking package $packageName: ${e.message}")
        }

        // Method 2: Try getApplicationInfo
        try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val isEnabled = appInfo.enabled

            Log.d(TAG, "✓ Package found via getApplicationInfo: $packageName (Enabled: $isEnabled)")

            if (!isEnabled) {
                Log.d(TAG, "⚠ Package is DISABLED - not counting as installed")
                return false
            }
            return true
        } catch (e: PackageManager.NameNotFoundException) {
            // Package not found
        } catch (e: Exception) {
            Log.w(TAG, "Error checking package $packageName: ${e.message}")
        }

        Log.d(TAG, "✗ Package NOT found or DISABLED: $packageName")
        return false
    }

    companion object {
        private const val TAG = "InstalledAppsChecker"
    }
}
