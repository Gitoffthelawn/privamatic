package com.techtrest.privamatic.data.maintenance

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.techtrest.privamatic.data.model.ManualCheckState
import com.techtrest.privamatic.data.model.ManualCheckType
import com.techtrest.privamatic.data.scanner.checks.GoogleServicesChecker
import com.techtrest.privamatic.data.scanner.checks.PlayServicesState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * The Advertising ID manual check must agree with the Ad ID scan row: not applicable
 * (hidden) when Play Services is absent, disabled or microG.
 */
class MaintenanceManagerTest {

    private companion object {
        const val GMS = "com.google.android.gms"
    }

    private enum class Gms { ABSENT, SANDBOXED, DISABLED, MICROG }

    /** Play Services state as GoogleServicesChecker decides it for each fixture. */
    private fun playServices(gms: Gms): PlayServicesState {
        val pm = mock(PackageManager::class.java)
        if (gms == Gms.ABSENT) {
            `when`(pm.getApplicationInfo(GMS, 0)).thenThrow(PackageManager.NameNotFoundException::class.java)
        } else {
            // Mocks skip constructors, so ApplicationInfo.enabled must be set explicitly
            val appInfo = mock(ApplicationInfo::class.java).apply { enabled = gms != Gms.DISABLED }
            `when`(pm.getApplicationInfo(GMS, 0)).thenReturn(appInfo)
        }
        `when`(pm.getApplicationInfo(GMS, PackageManager.MATCH_SYSTEM_ONLY))
            .thenThrow(PackageManager.NameNotFoundException::class.java)
        if (gms == Gms.MICROG) {
            val settings = mock(ActivityInfo::class.java).apply { name = "org.microg.gms.ui.SettingsActivity" }
            val info = mock(PackageInfo::class.java).apply { activities = arrayOf(settings) }
            `when`(pm.getPackageInfo(GMS, PackageManager.GET_ACTIVITIES)).thenReturn(info)
        }
        val context = mock(Context::class.java)
        `when`(context.packageManager).thenReturn(pm)
        return GoogleServicesChecker(context).playServicesState()
    }

    private fun neverCompleted(type: ManualCheckType) = ManualCheckState(
        type = type,
        lastCompletedTimestamp = 0L,
        daysRemaining = 0,
        fillPercentage = 0f,
        isOverdue = false
    )

    @Test
    fun `Ad ID manual check is hidden with microG, even when forced`() {
        val state = playServices(Gms.MICROG)
        assertEquals(PlayServicesState.MICROG, state)
        val adId = neverCompleted(ManualCheckType.ADVERTISING_ID_CHECK)
        assertFalse(isCheckVisible(adId, state, forceShowAdIdCheck = false))
        assertFalse(isCheckVisible(adId, state, forceShowAdIdCheck = true))
    }

    @Test
    fun `Ad ID manual check is hidden with disabled Play Services, even when forced`() {
        val state = playServices(Gms.DISABLED)
        assertEquals(PlayServicesState.DISABLED, state)
        val adId = neverCompleted(ManualCheckType.ADVERTISING_ID_CHECK)
        assertFalse(isCheckVisible(adId, state, forceShowAdIdCheck = false))
        assertFalse(isCheckVisible(adId, state, forceShowAdIdCheck = true))
    }

    @Test
    fun `Ad ID manual check is hidden without Play Services`() {
        val state = playServices(Gms.ABSENT)
        assertFalse(isCheckVisible(neverCompleted(ManualCheckType.ADVERTISING_ID_CHECK), state, false))
    }

    @Test
    fun `Ad ID manual check shows with sandboxed Play Services until completed`() {
        val state = playServices(Gms.SANDBOXED)
        assertEquals(PlayServicesState.SANDBOXED, state)
        assertTrue(isCheckVisible(neverCompleted(ManualCheckType.ADVERTISING_ID_CHECK), state, false))
        val completed = neverCompleted(ManualCheckType.ADVERTISING_ID_CHECK)
            .copy(lastCompletedTimestamp = 1L, daysRemaining = 100)
        assertFalse(isCheckVisible(completed, state, false))
        assertTrue(isCheckVisible(completed, state, forceShowAdIdCheck = true))
    }

    @Test
    fun `other manual checks are always visible`() {
        for (gms in Gms.entries) {
            val state = playServices(gms)
            ManualCheckType.entries
                .filter { it != ManualCheckType.ADVERTISING_ID_CHECK }
                .forEach { assertTrue(isCheckVisible(neverCompleted(it), state, false)) }
        }
    }
}
