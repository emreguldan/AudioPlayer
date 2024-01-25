package com.example.audioplayer.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home_route", "Home", Icons.Default.Search)
    data object List : Screen("list_route", "Music List", Icons.Default.List)
}

val items = listOf(
    Screen.Home,
    Screen.List
)

