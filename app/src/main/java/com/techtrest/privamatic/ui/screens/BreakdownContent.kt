package com.techtrest.privamatic.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.scanner.PrivacyScoreCalculator
import com.techtrest.privamatic.ui.components.DeductionChip
import com.techtrest.privamatic.ui.components.JumpHighlightAlpha
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** A ledger line: a deducting check, or the manual-check deduction when [check] is null. */
private data class BreakdownEntry(@StringRes val name: Int, val points: Int, val check: PrivacyCheck?)

/**
 * Score breakdown ledger: every deducting check plus the manual-check deduction in one card,
 * largest first, so the rows add up to 100 − score. The total lives in the Checks tab
 * header, so there is no footer here.
 *
 * @param onCheckClick shows that check in the Checks list view.
 * @param onManualChecksClick opens the manual checks (Actions tab).
 */
@Composable
fun BreakdownContent(
    privacyScore: PrivacyScore,
    onCheckClick: (PrivacyCheck) -> Unit,
    onManualChecksClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Tapped check row: washes in the jump-highlight colour, then the view switches.
    var pendingCheck by remember { mutableStateOf<PrivacyCheck?>(null) }
    val scope = rememberCoroutineScope()

    val entries = remember(privacyScore) {
        val manualDeduction = PrivacyScoreCalculator.manualCheckDeduction(privacyScore.manualCheckPoints)
        // Manual row goes in first so the stable sort keeps it ahead of checks with equal points.
        val manual = listOfNotNull(
            BreakdownEntry(R.string.label_breakdown_manual_checks, manualDeduction, check = null)
                .takeIf { manualDeduction > 0 }
        )
        val checks = privacyScore.issues
            .filter { it.pointDeduction > 0 }
            .map { BreakdownEntry(it.check.displayName, it.pointDeduction, it.check) }
        (manual + checks).sortedByDescending { it.points }
    }

    if (entries.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.label_breakdown_perfect_score),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    } else {
        // Same outer padding as the Checks list so switching views doesn't shift the card.
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    val openManualLabel = stringResource(R.string.label_breakdown_open_manual_checks)
                    val showCheckLabel = stringResource(R.string.label_breakdown_show_check)
                    entries.forEachIndexed { index, entry ->
                        BreakdownRow(
                            name = stringResource(entry.name),
                            points = entry.points,
                            isPending = entry.check != null && entry.check == pendingCheck,
                            modifier = Modifier.clickable(
                                onClickLabel = if (entry.check == null) openManualLabel else showCheckLabel,
                                role = Role.Button,
                                onClick = {
                                    val check = entry.check
                                    when {
                                        check == null -> onManualChecksClick()
                                        pendingCheck == null -> {
                                            pendingCheck = check
                                            scope.launch {
                                                delay(TapFeedbackMillis)
                                                onCheckClick(check)
                                            }
                                        }
                                    }
                                }
                            )
                        )
                        if (index < entries.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Wash-in time for a tapped row; the switch to the list follows [TapFeedbackMillis] after the tap. */
private const val TapFeedbackFadeMillis = 150
private const val TapFeedbackMillis = 200L

/**
 * One ledger line: name on the left, the Checks deduction chip on the right. [isPending] washes
 * it in the same colour the list uses to highlight the row it jumps to.
 */
@Composable
private fun BreakdownRow(
    name: String,
    points: Int,
    isPending: Boolean,
    modifier: Modifier = Modifier
) {
    val washColor = MaterialTheme.colorScheme.primaryContainer
    val washAlpha by animateFloatAsState(
        targetValue = if (isPending) JumpHighlightAlpha else 0f,
        animationSpec = tween(TapFeedbackFadeMillis),
        label = "breakdownTapWash"
    )
    Row(
        modifier = modifier
            .drawBehind { drawRect(washColor.copy(alpha = washAlpha)) }
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        DeductionChip(points = points)
    }
}
