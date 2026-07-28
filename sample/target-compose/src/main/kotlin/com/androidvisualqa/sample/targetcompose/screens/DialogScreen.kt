package com.androidvisualqa.sample.targetcompose.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.androidvisualqa.sdk.compose.feedbackTarget

/**
 * Screen that opens an [AlertDialog] on button tap.
 *
 * The dialog has its own [feedbackTarget] describing the dialog scope.
 * Used to verify that the SDK and matching engine correctly handle
 * overlaid content (dialog composable above the base activity content).
 */
class DialogScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DialogScreen() }
    }
}

@Composable
fun DialogScreen() {
    var showDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Dialog Demo",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        Button(
            onClick = { showDialog = true },
            modifier = Modifier.feedbackTarget(stableId = "dialog.open.button"),
        ) {
            Text("Open Dialog")
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            modifier = Modifier.feedbackTarget(stableId = "dialog.alert"),
            title = { Text("Sample Dialog") },
            text = { Text("This dialog overlays the base screen content.") },
            confirmButton = {
                TextButton(
                    onClick = { showDialog = false },
                    modifier = Modifier.feedbackTarget(stableId = "dialog.confirm"),
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDialog = false },
                    modifier = Modifier.feedbackTarget(stableId = "dialog.dismiss"),
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}
