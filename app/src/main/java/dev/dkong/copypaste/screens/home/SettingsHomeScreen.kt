package dev.dkong.copypaste.screens.home

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.dkong.copypaste.composables.SectionHeading
import dev.dkong.copypaste.composables.SettingsItem

@Composable
fun SettingsHomeScreen(navHostController: NavHostController) {
    LazyColumn(
//        verticalArrangement = Arrangement.spacedBy(8.dp),
//        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
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
                    text = "Not connected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
                Button(
                    onClick = {},
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
                var serverAddress by remember { mutableStateOf("192.168.1.188") }
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
                    supportingText = {
                        Text(text = "Please use a valid IPv4 address (no FQDN).")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                var serverPort by remember { mutableStateOf("5000") }
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
            var isAccessibilityGranted by remember { mutableStateOf(true) }

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
    }
}