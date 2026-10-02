package com.techtrest.privamatic.data.scanner.checks

import android.content.Context
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.model.getSecurityIssuesCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class SecuritySettingsCheckerTest {

    private companion object {
        const val SCREEN_LOCK_UNKNOWN = "Cannot detect — verify Screen lock in Settings"
    }

    private fun context(): Context = mock(Context::class.java).also {
        `when`(it.getString(R.string.status_screen_lock_unknown)).thenReturn(SCREEN_LOCK_UNKNOWN)
    }

    private fun assertUnknownAtZero(issue: PrivacyIssue) {
        assertTrue(issue.isUnknown)
        assertFalse(issue.isSecure)
        assertEquals(0, issue.pointDeduction)
        assertEquals(SCREEN_LOCK_UNKNOWN, issue.currentStatus)
        val score = PrivacyScore(score = 100, issues = listOf(issue))
        assertEquals("unknowns are not counted as issues", 0, score.getSecurityIssuesCount())
    }

    @Test
    fun `screen lock with no keyguard service is unknown at 0 points`() {
        val ctx = context()
        `when`(ctx.getSystemService(Context.KEYGUARD_SERVICE)).thenReturn(null)
        assertUnknownAtZero(SecuritySettingsChecker(ctx).checkScreenLock())
    }

    @Test
    fun `screen lock check that throws is unknown at 0 points`() {
        val ctx = context()
        `when`(ctx.getSystemService(Context.KEYGUARD_SERVICE)).thenThrow(SecurityException("denied"))
        assertUnknownAtZero(SecuritySettingsChecker(ctx).checkScreenLock())
    }
}
