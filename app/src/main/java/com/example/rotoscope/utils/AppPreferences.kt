package com.example.rotoscope.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "rotoscope_prefs")

object AppPreferences {
    private val KEY_SKIP_PROCESSING_WARNING = booleanPreferencesKey("skip_processing_warning")

    /** Whether the user checked "don't show this again" on the heavy-processing warning dialog. */
    fun shouldSkipWarning(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[KEY_SKIP_PROCESSING_WARNING] ?: false }

    suspend fun setSkipWarning(context: Context, skip: Boolean) {
        context.dataStore.edit { it[KEY_SKIP_PROCESSING_WARNING] = skip }
    }
}
