package dev.dkong.copypaste.utils

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.dkong.copypaste.objects.Sequence
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Manages the actions that are stored on the device
 */
object ActionManager {
    /**
     * DataStore for storing actions
     */
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "actions")

    /**
     * List of action sequences
     */
    var sequences: Array<Sequence> = emptyArray()

    // DataStore keys
    private val sequencesKey = stringPreferencesKey("sequences")

    /**
     * Initialise the ActionManager
     */
    suspend fun initialise(context: Context) {
        context.dataStore.data.map { preferences ->
            preferences[sequencesKey]
        }.collect { sequencesJson ->
            sequencesJson?.let {
                sequences = Json.decodeFromString(it)
                Log.d("Saved retrieval", sequences[0].toString())
            }
        }
    }

    /**
     * Add a new sequence to the list of sequences
     */
    suspend fun addSequence(context: Context, newSequence: Sequence) {
        context.dataStore.edit { preferences ->
            preferences[sequencesKey] = Json.encodeToString(sequences + newSequence)
        }
    }

    /**
     * Remove a sequence from the list of sequences
     */
    suspend fun removeSequence(context: Context, sequenceToRemove: Sequence) {
        context.dataStore.edit { preferneces ->
            val modifiedSequences = sequences.filter { sequence -> sequence != sequenceToRemove }
            preferneces[sequencesKey] = Json.encodeToString(modifiedSequences)
        }
    }
}