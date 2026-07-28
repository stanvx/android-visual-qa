package com.androidvisualqa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.androidvisualqa.annotation.EditorScreen
import com.androidvisualqa.annotation.EditorViewModel
import com.androidvisualqa.model.ids.DraftId

/**
 * Single-activity entry point for the Android Visual QA app.
 *
 * Hosts a [NavHost] with two M1 screens:
 * - `drafts` — stub draft list with a "+" FAB that opens a new editor session.
 * - `editor/{draftId}` — the M1 annotation editor, optional draftId for existing drafts.
 */
public class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
private fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "drafts",
    ) {
        composable("drafts") {
            // TODO(m2): replace DraftListScreen with real draft history from FileSystemReportHistoryIndex
            DraftListScreen(
                onNewDraft = { navController.navigate("editor/new") },
                onOpenDraft = { draftId -> navController.navigate("editor/$draftId") },
            )
        }
        composable(
            route = "editor/{draftId}",
            arguments = listOf(navArgument("draftId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val draftId = backStackEntry.arguments?.getString("draftId")
            val viewModel: EditorViewModel = viewModel()
            EditorScreenWrapper(
                viewModel = viewModel,
                draftId = if (draftId == "new") null else draftId,
                onSave = { _, _ -> navController.popBackStack() },
                onCancel = { navController.popBackStack() },
            )
        }
    }
}

/**
 * Stub draft list screen showing existing drafts and a FAB to create new ones.
 *
 * // TODO(m2): list drafts from FileSystemReportHistoryIndex (read-only).
 * // Currently shows a placeholder message.
 */
@Composable
private fun DraftListScreen(
    onNewDraft: () -> Unit,
    onOpenDraft: (String) -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNewDraft) {
                Text(text = "+", style = MaterialTheme.typography.headlineMedium)
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Visual QA",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Tap + to start a new draft",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/**
 * Wraps the [EditorScreen] with a [EditorViewModel], loading the given [draftId].
 *
 * Routes [EditorScreen.onStateChange] to the appropriate ViewModel methods
 * by detecting which field changed between the old and new state. This avoids
 * requiring [EditorScreen] to expose per-action callbacks while still keeping
 * the ViewModel as the single source of truth.
 */
@Composable
private fun EditorScreenWrapper(
    viewModel: EditorViewModel,
    draftId: String?,
    onSave: (Unit?, String) -> Unit,
    onCancel: () -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(draftId) {
        viewModel.loadDraft(
            draftId?.let { DraftId(it) }
        )
    }

    val state = viewModel.state.collectAsStateWithLifecycle().value

    // Remember the previous state so we can diff on the next recomposition.
    // Use an array to work around lambda capture of a mutable ref — this var
    // is mutated inside onStateChange but never drives recomposition directly.
    val lastStateRef = remember { arrayOf(state) }

    EditorScreen(
        state = state,
        onStateChange = { newState ->
            val prev = lastStateRef[0]
            lastStateRef[0] = newState

            // Detect what changed and route to the correct ViewModel method.
            // undoStack popped  → undo
            // redoStack popped  → redo
            // rectangles appended → addRectangle on the new one
            // feedbackText changed → setFeedbackText
            when {
                prev.undoStack.size > newState.undoStack.size -> viewModel.undo()
                prev.redoStack.size > newState.redoStack.size -> viewModel.redo()
                newState.rectangles.size > prev.rectangles.size -> {
                    val added = newState.rectangles.last()
                    viewModel.addRectangle(added)
                }
                newState.feedbackText != prev.feedbackText -> {
                    viewModel.setFeedbackText(newState.feedbackText)
                }
            }
        },
        onSave = { rect, text ->
            viewModel.save { r, t -> onSave(Unit, t) }
        },
        onCancel = onCancel,
    )
}
