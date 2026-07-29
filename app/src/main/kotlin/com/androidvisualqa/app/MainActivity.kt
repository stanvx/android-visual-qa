package com.androidvisualqa.app

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.androidvisualqa.annotation.AnnotationItem
import com.androidvisualqa.annotation.NormalizedBounds
import com.androidvisualqa.database.RetentionConfig
import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.files.FileSystemDraftStore
import com.androidvisualqa.model.ids.DraftId
import com.androidvisualqa.report.FileSystemReportHistoryIndex
import com.androidvisualqa.app.ui.dashboard.CapturesDashboardScreen
import com.androidvisualqa.app.ui.history.CaptureHistoryViewModel
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
    private val pendingDraftId = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDraftId.value = intent.getStringExtra(EXTRA_DRAFT_ID)?.takeIf(::isValidDraftId)
    }

    @androidx.compose.material3.ExperimentalMaterial3Api
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                    pendingDraftId = pendingDraftId.value,
                    onDraftOpened = { pendingDraftId.value = null },
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
@androidx.compose.material3.ExperimentalMaterial3Api
private fun AppNavigation(
    applicationContext: Context,
    startAtDisclosure: Boolean = false,
    initialDraftId: String? = null,
    pendingDraftId: String? = null,
    onDraftOpened: () -> Unit = {},
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

    androidx.compose.runtime.LaunchedEffect(pendingDraftId) {
        pendingDraftId?.let { id ->
            navController.navigate("editor/$id") {
                launchSingleTop = true
            }
            onDraftOpened()
        }
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
            val historyViewModel: CaptureHistoryViewModel = viewModel()
            CapturesDashboardScreen(
                uiState = historyViewModel.uiState.collectAsStateWithLifecycle().value,
                onRefresh = historyViewModel::refresh,
                onOpenCapture = { draftId -> navController.navigate("editor/$draftId") },
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
                onSave = { annotations, text ->
                    val id = draftId?.let(::DraftId)
                    if (id == null) {
                        navController.popBackStack()
                    } else {
                        scope.launch {
                            orchestrator.finishPersistedDraft(
                                draftId = id,
                                annotations = annotations,
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
                loadSnapCandidates = {
                    draftId?.let { id ->
                        orchestrator.readCaptureContext(DraftId(id), draftStore.directory)
                            .getOrNull()
                            ?.let { context ->
                                val screen = context.screenBounds()
                                if (screen.width <= 0.0 || screen.height <= 0.0) emptyList()
                                else context.candidates.mapNotNull { node ->
                                    if (!node.isVisibleToUser || node.boundsRight <= node.boundsLeft || node.boundsBottom <= node.boundsTop) {
                                        null
                                    } else {
                                        NormalizedBounds.from(
                                            ((node.boundsLeft - screen.left) / screen.width).toFloat(),
                                            ((node.boundsTop - screen.top) / screen.height).toFloat(),
                                            ((node.boundsRight - screen.left) / screen.width).toFloat(),
                                            ((node.boundsBottom - screen.top) / screen.height).toFloat(),
                                        )
                                    }
                                }
                            }
                    }.orEmpty()
                },
                onCancel = { navController.popBackStack() },
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
@androidx.compose.material3.ExperimentalMaterial3Api
private fun EditorScreenWrapper(
    viewModel: EditorViewModel,
    draftId: String?,
    onSave: (List<AnnotationItem>, String) -> Unit,
    loadSnapCandidates: suspend () -> List<NormalizedBounds> = { emptyList() },
    onCancel: () -> Unit,
) {
    androidx.compose.runtime.LaunchedEffect(draftId) {
        viewModel.loadDraft(
            draftId?.let { DraftId(it) }
        )
        viewModel.setSnapCandidates(loadSnapCandidates())
    }

    val state = viewModel.state.collectAsStateWithLifecycle().value
    EditorScreen(
        state = state,
        onStateChange = viewModel::replaceState,
        onSave = { _, _ -> },
        onSaveAnnotations = { _, _ -> viewModel.saveAnnotations { items, savedText -> onSave(items, savedText) } },
        onCancel = onCancel,
    )
}
