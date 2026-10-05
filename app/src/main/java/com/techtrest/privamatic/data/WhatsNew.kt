package com.techtrest.privamatic.data

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import com.techtrest.privamatic.R

private const val PREFS_NAME = "whats_new_prefs"
private const val KEY_LAST_SEEN_VERSION_CODE = "last_seen_version_code"
private const val NONE = -1

/**
 * Notes for the Dashboard's one-time "What's new" card. Holds one release's notes at a time,
 * keyed by the versionCode that introduced them: for a release with news, set
 * [NOTES_INTRODUCED_IN] to its versionCode and replace [items]. Later releases that leave this
 * file alone keep showing these notes, but only to users who haven't seen them yet.
 * (v1.6 plan: notes kept per version, showing every version newer than the last one seen.)
 */
object WhatsNew {
    /** versionCode that introduced these notes: v1.5.0. v1.5.1 (9) ships them unchanged. */
    const val NOTES_INTRODUCED_IN = 8

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
     * so it is marked as seen and shows nothing; an update shows the card until dismissed if the
     * last version the user saw is older than the one that introduced the notes. Dismissing on
     * any later version (markSeen with the running versionCode) also covers these notes.
     */
    fun shouldShow(currentVersionCode: Int, isUpdate: Boolean, notesIntroducedIn: Int): Boolean {
        val lastSeen = prefs.getInt(KEY_LAST_SEEN_VERSION_CODE, NONE)
        if (lastSeen == NONE && !isUpdate) {
            markSeen(currentVersionCode)
            return false
        }
        return lastSeen < notesIntroducedIn
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
