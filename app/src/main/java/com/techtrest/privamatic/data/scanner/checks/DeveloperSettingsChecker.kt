package com.techtrest.privamatic.data.scanner.checks

import android.content.Context
import android.content.IntentFilter
import android.os.Build
import android.provider.Settings
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue

/**
 * Android 17 (SDK 37) redacts adb_enabled and development_settings_enabled for apps: both read
 * 0 whatever the real value, with no exception (found on stock Pixel CP3A.260905.009). A read
 * of 1 is still the truth, but there a read of 0 proves nothing, so the check stays unknown
 * unless ADB is seen to be on another way. Below SDK 37 a read of 0 is trusted as before.
 */
class DeveloperSettingsChecker(private val context: Context) {

    fun checkUsbDebugging(): PrivacyIssue = checkUsbDebugging(Build.VERSION.SDK_INT)

    /** [sdkInt] is a parameter so tests can take the SDK 37 path (SDK_INT is 0 in unit tests). */
    internal fun checkUsbDebugging(sdkInt: Int): PrivacyIssue {
        return try {
            usbDebuggingIssue(readGlobal(Settings.Global.ADB_ENABLED), sdkInt, ::isAdbActiveOverUsb)
        } catch (e: Exception) {
            unknownUsbDebugging("Error: ${e.message}")
        }
    }

    fun checkDeveloperOptions(): PrivacyIssue = checkDeveloperOptions(Build.VERSION.SDK_INT)

    internal fun checkDeveloperOptions(sdkInt: Int): PrivacyIssue {
        return try {
            developerOptionsIssue(
                developmentSettings = readGlobal(Settings.Global.DEVELOPMENT_SETTINGS_ENABLED),
                adbSetting = readGlobal(Settings.Global.ADB_ENABLED),
                sdkInt = sdkInt,
                adbActiveOverUsb = ::isAdbActiveOverUsb
            )
        } catch (e: Exception) {
            unknownDeveloperOptions("Error: ${e.message}")
        }
    }

    /** [adbActiveOverUsb] is only a positive signal, read only when the setting can't decide. */
    internal fun usbDebuggingIssue(adbSetting: Int, sdkInt: Int, adbActiveOverUsb: () -> Boolean): PrivacyIssue =
        when {
            adbSetting == 1 -> usbDebuggingEnabled("Settings.Global.ADB_ENABLED = 1")
            sdkInt < REDACTING_SDK -> PrivacyIssue(
                check = PrivacyCheck.USB_DEBUGGING,
                isSecure = true,
                currentStatus = "Disabled",
                technicalDetails = "Checked Settings.Global.ADB_ENABLED"
            )
            adbActiveOverUsb() -> usbDebuggingEnabled("USB_STATE broadcast reports adb active")
            else -> unknownUsbDebugging("Settings.Global.ADB_ENABLED reads 0, which Android $sdkInt+ reports whatever the real value")
        }

    /** ADB can only be on while Developer options are, so a detected ADB proves them on. */
    internal fun developerOptionsIssue(
        developmentSettings: Int,
        adbSetting: Int,
        sdkInt: Int,
        adbActiveOverUsb: () -> Boolean
    ): PrivacyIssue = when {
        developmentSettings == 1 -> developerOptionsEnabled("Settings.Global.DEVELOPMENT_SETTINGS_ENABLED = 1")
        sdkInt < REDACTING_SDK -> PrivacyIssue(
            check = PrivacyCheck.DEVELOPER_OPTIONS,
            isSecure = true,
            currentStatus = "Disabled",
            technicalDetails = "Checked Settings.Global.DEVELOPMENT_SETTINGS_ENABLED"
        )
        adbSetting == 1 -> developerOptionsEnabled("Settings.Global.ADB_ENABLED = 1")
        adbActiveOverUsb() -> developerOptionsEnabled("USB_STATE broadcast reports adb active")
        else -> unknownDeveloperOptions("Settings.Global.DEVELOPMENT_SETTINGS_ENABLED reads 0, which Android $sdkInt+ reports whatever the real value")
    }

    private fun readGlobal(name: String): Int = Settings.Global.getInt(context.contentResolver, name, 0)

    /**
     * The sticky USB_STATE broadcast carries "adb" = true while ADB is an active USB function.
     * Both names are @hide, hence the literals. Any failure counts as no signal.
     */
    private fun isAdbActiveOverUsb(): Boolean = try {
        context.registerReceiver(null, IntentFilter(ACTION_USB_STATE))
            ?.getBooleanExtra(EXTRA_USB_FUNCTION_ADB, false) == true
    } catch (e: Exception) {
        false
    }

    private fun usbDebuggingEnabled(details: String) = PrivacyIssue(
        check = PrivacyCheck.USB_DEBUGGING,
        isSecure = false,
        currentStatus = "Enabled",
        technicalDetails = details
    )

    private fun developerOptionsEnabled(details: String) = PrivacyIssue(
        check = PrivacyCheck.DEVELOPER_OPTIONS,
        isSecure = false,
        currentStatus = "Enabled",
        technicalDetails = details
    )

    /**
     * Undetermined results cost 0 points and display as unknown, but stay isSecure = false
     * so the related tips still appear.
     */
    private fun unknownUsbDebugging(details: String) = PrivacyIssue(
        check = PrivacyCheck.USB_DEBUGGING,
        isSecure = false,
        isUnknown = true,
        customPointDeduction = 0,
        currentStatus = context.getString(R.string.status_usb_debugging_unknown),
        technicalDetails = details
    )

    private fun unknownDeveloperOptions(details: String) = PrivacyIssue(
        check = PrivacyCheck.DEVELOPER_OPTIONS,
        isSecure = false,
        isUnknown = true,
        customPointDeduction = 0,
        currentStatus = context.getString(R.string.status_developer_options_unknown),
        technicalDetails = details
    )

    private companion object {
        /** Android 17: the first SDK seen redacting the two settings. */
        const val REDACTING_SDK = 37
        const val ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE"
        const val EXTRA_USB_FUNCTION_ADB = "adb"
    }
}
