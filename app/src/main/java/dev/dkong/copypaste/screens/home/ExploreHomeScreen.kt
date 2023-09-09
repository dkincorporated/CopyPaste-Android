@file:OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package dev.dkong.copypaste.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.dkong.copypaste.R

@Composable
fun VideoItem() {
    OutlinedCard(
        onClick = {
            // TODO: launch the import process for this video
        }, modifier = Modifier.fillMaxWidth(0.48f)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_sample_dalle),
            contentDescription = null,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .fillMaxWidth()
        )
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Sample video 1",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Some description about this video.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ExploreHomeScreen(navHostController: NavHostController) {
    var viewMode by remember { mutableStateOf(0) }

    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        item {
            val viewModes = arrayOf("Grid", "Column")

            SingleChoiceSegmentedButtonRow {
                viewModes.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = viewMode == index,
                        onClick = { viewMode = index },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = viewModes.size
                        ),
                        icon = {}
                    ) {
                        Text(text = mode)
                    }
                }
            }
        }
        item {
            FlowRow(
                maxItemsInEachRow = 2,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoItem()
                VideoItem()
            }
        }
        item {
            Box(modifier = Modifier.navigationBarsPadding())
        }
    }
}