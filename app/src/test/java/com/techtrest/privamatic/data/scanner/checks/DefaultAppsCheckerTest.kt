package com.techtrest.privamatic.data.scanner.checks

import com.techtrest.privamatic.data.scanner.checks.DefaultAppsChecker.Companion.friendlyLauncher
import com.techtrest.privamatic.data.scanner.checks.DefaultAppsChecker.Companion.invasiveBrowser
import com.techtrest.privamatic.data.scanner.checks.DefaultAppsChecker.Companion.invasiveEmail
import com.techtrest.privamatic.data.scanner.checks.DefaultAppsChecker.Companion.invasiveLauncher
import com.techtrest.privamatic.data.scanner.checks.DefaultAppsChecker.Companion.invasiveSms
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Package classification for default apps. Each previously-unknown package here used
 * to fall through every branch and score as "unknown, 0 pts".
 */
class DefaultAppsCheckerTest {

    @Test
    fun `Pixel Launcher is privacy-invasive at 2 points`() {
        val match = invasiveLauncher("com.google.android.apps.nexuslauncher")
        assertEquals(2, match?.first)
        assertEquals("Pixel Launcher", match?.second)
        assertNull(friendlyLauncher("com.google.android.apps.nexuslauncher"))
    }

    @Test
    fun `KISS Launcher is detected by its real package`() {
        assertEquals("KISS Launcher", friendlyLauncher("fr.neamar.kiss"))
        assertNull(invasiveLauncher("fr.neamar.kiss"))
    }

    @Test
    fun `launcher matching is exact and independent of branch order`() {
        assertEquals("Neo Launcher", friendlyLauncher("com.saggitt.omega"))
        assertEquals("Olauncher", friendlyLauncher("app.olauncher"))
        assertEquals("AOSP Launcher", friendlyLauncher("com.android.launcher3"))
        // Substrings of known packages no longer match
        assertNull(friendlyLauncher("com.example.neolauncher"))
        assertNull(invasiveLauncher("com.google.android.apps.nexuslauncher.fake"))
    }

    @Test
    fun `launchers with several package variants still match`() {
        assertEquals("Lawnchair", friendlyLauncher("app.lawnchair"))
        assertEquals("Lawnchair", friendlyLauncher("ch.deletescape.lawnchair.plah"))
        assertEquals("Kvaesitso", friendlyLauncher("de.mm20.launcher2.release"))
        assertEquals("Kvaesitso", friendlyLauncher("de.mm20.launcher2.nightly"))
    }

    @Test
    fun `Microsoft Edge is detected by its emmx package`() {
        assertEquals(Triple(3, "Microsoft Edge", false), invasiveBrowser("com.microsoft.emmx"))
        assertEquals(Triple(3, "Microsoft Edge", false), invasiveBrowser("com.microsoft.emmx.beta"))
    }

    @Test
    fun `Samsung Messages is detected by its current package`() {
        assertEquals(Triple(2, "Samsung Messages", false), invasiveSms("com.samsung.android.messaging"))
    }

    @Test
    fun `Samsung Email is detected by its current package`() {
        assertEquals(Triple(1, "Samsung Email", false), invasiveEmail("com.samsung.android.email.provider"))
    }
}
