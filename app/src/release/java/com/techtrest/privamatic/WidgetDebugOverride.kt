package com.techtrest.privamatic

import android.content.Context
import android.content.Intent

/** Release builds: no override. The debug build's version (src/debug) lets device checks force the change. */
internal object WidgetDebugOverride {
    @Suppress("UNUSED_PARAMETER")
    fun applyExtras(context: Context, intent: Intent, appWidgetIds: IntArray) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun forcedDelta(context: Context): Int? = null

    @Suppress("UNUSED_PARAMETER")
    fun forcedDeviceName(context: Context): String? = null
}
