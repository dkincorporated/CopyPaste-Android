@file:OptIn(ExperimentalLayoutApi::class)

package dev.dkong.copypaste.screens.sequence

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.dkong.copypaste.R
import dev.dkong.copypaste.composables.LargeTopAppbarScaffold
import dev.dkong.copypaste.composables.SectionHeading
import dev.dkong.copypaste.objects.Sequence
import dev.dkong.copypaste.utils.ActionManager
import dev.dkong.copypaste.utils.ExecutionManager
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

@Composable
fun SequenceInfoScreen(
    navHostController: NavHostController,
    id: Long?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sequence by remember { mutableStateOf<Sequence?>(null) }

    LaunchedEffect(Unit) {
        id?.let {
            ActionManager.getSequence(context, it) { s ->
                sequence = s
            }
        }
    }

    LargeTopAppbarScaffold(
        navController = navHostController,
        title = sequence?.name ?: "Sequence",
        topAppBarActions = {
            IconButton(onClick = {
                // TODO: Edit name dialog
            }) {
                Image(
                    painter = painterResource(id = R.drawable.fancy_outlined_edit),
                    contentDescription = "Edit",
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
                )
            }
        },
        horizontalPadding = 16.dp,
        navigationBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            sequence?.let { s ->
                                ActionManager.removeSequence(context, s)
                                navHostController.navigateUp()
                            }
                        }
                    }) {
                        Image(
                            painter = painterResource(R.drawable.fancy_outlined_bin),
                            contentDescription = "Delete",
                            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error)
                        )
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            sequence?.let { s ->
                                ExecutionManager.setUpSequence(s)
                                ExecutionManager.start()
                            }
                        },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(Icons.Outlined.PlayArrow, Icons.Outlined.PlayArrow.name)
                    }
                }
            )
        }
    ) {
        sequence?.let { s ->
            item {
                SectionHeading(heading = "Info", includeHorizontalPadding = false)
            }
            s.creationTime?.let { time ->
                item {
                    val displayTime =
                        DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochSecond(time))
                    Text(text = "Created: $displayTime")
                }
            }
            item {
                SectionHeading(heading = "Actions", includeHorizontalPadding = false)
            }
            s.result?.let { actions ->
                itemsIndexed(actions) { _, action ->
                    Text(text = "Type: ${action.actType.toString()}")
                    Text(text = "Hint: ${action.actionHint}")
                    Text(text = "First frame: ${action.firstFrame}")
                    Text(text = "Positions:")
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        action.taps.forEach { tap ->
                            SuggestionChip(
                                onClick = {},
                                label = { Text(text = "(${tap.x}, ${tap.y})") })
                        }
                    }
                }
            }
        }
    }
}