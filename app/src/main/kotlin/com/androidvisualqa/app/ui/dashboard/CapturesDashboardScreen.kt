package com.androidvisualqa.app.ui.dashboard

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.androidvisualqa.app.R
import com.androidvisualqa.app.ui.history.CaptureHistoryItem
import com.androidvisualqa.app.ui.history.CaptureHistoryUiState
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun CapturesDashboardScreen(
    uiState: CaptureHistoryUiState,
    onRefresh: () -> Unit,
    onOpenCapture: (String) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onRefresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.captures_title)) }) }) { innerPadding ->
        when {
            uiState.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            uiState.error != null -> EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                title = stringResource(R.string.captures_load_error_title),
                body = stringResource(R.string.captures_load_error_body),
                action = stringResource(R.string.captures_retry),
                onAction = onRefresh,
            )

            uiState.items.isEmpty() -> EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                title = stringResource(R.string.captures_empty_title),
                body = stringResource(R.string.captures_empty_body),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    CaptureGuideCard()
                }
                items(uiState.items, key = { it.draftId.value }) { item ->
                    CaptureCard(item = item, onClick = { onOpenCapture(item.draftId.value) })
                }
            }
        }
    }
}

@Composable
private fun CaptureCard(item: CaptureHistoryItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = item.accessibilityDescription() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Thumbnail(item.thumbnailPath, item.packageName)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = item.packageName.takeUnless { it == "unknown" }
                        ?: stringResource(R.string.capture_unknown_app),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = if (item.isDraft) stringResource(R.string.capture_unfinished)
                    else item.status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.createdAt.toString().replace('T', ' ').take(16),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (item.commentCount > 0) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.capture_comment_count,
                            item.commentCount,
                            item.commentCount,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Surface(
                color = if (item.isDraft) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = if (item.isDraft) stringResource(R.string.capture_status_draft) else item.status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (item.isDraft) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun Thumbnail(path: Path?, description: String) {
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, path) {
        value = withContext(Dispatchers.IO) {
            path?.let { BitmapFactory.decodeFile(it.toString())?.asImageBitmap() }
        }
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap!!,
            contentDescription = description,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(width = 72.dp, height = 88.dp)
                .clip(MaterialTheme.shapes.medium),
        )
    } else {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.size(width = 72.dp, height = 88.dp),
        ) {}
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier,
    title: String,
    body: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        if (action != null && onAction != null) {
            androidx.compose.material3.Button(onClick = onAction) { Text(action) }
        } else {
            CaptureGuideCard(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CaptureGuideCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.capture_guide_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.capture_guide_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.capture_guide_step_one), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.capture_guide_step_two), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.capture_guide_step_three), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun CaptureHistoryItem.accessibilityDescription(): String =
    listOfNotNull(
        packageName.takeUnless { it == "unknown" },
        if (isDraft) "Unfinished draft" else status,
        commentCount.takeIf { it > 0 }?.let { "$it comments" },
    ).joinToString(", ")
