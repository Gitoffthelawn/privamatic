package com.techtrest.privamatic.data.util

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

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

    /** The Google package name microG installs itself under. */
    private const val MICROG_SPOOFED_PACKAGE = "com.google.android.gms"

    /** Class-name prefix shared by every component in microG's manifest. */
    private const val MICROG_COMPONENT_PREFIX = "org.microg."
}
