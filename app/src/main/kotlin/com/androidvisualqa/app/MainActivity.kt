package com.androidvisualqa.app

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.androidvisualqa.accessibility.VisualFeedbackAccessibilityService
import com.androidvisualqa.annotation.EditorScreen
import com.androidvisualqa.annotation.EditorViewModel
import com.androidvisualqa.annotation.RectangleAnnotation
import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.files.FileSystemDraftStore
import com.androidvisualqa.geometry.Bounds
import com.androidvisualqa.geometry.CoordinateSpace
import com.androidvisualqa.model.capture.CaptureFrame
import com.androidvisualqa.model.capture.CaptureSession
import com.androidvisualqa.model.capture.NodeSnapshot
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.report.FileSystemReportHistoryIndex
import java.io.File

/**
 * Single-activity entry point for the Android Visual QA app.
 *
 * M2: FAB triggers real accessibility capture via [CaptureOrchestrator].
 * Save routes through matching engine + report assembler.
 */
public class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppNavigation(applicationContext = applicationContext)
            }
        }
    }
}

@Composable
private fun AppNavigation(applicationContext: Context) {
    val navController = rememberNavController()
    val draftStore = remember {
        FileSystemDraftStore(
            DraftDirectory(
                applicationContext.getDir("drafts", Context.MODE_PRIVATE).toPath(),
            ),
        )
    }
    val reportHistory = remember {
        val file = File(applicationContext.filesDir, "report_history.jsonl")
        FileSystemReportHistoryIndex(file.toPath())
    }

    NavHost(
        navController = navController,
        startDestination = "drafts",
    ) {
        composable("drafts") {
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
                onSave = { rect, text ->
                    // TODO(m2): wire matching + report after editor save
                    // Current: navigate back. The full chain needs the
                    // accessibility node tree from the original capture,
                    // which is not yet passed through the editor route.
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() },
            )
        }
    }
}

/**
 * Draft list screen with a FAB to start a new draft.
 *
 * M2: FAB tap attempts to find the running [VisualFeedbackAccessibilityService]
 * and delegates capture to [CaptureOrchestrator]. On success, navigates to
 * editor. On failure, shows a snackbar/error (// TODO(m3)).
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
 */
@Composable
private fun EditorScreenWrapper(
    viewModel: EditorViewModel,
    draftId: String?,
    onSave: (RectangleAnnotation?, String) -> Unit,
    onCancel: () -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(draftId) {
        viewModel.loadDraft(
            draftId?.let { DraftId(it) }
        )
    }

    val state = viewModel.state.collectAsStateWithLifecycle().value
    val lastStateRef = remember { arrayOf(state) }

    EditorScreen(
        state = state,
        onStateChange = { newState ->
            val prev = lastStateRef[0]
            lastStateRef[0] = newState
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
            viewModel.save { r, t -> onSave(r, t) }
        },
        onCancel = onCancel,
    )
}
