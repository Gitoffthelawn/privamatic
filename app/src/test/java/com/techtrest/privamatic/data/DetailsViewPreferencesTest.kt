package com.techtrest.privamatic.data

import android.content.SharedPreferences
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

/** Minimal SharedPreferences backed by a map; edits apply on apply()/commit(). */
private class InMemorySharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any?>()

    override fun getAll(): Map<String, *> = values.toMap()
    override fun getString(key: String, defValue: String?): String? = values[key] as String? ?: defValue
    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        values[key] as Set<String>? ?: defValues
    override fun getInt(key: String, defValue: Int): Int = values[key] as Int? ?: defValue
    override fun getLong(key: String, defValue: Long): Long = values[key] as Long? ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = values[key] as Float? ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as Boolean? ?: defValue
    override fun contains(key: String): Boolean = key in values
    override fun edit(): SharedPreferences.Editor = Editor()
    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = Unit
    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener
    ) = Unit

    private inner class Editor : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clear = false

        override fun putString(key: String, value: String?) = apply { pending[key] = value }
        override fun putStringSet(key: String, values: Set<String>?) = apply { pending[key] = values }
        override fun putInt(key: String, value: Int) = apply { pending[key] = value }
        override fun putLong(key: String, value: Long) = apply { pending[key] = value }
        override fun putFloat(key: String, value: Float) = apply { pending[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
        override fun remove(key: String) = apply { removals += key }
        override fun clear() = apply { clear = true }
        override fun commit(): Boolean {
            if (clear) values.clear()
            removals.forEach { values.remove(it) }
            values.putAll(pending)
            return true
        }
        override fun apply() {
            commit()
        }
    }
}
