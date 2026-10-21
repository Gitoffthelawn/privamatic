package com.techtrest.privamatic.data.scanner.checks

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.Settings
import android.provider.Telephony
import android.util.Log
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.PrivacyIssue
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.MockedConstruction
import org.mockito.MockedStatic
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockConstruction
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.`when`

/**
 * Pins the score of every branch of the default-app checks, so copy changes can't move
 * points. Each row is (isSecure, isUnknown, pointDeduction).
 */
class DefaultAppsCheckerScoringTest {

    private val pm = mock(PackageManager::class.java)

    /** Context.getString is final; the inline mock maker stubs it with "res:<id>:<args>". */
    private val context = mock(Context::class.java) { invocation ->
        when (invocation.method.name) {
            "getPackageManager" -> pm
            "getString" -> "res:" + invocation.arguments.joinToString(":")
            else -> RETURNS_DEFAULTS.answer(invocation)
        }
    }

    private lateinit var intents: MockedConstruction<Intent>
    private lateinit var uri: MockedStatic<Uri>
    private lateinit var sms: MockedStatic<Telephony.Sms>
    private lateinit var secure: MockedStatic<Settings.Secure>
    private lateinit var log: MockedStatic<Log>

    private val checker get() = DefaultAppsChecker(context)

    @Before
    fun setUp() {
        intents = mockConstruction(Intent::class.java)
        uri = mockStatic(Uri::class.java)
        sms = mockStatic(Telephony.Sms::class.java)
        secure = mockStatic(Settings.Secure::class.java)
        log = mockStatic(Log::class.java)
    }

    @After
    fun tearDown() {
        intents.close()
        uri.close()
        sms.close()
        secure.close()
        log.close()
    }

    private fun resolvesTo(packageName: String?) {
        val info = packageName?.let { name ->
            mock(ResolveInfo::class.java).apply {
                activityInfo = mock(ActivityInfo::class.java).apply { this.packageName = name }
            }
        }
        `when`(pm.resolveActivity(any(), anyInt())).thenReturn(info)
    }

    private fun failsToResolve() {
        `when`(pm.resolveActivity(any(), anyInt())).thenThrow(RuntimeException("boom"))
    }

    private fun smsIs(packageName: String?) {
        sms.`when`<String?> { Telephony.Sms.getDefaultSmsPackage(any()) }.thenReturn(packageName)
    }

    private fun imeIs(imeId: String?) {
        secure.`when`<String?> { Settings.Secure.getString(any(), any()) }.thenReturn(imeId)
    }

    private fun PrivacyIssue.score() = Triple(isSecure, isUnknown, pointDeduction)

    private val rated = Triple(false, false, 0)
    private fun invasive(points: Int) = Triple(false, false, points)
    private val secureKnown = Triple(true, false, 0)
    private val unknown = Triple(true, true, 0)

    @Test
    fun `browser branches keep their points`() {
        resolvesTo("com.android.chrome"); assertEquals(invasive(3), checker.checkDefaultBrowser().score())
        resolvesTo("com.sec.android.app.sbrowser"); assertEquals(invasive(2), checker.checkDefaultBrowser().score())
        resolvesTo("org.mozilla.firefox"); assertEquals(secureKnown, checker.checkDefaultBrowser().score())
        resolvesTo("android"); assertEquals(secureKnown, checker.checkDefaultBrowser().score())
        resolvesTo(null); assertEquals(secureKnown, checker.checkDefaultBrowser().score())
        resolvesTo("org.example.browser"); assertEquals(unknown, checker.checkDefaultBrowser().score())
        failsToResolve(); assertEquals(unknown, checker.checkDefaultBrowser().score())
    }

    @Test
    fun `sms branches keep their points`() {
        smsIs("com.google.android.apps.messaging"); assertEquals(invasive(2), checker.checkDefaultSms().score())
        smsIs("com.whatsapp"); assertEquals(invasive(3), checker.checkDefaultSms().score())
        smsIs("org.fossify.messages"); assertEquals(secureKnown, checker.checkDefaultSms().score())
        smsIs(null); assertEquals(secureKnown, checker.checkDefaultSms().score())
        smsIs("org.example.sms"); assertEquals(unknown, checker.checkDefaultSms().score())
        sms.`when`<String?> { Telephony.Sms.getDefaultSmsPackage(any()) }.thenThrow(RuntimeException("boom"))
        assertEquals(unknown, checker.checkDefaultSms().score())
    }

