package com.androidvisualqa.app.ui.permission

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import com.androidvisualqa.app.R

/**
 * M3 permission disclosure screen shown on first launch.
 *
 * Renders a scrollable column of permission explanations and action buttons.
 *
 * ponytail: static display-only screen; no animations, no expandable sections.
 */
@Composable
public fun PermissionDisclosureScreen(
    onContinue: () -> Unit,
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
    ) {
        Text(
            text = context.getString(R.string.disclosure_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        // Accessibility Service section
        SectionHeading(R.string.disclosure_accessibility_heading)
        BodyText(R.string.disclosure_accessibility_body)
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(context.getString(R.string.disclosure_open_accessibility))
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Notifications section (shown only on Android 13+ if not already granted)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            SectionHeading(R.string.disclosure_notifications_heading)
            BodyText(R.string.disclosure_notifications_body)
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(context.getString(R.string.disclosure_open_notifications))
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            // Still show the info even if already granted, without a CTA button
            SectionHeading(R.string.disclosure_notifications_heading)
            BodyText(R.string.disclosure_notifications_body)
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Storage section
        SectionHeading(R.string.disclosure_storage_heading)
        BodyText(R.string.disclosure_storage_body)
        Spacer(modifier = Modifier.height(16.dp))

        // Network section
        SectionHeading(R.string.disclosure_network_heading)
        BodyText(R.string.disclosure_network_body)
        Spacer(modifier = Modifier.height(24.dp))

        // Continue button
        OutlinedButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(),
        ) {
            Text(context.getString(R.string.disclosure_continue))
        }
    }
}

@Composable
private fun SectionHeading(textResId: Int) {
    Text(
        text = androidx.compose.ui.res.stringResource(textResId),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun BodyText(textResId: Int) {
    Text(
        text = androidx.compose.ui.res.stringResource(textResId),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}
