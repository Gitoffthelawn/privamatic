package com.techtrest.privamatic.data.model

import com.techtrest.privamatic.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivacyIssueTest {

    private fun installed(check: PrivacyCheck, isSystemApp: Boolean) = PrivacyIssue(
        check = check,
        isSecure = false,
        currentStatus = "Installed",
        isSystemApp = isSystemApp
    )

    @Test
    fun `play services offers no settings action`() {
        // Disabling GMS on stock Android breaks the Play Store, push and Find Hub (audit H3).
        assertNull(PrivacyCheck.GOOGLE_PLAY_SERVICES.actionType)
    }

    @Test
    fun `play services is not treated as a preinstalled app row`() {
        val issue = installed(PrivacyCheck.GOOGLE_PLAY_SERVICES, isSystemApp = true)
        assertFalse(issue.isPreinstalledApp)
    }

    @Test
    fun `preinstalled app gets the generic disable advice`() {
        val issue = installed(PrivacyCheck.GOOGLE_MAPS, isSystemApp = true)
        assertTrue(issue.isPreinstalledApp)
        assertEquals(R.string.fmt_recommendation_system_app, issue.systemAppRecommendation)
    }

    @Test
    fun `preinstalled camera warns that disabling leaves no camera`() {
        val issue = installed(PrivacyCheck.GOOGLE_CAMERA, isSystemApp = true)
        assertEquals(
            R.string.privacy_check_google_camera_system_recommendation,
            issue.systemAppRecommendation
        )
    }

    @Test
    fun `user-installed app keeps its uninstall recommendation`() {
        val issue = installed(PrivacyCheck.GOOGLE_KEEP, isSystemApp = false)
        assertFalse(issue.isPreinstalledApp)
        assertEquals(PrivacyCheck.GOOGLE_KEEP.recommendation, issue.recommendation)
    }
}
