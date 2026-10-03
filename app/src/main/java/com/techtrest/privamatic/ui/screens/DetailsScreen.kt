package com.techtrest.privamatic.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.techtrest.privamatic.Cream
import com.techtrest.privamatic.R
import com.techtrest.privamatic.data.DetailsViewPreferences
import com.techtrest.privamatic.data.model.FlaggedApp
import com.techtrest.privamatic.data.model.PrivacyCategory
import com.techtrest.privamatic.data.model.PrivacyScore
import com.techtrest.privamatic.data.model.SdkScanResult
import com.techtrest.privamatic.data.scanner.PrivacyScoreCalculator
import com.techtrest.privamatic.ui.components.CategoryGroup
import com.techtrest.privamatic.ui.components.IssueDisplayStatus
import com.techtrest.privamatic.ui.components.statusCounts
import com.techtrest.privamatic.ui.navigation.ChecksView
import com.techtrest.privamatic.ui.navigation.DetailsTab
import com.techtrest.privamatic.ui.viewmodel.SdkScanState

@Composable
fun DetailsScreen(
    privacyScore: PrivacyScore,
    flaggedApps: List<FlaggedApp>,
    trustedPackages: Set<String>,
    selectedTab: DetailsTab,
    onTabSelected: (DetailsTab) -> Unit,
    isAppsBannerDismissed: Boolean,
    onDismissAppsBanner: () -> Unit,
    onTrustApp: (String) -> Unit,
    onUntrustApp: (String) -> Unit,
    sdkScanResult: SdkScanResult?,
    sdkScanState: SdkScanState,
    onRunSdkScan: () -> Unit,
    onNavigateToManualChecks: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Dark primary is the lighter brand green (#00854A): onPrimary at 0.8 alpha only reaches
    // 3.6:1 on it, so dark mode keeps unselected labels at full onPrimary (4.7:1) and marks
    // the selected tab with the Cream indicator plus a heavier weight instead.
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Column(modifier = modifier.fillMaxSize()) {
        // Tabs sit on the app bar colour so app bar + tabs read as one header.
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                    color = Cream
                )
            },
            divider = {}
        ) {
            DetailsTab.entries.forEach { tab ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                    unselectedContentColor = if (isDark) MaterialTheme.colorScheme.onPrimary
                                             else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    // TabRow splits width evenly, so at large font scales a long label
                    // would wrap and make its tab taller than the rest. Truncate
                    // instead of wrapping to keep the row even.
                    text = {
                        Text(
                            text = stringResource(tab.label),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = when {
                                !isDark -> null
                                selectedTab == tab -> FontWeight.SemiBold
                                else -> FontWeight.Normal
                            }
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            DetailsTab.CHECKS -> ChecksTab(
                privacyScore = privacyScore,
                trustedPackages = trustedPackages,
                onNavigateToManualChecks = onNavigateToManualChecks
            )
            DetailsTab.APPS -> AppsContent(
                flaggedApps = flaggedApps,
                trustedPackages = trustedPackages,
                isAppsBannerDismissed = isAppsBannerDismissed,
                onDismissAppsBanner = onDismissAppsBanner,
                onTrustApp = onTrustApp,
                onUntrustApp = onUntrustApp
            )
            DetailsTab.SDK -> SdkTabContent(
                scanResult = sdkScanResult,
                scanState = sdkScanState,
                onRunScan = onRunSdkScan
            )
        }
    }
}

/**
 * Checks tab: a fixed header (summary + view toggle) over either the check list or the
 * score breakdown. Same data, two views, so this is a toggle rather than another tab.
 */
@Composable
private fun ChecksTab(
    privacyScore: PrivacyScore,
    trustedPackages: Set<String>,
    onNavigateToManualChecks: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewPrefs = remember { DetailsViewPreferences(context) }
    var view by remember { mutableStateOf(viewPrefs.getChecksView()) }

    Column(modifier = modifier.fillMaxSize()) {
        ChecksHeader(
            privacyScore = privacyScore,
            trustedPackages = trustedPackages,
            view = view,
            onToggleView = {
                view = if (view == ChecksView.LIST) ChecksView.BREAKDOWN else ChecksView.LIST
                viewPrefs.setChecksView(view)
            }
        )

        when (view) {
            ChecksView.LIST -> ChecksContent(
                privacyScore = privacyScore,
                trustedPackages = trustedPackages
            )
            ChecksView.BREAKDOWN -> BreakdownContent(
                privacyScore = privacyScore,
                onManualChecksClick = onNavigateToManualChecks
            )
        }
    }
}

/**
 * List view: status summary over every category, counted exactly like the category chips.
 * Breakdown view: the breakdown's total (checks + manual checks) and the resulting score.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecksHeader(
    privacyScore: PrivacyScore,
    trustedPackages: Set<String>,
    view: ChecksView,
    onToggleView: () -> Unit,
    modifier: Modifier = Modifier
) {
    val summary = when (view) {
        ChecksView.LIST -> {
            val counts = remember(privacyScore, trustedPackages) {
                PrivacyCategory.entries
                    .flatMap { PrivacyCategory.getIssuesForCategory(it, privacyScore) }
                    .statusCounts(trustedPackages)
            }
            val issuesCount = counts[IssueDisplayStatus.FAIL] ?: 0
            val passCount = counts[IssueDisplayStatus.PASS] ?: 0
            val infoCount = counts[IssueDisplayStatus.INFO] ?: 0
            val issues = pluralStringResource(R.plurals.plural_category_issues, issuesCount, issuesCount)
            val pass = pluralStringResource(R.plurals.plural_category_pass, passCount, passCount)
            if (infoCount > 0) {
                val review = pluralStringResource(R.plurals.plural_category_review, infoCount, infoCount)
                stringResource(R.string.fmt_details_checks_summary_review, issues, pass, review)
            } else {
                stringResource(R.string.fmt_details_checks_summary, issues, pass)
            }
        }
        ChecksView.BREAKDOWN -> {
            val total = PrivacyScoreCalculator.totalDeduction(privacyScore)
            if (total > 0) stringResource(R.string.fmt_details_breakdown_total, total, privacyScore.score)
            else stringResource(R.string.label_details_breakdown_no_deductions)
        }
    }
    // Names the view the button switches to, for both the tooltip and TalkBack.
    val toggleLabel = stringResource(
        if (view == ChecksView.LIST) R.string.label_details_view_breakdown
        else R.string.label_details_view_checks
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = summary,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        )
        TooltipBox(
            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
            tooltip = { PlainTooltip { Text(toggleLabel) } },
            state = rememberTooltipState()
        ) {
            IconButton(onClick = onToggleView) {
                Icon(
                    imageVector = if (view == ChecksView.LIST) Icons.Outlined.BarChart
                                  else Icons.AutoMirrored.Outlined.List,
                    contentDescription = toggleLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ChecksContent(
    privacyScore: PrivacyScore,
    trustedPackages: Set<String>,
    modifier: Modifier = Modifier
) {
    val securityCategories = listOf(PrivacyCategory.SYSTEM_SECURITY)

    val surveillanceCategories = listOf(
        PrivacyCategory.NETWORK_PRIVACY,
        PrivacyCategory.GOOGLE_SERVICES,
        PrivacyCategory.DEFAULT_APPS,
        PrivacyCategory.GOOGLE_APPS,
        PrivacyCategory.META_FACEBOOK_APPS,
        PrivacyCategory.MICROSOFT_APPS,
        PrivacyCategory.AI_AND_OTHER_APPS
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.details_section_security),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        items(securityCategories, key = { it.name }) { category ->
            CategoryGroup(
                category = category,
                privacyScore = privacyScore,
                trustedPackages = trustedPackages
            )
        }

        item {
            Text(
                text = stringResource(R.string.details_section_surveillance),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }

        items(surveillanceCategories, key = { it.name }) { category ->
            CategoryGroup(
                category = category,
                privacyScore = privacyScore,
                trustedPackages = trustedPackages
            )
        }
    }
}

@Composable
private fun AppsContent(
    flaggedApps: List<FlaggedApp>,
    trustedPackages: Set<String>,
    isAppsBannerDismissed: Boolean,
    onDismissAppsBanner: () -> Unit,
    onTrustApp: (String) -> Unit,
    onUntrustApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!isAppsBannerDismissed) {
            item(key = "banner") {
                AppsBannerCard(onDismiss = onDismissAppsBanner)
            }
        }

        if (flaggedApps.isEmpty()) {
            item(key = "empty") {
                EmptyAppsState()
            }
        } else {
            items(flaggedApps, key = { it.packageName }) { app ->
                AppTrustRow(
                    app = app,
                    isTrusted = app.packageName in trustedPackages,
                    onToggle = { checked ->
                        if (checked) onTrustApp(app.packageName)
                        else onUntrustApp(app.packageName)
                    }
                )
            }
        }
    }
}

@Composable
private fun AppsBannerCard(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.copy_details_apps_banner),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.label_common_dismiss),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyAppsState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.label_details_no_flagged_apps),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AppTrustRow(
    app: FlaggedApp,
    isTrusted: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val iconBitmap: Bitmap? = remember(app.packageName) {
        try {
            context.packageManager.getApplicationIcon(app.packageName).toBitmap()
        } catch (_: Exception) { null }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTrusted) MaterialTheme.colorScheme.primaryContainer
                             else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (iconBitmap != null) {
                Image(
                    painter = BitmapPainter(iconBitmap.asImageBitmap()),
                    contentDescription = stringResource(R.string.fmt_app_icon_cd, app.appName),
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = stringResource(R.string.fmt_app_icon_cd, app.appName),
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = if (isTrusted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = if (isTrusted) stringResource(R.string.label_details_app_trusted)
                                     else stringResource(R.string.label_details_app_flagged),
                tint = if (isTrusted) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Switch(
                checked = isTrusted,
                onCheckedChange = if (app.isBlacklisted) null else onToggle,
                enabled = !app.isBlacklisted
            )
        }
    }
}
