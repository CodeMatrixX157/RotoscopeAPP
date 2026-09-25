package com.example.rotoscope.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onNewProject: () -> Unit, onConvertVideo: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Rotoscope", style = MaterialTheme.typography.titleLarge)
            Text(
                "On-device object cutout, frame by frame.",
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = onNewProject, modifier = Modifier.padding(top = 24.dp)) {
                Text("New Project")
            }
            OutlinedButton(onClick = onConvertVideo, modifier = Modifier.padding(top = 12.dp)) {
                Text("Convert Video to Image Sequence")
            }
        }
    }
}
