package com.techtrest.privamatic

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.techtrest.privamatic.R
import kotlin.math.roundToInt

class WidgetConfigurationActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Default result to CANCELED so backing out aborts widget placement
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        // Show what the widget draws: a stored value below the card minimum shows as the minimum.
        val currentSettings = WidgetStyle.settingsFor(WidgetPreferences.getOpacity(this, appWidgetId))

        setContent {
            PrivacyWidgetTheme {
                Scaffold(topBar = { WidgetConfigurationTopBar() }) { innerPadding ->
                    WidgetConfigurationScreen(
                        initialSettings = currentSettings,
                        onSave = { settings -> saveAndFinish(WidgetStyle.storedOpacity(settings)) },
                        onCancel = { finish() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun saveAndFinish(opacity: Float) {
        WidgetPreferences.saveOpacity(this, appWidgetId, opacity)

        // Trigger widget update via broadcast so goAsync() has a valid PendingResult
        PrivacyWidgetProvider.requestImmediateUpdate(this)

        val resultIntent = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}

/**
 * Same green bar as the app's other screens: the theme forces light status-bar icons, which
 * were invisible on this screen's bare light background.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WidgetConfigurationTopBar() {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.label_widget_config_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary
        ),
        windowInsets = WindowInsets.statusBars
    )
}

@Composable
private fun WidgetConfigurationScreen(
    initialSettings: WidgetStyle.Settings,
    onSave: (WidgetStyle.Settings) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBackground by remember { mutableStateOf(initialSettings.showBackground) }
    var opacity by remember { mutableFloatStateOf(initialSettings.opacity) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                // One toggleable node so TalkBack reads the row as a switch with its state
                .toggleable(value = showBackground, role = Role.Switch) { showBackground = it }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_widget_show_background),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = showBackground,
                onCheckedChange = null
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (showBackground) {
                stringResource(R.string.fmt_widget_opacity, opacity.roundToInt())
            } else {
                stringResource(R.string.label_widget_no_background)
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Below the minimum the card would blend through its own text colour (docs/specs/widget-pack.md).
        Slider(
            value = opacity,
            onValueChange = { opacity = it },
            valueRange = WidgetStyle.MIN_CARD_OPACITY..WidgetStyle.MAX_OPACITY,
            enabled = showBackground,
            steps = 0,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.fmt_widget_opacity_bound, WidgetStyle.MIN_CARD_OPACITY.roundToInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(R.string.fmt_widget_opacity_bound, WidgetStyle.MAX_OPACITY.roundToInt()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(onClick = onCancel, colors = accentOutlinedButtonColors()) {
                Text(stringResource(R.string.label_widget_cancel))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = { onSave(WidgetStyle.Settings(showBackground, opacity)) }) {
                Text(stringResource(R.string.label_widget_save))
            }
        }
    }
}
