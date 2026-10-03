package com.techtrest.privamatic.data

import android.content.Context
import android.content.SharedPreferences
import com.techtrest.privamatic.ui.navigation.ChecksView

private const val PREFS_NAME = "details_view_prefs"
private const val KEY_CHECKS_VIEW = "checks_view"

/**
 * Remembers which view (list or breakdown) the Details > Checks tab last showed.
 * Read synchronously so the saved view is on screen from the first frame.
 */
class DetailsViewPreferences(private val prefs: SharedPreferences) {

    constructor(context: Context) :
        this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    /** Missing or unrecognised values (e.g. a renamed entry) fall back to [ChecksView.LIST]. */
    fun getChecksView(): ChecksView {
        val stored = prefs.getString(KEY_CHECKS_VIEW, null)
        return ChecksView.entries.firstOrNull { it.name == stored } ?: ChecksView.LIST
    }

    fun setChecksView(view: ChecksView) {
        prefs.edit().putString(KEY_CHECKS_VIEW, view.name).apply()
    }
}
