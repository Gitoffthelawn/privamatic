package com.techtrest.privamatic.data.scanner.checks

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.PrivacyCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Regression guard for #7 (LineageOS for microG) and #19 (CalyxOS): microG bundled
 * in the system partition must score as microG, never as privileged Play Services.
 * This failed twice before because both scoring and the "is it microG" answer hinge
 * on [com.techtrest.privamatic.data.util.PackageManagerUtil.isMicroGInstalled], so the
 * test pins that the microG branch wins over the MATCH_SYSTEM_ONLY branch — and that
 * real GMS in the same position is still penalised.
 */
class GoogleServicesCheckerTest {

    private companion object {
        const val GMS = "com.google.android.gms"
        const val MICROG_STATUS = "MicroG installed (privacy-friendly replacement)"
    }

    /** android.jar stubs throw on construction, so build the PM data objects as mocks with fields set. */
    private fun activity(className: String): ActivityInfo =
        mock(ActivityInfo::class.java).apply { name = className }

    /** A gms package that resolves under MATCH_SYSTEM_ONLY (system partition) with the given activities. */
    private fun checkerWithSystemGms(vararg activityNames: String): GoogleServicesChecker {
        val pm = mock(PackageManager::class.java)
        val appInfo = mock(ApplicationInfo::class.java).apply { enabled = true }
        val info = mock(PackageInfo::class.java).apply {
            activities = activityNames.map { activity(it) }.toTypedArray()
        }
        `when`(pm.getApplicationInfo(GMS, 0)).thenReturn(appInfo)
        `when`(pm.getApplicationInfo(GMS, PackageManager.MATCH_SYSTEM_ONLY)).thenReturn(appInfo)
        `when`(pm.getPackageInfo(GMS, PackageManager.GET_ACTIVITIES)).thenReturn(info)

        val context = mock(Context::class.java)
        `when`(context.packageManager).thenReturn(pm)
        // The checker reads this string for the microG branch; an unstubbed mock would
        // return null and trip the catch-all into "Unable to determine" (a false pass).
        `when`(context.getString(R.string.status_google_play_services_microg)).thenReturn(MICROG_STATUS)
        return GoogleServicesChecker(context)
    }

    @Test
    fun `microG bundled as a system app scores as microG, not as privileged Play Services`() {
        val issue = checkerWithSystemGms(
            "org.microg.gms.ui.SettingsActivity",
            "org.microg.gms.ui.SelfCheckActivity"
        ).checkGooglePlayServices()

        assertEquals(PrivacyCheck.GOOGLE_PLAY_SERVICES, issue.check)
        assertTrue("system-partition microG must not fall through to the -8 branch", issue.isSecure)
        assertEquals(0, issue.pointDeduction)
        assertFalse(issue.isSystemApp)
        assertEquals(MICROG_STATUS, issue.currentStatus)
    }

    @Test
    fun `real Play Services as a system app is still penalised with full privileges`() {
        val issue = checkerWithSystemGms(
            "com.google.android.gms.app.settings.GoogleSettingsActivity",
            "com.google.android.gms.auth.uiflows.common.LoginActivity"
        ).checkGooglePlayServices()

        assertFalse(issue.isSecure)
        assertTrue(issue.isSystemApp)
        assertEquals(8, issue.pointDeduction)
        assertEquals("Installed with full system privileges", issue.currentStatus)
    }
}
