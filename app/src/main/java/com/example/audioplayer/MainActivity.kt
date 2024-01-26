package com.example.audioplayer

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.audioplayer.data.local.model.Audio
import com.example.audioplayer.player.service.AudioService
import com.example.audioplayer.ui.Screen
import com.example.audioplayer.ui.audio.AudioViewModel
import com.example.audioplayer.ui.audio.HomeScreen
import com.example.audioplayer.ui.audio.MusicListScreen
import com.example.audioplayer.ui.audio.UIEvents
import com.example.audioplayer.ui.items
import com.example.audioplayer.ui.theme.AudioPlayerTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: AudioViewModel by viewModels()
    private var isServiceRunning = false
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioPlayerTheme {
                val permissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    rememberPermissionState(
                        permission = Manifest.permission.READ_MEDIA_AUDIO
                    )
                } else {
                    rememberPermissionState(
                        permission = Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                }
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(key1 = lifecycleOwner){
                    val observer = LifecycleEventObserver{ _, event->
                        if (event == Lifecycle.Event.ON_RESUME){
                            permissionState.launchPermissionRequest()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose{
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }
                @Composable
                fun ScreenNavigation() {
                    val musicList = remember { mutableStateListOf<Audio>()}
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
                                        selected = currentDestination?.hierarchy?.any { it.route == screen.route} == true,
                                        onClick = {
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
                        NavHost(navController, startDestination = Screen.Home.route, Modifier.padding(innerPadding)) {
                            composable(Screen.Home.route) { HomeScreen(
                                progress = viewModel.progress,
                                onProgress = {viewModel.onUiEvents(UIEvents.SeekTo(it))},
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
                            ) }
                            composable(Screen.List.route) { MusicListScreen(
                                progress = viewModel.progress,
                                onProgress = {viewModel.onUiEvents(UIEvents.SeekTo(it))},
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
                            )}
                        }
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScreenNavigation()
                }
            }

        }
    }

    private fun startService(){
        if (!isServiceRunning){
            val intent = Intent(this, AudioService::class.java)
            startForegroundService(intent)
        }else{
            startService(intent)
        }
        isServiceRunning = true
    }
}


