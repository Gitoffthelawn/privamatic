package com.techtrest.privamatic.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlaggedAppTest {

    // -------------------------------------------------------------------------
    // Vendor blacklist
    // -------------------------------------------------------------------------

    @Test
    fun `WhatsApp main package is blacklisted`() {
        assertTrue(FlaggedApp.isBlacklisted("com.whatsapp"))
    }

    @Test
    fun `WhatsApp Business is blacklisted`() {
        assertTrue(FlaggedApp.isBlacklisted("com.whatsapp.w4b"))
    }

    @Test
    fun `Gboard is blacklisted`() {
        assertTrue(FlaggedApp.isBlacklisted("com.google.android.inputmethod.latin"))
    }

    @Test
    fun `Facebook is blacklisted`() {
        assertTrue(FlaggedApp.isBlacklisted("com.facebook.katana"))
    }

    @Test
    fun `lookalike sharing only a string prefix is not blacklisted`() {
        assertFalse(FlaggedApp.isBlacklisted("com.whatsappfoo"))
        assertFalse(FlaggedApp.isBlacklisted("com.googlefoo.app"))
    }

    // -------------------------------------------------------------------------
    // microG exemption
    // -------------------------------------------------------------------------

    @Test
    fun `microG is not blacklisted`() {
        assertFalse(FlaggedApp.isBlacklisted("com.google.android.gms", isMicroGInstalled = true))
    }

    @Test
    fun `genuine Play Services stays blacklisted`() {
        assertTrue(FlaggedApp.isBlacklisted("com.google.android.gms", isMicroGInstalled = false))
    }
}
