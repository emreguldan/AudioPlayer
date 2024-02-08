package com.example.audioplayer.ui.audio

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.audioplayer.data.local.model.Audio
import kotlin.math.floor

var audioPlayerVisible = false
@Composable
fun HomeScreen(
    progress: Float,
    onProgress: (Float) -> Unit,
    isAudioPlaying: Boolean,
    currentPlayingAudio: Audio,
    audioList: List<Audio>,
    onStart: () -> Unit,
    onItemClick: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    addedAudioList: MutableList<Audio>
) {
    Scaffold(
        bottomBar = {
            if (audioPlayerVisible) {
                BottomBarPlayer(
                    progress = progress,
                    onProgress = onProgress,
                    audio = currentPlayingAudio,
                    onStart = onStart,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    isAudioPlaying = isAudioPlaying
                )
            } else {null}
        }
    ) {
        val context = LocalContext.current
        LazyColumn(
            contentPadding = it
        ) {
            itemsIndexed(audioList) { index, audio ->
                val alreadyAdded = addedAudioList.contains(audio)
                AudioItem(
                    audio = audio,
                    onItemClick = { onItemClick(index) } ,
                    onAddClick = {
                        if (!alreadyAdded) {
                            addedAudioList.add(audio)
                        } else {
                            Toast.makeText(
                                context,
                                "This audio is already added!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    icon = Icons.Default.Add
                )
            }
        }
    }
}

@Composable
fun MusicListScreen(
    audioList: List<Audio>,
    progress: Float,
    onProgress: (Float) -> Unit,
    isAudioPlaying: Boolean,
    currentPlayingAudio: Audio,
    onStart: () -> Unit,
    onItemClick: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    addedAudioList: MutableList<Audio>,
    viewModel: AudioViewModel

) {
    Scaffold(
        bottomBar = {
            if (audioPlayerVisible) {
                BottomBarPlayer(
                    progress = progress,
                    onProgress = onProgress,
                    audio = currentPlayingAudio,
                    onStart = onStart,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    isAudioPlaying = isAudioPlaying
                )
            } else {null}
        }
    ) {
        LazyColumn(
            contentPadding = it
        ) {
            itemsIndexed(addedAudioList) { _, audio ->
                AudioItem(
                    audio = audio,
                    onItemClick = { onItemClick(audioList.indexOf(audio)) },
                    onAddClick = {
                        addedAudioList.remove(audio);
                        viewModel.clearMediaItems()
                        captureList(addedAudioList)
                        viewModel.loadAudioListData()
                    },
                    icon = Icons.Default.Delete
                )
            }
        }
    }
}

@Composable
fun AudioItem(
    audio: Audio,
    onItemClick: () -> Unit,
    onAddClick: () -> Unit,
    icon: ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clickable {
                onItemClick()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.size(4.dp))
                audio.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleMedium,
                        overflow = TextOverflow.Clip,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.size(4.dp))
                audio.artist?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
            }
            Text(
                text = timeStampToDuration(audio.duration.toLong())
            )
            Spacer(modifier = Modifier.size(8.dp))
            IconButton(onClick = onAddClick) {
                Icon(icon, contentDescription = "Add")
            }
        }
    }
}

@Composable
fun BottomBarPlayer(
    progress: Float,
    onProgress: (Float) -> Unit,
    audio: Audio,
    isAudioPlaying: Boolean,
    onStart: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {

    BottomAppBar(
        modifier = Modifier.fillMaxHeight(0.15f),
        content = {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArtistInfo(
                        audio = audio,
                        modifier = Modifier.weight(1f)
                    )
                    MediaPlayerController(
                        isAudioPlaying, onStart, onNext, onPrevious
                    )
                }
                Slider(
                    value = progress,
                    onValueChange = { onProgress(it) },
                    valueRange = 0f..100f
                )
            }
        }
    )
}

@Composable
fun MediaPlayerController(
    isAudioPlaying: Boolean,
    onStart: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .height(56.dp)
            .padding(4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.SkipPrevious,
            modifier = Modifier.clickable {
                onPrevious()
            },
            contentDescription = null
        )
        Spacer(modifier = Modifier.size(8.dp))
        PlayerIconItem(
            icon = if (isAudioPlaying) Icons.Default.Pause
            else Icons.Default.PlayArrow
        ) {
            onStart()
        }
        Spacer(modifier = Modifier.size(8.dp))
        Icon(
            imageVector = Icons.Default.SkipNext,
            modifier = Modifier.clickable {
                onNext()
            },
            contentDescription = null
        )
    }
}

@Composable
fun ArtistInfo(
    modifier: Modifier = Modifier,
    audio: Audio
) {
    Row(
        modifier = modifier.padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerIconItem(
            icon = Icons.Default.MusicNote,
            borderStroke = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.onSurface
            )
        ) {}
        Spacer(modifier = Modifier.size(4.dp))
        Column {
            audio.title?.let {
                Text(
                    text = it,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.size(4.dp))
            audio.artist?.let {
                Text(
                    text = it,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.bodySmall,
                    overflow = TextOverflow.Clip,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun PlayerIconItem(
    icon: ImageVector,
    borderStroke: BorderStroke? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    color: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        shape = CircleShape,
        border = borderStroke,
        modifier = Modifier
            .clip(CircleShape)
            .clickable {
                onClick()
            },
        contentColor = color,
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier.padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
        }
    }
}

private fun timeStampToDuration(position: Long): String {
    val totalSecond = floor(position / 1E3).toInt()
    val minutes = totalSecond / 60
    val remainingSeconds = totalSecond - (minutes * 60)
    return if (position < 0) "--:--"
    else "%d:%02d".format(minutes, remainingSeconds)
}