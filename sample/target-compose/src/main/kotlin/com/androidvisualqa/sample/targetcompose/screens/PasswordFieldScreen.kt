package com.androidvisualqa.sample.targetcompose.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.androidvisualqa.sdk.compose.feedbackTarget

/**
 * Screen with a password [OutlinedTextField].
 *
 * The TextField uses [PasswordVisualTransformation] to mask input.
 * The wrapping Column carries [feedbackTarget] so the matching engine
 * can identify this as a password screen.
 *
 * Used to verify that the privacy classifier correctly flags password
 * fields based on [OutlinedTextField] with [PasswordVisualTransformation].
 */
class PasswordFieldScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PasswordFieldScreen() }
    }
}

@Composable
fun PasswordFieldScreen() {
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .feedbackTarget(stableId = "password.screen.root")
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Text(
            text = "Password Field",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .feedbackTarget(stableId = "password.field.input")
                .fillMaxSize(),
            singleLine = true,
        )
    }
}
