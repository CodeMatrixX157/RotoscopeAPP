package com.example.rotoscope.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rotoscope.data.FrameSequence
import com.example.rotoscope.data.IngestProgress
import com.example.rotoscope.data.SequenceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ImportUiState {
    object Idle : ImportUiState()
    data class Importing(val done: Int, val total: Int) : ImportUiState()
    data class Complete(val sequence: FrameSequence, val skippedCount: Int) : ImportUiState()
    data class Error(val reason: String) : ImportUiState()
}

/**
 * Rotoscoping now only accepts an already-prepared image sequence — no video path here anymore.
 * Video → sequence conversion lives in ConverterViewModel/ConverterScreen as its own standalone
 * tool, which keeps this pipeline's assumptions simple: by the time frames get here, they're
 * already a validated numbered folder.
 *
 * This is plain file I/O, not heavy compute — no thermal gating here (unlike Converter/future
 * segmentation), since copying files doesn't meaningfully heat the phone.
 */
class ImportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SequenceRepository(application)

    private val _uiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val uiState: StateFlow<ImportUiState> = _uiState

    private var currentJob: Job? = null

    fun ingestImageSequence(imageUris: List<Uri>, projectName: String) {
        currentJob = viewModelScope.launch {
            repository.ingestImageFolder(imageUris, projectName).collect { progress ->
                _uiState.value = when (progress) {
                    is IngestProgress.InProgress -> ImportUiState.Importing(progress.done, progress.total)
                    is IngestProgress.Done -> ImportUiState.Complete(progress.sequence, progress.skippedCount)
                    is IngestProgress.Failed -> ImportUiState.Error(progress.reason)
                }
            }
        }
    }

    fun cancel() {
        currentJob?.cancel()
        _uiState.value = ImportUiState.Idle
    }
}
