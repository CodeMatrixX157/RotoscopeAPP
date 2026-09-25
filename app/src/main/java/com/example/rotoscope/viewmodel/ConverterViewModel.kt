package com.example.rotoscope.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rotoscope.data.ExtractionProgress
import com.example.rotoscope.data.FrameExtractor
import com.example.rotoscope.data.MediaStoreFrameWriter
import com.example.rotoscope.utils.ThermalMonitor
import com.example.rotoscope.utils.ThermalState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class ConvertUiState {
    object Idle : ConvertUiState()
    data class Converting(val done: Int, val total: Int, val paused: Boolean) : ConvertUiState()
    data class Complete(val folderName: String, val frameCount: Int) : ConvertUiState()
    data class Error(val reason: String) : ConvertUiState()
}

class ConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val extractor = FrameExtractor(application)
    private val mediaStoreWriter = MediaStoreFrameWriter(application)
    private val thermalMonitor = ThermalMonitor(application)

    private val _uiState = MutableStateFlow<ConvertUiState>(ConvertUiState.Idle)
    val uiState: StateFlow<ConvertUiState> = _uiState

    private val _thermalState = MutableStateFlow(ThermalState.NORMAL)
    val thermalState: StateFlow<ThermalState> = _thermalState

    private var currentJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            thermalMonitor.thermalState().collect { _thermalState.value = it }
        }
    }

    fun needsLegacyWritePermission(): Boolean = mediaStoreWriter.requiresLegacyWritePermission()

    fun cancel() {
        currentJob?.cancel()
        _uiState.value = ConvertUiState.Idle
    }

    fun convert(videoUri: Uri, folderName: String, targetFps: Double = 24.0) {
        currentJob = viewModelScope.launch {
            extractor.extract(videoUri, targetFps) { index, bitmap ->
                mediaStoreWriter.writeFrame(folderName, index, bitmap)
            }.collect { progress ->
                // Same thermal gate as the original import flow — pause on HOT/CRITICAL, resume on NORMAL/WARM.
                while (_thermalState.value == ThermalState.HOT || _thermalState.value == ThermalState.CRITICAL) {
                    if (progress is ExtractionProgress.InProgress) {
                        _uiState.value = ConvertUiState.Converting(progress.framesDone, progress.framesTotal, paused = true)
                    }
                    thermalMonitor.thermalState().first { it == ThermalState.NORMAL || it == ThermalState.WARM }
                }

                _uiState.value = when (progress) {
                    is ExtractionProgress.InProgress -> ConvertUiState.Converting(progress.framesDone, progress.framesTotal, paused = false)
                    is ExtractionProgress.Done -> ConvertUiState.Complete(folderName, progress.totalFrames)
                    is ExtractionProgress.Failed -> ConvertUiState.Error(progress.reason)
                }
            }
        }
    }
}
