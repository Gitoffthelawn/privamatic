package com.techtrest.privamatic.data.scanner.checks

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.techtrest.privamatic.data.model.PackageNames
import com.techtrest.privamatic.data.model.PrivacyCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * End-to-end behaviour of the GOOGLE_PHOTOS check against Gcam photo-preview
 * shims that squat Google Photos' package name (#20).
 */
class InstalledAppsCheckerTest {

    private companion object {
        const val PHOTOS = PackageNames.GOOGLE_PHOTOS
    }

    /** android.jar stubs throw on construction, so build the PM data objects as mocks with fields set. */
    private fun checkerWithPhotos(
        permissions: Array<String>?,
        hasLauncher: Boolean,
        isSystem: Boolean = false
    ): InstalledAppsChecker {
        val pm = mock(PackageManager::class.java)
        val appInfo = mock(ApplicationInfo::class.java).apply {
            enabled = true
            flags = if (isSystem) ApplicationInfo.FLAG_SYSTEM else 0
        }
        val info = mock(PackageInfo::class.java).apply { requestedPermissions = permissions }
        `when`(pm.getPackageInfo(eq(PHOTOS), anyInt())).thenReturn(info)
        `when`(pm.getApplicationInfo(PHOTOS, 0)).thenReturn(appInfo)
        `when`(pm.getLaunchIntentForPackage(PHOTOS))
            .thenReturn(if (hasLauncher) mock(Intent::class.java) else null)
        return checker(pm)
    }

    private fun checker(pm: PackageManager): InstalledAppsChecker {
        val context = mock(Context::class.java)
        `when`(context.packageManager).thenReturn(pm)
        return InstalledAppsChecker(context)
    }

    @Test
    fun `real Google Photos is still penalised`() {
        val issue = checkerWithPhotos(
            permissions = arrayOf(Manifest.permission.INTERNET, Manifest.permission.CAMERA),
            hasLauncher = true
        ).checkGooglePhotosInstalled()

        assertEquals(PrivacyCheck.GOOGLE_PHOTOS, issue.check)
        assertFalse(issue.isSecure)
        assertEquals("Installed", issue.currentStatus)
        assertEquals(PrivacyCheck.GOOGLE_PHOTOS.pointDeduction, issue.pointDeduction)
    }

    @Test
    fun `Gcam Services Provider photosonly flavor is not penalised`() {
        val issue = checkerWithPhotos(permissions = null, hasLauncher = false)
            .checkGooglePhotosInstalled()

        assertTrue(issue.isSecure)
        assertEquals(0, issue.pointDeduction)
        assertEquals(
            "Not installed (Gcam photo-preview shim detected, not Google Photos)",
            issue.currentStatus
        )
        assertEquals(
            "Package: $PHOTOS does not request android.permission.INTERNET",
            issue.technicalDetails
        )
    }

    @Test
    fun `CalyxOS GCamPhotosPreview system shim is not penalised`() {
        val issue = checkerWithPhotos(
            permissions = arrayOf(Manifest.permission.QUERY_ALL_PACKAGES),
            hasLauncher = false,
            isSystem = true
        ).checkGooglePhotosInstalled()

        assertTrue(issue.isSecure)
        assertEquals(0, issue.pointDeduction)
        assertEquals(
            "Not installed (Gcam photo-preview shim detected, not Google Photos)",
            issue.currentStatus
        )
    }

    @Test
    fun `Google Photos not installed keeps the existing not-installed result`() {
        val pm = mock(PackageManager::class.java)
        `when`(pm.getPackageInfo(anyString(), anyInt()))
            .thenThrow(PackageManager.NameNotFoundException(PHOTOS))
        `when`(pm.getApplicationInfo(anyString(), anyInt()))
            .thenThrow(PackageManager.NameNotFoundException(PHOTOS))

        val issue = checker(pm).checkGooglePhotosInstalled()

        assertTrue(issue.isSecure)
        assertEquals("Not installed", issue.currentStatus)
        assertEquals("Package: $PHOTOS", issue.technicalDetails)
    }
}
