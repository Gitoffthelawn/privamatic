package com.techtrest.privamatic.data.model

import androidx.annotation.StringRes

data class PrivacyIssue(
    val check: PrivacyCheck,
    val isSecure: Boolean,
    val currentStatus: String,
    val technicalDetails: String? = null,
    val customPointDeduction: Int? = null,
    val isSystemApp: Boolean = false,
    val flaggedPackages: List<String> = emptyList(),
    /**
     * The checker could not determine the real state (no API, unrecognised app, or an
     * error). Unknown results cost 0 points. isUnknown controls display and counting
     * everywhere: info icon, excluded from both pass and issue counts. isSecure only
     * decides whether the related quick win and tips appear.
     */
    val isUnknown: Boolean = false
) {
    val pointDeduction: Int
        get() = if (isSecure) 0 else (customPointDeduction ?: check.pointDeduction)

    @get:StringRes
    val recommendation: Int
        get() = check.recommendation
}

/**
 * True when this issue has flagged packages and every one of them is in the
 * user's trusted set — i.e. the issue should be treated as fully resolved by trust.
 */
fun PrivacyIssue.isFullyTrusted(trusted: Set<String>): Boolean =
    flaggedPackages.isNotEmpty() && flaggedPackages.all { it in trusted }
