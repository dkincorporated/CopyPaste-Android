package dev.dkong.copypaste.screens

/**
 * Set of screens at the root level (controlled by MainActivity)
 * @param route the navigation route path
 * @param name the display name of the screen
 * @see Screen
 */
sealed class RootScreen (route: String, name: String) : Screen(route, name) {
    /**
     * Main screen when entering the app
     */
    object Home : RootScreen("home", "Home")
}