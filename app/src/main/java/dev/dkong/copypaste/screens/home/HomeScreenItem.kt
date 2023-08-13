package dev.dkong.copypaste.screens.home

import androidx.annotation.DrawableRes
import dev.dkong.copypaste.R

enum class HomeScreenItem(
    val route: String,
    val displayName: String,
    @DrawableRes val icon: Int,
    @DrawableRes val selectedIcon: Int? = null
) {
    Dashboard("dashboard", "Dashboard", R.drawable.outline_space_dashboard_24),
    Explore("explore", "Explore", R.drawable.outline_explore_24),
    Settings("settings", "Settings", R.drawable.fancy_outlined_settings)
}
