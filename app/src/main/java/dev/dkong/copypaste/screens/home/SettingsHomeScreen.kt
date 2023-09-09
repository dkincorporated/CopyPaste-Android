package dev.dkong.copypaste.screens.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.github.kittinunf.fuel.Fuel
import dev.dkong.copypaste.composables.SectionHeading
import dev.dkong.copypaste.composables.SettingsItem
import dev.dkong.copypaste.utils.ConnectionManager
import kotlinx.coroutines.launch

@Composable
fun SettingsHomeScreen(navHostController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var connected by remember { mutableStateOf(false) }
    ConnectionManager.checkConnection { connected = it }

    /*
    This permission-related stuff isn't actually needed :(
    @Composable
    fun permCallback(callback: (Boolean) -> Unit): ManagedActivityResultLauncher<String, Boolean> =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean -> callback(isGranted) }

    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
     */

    var serverAddress by remember { mutableStateOf(ConnectionManager.serverAddress) }
    var serverPort by remember { mutableStateOf(ConnectionManager.serverPort) }
    var isAccessibilityGranted by remember { mutableStateOf(true) }
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }


    LazyColumn {
        item {
            SectionHeading(heading = "Server connection")
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (connected) "Connected" else "Not connected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (connected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                )
                Button(
                    onClick = {
                        connected = false
                        ConnectionManager.connect(
                            serverAddress,
                            serverPort
                        ) { _, _, _, successful ->
                            connected = successful
                            if (successful) {
                                scope.launch {
                                    ConnectionManager.updateConnection(
                                        context,
                                        serverAddress,
                                        serverPort
                                    )
                                }
                            }
                        }
                    },
                ) {
                    Text(text = "Connect")
                }
            }
        }
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                var isAddressValid by remember { mutableStateOf(true) }
                OutlinedTextField(
                    value = serverAddress,
                    onValueChange = { text ->
                        serverAddress = text
                        isAddressValid =
                            text.matches(Regex("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"))
                    },
                    label = {
                        Text(text = "Server address")
                    },
                    singleLine = true,
                    placeholder = {
                        Text(text = "192.168.1.100")
                    },
                    isError = !isAddressValid,
                    modifier = Modifier.fillMaxWidth()
                )

                var isPortValid by remember { mutableStateOf(true) }
                OutlinedTextField(
                    value = serverPort,
                    onValueChange = { text ->
                        serverPort = text
                        isPortValid = text.matches(Regex("[0-9]+"))
                    },
                    label = {
                        Text(text = "Server port")
                    },
                    singleLine = true,
                    placeholder = {
                        Text(text = "5000")
                    },
                    isError = !isPortValid,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            SectionHeading(heading = "Services")
        }
        item {
            fun handleAccessibility() {
                isAccessibilityGranted = !isAccessibilityGranted
                // TODO
            }

            SettingsItem(
                name = "Accessibility service",
                onClick = {
                    handleAccessibility()
                },
                description = "Copy Paste uses Android's accessibility service to replay actions.",
                horizontalPadding = 16.dp,
                actionContent = {
                    Switch(checked = isAccessibilityGranted, onCheckedChange = {
                        handleAccessibility()
                    })
                }
            )
        }
        item {
            val overlayPermResult =
                rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) {
                    isOverlayGranted = Settings.canDrawOverlays(context)
                }

            fun handleAccessibility() {
                // Request for permission
                overlayPermResult.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            }

            SettingsItem(
                name = "Overlay service",
                onClick = {
                    handleAccessibility()
                },
                description = "Copy Paste overlays controls when playing actions.",
                horizontalPadding = 16.dp,
                actionContent = {
                    Switch(checked = isOverlayGranted, onCheckedChange = {
                        handleAccessibility()
                    })
                }
            )
        }
    }
}
