package com.techtrest.privamatic.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The 1.5 notes were introduced in versionCode 8 (1.5.0) and ship unchanged in 9 (1.5.1):
 * anyone whose last-seen version is below 8 sees them once, on either version.
 */
class WhatsNewPreferencesTest {

    private val notes = 8

    @Test
    fun `fresh install shows nothing and marks the version as seen`() {
        val storage = InMemorySharedPreferences()
        assertFalse(WhatsNewPreferences(storage).shouldShow(8, isUpdate = false, notesIntroducedIn = notes))

        // A later update to the same version (e.g. a reinstall) still shows nothing.
        assertFalse(WhatsNewPreferences(storage).shouldShow(8, isUpdate = true, notesIntroducedIn = notes))
    }

    @Test
    fun `fresh install of 1_5_1 shows nothing`() {
        val storage = InMemorySharedPreferences()
        assertFalse(WhatsNewPreferences(storage).shouldShow(9, isUpdate = false, notesIntroducedIn = notes))
        assertFalse(WhatsNewPreferences(storage).shouldShow(9, isUpdate = true, notesIntroducedIn = notes))
    }

    @Test
    fun `update from 1_4_1 to 1_5_0 shows the card`() {
        // v1.4.1 users: no stored value, but the install was updated.
        assertTrue(WhatsNewPreferences(InMemorySharedPreferences()).shouldShow(8, isUpdate = true, notesIntroducedIn = notes))
    }

    @Test
    fun `update from 1_4_1 straight to 1_5_1 shows the card`() {
        assertTrue(WhatsNewPreferences(InMemorySharedPreferences()).shouldShow(9, isUpdate = true, notesIntroducedIn = notes))
    }

    @Test
    fun `last seen below the notes version shows the card`() {
        val storage = InMemorySharedPreferences()
        WhatsNewPreferences(storage).markSeen(7)
        assertTrue(WhatsNewPreferences(storage).shouldShow(9, isUpdate = true, notesIntroducedIn = notes))
    }

    @Test
    fun `update from 1_5_0 with the card dismissed does not show it again on 1_5_1`() {
        val storage = InMemorySharedPreferences()
        WhatsNewPreferences(storage).markSeen(8)
        assertFalse(WhatsNewPreferences(storage).shouldShow(9, isUpdate = true, notesIntroducedIn = notes))
    }

    @Test
    fun `card keeps showing until dismissed`() {
        val storage = InMemorySharedPreferences()
        assertTrue(WhatsNewPreferences(storage).shouldShow(9, isUpdate = true, notesIntroducedIn = notes))
        assertTrue(WhatsNewPreferences(storage).shouldShow(9, isUpdate = true, notesIntroducedIn = notes))
    }

    @Test
    fun `dismissing on 1_5_1 stores 9 and stays dismissed across restarts`() {
        val storage = InMemorySharedPreferences()
        WhatsNewPreferences(storage).markSeen(9)
        assertEquals(9, storage.getInt("last_seen_version_code", -1))
        assertFalse(WhatsNewPreferences(storage).shouldShow(9, isUpdate = true, notesIntroducedIn = notes))
    }

    @Test
    fun `next release with new notes shows again after an earlier dismissal`() {
        val storage = InMemorySharedPreferences()
        WhatsNewPreferences(storage).markSeen(9)
        assertTrue(WhatsNewPreferences(storage).shouldShow(10, isUpdate = true, notesIntroducedIn = 10))
    }

    // v1.5.2 (versionCode 10) ships new notes: the shipped WhatsNew values, not a test constant.

    @Test
    fun `1_5_2 notes are introduced in versionCode 10`() {
        assertEquals(10, WhatsNew.NOTES_INTRODUCED_IN)
        assertEquals(4, WhatsNew.items.size)
    }

    @Test
    fun `update to 1_5_2 after dismissing on 1_5_1 or 1_5_0 shows the card once`() {
        for (lastSeen in listOf(8, 9)) {
            val storage = InMemorySharedPreferences()
            WhatsNewPreferences(storage).markSeen(lastSeen)
            assertTrue(WhatsNewPreferences(storage).shouldShow(10, isUpdate = true, notesIntroducedIn = WhatsNew.NOTES_INTRODUCED_IN))

            WhatsNewPreferences(storage).markSeen(10)
            assertFalse(WhatsNewPreferences(storage).shouldShow(10, isUpdate = true, notesIntroducedIn = WhatsNew.NOTES_INTRODUCED_IN))
        }
    }

    @Test
    fun `update to 1_5_2 from 1_4_1 shows the card`() {
        assertTrue(WhatsNewPreferences(InMemorySharedPreferences()).shouldShow(10, isUpdate = true, notesIntroducedIn = WhatsNew.NOTES_INTRODUCED_IN))
    }

    @Test
    fun `fresh install of 1_5_2 shows nothing`() {
        val storage = InMemorySharedPreferences()
        assertFalse(WhatsNewPreferences(storage).shouldShow(10, isUpdate = false, notesIntroducedIn = WhatsNew.NOTES_INTRODUCED_IN))
        assertFalse(WhatsNewPreferences(storage).shouldShow(10, isUpdate = true, notesIntroducedIn = WhatsNew.NOTES_INTRODUCED_IN))
    }
}
