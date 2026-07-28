package com.androidvisualqa.app.ui.export

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.androidvisualqa.app.R

/**
 * M3 export bottom sheet triggered from the editor save bar after save.
 *
 * Three options:
 * 1. "Share ZIP" — fires a system share sheet via FileProvider.
 * 2. "Save to Downloads" — saves to MediaStore (API 29+) or FileProvider (lower).
 * 3. "Cancel" — dismisses the sheet.
 *
 * ponytail: static bottom sheet; no progress indicator, no error retry.
 * M4 may add in-flight progress and error states.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun ExportSheet(
    onShareZip: () -> Unit,
    onSaveToDownloads: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(
                text = context.getString(R.string.export_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            Button(
                onClick = {
                    onShareZip()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(context.getString(R.string.export_share_zip))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onSaveToDownloads()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                ),
            ) {
                Text(context.getString(R.string.export_save_downloads))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(context.getString(R.string.export_cancel))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
