package dev.dkong.copypaste.screens.home

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.dkong.copypaste.composables.LargeTopAppbarScaffoldBox
import dev.dkong.copypaste.utils.Constants

@Composable
fun HomeScreen(navHostController: NavHostController) {
    val selectedPage = remember { mutableStateOf(HomeScreenItem.Dashboard) }
    val homeNavHostController = rememberNavController()

    LargeTopAppbarScaffoldBox(
        navController = navHostController,
        title = selectedPage.value.displayName,
        navigationBar = {
            NavigationBar {
                HomeScreenItem.values().forEach { item ->
                    NavigationBarItem(
                        selected = selectedPage.value == item,
                        onClick = {
                            homeNavHostController.navigate(item.route)
                            selectedPage.value = item
                        },
                        icon = {
                            Image(
                                painter = painterResource(id = item.icon),
                                contentDescription = item.displayName,
                                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                            )
                        },
                        label = {
                            Text(
                                text = item.displayName,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                }
            }
        },
        onNavigationIconClick = null
    ) {
        NavHost(
            navController = homeNavHostController,
            startDestination = HomeScreenItem.Dashboard.route,
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it / Constants.transitionOffsetProportion },
                    animationSpec = Constants.transitionAnimationSpec
                ) + fadeIn()
            },
            exitTransition = {
                fadeOut() + slideOutVertically(
                    targetOffsetY = { -it / Constants.transitionOffsetProportion },
                    animationSpec = Constants.transitionAnimationSpec
                )
            },
            popEnterTransition = {
                slideInVertically(
                    initialOffsetY = { -it / Constants.transitionOffsetProportion },
                    animationSpec = Constants.transitionAnimationSpec
                ) + fadeIn()
            },
            popExitTransition = {
                fadeOut() + slideOutVertically(
                    targetOffsetY = { it / Constants.transitionOffsetProportion },
                    animationSpec = Constants.transitionAnimationSpec
                )
            }
        ) {
            composable(HomeScreenItem.Dashboard.route) {
                DashboardHomeScreen(navHostController = navHostController, selectedPage = selectedPage, homeNavHostController = homeNavHostController)
            }
            composable(HomeScreenItem.Explore.route) {
                ExploreHomeScreen(navHostController = homeNavHostController)
            }
            composable(HomeScreenItem.Settings.route) {
                SettingsHomeScreen(navHostController = homeNavHostController)
            }
        }
    }
}