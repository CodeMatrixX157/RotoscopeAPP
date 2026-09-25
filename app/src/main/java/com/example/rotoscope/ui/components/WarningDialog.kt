package com.example.rotoscope.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rotoscope.ui.theme.WarningAmber

/**
 * Shown once before the user starts their first heavy job (frame extraction / mask propagation /
 * matting export), unless they've checked "don't show again". Plain-language, not legalese —
 * the goal is the user actually reads it, not that it covers us.
 */
@Composable
fun ProcessingWarningDialog(
    onDismiss: () -> Unit,
    onConfirm: (dontShowAgain: Boolean) -> Unit
) {
    var dontShowAgain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = WarningAmber) },
        title = { Text("Before you start") },
        text = {
            Column {
                Text(
                    "Rotoscoping runs entirely on your phone, so this uses a lot of CPU/GPU for as long as it's processing. A few things to know:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                BulletLine("Your phone will get warm — this is expected, not a malfunction.")
                BulletLine("Processing pauses automatically if your phone gets too hot, and resumes on its own once it cools down. You don't need to do anything.")
                BulletLine("Keep the app open and your phone plugged in for long sequences — closing the app stops the job.")
                BulletLine("Longer sequences and higher resolutions take longer. You can cancel anytime without losing your object selection.")
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = dontShowAgain, onCheckedChange = { dontShowAgain = it })
                    Text("Don't show this again", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(dontShowAgain) }) { Text("Got it, start processing") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun BulletLine(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", style = MaterialTheme.typography.bodyMedium)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
    Spacer(Modifier.height(6.dp))
}
