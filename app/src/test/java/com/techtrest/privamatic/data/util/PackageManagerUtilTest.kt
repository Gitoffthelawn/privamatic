package com.techtrest.privamatic.data.util

import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class PackageManagerUtilTest {

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private companion object {
        const val GMS = "com.google.android.gms"
    }

    /** android.jar stubs throw on construction, so build the PM data objects as mocks with fields set. */
    private fun activity(className: String): ActivityInfo =
        mock(ActivityInfo::class.java).apply { name = className }

    private fun packageManagerWithGmsActivities(vararg classNames: String): PackageManager {
        val info = mock(PackageInfo::class.java).apply {
            activities = classNames.map { activity(it) }.toTypedArray()
        }
        val pm = mock(PackageManager::class.java)
        `when`(pm.getPackageInfo(GMS, PackageManager.GET_ACTIVITIES)).thenReturn(info)
        return pm
    }

    // -------------------------------------------------------------------------
    // isMicroGInstalled
    // -------------------------------------------------------------------------

    @Test
    fun `microG detected when gms package declares an org microg activity`() {
        val pm = packageManagerWithGmsActivities(
            "org.microg.gms.ui.SettingsActivity",
            "org.microg.gms.ui.SelfCheckActivity"
        )
        assertTrue(PackageManagerUtil.isMicroGInstalled(pm))
    }

    @Test
    fun `microG detected even when only one org microg component sits among google ones`() {
        val pm = packageManagerWithGmsActivities(
            "com.google.android.gms.app.settings.GoogleSettingsActivity",
            "org.microg.gms.ui.SettingsActivity"
        )
        assertTrue(PackageManagerUtil.isMicroGInstalled(pm))
    }

    @Test
    fun `real Play Services with no org microg components is not microG`() {
        val pm = packageManagerWithGmsActivities(
            "com.google.android.gms.app.settings.GoogleSettingsActivity",
            "com.google.android.gms.auth.uiflows.common.LoginActivity",
            "com.google.android.gms.ads.AdActivity"
        )
        assertFalse(PackageManagerUtil.isMicroGInstalled(pm))
    }

    @Test
    fun `gms package with no activities at all is not microG`() {
        val info = mock(PackageInfo::class.java) // activities stays null
        val pm = mock(PackageManager::class.java)
        `when`(pm.getPackageInfo(GMS, PackageManager.GET_ACTIVITIES)).thenReturn(info)
        assertFalse(PackageManagerUtil.isMicroGInstalled(pm))
    }

    @Test
    fun `gms package not installed falls through to not microG`() {
        val pm = mock(PackageManager::class.java)
        `when`(pm.getPackageInfo(GMS, PackageManager.GET_ACTIVITIES))
            .thenThrow(PackageManager.NameNotFoundException(GMS))
        assertFalse(PackageManagerUtil.isMicroGInstalled(pm))
    }

    @Test
    fun `package manager failure degrades to not microG rather than crashing the scan`() {
        val pm = mock(PackageManager::class.java)
        `when`(pm.getPackageInfo(GMS, PackageManager.GET_ACTIVITIES))
            .thenThrow(RuntimeException("binder transaction too large"))
        assertFalse(PackageManagerUtil.isMicroGInstalled(pm))
    }

    // -------------------------------------------------------------------------
    // isMicroGPackage — the flag consumed by the #11 trust-toggle bypass
    // -------------------------------------------------------------------------

    @Test
    fun `isMicroGPackage only claims the gms package and only when microG is present`() {
        assertTrue(PackageManagerUtil.isMicroGPackage(GMS, isMicroGInstalled = true))
        assertFalse(PackageManagerUtil.isMicroGPackage(GMS, isMicroGInstalled = false))
        assertFalse(PackageManagerUtil.isMicroGPackage("com.google.android.gsf", isMicroGInstalled = true))
    }
}
