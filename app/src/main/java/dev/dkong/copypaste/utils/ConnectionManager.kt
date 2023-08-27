package dev.dkong.copypaste.utils

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.preferencesDataStore
import com.github.kittinunf.fuel.Fuel
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.map

object ConnectionManager {
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    var serverAddress = ""
    var serverPort = ""
    var isConnected: Boolean = false

    // DataStore keys
    private val serverAddressKey = stringPreferencesKey("serverAddress")
    private val serverPortKey = stringPreferencesKey("serverPort")

    suspend fun initialise(context: Context) {
        context.dataStore.data.map { preferences ->
            arrayOf(
                preferences[serverAddressKey] ?: "",
                preferences[serverPortKey] ?: ""
            )
        }.collect {
            serverAddress = it[0]
            serverPort = it[1]
        }

        // Run a connection check upon start-up
        checkConnection {}
    }

    suspend fun updateConnection(context: Context, ipAddress: String, port: String) {
        context.dataStore.edit { preferences ->
            preferences[serverAddressKey] = ipAddress
            preferences[serverPortKey] = port
            println("Updated connection to $ipAddress:$port")
        }
    }

    fun checkConnection(callback: (Boolean) -> Unit) {
        connect(serverAddress, serverPort) { _, _, _, successful ->
            isConnected = successful
            callback(successful)
        }
    }

    fun connect(ipAddress: String, port: String, callback: (String, String, Int, Boolean) -> Unit) {
        Fuel.get("http://$ipAddress:$port")
            .response { _, response, _ ->
                isConnected = response.statusCode == 200
                callback(
                    ipAddress,
                    port,
                    response.statusCode,
                    response.data.toString(Charsets.UTF_8).contains("Upload new File")
                )
            }
    }
}