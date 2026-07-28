package com.androidvisualqa.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
import com.androidvisualqa.annotation.RectangleAnnotation
import com.androidvisualqa.database.RetentionConfig
import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.files.FileSystemDraftStore
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.report.FileSystemReportHistoryIndex
import kotlinx.coroutines.launch
import java.io.File

/**
 * Single-activity entry point for the Android Visual QA app.
 *
 * Capture entry points open the review editor after real accessibility capture.
 * Save routes through matching, report assembly, and local history persistence.
 *
 * M3: First launch routes to [PermissionDisclosureScreen]. After that,
 * retention scheduling runs daily via [RetentionScheduler]. Process-death
 * resumption is available through [ResumeDraftCoordinator].
 */
public class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Track first-launch in SharedPreferences
        val prefs = getSharedPreferences("visual_qa_prefs", Context.MODE_PRIVATE)
        val firstLaunchDone = prefs.getBoolean("first_launch_done", false)
        val initialDraftId = intent.getStringExtra(EXTRA_DRAFT_ID)
            ?.takeIf(::isValidDraftId)

        // Schedule retention cleanup once per day
        RetentionScheduler(applicationContext).schedule(
            RetentionConfig(),
        )

        setContent {
            VisualQaTheme {
                AppNavigation(
                    applicationContext = applicationContext,
                    startAtDisclosure = !firstLaunchDone,
                    initialDraftId = initialDraftId,
                    onDisclosureComplete = {
                        prefs.edit().putBoolean("first_launch_done", true).apply()
                    },
                )
            }
        }
    }
}

@Composable
private fun VisualQaTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> darkColorScheme(primary = Color(0xFF6750A4))
        else -> lightColorScheme(primary = Color(0xFF6750A4))
    }

    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
private fun AppNavigation(
    applicationContext: Context,
    startAtDisclosure: Boolean = false,
    initialDraftId: String? = null,
    onDisclosureComplete: () -> Unit = {},
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val orchestrator = remember { CaptureOrchestrator() }
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
        startDestination = when {
            startAtDisclosure -> "disclosure"
            initialDraftId != null -> "editor/$initialDraftId"
            else -> "drafts"
        },
    ) {
        composable("disclosure") {
            com.androidvisualqa.app.ui.permission.PermissionDisclosureScreen(
                onContinue = {
                    onDisclosureComplete()
                    navController.navigate(initialDraftId?.let { "editor/$it" } ?: "drafts") {
                        popUpTo("disclosure") { inclusive = true }
                    }
                },
            )
        }
        composable("drafts") {
            DraftListScreen(
                onNewDraft = {
                    applicationContext.startActivity(
                        Intent(applicationContext, CaptureLaunchActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
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
                    val id = draftId?.let(::DraftId)
                    if (id == null) {
                        navController.popBackStack()
                    } else {
                        scope.launch {
                            orchestrator.finishPersistedDraft(
                                draftId = id,
                                rectangle = rect,
                                feedback = text,
                                draftStore = draftStore,
                                reportHistory = reportHistory,
                                draftDirectory = draftStore.directory,
                            ).onSuccess {
                                navController.popBackStack()
                            }.onFailure { error ->
                                Toast.makeText(
                                    applicationContext,
                                    "Could not save report: ${error.message ?: "unknown error"}",
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        }
                    }
                },
                onCancel = { navController.popBackStack() },
            )
        }
    }
}

/**
 * Draft list screen. Its capture action uses the same entry point as Quick
 * Settings and S Pen Air Command.
 */
@Composable
private fun DraftListScreen(
    onNewDraft: () -> Unit,
    onOpenDraft: (String) -> Unit,
) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewDraft,
                icon = { Text(text = "+", style = MaterialTheme.typography.titleLarge) },
                text = { Text(text = stringResource(R.string.capture_screen_action)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = stringResource(R.string.draft_list_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.draft_list_empty_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.draft_list_saved_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp),
            )
        }
    }
}

private fun isValidDraftId(value: String): Boolean =
    Regex("draft-[0-9a-fA-F-]{36}").matches(value)

private const val EXTRA_DRAFT_ID = "draftId"

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
