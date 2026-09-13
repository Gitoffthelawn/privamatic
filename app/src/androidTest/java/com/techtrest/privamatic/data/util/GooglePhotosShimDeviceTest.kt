package com.techtrest.privamatic.data.util

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest

/**
 * On-device verification for #20. Both tests skip when nothing is installed under
 * Google Photos' package name, and each skips unless the package carries the signer it
 * expects, so exactly one runs per device:
 *
 *  - [realGooglePhotosIsNotDetectedAsShim]: non-regression control on a device running
 *    *real* Google Photos (e.g. a stock Pixel).
 *  - [realGcamServicesProviderIsDetectedAsShim]: positive path on a device where the
 *    official Gcam Services Provider *photosonly* (or *photos*) release APK is installed
 *    as com.google.android.apps.photos (e.g. the AOSP `microg_test_api35` emulator with
 *    `adb install` of the APK from the project's GitHub releases).
 *
 * Which flavour is present is decided by the signer, not by the code under test, so the
 * assertions cannot be trivially self-fulfilling.
 */
@RunWith(AndroidJUnit4::class)
class GooglePhotosShimDeviceTest {

    @Test
    fun realGooglePhotosIsNotDetectedAsShim() {
        val pm = InstrumentationRegistry.getInstrumentation().targetContext.packageManager
        val photos = installedPhotos(pm)
        assumeTrue("Google Photos not installed on this device; control test not applicable", photos != null)
        assumeTrue("com.google.android.apps.photos is not signed by Google; control test not applicable", isSignedBy(pm, GOOGLE_SIGNER_SHA256))

        val (requestsInternet, hasLauncher) = logSignals(pm, photos!!, "Google Photos")

        assertTrue("real Google Photos must request INTERNET", requestsInternet)
        assertTrue("real Google Photos must have a launcher activity", hasLauncher)
        assertNull("googlePhotosShimReason must be null on real Google Photos", PackageManagerUtil.googlePhotosShimReason(pm))
        assertFalse("isGooglePhotosShim must be false on real Google Photos", PackageManagerUtil.isGooglePhotosShim(pm))
    }

    @Test
    fun realGcamServicesProviderIsDetectedAsShim() {
        val pm = InstrumentationRegistry.getInstrumentation().targetContext.packageManager
        val photos = installedPhotos(pm)
        assumeTrue("No com.google.android.apps.photos on this device; shim positive test not applicable", photos != null)
        assumeTrue("com.google.android.apps.photos is not signed by Gcam Services Provider's key; positive test not applicable", isSignedBy(pm, GSP_SIGNER_SHA256))

        val (requestsInternet, hasLauncher) = logSignals(pm, photos!!, "Gcam Services Provider")

        assertFalse("Gcam Services Provider must not request INTERNET", requestsInternet)
        assertFalse("Gcam Services Provider must have no launcher activity", hasLauncher)
        assertEquals(
            "does not request android.permission.INTERNET",
            PackageManagerUtil.googlePhotosShimReason(pm)
        )
        assertTrue("isGooglePhotosShim must be true on a real Gcam Services Provider install", PackageManagerUtil.isGooglePhotosShim(pm))
    }

    /** Returns (requestsInternet, hasLauncher) and logs the raw evidence for the test report. */
    private fun logSignals(pm: PackageManager, photos: android.content.pm.PackageInfo, what: String): Pair<Boolean, Boolean> {
        val permissions = photos.requestedPermissions.orEmpty()
        val requestsInternet = Manifest.permission.INTERNET in permissions
        val launcher = pm.getLaunchIntentForPackage(PHOTOS)
        val label = pm.getApplicationLabel(photos.applicationInfo!!)
        Log.i(TAG, "$what '$label' ${photos.versionName}: ${permissions.size} requested permissions " +
            "(INTERNET=$requestsInternet, e.g. ${permissions.take(5)}), launcher=${launcher?.component}")
        return requestsInternet to (launcher != null)
    }

    private fun installedPhotos(pm: PackageManager) = try {
        pm.getPackageInfo(PHOTOS, PackageManager.GET_PERMISSIONS)
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    /** True when any certificate in the package's signing lineage matches one of [sha256s]. */
    private fun isSignedBy(pm: PackageManager, sha256s: Set<String>): Boolean {
        val info = pm.getPackageInfo(PHOTOS, PackageManager.GET_SIGNING_CERTIFICATES)
        val signingInfo = info.signingInfo ?: return false
        val certs = if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo.signingCertificateHistory
        }
        return certs.orEmpty().any { sig ->
            val digest = MessageDigest.getInstance("SHA-256").digest(sig.toByteArray())
            digest.joinToString("") { "%02x".format(it) } in sha256s
        }
    }

    private companion object {
        const val TAG = "GooglePhotosShimDeviceTest"
        const val PHOTOS = "com.google.android.apps.photos"

        /** SHA-256 of Gcam Services Provider's release signing certificate (CN=Lukas Pieper). */
        val GSP_SIGNER_SHA256 = setOf("357c243c84da6b10b6b34fcdccabb6c9f839d8cf0f7d93d4f45f0213fed6bc5e")

        /**
         * SHA-256 of Google Photos' signing certificates: the rotated key in use since
         * API 33 (O=Google Inc.) and the original key it rotated from (O="Google, Inc").
         */
        val GOOGLE_SIGNER_SHA256 = setOf(
            "5aad2bee6db95d17e05a08d7d1e64c10a1511879154483916b6ae6c7fd9cb0c6",
            "3d7a1223019aa39d9ea0e3436ab7c0896bfb4fb679f4de5fe7c23f326c8f994a"
        )
    }
}
