package com.androidvisualqa.sample.targetcompose.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.androidvisualqa.sample.targetcompose.R

/**
 * Screen demonstrating merged semantics.
 *
 * A [Card] containing a clickable [Row] whose semantic children (icon, text)
 * are merged into the parent via [Modifier.semantics] with mergeDescendants = true.
 *
 * The accessibility tree sees only one clickable node for the entire row,
 * which tests the matching engine's ability to resolve merged vs. unmerged
 * semantics correctly.
 *
 * The card itself also carries a [com.androidvisualqa.sdk.compose.Modifier.feedbackTarget]
 * with a stable ID.
 */
class MergedSemanticsScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MergedSemanticsScreen() }
    }
}

@Composable
fun MergedSemanticsScreen() {
    Card(
        modifier = Modifier.padding(16.dp),
    ) {
        // The Row merges its children's semantics into itself.
        // The accessibility tree will see ONE clickable node for the whole row.
        Row(
            modifier = Modifier
                .semantics(mergeDescendants = true) {}
                .clickable { /* tap the whole row */ }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ponytail: placeholder icon — uses a built-in Android drawable
            Icon(
                painter = painterResource(android.R.drawable.ic_dialog_info),
                contentDescription = "Info icon",
                modifier = Modifier.padding(end = 8.dp),
            )
            Text(
                text = "Merged semantics row — click anywhere",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
