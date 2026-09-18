package com.techtrest.privamatic.ui.components

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.ManualCheckState

/**
 * Progress bar colour for a manual check based on how close it is to its review window.
 * A hard switch, not a gradient: secondary (calm) while time remains, tertiary (the app's
 * existing "attention" role, also used for warning icons in IssueItem/DetailsScreen) once
 * the check is due, matching the point where the status text switches to "Review needed".
 */
@Composable
fun getProgressColor(checkState: ManualCheckState): Color {
    return if (checkState.fillPercentage >= 1f) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.secondary
    }
}

/**
 * Human-readable status text describing how long until a manual check is due for review.
 */
fun getStatusText(checkState: ManualCheckState, context: Context): String {
    return when {
        checkState.isOverdue -> "Review needed"
        else -> context.resources.getQuantityString(
            R.plurals.plural_days_remaining,
            checkState.daysRemaining,
            checkState.daysRemaining
        )
    }
}
