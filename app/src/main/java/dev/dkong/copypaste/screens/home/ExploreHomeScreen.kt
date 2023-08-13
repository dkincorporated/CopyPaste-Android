package dev.dkong.copypaste.screens.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController

@Composable
fun ExploreHomeScreen(navHostController: NavHostController) {
    Text(
        text = "Explore",
        style = MaterialTheme.typography.titleLarge
    )
}