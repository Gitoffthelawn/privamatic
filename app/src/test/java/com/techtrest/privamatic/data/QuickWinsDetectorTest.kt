package com.techtrest.privamatic.data

import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.model.QuickWin
import com.techtrest.privamatic.data.model.QuickWinType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickWinsDetectorTest {

    private fun issue(check: PrivacyCheck, isSecure: Boolean, isUnknown: Boolean = false) =
        PrivacyIssue(check = check, isSecure = isSecure, isUnknown = isUnknown, currentStatus = "")

    private fun score(vararg issues: PrivacyIssue) = PrivacyScore(score = 50, issues = issues.toList())

    @Test
    fun `dev options win includes usb debugging while it is on`() {
        val win = QuickWinsDetector.checkDeveloperOptions(score(
            issue(PrivacyCheck.DEVELOPER_OPTIONS, isSecure = false),
            issue(PrivacyCheck.USB_DEBUGGING, isSecure = false)
        ))!!

        assertEquals(listOf(PrivacyCheck.USB_DEBUGGING), win.alsoFixes.map { it.check })
        assertEquals(
            PrivacyCheck.DEVELOPER_OPTIONS.pointDeduction + PrivacyCheck.USB_DEBUGGING.pointDeduction,
            win.impact
        )
    }

    @Test
    fun `dev options win is its own points when usb debugging is off`() {
        val win = QuickWinsDetector.checkDeveloperOptions(score(
            issue(PrivacyCheck.DEVELOPER_OPTIONS, isSecure = false),
            issue(PrivacyCheck.USB_DEBUGGING, isSecure = true)
        ))!!

        assertTrue(win.alsoFixes.isEmpty())
        assertEquals(PrivacyCheck.DEVELOPER_OPTIONS.pointDeduction, win.impact)
    }

    @Test
    fun `unknown usb debugging adds nothing`() {
        // Unknown results cost 0 points, so clearing them gains nothing either.
        val win = QuickWinsDetector.checkDeveloperOptions(score(
            issue(PrivacyCheck.DEVELOPER_OPTIONS, isSecure = false),
            issue(PrivacyCheck.USB_DEBUGGING, isSecure = true, isUnknown = true)
        ))!!

        assertEquals(PrivacyCheck.DEVELOPER_OPTIONS.pointDeduction, win.impact)
    }

    @Test
    fun `no dev options win when developer options are off`() {
        assertNull(QuickWinsDetector.checkDeveloperOptions(score(
            issue(PrivacyCheck.DEVELOPER_OPTIONS, isSecure = true),
            issue(PrivacyCheck.USB_DEBUGGING, isSecure = false)
        )))
    }

    @Test
    fun `combined gain is what sorting sees`() {
        val devOptions = QuickWinsDetector.checkDeveloperOptions(score(
            issue(PrivacyCheck.DEVELOPER_OPTIONS, isSecure = false),
            issue(PrivacyCheck.USB_DEBUGGING, isSecure = false)
        ))!!
        val browser = QuickWin(QuickWinType.REPLACE_BROWSER, PrivacyCheck.DEFAULT_BROWSER)

        // 1 + 4 = 5 now outranks the browser's 3; alone, Dev Options (1) would sort last.
        assertEquals(devOptions, listOf(browser, devOptions).sortedByDescending { it.impact }.first())
    }
}
