package com.techtrest.privamatic.data

import android.content.Context
import android.content.SharedPreferences
import com.techtrest.privamatic.BuildConfig
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val PREFS_NAME = "onboarding_prefs"
private const val KEY_COMPLETE = "onboarding_complete"
private const val KEY_FORCE_SHOW_AD_ID = "force_show_ad_id_check"

class OnboardingPreferences(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isComplete(): Boolean = prefs.getBoolean(KEY_COMPLETE, false)

    fun setComplete() {
        prefs.edit().putBoolean(KEY_COMPLETE, true).apply()
    }

    fun reset() {
        prefs.edit().putBoolean(KEY_COMPLETE, false).apply()
    }

    /**
     * Debug-only testing aid (its Settings toggle exists only in debug builds). Release builds
     * ignore any stored value, so a user who switched it on in v1.4.1 isn't stuck with it.
     */
    fun isForceShowAdIdCheck(): Boolean =
        BuildConfig.DEBUG && prefs.getBoolean(KEY_FORCE_SHOW_AD_ID, false)

    fun setForceShowAdIdCheck(value: Boolean) {
        prefs.edit().putBoolean(KEY_FORCE_SHOW_AD_ID, value).apply()
    }

    fun forceShowAdIdCheckFlow(): Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_FORCE_SHOW_AD_ID) {
                trySend(isForceShowAdIdCheck())
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(isForceShowAdIdCheck())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
}
