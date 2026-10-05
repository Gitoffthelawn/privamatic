package com.techtrest.privamatic.data.scanner.checks

import android.content.Context
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.model.getSecurityIssuesCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Android 17 (SDK 37) redacts adb_enabled and development_settings_enabled to 0 for apps,
 * so there a 0 is never a pass: only a 1 or a detected ADB decides, anything else is unknown.
 */
class DeveloperSettingsCheckerTest {

    private companion object {
        const val SDK_16 = 36
        const val SDK_17 = 37
        const val USB_UNKNOWN = "Cannot detect — verify USB debugging"
        const val DEV_UNKNOWN = "Cannot detect — verify Developer options"
        const val REDACTED = "Android hides this setting from apps."
    }

    private val checker = DeveloperSettingsChecker(mock(Context::class.java).also {
        `when`(it.getString(R.string.status_usb_debugging_unknown)).thenReturn(USB_UNKNOWN)
        `when`(it.getString(R.string.status_developer_options_unknown)).thenReturn(DEV_UNKNOWN)
        `when`(it.getString(R.string.status_developer_settings_redacted)).thenReturn(REDACTED)
    })

    private val noSignal = { false }
    private val adbSignal = { true }
    private val mustNotRead: () -> Boolean = { fail("USB_STATE must not be read when the setting decides"); false }

    private fun assertFails(issue: PrivacyIssue, check: PrivacyCheck) {
        assertFalse(issue.isSecure)
        assertFalse(issue.isUnknown)
        assertEquals(check.pointDeduction, issue.pointDeduction)
        assertEquals("Enabled", issue.currentStatus)
    }

    private fun assertPasses(issue: PrivacyIssue) {
        assertTrue(issue.isSecure)
        assertFalse(issue.isUnknown)
        assertEquals(0, issue.pointDeduction)
        assertEquals("Disabled", issue.currentStatus)
    }

    private fun assertUnknownAtZero(issue: PrivacyIssue, status: String) {
        assertTrue(issue.isUnknown)
        assertFalse("never a pass", issue.isSecure)
        assertEquals(0, issue.pointDeduction)
        assertEquals(status, issue.currentStatus)
        assertEquals(0, PrivacyScore(score = 100, issues = listOf(issue)).getSecurityIssuesCount())
    }

    // ===== USB debugging =====

    @Test
    fun `usb debugging read as 1 fails on every SDK`() {
        assertFails(checker.usbDebuggingIssue(1, SDK_16, mustNotRead), PrivacyCheck.USB_DEBUGGING)
        assertFails(checker.usbDebuggingIssue(1, SDK_17, mustNotRead), PrivacyCheck.USB_DEBUGGING)
    }

    @Test
    fun `usb debugging read as 0 below SDK 37 passes`() {
        assertPasses(checker.usbDebuggingIssue(0, SDK_16, mustNotRead))
    }

    @Test
    fun `usb debugging read as 0 on SDK 37 without an ADB signal is unknown`() {
        assertUnknownAtZero(checker.usbDebuggingIssue(0, SDK_17, noSignal), REDACTED)
    }

    @Test
    fun `usb debugging read as 0 on SDK 37 with an ADB signal fails`() {
        assertFails(checker.usbDebuggingIssue(0, SDK_17, adbSignal), PrivacyCheck.USB_DEBUGGING)
    }

    @Test
    fun `usb debugging check that throws is unknown`() {
        // Settings.Global is an android.jar stub here, so reading it throws like a real failure
        val issue = checker.checkUsbDebugging(SDK_16)
        assertUnknownAtZero(issue, USB_UNKNOWN)
        assertTrue(issue.technicalDetails!!.startsWith("Error:"))
    }

    // ===== Developer options =====

    @Test
    fun `developer options read as 1 fail on every SDK`() {
        assertFails(checker.developerOptionsIssue(1, 0, SDK_16, mustNotRead), PrivacyCheck.DEVELOPER_OPTIONS)
        assertFails(checker.developerOptionsIssue(1, 0, SDK_17, mustNotRead), PrivacyCheck.DEVELOPER_OPTIONS)
    }

    @Test
    fun `developer options read as 0 below SDK 37 pass`() {
        assertPasses(checker.developerOptionsIssue(0, 0, SDK_16, mustNotRead))
    }

    @Test
    fun `developer options read as 0 on SDK 37 without an ADB signal are unknown`() {
        assertUnknownAtZero(checker.developerOptionsIssue(0, 0, SDK_17, noSignal), REDACTED)
    }

    @Test
    fun `developer options read as 0 on SDK 37 fail when the USB_STATE broadcast shows ADB`() {
        assertFails(checker.developerOptionsIssue(0, 0, SDK_17, adbSignal), PrivacyCheck.DEVELOPER_OPTIONS)
    }

    @Test
    fun `developer options read as 0 on SDK 37 fail when adb_enabled reads 1`() {
        assertFails(checker.developerOptionsIssue(0, 1, SDK_17, mustNotRead), PrivacyCheck.DEVELOPER_OPTIONS)
    }

    @Test
    fun `developer options check that throws is unknown`() {
        val issue = checker.checkDeveloperOptions(SDK_17)
        assertUnknownAtZero(issue, DEV_UNKNOWN)
        assertTrue(issue.technicalDetails!!.startsWith("Error:"))
    }
}
