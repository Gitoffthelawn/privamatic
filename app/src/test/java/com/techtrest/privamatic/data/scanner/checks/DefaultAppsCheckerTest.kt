package com.techtrest.privamatic.data.scanner.checks

import com.techtrest.privamatic.data.scanner.checks.DefaultAppsChecker.Companion.friendlyBrowser
import com.techtrest.privamatic.data.scanner.checks.DefaultAppsChecker.Companion.friendlyEmail
import com.techtrest.privamatic.data.scanner.checks.DefaultAppsChecker.Companion.friendlyKeyboard
import com.techtrest.privamatic.data.scanner.checks.DefaultAppsChecker.Companion.friendlyLauncher
import com.techtrest.privamatic.data.scanner.checks.DefaultAppsChecker.Companion.friendlySms
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

    // ===== Keyboard: match the package part of "package/class" only =====

    @Test
    fun `Gboard on Pixel 8 is not mistaken for the AOSP keyboard`() {
        // Exact Settings.Secure.DEFAULT_INPUT_METHOD value read from a Pixel 8
        assertNull(friendlyKeyboard("com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME"))
    }

    @Test
    fun `AOSP keyboard is privacy-friendly`() {
        assertEquals("AOSP Keyboard", friendlyKeyboard("com.android.inputmethod.latin/.LatinIME"))
    }

    @Test
    fun `FlorisBoard is privacy-friendly`() {
        assertEquals("FlorisBoard", friendlyKeyboard("dev.patrickgold.florisboard/.FlorisImeService"))
        assertEquals("FlorisBoard", friendlyKeyboard("dev.patrickgold.florisboard.beta/dev.patrickgold.florisboard.FlorisImeService"))
    }

    @Test
    fun `HeliBoard is privacy-friendly`() {
        assertEquals("HeliBoard", friendlyKeyboard("helium314.keyboard/helium314.keyboard.latin.LatinIME"))
    }

    @Test
    fun `AnySoftKeyboard is privacy-friendly`() {
        assertEquals("AnySoftKeyboard", friendlyKeyboard("com.menny.android.anysoftkeyboard/.SoftKeyboard"))
    }

    @Test
    fun `Simple Keyboard is privacy-friendly`() {
        assertEquals("Simple Keyboard", friendlyKeyboard("rkr.simplekeyboard.inputmethod/.latin.LatinIME"))
    }

    @Test
    fun `FUTO Keyboard is privacy-friendly in both builds`() {
        assertEquals("FUTO Keyboard", friendlyKeyboard("org.futo.inputmethod.latin/.LatinIME"))
        assertEquals("FUTO Keyboard", friendlyKeyboard("org.futo.inputmethod.latin.playstore/org.futo.inputmethod.latin.LatinIME"))
    }

    @Test
    fun `Unexpected Keyboard is privacy-friendly`() {
        assertEquals("Unexpected Keyboard", friendlyKeyboard("juloo.keyboard2/.Keyboard2"))
    }

    @Test
    fun `OpenBoard is privacy-friendly`() {
        assertEquals("OpenBoard", friendlyKeyboard("org.dslul.openboard.inputmethod.latin/.LatinIME"))
    }

    @Test
    fun `a keyboard is never identified by its class name`() {
        assertNull(friendlyKeyboard("com.example.ime/dev.patrickgold.florisboard.FlorisImeService"))
    }

    // ===== Substring collisions fixed in the same audit =====

    @Test
    fun `Play Services is not mistaken for Gmail`() {
        assertNull(invasiveEmail("com.google.android.gms"))
        assertEquals(2, invasiveEmail("com.google.android.gm")?.first)
    }

    @Test
    fun `Chromium builds are not mistaken for Chrome`() {
        assertNull(invasiveBrowser("org.chromium.chrome"))
        assertEquals("Chrome", invasiveBrowser("com.android.chrome")?.second)
        assertEquals("Chrome", invasiveBrowser("com.chrome.beta")?.second)
    }

    @Test
    fun `generic words in browser packages do not match`() {
        assertNull(friendlyBrowser("com.example.navigator.browser"))   // "tor" + "browser"
        assertNull(friendlyBrowser("com.example.focus"))
        assertNull(invasiveBrowser("com.microsoft.knowledge.browser"))  // "edge" + "microsoft"
        assertNull(invasiveBrowser("com.cooperative.browser"))          // "opera"
        assertEquals("Tor Browser", friendlyBrowser("org.torproject.torbrowser"))
        assertEquals("Firefox Focus", friendlyBrowser("org.mozilla.focus"))
    }

    @Test
    fun `Mull is matched by its real package, not Mullvad Browser`() {
        assertEquals("Mull", friendlyBrowser("us.spotco.fennec_dos"))
        assertNull(friendlyBrowser("net.mullvad.mullvadbrowser"))
    }

    @Test
    fun `Signal and Silence are matched by their real packages`() {
        assertEquals("Signal", friendlySms("org.thoughtcrime.securesms"))
        assertEquals("Silence", friendlySms("org.smssecure.smssecure"))
        assertNull(friendlySms("com.example.signalstrength"))
    }

    @Test
    fun `privacy-friendly email apps match exact packages`() {
        assertEquals("K-9 Mail", friendlyEmail("com.fsck.k9"))
        assertEquals("Thunderbird", friendlyEmail("net.thunderbird.android.beta"))
    }
}
