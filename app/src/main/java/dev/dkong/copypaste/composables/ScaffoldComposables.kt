@file:OptIn(ExperimentalMaterial3Api::class)

package dev.dkong.copypaste.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import dev.dkong.copypaste.utils.Constants

// Scaffold-type Composables, used throughout the app
// Author: David Kong

/**
 * Base template for screens using a scaffold
 * @param navController the nav controller for the app
 * @param title the title to be displayed at the top
 * @param horizontalPadding the horizontal padding for the content
 * @param navigationIcon the icon to be displayed in the navigation button
 * @param onNavigationIconClick the click action for the navigation icon
 * @param content the content to be displayed (must be lazy list)
 */
@Composable
fun LargeTopAppbarScaffold(
    navController: NavHostController,
    title: String,
    topAppBarActions: @Composable RowScope.() -> Unit = {},
    horizontalPadding: Dp = 0.dp,
    navigationIcon: ImageVector = Icons.Default.ArrowBack,
    onNavigationIconClick: () -> Unit = { navController.navigateUp() },
    navigationBar: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Constants.appSurfaceColour(),
        contentWindowInsets = WindowInsets(bottom = 0.dp),
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Constants.appSurfaceColour(),
                    scrolledContainerColor = Constants.scrolledAppbarContainerColour()
                ),
                navigationIcon = {
                    ElevatedAppBarNavigationIcon(
                        onClick = onNavigationIconClick,
                        icon = navigationIcon
                    )
                },
                actions = topAppBarActions
            )
        },
        bottomBar = navigationBar
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = horizontalPadding)
                .fillMaxWidth()
        ) {
            content()
            item {
                Box(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}

/**
 * Base template for screens using a scaffold
 * @param navController the nav controller for the app
 * @param title the title to be displayed at the top
 * @param horizontalPadding the horizontal padding for the content
 * @param navigationIcon the icon to be displayed in the navigation button
 * @param onNavigationIconClick the click action for the navigation icon
 * @param content the content to be displayed (in box form)
 */
@Composable
fun LargeTopAppbarScaffoldBox(
    navController: NavHostController,
    title: String,
    horizontalPadding: Dp = 0.dp,
    navigationIcon: ImageVector = Icons.Default.ArrowBack,
    onNavigationIconClick: (() -> Unit)? = { navController.navigateUp() },
    navigationBar: @Composable () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Constants.appSurfaceColour(),
        contentWindowInsets = WindowInsets(bottom = 0.dp),
        topBar = {
            LargeTopAppBar(
                title = { Text(title) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = Constants.appSurfaceColour(),
                    scrolledContainerColor = Constants.scrolledAppbarContainerColour()
                ),
                navigationIcon = {
                    onNavigationIconClick?.let {
                        ElevatedAppBarNavigationIcon(
                            onClick = onNavigationIconClick,
                            icon = navigationIcon
                        )
                    }
                }
            )
        },
        bottomBar = navigationBar
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = horizontalPadding)
                .navigationBarsPadding()
                .fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Heading for a section
 * @param heading the text to be displayed
 * @param modifier any modifier for the heading
 */
@Composable
fun SectionHeading(
    heading: String,
    modifier: Modifier = Modifier,
    includeHorizontalPadding: Boolean = true
) {
    Text(
        text = heading,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(
            vertical = 8.dp,
            horizontal = if (includeHorizontalPadding) 16.dp else 0.dp
        )
    )
}

/**
 * A rounded, elevated column to display content that stands out from the background surface
 * @param modifier any modifier for the column
 * @param content the content to be displayed in the column
 */
@Composable
fun MaterialColumn(modifier: Modifier = Modifier, content: @Composable (ColumnScope.() -> Unit)) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Constants.materialListCardContainerColour()),
        content = content
    )
}

/**
 * Implementation of a vertical divider, because [Divider]s are only horizontal
 * @param modifier additional modifiers for the divider
 * @param color override on surface variant
 * @param thickness override the thickness
 */
@Composable
fun VerticalDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    thickness: Dp = 1.dp
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(thickness)
            .background(color = color)
    )
}

/**
 * A section that can be expanded or collapsed with a heading
 * @param sectionName the name of the section
 * @param initiallyExpanded whether the section should be expanded upon loading
 * @param horizontalPadding the horizontal padding for the column
 * @param content the content to be displayed in the column
 */
@Composable
fun ExpandableSection(
    sectionName: String,
    initiallyExpanded: Boolean = false,
    horizontalPadding: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val expanded = remember { mutableStateOf(initiallyExpanded) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded.value = !expanded.value }
                .padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeading(heading = sectionName, modifier = Modifier.weight(1f))
            IconButton(onClick = { expanded.value = !expanded.value }) {
                Image(
                    if (expanded.value) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    if (expanded.value) "Collapse $sectionName" else "Expand $sectionName",
                    colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
                )
            }
        }
        AnimatedVisibility(
            visible = expanded.value,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(horizontal = horizontalPadding), content = content)
        }
    }
}

/**
 * Icon to be set as the navigation icon in a [TopAppBar] that is at the same elevation as an elevated/collapsed [TopAppBar]
 * @param onClick the on-click action
 * @param icon the [ImageVector] of the icon to be in the button
 * @param iconContentDescription accessible content description for the icon in the button
 */
@Composable
fun ElevatedAppBarNavigationIcon(
    onClick: () -> Unit,
    icon: ImageVector = Icons.Default.ArrowBack,
    iconContentDescription: String = "Go back"
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .clip(CircleShape)
            .background(Constants.scrolledAppbarContainerColour())
    ) {
        Icon(icon, contentDescription = iconContentDescription)
    }
}