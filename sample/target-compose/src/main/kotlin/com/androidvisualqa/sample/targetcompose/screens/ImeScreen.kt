package com.androidvisualqa.sample.targetcompose.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.androidvisualqa.sdk.compose.feedbackTarget

/**
 * Screen with a [TextField] and a submit button.
 *
 * Uses [Modifier.imePadding] to correctly inset content when the keyboard
 * appears. The activity also declares `android:windowSoftInputMode="adjustResize"`
 * in the manifest.
 *
 * Used to verify IME inset behavior — the SDK must report correct bounds
 * both before and after keyboard arrival, and the privacy classifier must
 * not flag the text field as sensitive based solely on field type.
 */
class ImeScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ImeScreen() }
    }
}

@Composable
fun ImeScreen() {
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .feedbackTarget(stableId = "ime.screen.root")
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
    ) {
        Text(
            text = "IME / Keyboard Screen",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Type here") },
            modifier = Modifier
                .feedbackTarget(stableId = "ime.field.input")
                .fillMaxSize(),
            singleLine = false,
        )

        Button(
            onClick = { /* submit */ },
            modifier = Modifier
                .feedbackTarget(stableId = "ime.submit.button")
                .padding(top = 8.dp),
        ) {
            Text("Submit")
        }
    }
}
