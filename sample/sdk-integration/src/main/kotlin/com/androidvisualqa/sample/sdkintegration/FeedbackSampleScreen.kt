package com.androidvisualqa.sample.sdkintegration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.androidvisualqa.sdk.compose.feedbackTarget

/**
 * Sample Compose screen demonstrating [Modifier.feedbackTarget] usage.
 *
 * Each interactive element is explicitly marked with a stable ID so the SDK
 * can capture its metadata and bounds for visual-feedback evidence reports.
 */
@Composable
fun FeedbackSampleScreen() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "SDK Integration Sample",
                    style = MaterialTheme.typography.headlineMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                var email by remember { mutableStateOf("") }

                /**
                 * A text field marked as an SDK feedback target.
                 *
                 * Stable ID: "sample.email_field"
                 * Semantics: includes a "hint" key describing the field purpose.
                 */
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .feedbackTarget(
                            stableId = "sample.email_field",
                            semantics = mapOf("hint" to "email address input"),
                        ),
                )

                /**
                 * A submit button marked as an SDK feedback target.
                 *
                 * Stable ID: "sample.submit_button"
                 * Semantics: includes a "variant" key identifying the button style.
                 */
                Button(
                    onClick = { /* submit action */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .feedbackTarget(
                            stableId = "sample.submit_button",
                            semantics = mapOf("variant" to "primary"),
                        ),
                ) {
                    Text("Submit")
                }
            }
        }
    }
}
