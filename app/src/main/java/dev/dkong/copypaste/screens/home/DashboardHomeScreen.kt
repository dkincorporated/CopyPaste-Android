package dev.dkong.copypaste.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.dkong.copypaste.composables.SectionHeading
import dev.dkong.copypaste.screens.RootScreen
import dev.dkong.copypaste.utils.ConnectionManager

@Composable
fun DashboardHomeScreen(
    navHostController: NavHostController,
    selectedPage: MutableState<HomeScreenItem>,
    homeNavHostController: NavHostController
) {
    LazyColumn(
        modifier = Modifier.padding(16.dp)
    ) {
        item {
            // Card for no connection to server
            Card(
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
                ) {
                    Text(text = "New")
                }
            }
        }
    }
}