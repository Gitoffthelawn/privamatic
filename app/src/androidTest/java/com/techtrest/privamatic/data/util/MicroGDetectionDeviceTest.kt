package com.techtrest.privamatic.data.util

import android.content.pm.PackageManager
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

/**
 * On-device verification for #11 / #19. Both tests skip when GMS is absent, and each
 * skips when the *other* flavour of GMS is installed, so exactly one runs per device:
 *
 *  - [realPlayServicesIsNotDetectedAsMicroG]: non-regression control on a device
 *    running *real* Google Play Services (e.g. a stock Pixel).
 *  - [realMicroGInstallIsDetected]: positive path on a device where the official
 *    microG GmsCore APK is installed as com.google.android.gms (e.g. an AOSP emulator
 *    with `adb install` of the APK from https://repo.microg.org/fdroid/repo).
 *
 * Which flavour is present is decided by the signer, not by the code under test, so the
 * assertions cannot be trivially self-fulfilling.
 */
@RunWith(AndroidJUnit4::class)
class MicroGDetectionDeviceTest {

    @Test
    fun realPlayServicesIsNotDetectedAsMicroG() {
        val pm = InstrumentationRegistry.getInstrumentation().targetContext.packageManager
        val gms = installedGms(pm)
        assumeTrue("Play Services not installed on this device; control test not applicable", gms != null)
        assumeTrue("com.google.android.gms is signed by microG, not Google; control test not applicable", !isMicroGSigned(pm))

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

    @Test
    fun realMicroGInstallIsDetected() {
        val pm = InstrumentationRegistry.getInstrumentation().targetContext.packageManager
        val gms = installedGms(pm)
        assumeTrue("No com.google.android.gms on this device; microG positive test not applicable", gms != null)
        assumeTrue("com.google.android.gms is signed by Google, not microG; positive test not applicable", isMicroGSigned(pm))

        val activities = gms!!.activities.orEmpty()
        val microgComponents = activities.filter { it.name.startsWith("org.microg.") }
        val label = pm.getApplicationLabel(gms.applicationInfo!!)
        Log.i(TAG, "microG '$label' ${gms.versionName}: ${activities.size} activities, ${microgComponents.size} org.microg.* e.g. ${microgComponents.take(3).map { it.name }}")

        assertTrue("microG must expose org.microg components under the gms package", microgComponents.isNotEmpty())
        assertTrue("isMicroGInstalled must be true on a real microG install", PackageManagerUtil.isMicroGInstalled(pm))
    }

    private fun installedGms(pm: PackageManager) = try {
        pm.getPackageInfo(GMS, PackageManager.GET_ACTIVITIES)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    /** True when the installed gms package is signed with microG's release key rather than Google's. */
    private fun isMicroGSigned(pm: PackageManager): Boolean {
        val info = pm.getPackageInfo(GMS, PackageManager.GET_SIGNING_CERTIFICATES)
        val signers = info.signingInfo?.apkContentsSigners.orEmpty()
        return signers.any { sig ->
            val digest = MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
            digest.joinToString("") { "%02x".format(it) } == MICROG_SIGNER_SHA256
        }
    }

    private companion object {
        const val TAG = "MicroGDetectionDeviceTest"
        const val GMS = "com.google.android.gms"

        /** SHA-256 of microG's release signing certificate (O=NOGAPPS Project, C=DE). */
        const val MICROG_SIGNER_SHA256 = "9bd06727e62796c0130eb6dab39b73157451582cbd138e86c468acc395d14165"
    }
}
