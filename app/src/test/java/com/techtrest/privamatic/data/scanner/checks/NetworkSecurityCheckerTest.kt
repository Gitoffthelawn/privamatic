package com.techtrest.privamatic.data.scanner.checks

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.model.getTrackingIssuesCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * Unknown rule: an undetermined result costs 0 points and is isUnknown (excluded from
 * pass and issue counts) but stays isSecure = false so its quick win and tips remain.
 * Advertising ID is the deliberate exception: it keeps -5 until the user confirms.
 */
class NetworkSecurityCheckerTest {

    private companion object {
        const val GMS = "com.google.android.gms"
        const val VPN_UNKNOWN = "Cannot detect — verify VPN in Settings"
        const val DNS_UNKNOWN = "Cannot detect — verify Private DNS in Settings"
        const val AD_ID_NOT_VERIFIED = "Not verified — confirm in Actions"
        const val AD_ID_NOT_APPLICABLE = "Not applicable — no Google Play Services"
    }

    private fun context(): Context = mock(Context::class.java).also {
        `when`(it.getString(R.string.status_vpn_unknown)).thenReturn(VPN_UNKNOWN)
        `when`(it.getString(R.string.status_private_dns_unknown)).thenReturn(DNS_UNKNOWN)
        `when`(it.getString(R.string.status_ad_id_not_verified)).thenReturn(AD_ID_NOT_VERIFIED)
        `when`(it.getString(R.string.status_ad_id_not_applicable)).thenReturn(AD_ID_NOT_APPLICABLE)
    }

    private fun assertUnknownAtZero(issue: PrivacyIssue, status: String) {
        assertTrue(issue.isUnknown)
        assertFalse("quick wins and tips key off !isSecure", issue.isSecure)
        assertEquals(0, issue.pointDeduction)
        assertEquals(status, issue.currentStatus)
        val score = PrivacyScore(score = 100, issues = listOf(issue))
        assertEquals("unknowns are not counted as issues", 0, score.getTrackingIssuesCount())
    }

    @Test
    fun `VPN with no connectivity service is unknown at 0 points`() {
        val ctx = context()
        `when`(ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(null)
        assertUnknownAtZero(NetworkSecurityChecker(ctx).checkVpnConnection(), VPN_UNKNOWN)
    }

    @Test
    fun `VPN check that throws is unknown at 0 points`() {
        val ctx = context()
        `when`(ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).thenThrow(SecurityException("denied"))
        assertUnknownAtZero(NetworkSecurityChecker(ctx).checkVpnConnection(), VPN_UNKNOWN)
    }

    @Test
    fun `Private DNS check that throws is unknown at 0 points`() {
        // Settings.Global is an android.jar stub here, so reading it throws like a real failure
        val issue = NetworkSecurityChecker(context()).checkPrivateDns(Build.VERSION_CODES.P)
        assertUnknownAtZero(issue, DNS_UNKNOWN)
        assertTrue(issue.technicalDetails!!.startsWith("Error:"))
    }

    @Test
    fun `Private DNS in an unrecognised mode is unknown at 0 points`() {
        val issue = NetworkSecurityChecker(context()).privateDnsIssueForMode("vendor_mode", null)
        assertUnknownAtZero(issue, DNS_UNKNOWN)
    }

    @Test
    fun `Private DNS known modes are unchanged`() {
        val checker = NetworkSecurityChecker(context())
        assertTrue(checker.privateDnsIssueForMode(null, null).isSecure)
        assertTrue(checker.privateDnsIssueForMode("opportunistic", null).isSecure)
        assertTrue(checker.privateDnsIssueForMode("hostname", "dns.quad9.net").isSecure)
        val off = checker.privateDnsIssueForMode("off", null)
        assertFalse(off.isSecure)
        assertFalse(off.isUnknown)
        assertEquals(6, off.pointDeduction)
        val noHost = checker.privateDnsIssueForMode("hostname", "")
        assertFalse(noHost.isUnknown)
        assertEquals(6, noHost.pointDeduction)
    }

    private fun adIdContext(gmsInstalled: Boolean, verified: Boolean): Context {
        val ctx = context()
        val pm = mock(PackageManager::class.java)
        if (gmsInstalled) {
            `when`(pm.getApplicationInfo(GMS, 0)).thenReturn(mock(ApplicationInfo::class.java))
        } else {
            `when`(pm.getApplicationInfo(GMS, 0)).thenThrow(PackageManager.NameNotFoundException::class.java)
        }
        `when`(ctx.packageManager).thenReturn(pm)
        val prefs = mock(SharedPreferences::class.java)
        `when`(prefs.getBoolean(NetworkSecurityChecker.KEY_AD_ID_VERIFIED, false)).thenReturn(verified)
        `when`(ctx.getSharedPreferences(NetworkSecurityChecker.AD_ID_PREFS_NAME, Context.MODE_PRIVATE)).thenReturn(prefs)
        return ctx
    }

    @Test
    fun `unverified Ad ID keeps its 5 point deduction and says not verified`() {
        val issue = NetworkSecurityChecker(adIdContext(gmsInstalled = true, verified = false)).checkAdvertisingId()
        assertEquals(PrivacyCheck.ADVERTISING_ID, issue.check)
        assertFalse(issue.isSecure)
        assertFalse("deliberate exception to the unknown rule", issue.isUnknown)
        assertEquals(5, issue.pointDeduction)
        assertEquals(AD_ID_NOT_VERIFIED, issue.currentStatus)
    }

    @Test
    fun `Ad ID without Play Services is not applicable and costs nothing`() {
        val issue = NetworkSecurityChecker(adIdContext(gmsInstalled = false, verified = false)).checkAdvertisingId()
        assertTrue(issue.isSecure)
        assertEquals(0, issue.pointDeduction)
        assertEquals(AD_ID_NOT_APPLICABLE, issue.currentStatus)
    }
}
