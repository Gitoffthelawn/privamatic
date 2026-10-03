package com.techtrest.privamatic.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.ui.components.DeductionChip

/**
 * Score breakdown ledger: every deducting check in one card, largest first. The total lives
 * in the Checks tab header, so there is no footer here.
 */
@Composable
fun BreakdownContent(
    privacyScore: PrivacyScore,
    modifier: Modifier = Modifier
) {
    val sortedIssues = remember(privacyScore) {
        privacyScore.issues
            .filter { it.pointDeduction > 0 }
            .sortedByDescending { it.pointDeduction }
    }

    if (sortedIssues.isEmpty()) {
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
                    sortedIssues.forEachIndexed { index, issue ->
                        BreakdownRow(
                            name = stringResource(issue.check.displayName),
                            points = issue.pointDeduction
                        )
                        if (index < sortedIssues.lastIndex) {
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

/** One ledger line: name on the left, the Checks deduction chip on the right. */
@Composable
private fun BreakdownRow(
    name: String,
    points: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