    @Test
    fun `email branches keep their points`() {
        resolvesTo("com.google.android.gm"); assertEquals(invasive(2), checker.checkDefaultEmail().score())
        resolvesTo("com.samsung.android.email.provider"); assertEquals(invasive(1), checker.checkDefaultEmail().score())
        resolvesTo("eu.faircode.email"); assertEquals(secureKnown, checker.checkDefaultEmail().score())
        resolvesTo("android"); assertEquals(secureKnown, checker.checkDefaultEmail().score())
        resolvesTo(null); assertEquals(secureKnown, checker.checkDefaultEmail().score())
        resolvesTo("org.example.mail"); assertEquals(unknown, checker.checkDefaultEmail().score())
        failsToResolve(); assertEquals(unknown, checker.checkDefaultEmail().score())
    }

    @Test
    fun `launcher branches keep their points`() {
        resolvesTo("com.google.android.apps.nexuslauncher"); assertEquals(invasive(2), checker.checkDefaultLauncher().score())
        resolvesTo("fr.neamar.kiss"); assertEquals(secureKnown, checker.checkDefaultLauncher().score())
        resolvesTo("android"); assertEquals(secureKnown, checker.checkDefaultLauncher().score())
        resolvesTo(null); assertEquals(secureKnown, checker.checkDefaultLauncher().score())
        resolvesTo("org.example.home"); assertEquals(unknown, checker.checkDefaultLauncher().score())
        failsToResolve(); assertEquals(unknown, checker.checkDefaultLauncher().score())
    }

    @Test
    fun `keyboard branches keep their points`() {
        imeIs("com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME")
        assertEquals(invasive(3), checker.checkDefaultKeyboard().score())
        imeIs("helium314.keyboard/.LatinIME"); assertEquals(secureKnown, checker.checkDefaultKeyboard().score())
        imeIs(null); assertEquals(secureKnown, checker.checkDefaultKeyboard().score())
        secure.`when`<String?> { Settings.Secure.getString(any(), any()) }.thenThrow(RuntimeException("boom"))
        assertEquals(unknown, checker.checkDefaultKeyboard().score())
    }

    @Test
    fun `unrated default apps show the not-rated copy and carry their name`() {
        // getApplicationInfo is unstubbed, so the label lookup falls back to the package name
        val notRated = { pkg: String -> "res:${R.string.fmt_default_app_not_rated}:$pkg" }

        resolvesTo("org.example.browser")
        checker.checkDefaultBrowser().let {
            assertEquals(notRated("org.example.browser"), it.currentStatus)
            assertEquals("org.example.browser", it.appName)
        }
        smsIs("org.example.sms")
        checker.checkDefaultSms().let {
            assertEquals(notRated("org.example.sms"), it.currentStatus)
            assertEquals("org.example.sms", it.appName)
        }
        resolvesTo("org.example.mail")
        checker.checkDefaultEmail().let {
            assertEquals(notRated("org.example.mail"), it.currentStatus)
            assertEquals("org.example.mail", it.appName)
        }
        resolvesTo("org.example.home")
        checker.checkDefaultLauncher().let {
            assertEquals(notRated("org.example.home"), it.currentStatus)
            assertEquals("org.example.home", it.appName)
        }
    }

    @Test
    fun `rated default apps carry their display name`() {
        resolvesTo("com.android.chrome"); assertEquals("Chrome", checker.checkDefaultBrowser().appName)
        smsIs("org.fossify.messages"); assertEquals("Fossify Messages", checker.checkDefaultSms().appName)
        resolvesTo("com.google.android.gm"); assertEquals("Gmail", checker.checkDefaultEmail().appName)
        resolvesTo("com.google.android.apps.nexuslauncher"); assertEquals("Pixel Launcher", checker.checkDefaultLauncher().appName)
        imeIs("com.google.android.inputmethod.latin/com.android.inputmethod.latin.LatinIME")
        assertEquals("com.google.android.inputmethod.latin", checker.checkDefaultKeyboard().appName)
        resolvesTo("android"); assertNull(checker.checkDefaultBrowser().appName)
    }
}
