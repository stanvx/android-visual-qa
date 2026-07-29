package com.androidvisualqa.app.ui.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.androidvisualqa.files.DraftDirectory
import com.androidvisualqa.files.FileSystemDraftStore
import com.androidvisualqa.report.FileSystemReportHistoryIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

public data class CaptureHistoryUiState(
    val isLoading: Boolean = true,
    val items: List<CaptureHistoryItem> = emptyList(),
    val error: Throwable? = null,
)

public class CaptureHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CaptureHistoryRepository(
        draftStore = FileSystemDraftStore(
            DraftDirectory(application.getDir("drafts", android.content.Context.MODE_PRIVATE).toPath()),
        ),
        reportHistory = FileSystemReportHistoryIndex(
            File(application.filesDir, "report_history.jsonl").toPath(),
        ),
    )

    private val _uiState = MutableStateFlow(CaptureHistoryUiState())
    public val uiState: StateFlow<CaptureHistoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    public fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { repository.load() }
                .onSuccess { items -> _uiState.value = CaptureHistoryUiState(isLoading = false, items = items) }
                .onFailure { error -> _uiState.value = CaptureHistoryUiState(isLoading = false, error = error) }
        }
    }
}
