package dev.dkong.copypaste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.dkong.copypaste.screens.RootScreen
import dev.dkong.copypaste.screens.home.HomeScreen
import dev.dkong.copypaste.ui.theme.CopyPasteTheme
import dev.dkong.copypaste.utils.Constants.Companion.transitionAnimationSpec
import dev.dkong.copypaste.utils.Constants.Companion.transitionOffsetProportion

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CopyPasteTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navHostController = rememberNavController()
                    MainScreen(navHostController)
                }
            }
        }
    }
}

@Composable
fun MainScreen(navHostController: NavHostController) {
    NavHost(
        navController = navHostController,
        startDestination = RootScreen.Home.route,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it / transitionOffsetProportion },
                animationSpec = transitionAnimationSpec
            ) + fadeIn()
        },
        exitTransition = {
            fadeOut() + slideOutHorizontally(
                targetOffsetX = { -it / transitionOffsetProportion },
                animationSpec = transitionAnimationSpec
            )
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / transitionOffsetProportion },
                animationSpec = transitionAnimationSpec
            ) + fadeIn()
        },
        popExitTransition = {
            fadeOut() + slideOutHorizontally(
                targetOffsetX = { it / transitionOffsetProportion },
                animationSpec = transitionAnimationSpec
            )
        }
    ) {
        composable(RootScreen.Home.route) {
            HomeScreen(navHostController = navHostController)
        }
    }
}
