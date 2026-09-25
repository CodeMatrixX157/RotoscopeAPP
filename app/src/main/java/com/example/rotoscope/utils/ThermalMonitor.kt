package com.example.rotoscope.utils

import android.content.Context
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Simplified thermal state the rest of the app reacts to.
 * Any heavy processing loop (frame extraction, mask propagation, matting)
 * should collect [ThermalMonitor.thermalState] and pause on [ThermalState.HOT]/[ThermalState.CRITICAL],
 * resuming automatically once it drops back to [ThermalState.NORMAL].
 */
enum class ThermalState {
    NORMAL,     // safe to run at full speed
    WARM,       // still safe, but a good point to reduce batch size / show a soft warning
    HOT,        // pause heavy work
    CRITICAL,   // pause immediately, surface a hard warning
    UNSUPPORTED // API < 29 — device has no thermal status API; app cannot auto pause/resume on this device
}

class ThermalMonitor(private val context: Context) {

    private val powerManager: PowerManager?
        get() = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    /** True only on API 29+, where PowerManager exposes thermal status. */
    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun mapStatus(status: Int): ThermalState = when (status) {
        PowerManager.THERMAL_STATUS_NONE, PowerManager.THERMAL_STATUS_LIGHT -> ThermalState.NORMAL
        PowerManager.THERMAL_STATUS_MODERATE -> ThermalState.WARM
        PowerManager.THERMAL_STATUS_SEVERE -> ThermalState.HOT
        PowerManager.THERMAL_STATUS_CRITICAL,
        PowerManager.THERMAL_STATUS_EMERGENCY,
        PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalState.CRITICAL
        else -> ThermalState.NORMAL
    }

    /**
     * Cold-flow of thermal state changes. Callers (e.g. the mask-propagation module)
     * collect this and gate their per-frame work loop on it — never poll manually.
     */
    fun thermalState(): Flow<ThermalState> = callbackFlow {
        if (!isSupported) {
            trySend(ThermalState.UNSUPPORTED)
            awaitClose { }
            return@callbackFlow
        }
        val pm = powerManager
        if (pm == null) {
            trySend(ThermalState.UNSUPPORTED)
            awaitClose { }
            return@callbackFlow
        }

        // Emit current state immediately so collectors don't wait for the first change.
        trySend(mapStatus(pm.currentThermalStatus))

        val listener = PowerManager.OnThermalStatusChangedListener { status ->
            trySend(mapStatus(status))
        }
        pm.addThermalStatusListener(listener)
        awaitClose { pm.removeThermalStatusListener(listener) }
    }
}
