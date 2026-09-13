package com.techtrest.privamatic.data.util

import android.content.pm.PackageManager
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Non-regression control for #11 / #19: on a device running *real* Google Play
 * Services, the org.microg component scan must not claim microG is installed.
 * Skips when GMS is absent (e.g. an emulator without Google APIs).
 */
@RunWith(AndroidJUnit4::class)
class MicroGDetectionDeviceTest {

    @Test
    fun realPlayServicesIsNotDetectedAsMicroG() {
        val pm = InstrumentationRegistry.getInstrumentation().targetContext.packageManager
        val gms = try {
            pm.getPackageInfo(GMS, PackageManager.GET_ACTIVITIES)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        assumeTrue("Play Services not installed on this device; control test not applicable", gms != null)

        val activities = gms!!.activities.orEmpty()
        val microgComponents = activities.filter { it.name.startsWith("org.microg.") }
        Log.i(TAG, "GMS ${gms.versionName}: ${activities.size} activities, ${microgComponents.size} org.microg.*")

        // Real GMS resolves as a system app — the very condition that made microG score -8 on CalyxOS.
        val isSystem = try {
            pm.getApplicationInfo(GMS, PackageManager.MATCH_SYSTEM_ONLY); true
        } catch (_: PackageManager.NameNotFoundException) { false }
        Log.i(TAG, "GMS MATCH_SYSTEM_ONLY=$isSystem")

        assertFalse("real GMS must not expose org.microg components: $microgComponents", microgComponents.isNotEmpty())
        assertFalse("isMicroGInstalled must be false on stock Google", PackageManagerUtil.isMicroGInstalled(pm))
    }

    private companion object {
        const val TAG = "MicroGDetectionDeviceTest"
        const val GMS = "com.google.android.gms"
    }
}
