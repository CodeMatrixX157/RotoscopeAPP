package com.example.rotoscope.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rotoscope.ui.theme.ErrorRed
import com.example.rotoscope.ui.theme.WarningAmber
import com.example.rotoscope.utils.ThermalState

/**
 * Sits at the top of the processing screen. Only visible when there's something the user
 * needs to know (paused / cooling down) — silent during normal operation so it doesn't nag.
 */
@Composable
fun ThermalStatusBanner(state: ThermalState, modifier: Modifier = Modifier) {
    val (message, color) = when (state) {
        ThermalState.HOT -> "Paused — your phone is running hot. Resuming automatically once it cools." to WarningAmber
        ThermalState.CRITICAL -> "Paused — phone is too hot to continue safely right now." to ErrorRed
        ThermalState.UNSUPPORTED -> "Heads up: this device can't report temperature, so auto-pause isn't available. If it feels hot, close the app and let it cool." to WarningAmber
        else -> null to WarningAmber
    }

    AnimatedVisibility(visible = message != null) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message ?: "", style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}
