package dev.dkong.copypaste.utils

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

    /**
     * Next surrogate ID
     */
    var nextId: Int = 0

    // DataStore keys
    private val sequencesKey = stringPreferencesKey("sequences")

    /**
     * Key to retrieve current surrogate ID value
     */
    private val idKey = intPreferencesKey("id")

    /**
     * Initialise the ActionManager
     */
    suspend fun initialise(context: Context) {
        context.dataStore.data.map { preferences ->
            preferences[sequencesKey]
        }.collect { sequencesJson ->
            sequencesJson?.let {
                sequences = Json.decodeFromString(it)
            }
        }
    }

    /**
     * Listen for changes to the list of sequences
     */
    suspend fun listen(context: Context, callback: (Array<Sequence>) -> Unit) {
        context.dataStore.data.map { preferences ->
            preferences[sequencesKey]
        }.collect { sequencesJson ->
            sequencesJson?.let {
                sequences = Json.decodeFromString(it)
                callback(sequences)
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

    suspend fun getSequence(context: Context, id: Long, callback: (Sequence?) -> Unit) {
        context.dataStore.data.map { preferences ->
            preferences[sequencesKey]
        }.collect { sequences ->
            sequences?.let {
                val sequence = Json.decodeFromString<Array<Sequence>>(it)
                callback(sequence.find { s -> s.id == id })
            }
        }
    }

    /**
     * Remove a sequence from the list of sequences
     */
    suspend fun removeSequence(context: Context, sequenceToRemove: Sequence) {
        context.dataStore.edit { preferences ->
            val modifiedSequences = sequences.filter { sequence -> sequence != sequenceToRemove }
            Log.d("DELETING SEQ", modifiedSequences.size.toString())
            preferences[sequencesKey] = Json.encodeToString(modifiedSequences)
        }
    }
}