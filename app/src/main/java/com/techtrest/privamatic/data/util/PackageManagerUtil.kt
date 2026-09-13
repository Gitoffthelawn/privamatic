package com.techtrest.privamatic.data.util

import android.Manifest
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.techtrest.privamatic.data.model.PackageNames

/**
 * Shared helpers for resolving package metadata via the [PackageManager].
 * Centralises the duplicated app-name / system-app lookups used by the scanners.
 */
object PackageManagerUtil {

    /**
     * Resolve the user-facing label for a package, falling back to the package
     * name itself when the app cannot be resolved.
     */
    fun getAppName(packageManager: PackageManager, packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    /**
     * Whether the given package is a system app. On failure to resolve, assumes
     * a system app to avoid false positives.
     */
    fun isSystemApp(packageManager: PackageManager, packageName: String): Boolean {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Whether microG is installed. microG installs under Google's package name via
     * signature spoofing, but spoofing only fakes the certificate — the manifest is
     * microG's own, so its components live in the `org.microg.` namespace (e.g.
     * `org.microg.gms.ui.SettingsActivity`). Real Play Services never ships such
     * components. Sibling packages (droidguard, nlp) are no longer a usable signal:
     * modern microG compiles them into GmsCore itself (#11, #19).
     *
     * This lookup hits the PackageManager; call it once per scan, not per package.
     */
    fun isMicroGInstalled(packageManager: PackageManager): Boolean {
        return try {
            val info = packageManager.getPackageInfo(
                MICROG_SPOOFED_PACKAGE, PackageManager.GET_ACTIVITIES
            )
            info.activities?.any { it.name.startsWith(MICROG_COMPONENT_PREFIX) } == true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Whether [packageName] is served by microG rather than by Google. Takes the
     * already-resolved [isMicroGInstalled] result so callers can evaluate microG
     * presence once and still check many packages cheaply.
     */
    fun isMicroGPackage(packageName: String, isMicroGInstalled: Boolean): Boolean =
        isMicroGInstalled && packageName == MICROG_SPOOFED_PACKAGE

    /**
     * Why the installed `com.google.android.apps.photos` package is a Gcam
     * photo-preview shim rather than Google Photos, or null when it is genuine
     * (or not installed at all — the caller owns that case).
     *
     * Gcam only checks that a package by that name exists and handles the REVIEW
     * intent, so shims simply take Google's applicationId outright — no signature
     * spoofing, and unlike microG no foreign component namespace to key on
     * (CalyxOS's shim even lives in `com.google.android.apps.photos.*`). What every
     * shim shares is the reason it exists: no network. Google Photos cannot ship
     * without INTERNET, so a package that never requests it is not Google Photos.
     * A missing launcher activity corroborates (shims are headless, Photos is
     * not) so either signal is enough (#20).
     */
    fun googlePhotosShimReason(packageManager: PackageManager): String? {
        return try {
            @Suppress("DEPRECATION")
            val info = packageManager.getPackageInfo(
                PackageNames.GOOGLE_PHOTOS, PackageManager.GET_PERMISSIONS
            )
            val requestsInternet = info.requestedPermissions
                ?.contains(Manifest.permission.INTERNET) == true
            val hasLauncher =
                packageManager.getLaunchIntentForPackage(PackageNames.GOOGLE_PHOTOS) != null
            when {
                !requestsInternet -> "does not request android.permission.INTERNET"
                !hasLauncher -> "has no launcher activity"
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Whether the installed Google Photos package is a shim; see [googlePhotosShimReason]. */
    fun isGooglePhotosShim(packageManager: PackageManager): Boolean =
        googlePhotosShimReason(packageManager) != null

    /** The Google package name microG installs itself under. */
    private const val MICROG_SPOOFED_PACKAGE = "com.google.android.gms"

    /** Class-name prefix shared by every component in microG's manifest. */
    private const val MICROG_COMPONENT_PREFIX = "org.microg."
}
