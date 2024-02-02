package com.example.audioplayer.ui

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.audioplayer.data.local.model.Audio
import com.example.audioplayer.ui.audio.AudioViewModel
import com.example.audioplayer.ui.audio.HomeScreen
import com.example.audioplayer.ui.audio.MusicListScreen
import com.example.audioplayer.ui.audio.UIEvents
import com.example.audioplayer.ui.audio.captureList

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Home : Screen("home_route", "Find Music", Icons.Default.Search)
    data object List : Screen("list_route", "Music List", Icons.Default.List)
}

val items = listOf(
    Screen.Home,
    Screen.List
)

@Composable
fun ScreenNavigation(
    viewModel: AudioViewModel,
    startService: () -> Unit
) {
    val musicList = remember { mutableStateListOf<Audio>() }

    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    BottomNavigationItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            if (screen.title == "Music List"){
                                viewModel.clearMediaItems()
                                captureList(musicList)
                                viewModel.loadAudioListData()
                            }else if (screen.title == "Find Music") {
                                viewModel.clearMediaItems()
                                viewModel.loadAudioData()
                            }

                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController,
            startDestination = Screen.Home.route,
            Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {

                HomeScreen(
                    progress = viewModel.progress,
                    onProgress = { viewModel.onUiEvents(UIEvents.SeekTo(it)) },
                    isAudioPlaying = viewModel.isPlaying,
                    audioList = viewModel.audioList,
                    currentPlayingAudio = viewModel.currentSelectedAudio,
                    onStart = {
                        viewModel.onUiEvents(UIEvents.PlayPause)
                    },
                    onItemClick = {
                        viewModel.onUiEvents(UIEvents.SelectedAudioChange(it))
                        startService()
                    },
                    onNext = {
                        viewModel.onUiEvents(UIEvents.SeekToNext)
                    },
                    onPrevious = {
                        viewModel.onUiEvents(UIEvents.SeekToPrevious)
                    },
                    addedAudioList = musicList
                )
            }
            composable(Screen.List.route) {

                MusicListScreen(
                    progress = viewModel.progress,
                    onProgress = { viewModel.onUiEvents(UIEvents.SeekTo(it)) },
                    isAudioPlaying = viewModel.isPlaying,
                    currentPlayingAudio = viewModel.currentSelectedAudio,
                    onStart = {
                        viewModel.onUiEvents(UIEvents.PlayPause)
                    },
                    onItemClick = {
                        viewModel.onUiEvents(UIEvents.SelectedAudioChange(it))
                        startService()
                    },
                    onNext = {
                        viewModel.onUiEvents(UIEvents.SeekToNext)
                    },
                    onPrevious = {
                        viewModel.onUiEvents(UIEvents.SeekToPrevious)
                    },
                    addedAudioList = musicList,
                    audioList = viewModel.audioList,
                    viewModel = viewModel
                )

            }
        }
    }
}
