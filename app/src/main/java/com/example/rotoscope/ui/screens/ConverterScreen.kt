package com.example.rotoscope.ui.screens

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rotoscope.ui.components.ProcessingWarningDialog
import com.example.rotoscope.ui.components.ThermalStatusBanner
import com.example.rotoscope.utils.AppPreferences
import com.example.rotoscope.viewmodel.ConvertUiState
import com.example.rotoscope.viewmodel.ConverterViewModel
import kotlinx.coroutines.launch

@Composable
fun ConverterScreen() {
    val context = LocalContext.current
    val viewModel: ConverterViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val thermalState by viewModel.thermalState.collectAsState()
    val scope = rememberCoroutineScope()

    var folderName by remember { mutableStateOf("converted_${System.currentTimeMillis()}") }
    var pendingVideoUri: Uri? by remember { mutableStateOf(null) }
    var showWarning by remember { mutableStateOf(false) }
    var permissionDeniedMessage: String? by remember { mutableStateOf(null) }
    val skipWarning by AppPreferences.shouldSkipWarning(context).collectAsState(initial = false)

    val legacyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val uri = pendingVideoUri
        pendingVideoUri = null
        if (granted && uri != null) {
            permissionDeniedMessage = null
            viewModel.convert(uri, folderName)
        } else {
            permissionDeniedMessage = "Storage permission is needed to save converted frames on this Android version."
        }
    }

    fun startConversionOrRequestPermission(uri: Uri) {
        if (viewModel.needsLegacyWritePermission()) {
            pendingVideoUri = uri
            legacyPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            viewModel.convert(uri, folderName)
        }
    }

    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        permissionDeniedMessage = null
        if (skipWarning) {
            startConversionOrRequestPermission(uri)
        } else {
            pendingVideoUri = uri
            showWarning = true
        }
    }

    if (showWarning) {
        ProcessingWarningDialog(
            onDismiss = { showWarning = false; pendingVideoUri = null },
            onConfirm = { dontShowAgain ->
                showWarning = false
                if (dontShowAgain) scope.launch { AppPreferences.setSkipWarning(context, true) }
                pendingVideoUri?.let { startConversionOrRequestPermission(it) }
            }
        )
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ThermalStatusBanner(state = thermalState)

            Text("Convert Video to Image Sequence", style = MaterialTheme.typography.titleLarge)
            Text(
                "Turns a video into a numbered PNG sequence saved to Pictures/RotoscopeFrames — " +
                    "find it in your gallery afterward, or pick it as an image sequence from the main import screen.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Folder name") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = { pickVideo.launch("video/*") }) {
                Text("Choose Video File")
            }

            permissionDeniedMessage?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }

            when (val state = uiState) {
                is ConvertUiState.Converting -> {
                    val progress = if (state.total > 0) state.done.toFloat() / state.total else 0f
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text(
                        if (state.paused) "Paused (${state.done}/${state.total}) — cooling down…"
                        else "Converting frame ${state.done}/${state.total}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(onClick = { viewModel.cancel() }) { Text("Cancel") }
                }
                is ConvertUiState.Complete -> {
                    Text(
                        "Saved ${state.frameCount} frames to Pictures/RotoscopeFrames/${state.folderName}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is ConvertUiState.Error -> {
                    Text("Error: ${state.reason}", style = MaterialTheme.typography.bodyMedium)
                }
                ConvertUiState.Idle -> Unit
            }
        }
    }
}
