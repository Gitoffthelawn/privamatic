package com.techtrest.privamatic.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.model.PrivacyCategory
import com.techtrest.privamatic.data.model.PrivacyCheck
import com.techtrest.privamatic.data.model.PrivacyIssue
import com.techtrest.privamatic.data.model.PrivacyScore
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Expandable category card. Expansion is hoisted so the Checks tab can open a category when
 * jumping to one of its checks; [onScrollTargetPlaced] then reports the [scrollTarget] row's
 * top and bottom y inside this card, and [highlightedCheck]'s row gets a brief fading wash
 * ([onHighlightFinished] when it has faded). Row expansion ([expandedChecks]) is hoisted too,
 * so the jump can also open the target row.
 */
@Composable
fun CategoryGroup(
    category: PrivacyCategory,
    privacyScore: PrivacyScore,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    expandedChecks: Set<PrivacyCheck>,
    onToggleCheck: (PrivacyCheck) -> Unit,
    trustedPackages: Set<String> = emptySet(),
    scrollTarget: PrivacyCheck? = null,
    onScrollTargetPlaced: (top: Int, bottom: Int) -> Unit = { _, _ -> },
    highlightedCheck: PrivacyCheck? = null,
    onHighlightFinished: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val issues = PrivacyCategory.getIssuesForCategory(category, privacyScore)

    val statusCounts = remember(privacyScore, trustedPackages) {
        issues.statusCounts(trustedPackages)
    }
    val issuesCount = statusCounts[IssueDisplayStatus.FAIL] ?: 0
    val passCount = statusCounts[IssueDisplayStatus.PASS] ?: 0
    val infoCount = statusCounts[IssueDisplayStatus.INFO] ?: 0

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpanded)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = stringResource(category.displayName),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                CategoryStatusChip(
                    issuesCount = issuesCount,
                    passCount = passCount,
                    infoCount = infoCount
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(R.string.label_category_collapse)
                                         else stringResource(R.string.label_category_expand),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isExpanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                issues.forEachIndexed { index, issue ->
                    IssueItem(
                        issue = issue,
                        isExpanded = issue.check in expandedChecks,
                        onToggleExpanded = { onToggleCheck(issue.check) },
                        trustedPackages = trustedPackages,
                        modifier = Modifier
                            .then(
                                if (issue.check == scrollTarget) {
                                    Modifier.onPlaced {
                                        val top = it.positionInParent().y.roundToInt()
                                        onScrollTargetPlaced(top, top + it.size.height)
                                    }
                                } else {
                                    Modifier
                                }
                            )
                            .then(
                                if (issue.check == highlightedCheck) jumpHighlight(onHighlightFinished)
                                else Modifier
                            )
                    )
                    if (index < issues.lastIndex) {
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

/**
 * Rows per [IssueDisplayStatus]; drives the category chips and the Checks tab summary.
 * Statuses with no rows are absent from the map.
 */
internal fun List<PrivacyIssue>.statusCounts(
    trustedPackages: Set<String>
): Map<IssueDisplayStatus, Int> = groupingBy { it.displayStatus(trustedPackages) }.eachCount()

/** Peak alpha of the primaryContainer jump wash; Breakdown's tap feedback uses the same. */
internal const val JumpHighlightAlpha = 0.8f
private const val JumpHighlightHoldMillis = 400L
private const val JumpHighlightFadeMillis = 1000

/**
 * Wash behind a row reached from Breakdown so it stands out among identical rows: holds
 * briefly, then fades out (~1.4 s in total). primaryContainer reads in both themes.
 */
@Composable
private fun jumpHighlight(onFinished: () -> Unit): Modifier {
    val color = MaterialTheme.colorScheme.primaryContainer
    val alpha = remember { Animatable(JumpHighlightAlpha) }
    LaunchedEffect(Unit) {
        delay(JumpHighlightHoldMillis)
        alpha.animateTo(0f, tween(JumpHighlightFadeMillis))
        onFinished()
    }
    // Read in the draw phase only, so the fade redraws without recomposing the row.
    return Modifier.drawBehind { drawRect(color.copy(alpha = alpha.value)) }
}

/**
 * Alpha for status tints (category chips, point badges): the container role is
 * laid over the card surface as a pale wash rather than a solid fill.
 */
internal const val StatusTintAlpha = 0.5f

/**
 * Category header status: "N issues" in error on a pale errorContainer tint when the
 * group has failures, otherwise "N pass" in green on a pale primaryContainer tint.
 * Unknown and informational rows never count as passes; a group made up only of those
 * gets a neutral "N to review" chip in onSurfaceVariant on surfaceVariant.
 */
@Composable
private fun CategoryStatusChip(
    issuesCount: Int,
    passCount: Int,
    infoCount: Int,
    modifier: Modifier = Modifier
) {
    val status = when {
        issuesCount > 0 -> IssueDisplayStatus.FAIL
        passCount > 0 -> IssueDisplayStatus.PASS
        else -> IssueDisplayStatus.INFO
    }
    val colorScheme = MaterialTheme.colorScheme
    // Dark primary is pinned to the mid-green brand colour (#00854A), which only reaches
    // ~2.5:1 on the dark tint at any alpha, so dark mode uses onPrimaryContainer instead.
    val isDark = colorScheme.surface.luminance() < 0.5f
    val passColor = if (isDark) colorScheme.onPrimaryContainer else colorScheme.primary

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = when (status) {
            IssueDisplayStatus.FAIL -> colorScheme.errorContainer.copy(alpha = StatusTintAlpha)
            IssueDisplayStatus.PASS -> colorScheme.primaryContainer.copy(alpha = StatusTintAlpha)
            IssueDisplayStatus.INFO -> colorScheme.surfaceVariant
        },
        contentColor = when (status) {
            IssueDisplayStatus.FAIL -> colorScheme.error
            IssueDisplayStatus.PASS -> passColor
            IssueDisplayStatus.INFO -> colorScheme.onSurfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (status) {
                    IssueDisplayStatus.FAIL -> Icons.Outlined.RemoveCircleOutline
                    IssueDisplayStatus.PASS -> Icons.Outlined.CheckCircle
                    IssueDisplayStatus.INFO -> Icons.Outlined.Info
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = when (status) {
                    IssueDisplayStatus.FAIL ->
                        pluralStringResource(R.plurals.plural_category_issues, issuesCount, issuesCount)
                    IssueDisplayStatus.PASS ->
                        pluralStringResource(R.plurals.plural_category_pass, passCount, passCount)
                    IssueDisplayStatus.INFO ->
                        pluralStringResource(R.plurals.plural_category_review, infoCount, infoCount)
                },
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
