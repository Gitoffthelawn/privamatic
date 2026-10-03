package com.techtrest.privamatic.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsNewPreferencesTest {

    private val notes = 8

    @Test
    fun `fresh install shows nothing and marks the version as seen`() {
        val storage = InMemorySharedPreferences()
        assertFalse(WhatsNewPreferences(storage).shouldShow(8, isUpdate = false, notesVersionCode = notes))

        // A later update to the same version (e.g. a reinstall) still shows nothing.
        assertFalse(WhatsNewPreferences(storage).shouldShow(8, isUpdate = true, notesVersionCode = notes))
    }

    @Test
    fun `update from a version that never stored anything shows the card`() {
        // v1.4.1 users: no stored value, but the install was updated.
        assertTrue(WhatsNewPreferences(InMemorySharedPreferences()).shouldShow(8, isUpdate = true, notesVersionCode = notes))
    }

    @Test
    fun `card keeps showing until dismissed`() {
        val storage = InMemorySharedPreferences()
        assertTrue(WhatsNewPreferences(storage).shouldShow(8, isUpdate = true, notesVersionCode = notes))
        assertTrue(WhatsNewPreferences(storage).shouldShow(8, isUpdate = true, notesVersionCode = notes))
    }

    @Test
    fun `dismissed card stays dismissed across restarts`() {
        val storage = InMemorySharedPreferences()
        WhatsNewPreferences(storage).markSeen(8)
        assertFalse(WhatsNewPreferences(storage).shouldShow(8, isUpdate = true, notesVersionCode = notes))
    }

    @Test
    fun `update without notes for this version shows nothing`() {
        // A release that didn't update WhatsNew must not repeat the previous notes.
        val storage = InMemorySharedPreferences()
        WhatsNewPreferences(storage).markSeen(8)
        assertFalse(WhatsNewPreferences(storage).shouldShow(9, isUpdate = true, notesVersionCode = notes))
    }

    @Test
    fun `next release with notes shows again after an earlier dismissal`() {
        val storage = InMemorySharedPreferences()
        WhatsNewPreferences(storage).markSeen(8)
        assertTrue(WhatsNewPreferences(storage).shouldShow(9, isUpdate = true, notesVersionCode = 9))
    }

    @Test
    fun `fresh install of a later version marks it seen`() {
        val storage = InMemorySharedPreferences()
        assertFalse(WhatsNewPreferences(storage).shouldShow(9, isUpdate = false, notesVersionCode = 9))
        assertFalse(WhatsNewPreferences(storage).shouldShow(9, isUpdate = true, notesVersionCode = 9))
    }
}
