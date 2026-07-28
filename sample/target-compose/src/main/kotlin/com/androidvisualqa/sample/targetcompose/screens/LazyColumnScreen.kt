package com.androidvisualqa.sample.targetcompose.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.androidvisualqa.sdk.compose.feedbackTarget

/**
 * 1000-item [LazyColumn] with mixed item types.
 *
 * Item types:
 * - Header (every 50 items): coloured background, labelled "Header N"
 * - Item (standard): labelled "Item N"
 * - Footer (every 50 items, after header): labelled "Footer N"
 *
 * Each item carries a [feedbackTarget] with a unique stable ID
 * (e.g. "lazy.header.0", "lazy.item.1", "lazy.footer.50").
 *
 * Used to benchmark the matching engine's performance on large,
 * recycled-composable trees where the accessibility node hierarchy
 * is shallow but the item count is high.
 */
class LazyColumnScreenActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LazyColumnScreen() }
    }
}

@Composable
fun LazyColumnScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        itemsIndexed(
            items = (0 until 1000).toList(),
            key = { index, _ -> index },
        ) { index, _ ->
            when {
                index % 50 == 0 -> {
                    // Header item
                    Box(
                        modifier = Modifier
                            .feedbackTarget(stableId = "lazy.header.$index")
                            .background(Color(0xFFE3F2FD))
                            .fillMaxWidth()
                            .height(48.dp)
                            .padding(horizontal = 8.dp),
                    ) {
                        Text(
                            text = "Header $index",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterStart),
                        )
                    }
                }
                (index - 25) % 50 == 0 -> {
                    // Footer item (25 items after each header)
                    Box(
                        modifier = Modifier
                            .feedbackTarget(stableId = "lazy.footer.$index")
                            .background(Color(0xFFFFF3E0))
                            .fillMaxWidth()
                            .height(36.dp)
                            .padding(horizontal = 8.dp),
                    ) {
                        Text(
                            text = "Footer $index",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterStart),
                        )
                    }
                }
                else -> {
                    // Standard item
                    Text(
                        text = "Item $index",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .feedbackTarget(stableId = "lazy.item.$index")
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                    )
                }
            }
            if (index < 999) {
                HorizontalDivider()
            }
        }
    }
}
