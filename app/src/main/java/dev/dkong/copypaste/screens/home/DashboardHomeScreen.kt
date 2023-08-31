@file:OptIn(ExperimentalMaterial3Api::class)

package dev.dkong.copypaste.screens.home

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.dkong.copypaste.composables.SectionHeading
import dev.dkong.copypaste.screens.RootScreen
import dev.dkong.copypaste.utils.ConnectionManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.TextUnit
import dev.dkong.copypaste.objects.Sequence
import dev.dkong.copypaste.objects.Action
import dev.dkong.copypaste.objects.Position
import dev.dkong.copypaste.utils.ActionManager
import kotlinx.coroutines.launch

@Composable
fun DashboardHomeScreen(
    navHostController: NavHostController,
    selectedPage: MutableState<HomeScreenItem>,
    homeNavHostController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isConnected by remember { mutableStateOf(false) }
    var sequences = remember { mutableStateListOf<Sequence>() }

    LaunchedEffect(Unit) {
        ActionManager.listen(context) { s ->
            sequences.clear()
            sequences.addAll(s)
            Log.d("FETCHING SAVED", sequences.toString())
            Log.d("FETCHING SAVED", sequences.size.toString())
        }
    }

    LazyColumn(
        modifier = Modifier.padding(16.dp)
    ) {
        item {
            // Card for no connection to server
            ConnectionManager.checkConnection { connected ->
                isConnected = connected
            }
            if (!isConnected) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Couldn't connect to the server",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Text(
                            text = "Please check the connection and the server settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Button(
                            onClick = {
                                homeNavHostController.navigate(HomeScreenItem.Settings.route)
                                selectedPage.value = HomeScreenItem.Settings
                            },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 8.dp)
                        ) {
                            Text(text = "Server settings")
                        }
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            Icons.Default.Check.name,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                        Text(
                            text = "Connected to server",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                }
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                SectionHeading(heading = "Actions", includeHorizontalPadding = false)
                Button(
                    onClick = {
                        navHostController.navigate(RootScreen.Upload.route)
                    },
                    enabled = isConnected
                ) {
                    Text(text = "New")
                }
            }
        }
        itemsIndexed(sequences) { _, item ->
            SequenceCard(sequence = item, navHostController = navHostController)
        }
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = {
                    scope.launch {
                        ActionManager.addSequence(
                            context,
                            Sequence(
                                id = System.currentTimeMillis() / 1000,
                                creationTime = System.currentTimeMillis() / 1000,
                                name = "Test sequence",
                                status = "SUCCESS",
                                result = arrayOf(
                                    Action(
                                        actType = "SWIPE",
                                        firstFrame = 13,
                                        resultingScreenOcr = "NeverGonnaGiveYouUpNeverGonnaLetYouDown",
                                        taps = arrayOf(
                                            Position(1f, 3f),
                                            Position(2f, 4f),
                                            Position(3f, 5f),
                                            Position(4f, 6f)
                                        )
                                    )
                                )
                            )
                        )
                    }
                }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Add test data")
                }
            }
        }
    }
}

@Composable
fun SequenceCard(sequence: Sequence, navHostController: NavHostController) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = {
            navHostController.navigate("seq_info?id=${sequence.id}")
            // TODO: Launch the sequence detail page
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column {
                sequence.name?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "${sequence.result?.size ?: "No"} actions | ${sequence.id}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            OutlinedButton(
                onClick = {
                    // TODO: Execute the action
                }
            ) {
                Text("Execute")
            }
        }
    }
}