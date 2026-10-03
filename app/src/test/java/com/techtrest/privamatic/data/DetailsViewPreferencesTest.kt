package com.techtrest.privamatic.data

import com.techtrest.privamatic.ui.navigation.ChecksView
import org.junit.Assert.assertEquals
import org.junit.Test

class DetailsViewPreferencesTest {

    @Test
    fun `defaults to list view when nothing is stored`() {
        assertEquals(ChecksView.LIST, DetailsViewPreferences(InMemorySharedPreferences()).getChecksView())
    }

    @Test
    fun `selected view survives a new instance over the same storage`() {
        val storage = InMemorySharedPreferences()
        DetailsViewPreferences(storage).setChecksView(ChecksView.BREAKDOWN)

        // A fresh instance stands in for an app restart: only the stored value carries over.
        assertEquals(ChecksView.BREAKDOWN, DetailsViewPreferences(storage).getChecksView())
    }

    @Test
    fun `switching back to list view is persisted`() {
        val storage = InMemorySharedPreferences()
        DetailsViewPreferences(storage).setChecksView(ChecksView.BREAKDOWN)
        DetailsViewPreferences(storage).setChecksView(ChecksView.LIST)

        assertEquals(ChecksView.LIST, DetailsViewPreferences(storage).getChecksView())
    }

    @Test
    fun `unrecognised stored value falls back to list view`() {
        val storage = InMemorySharedPreferences()
        storage.edit().putString("checks_view", "REMOVED_VIEW").apply()

        assertEquals(ChecksView.LIST, DetailsViewPreferences(storage).getChecksView())
    }
}
