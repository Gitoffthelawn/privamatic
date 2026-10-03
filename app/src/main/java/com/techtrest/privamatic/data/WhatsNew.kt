package com.techtrest.privamatic.data

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import com.techtrest.privamatic.R

private const val PREFS_NAME = "whats_new_prefs"
private const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"
private const val NONE = -1

/**
 * Notes for the Dashboard's one-time "What's new" card. Holds one release at a time: for a
 * release with news, set [NOTES_VERSION_CODE] to its versionCode and replace [items]. A release
 * that doesn't touch this file shows no card, rather than repeating the previous notes.
 */
object WhatsNew {
    /** versionCode these notes ship with: v1.5.0. */
    const val NOTES_VERSION_CODE = 8

    @StringRes
    val items: List<Int> = listOf(
        R.string.copy_whats_new_breakdown_moved,
        R.string.copy_whats_new_unknown_free,
        R.string.copy_whats_new_detection
    )
}

/**
 * Remembers the last versionCode whose What's new card the user has dealt with, so the card
 * appears once per update and never on a fresh install.
 */
class WhatsNewPreferences(private val prefs: SharedPreferences) {

    constructor(context: Context) :
        this(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    /**
     * Whether to show the card on this launch. A fresh install has nothing to compare against,
     * so it is marked as seen and shows nothing; an update shows the card while notes exist for
     * this exact versionCode and the user hasn't dismissed it.
     */
    fun shouldShow(currentVersionCode: Int, isUpdate: Boolean, notesVersionCode: Int): Boolean {
        val lastSeen = prefs.getInt(KEY_LAST_SEEN_VERSION_CODE, NONE)
        if (lastSeen == NONE && !isUpdate) {
            markSeen(currentVersionCode)
            return false
        }
        return notesVersionCode == currentVersionCode && lastSeen < currentVersionCode
    }

    fun markSeen(versionCode: Int) {
        prefs.edit().putInt(KEY_LAST_SEEN_VERSION_CODE, versionCode).apply()
    }

    companion object {
        /**
         * True when this install replaced an earlier one. v1.4.1 and older never stored a
         * last-seen version, so the install and update times are the only signal; they are
         * equal on a fresh install.
         */
        fun isUpdate(context: Context): Boolean = try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            info.lastUpdateTime > info.firstInstallTime
        } catch (_: Exception) {
            false
        }
    }
}
