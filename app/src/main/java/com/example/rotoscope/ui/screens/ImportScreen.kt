package com.example.rotoscope.ui.screens

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rotoscope.viewmodel.ImportUiState
import com.example.rotoscope.viewmodel.ImportViewModel

/**
 * NOTE: no heavy-processing warning dialog here — copying already-existing image files is plain
 * I/O, not the kind of sustained CPU/GPU work that heats the phone. That warning belongs on
 * Converter (video decode) and, later, the segmentation/propagation screens.
 */
@Composable
fun ImportScreen(onSequenceReady: () -> Unit, onNeedConversion: () -> Unit) {
    val viewModel: ImportViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) viewModel.ingestImageSequence(uris, projectName = "project_${System.currentTimeMillis()}")
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Import", style = MaterialTheme.typography.titleLarge)
            Text(
                "Rotoscoping works from an image sequence. Have a video instead? Convert it to a sequence first.",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(onClick = { pickImages.launch("image/*") }) {
                Text("Choose Image Sequence")
            }
            OutlinedButton(onClick = onNeedConversion) {
                Text("I have a video — convert it first")
            }

            when (val state = uiState) {
                is ImportUiState.Importing -> {
                    val progress = if (state.total > 0) state.done.toFloat() / state.total else 0f
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("Importing frame ${state.done}/${state.total}", style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(onClick = { viewModel.cancel() }) { Text("Cancel") }
                }
                is ImportUiState.Complete -> {
                    Text("Imported ${state.sequence.frameCount} frames.", style = MaterialTheme.typography.bodyMedium)
                    if (state.skippedCount > 0) {
                        Text(
                            "${state.skippedCount} file(s) couldn't be read and were skipped.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Button(onClick = onSequenceReady) { Text("Continue") }
                }
                is ImportUiState.Error -> {
                    Text("Error: ${state.reason}", style = MaterialTheme.typography.bodyMedium)
                }
                ImportUiState.Idle -> Unit
            }
        }
    }
}
