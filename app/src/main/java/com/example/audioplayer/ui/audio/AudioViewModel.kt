package com.example.audioplayer.ui.audio

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi
import androidx.lifecycle.viewmodel.compose.saveable
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.audioplayer.data.local.model.Audio
import com.example.audioplayer.data.repository.AudioRepository
import com.example.audioplayer.player.service.AudioServiceHandler
import com.example.audioplayer.player.service.AudioState
import com.example.audioplayer.player.service.PlayerEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject


val audioDummy = Audio(
    "".toUri(),"",0L,"","",0,""
)

var newList = mutableListOf<Audio>()

fun captureList(
    musicList: MutableList<Audio>
) {
    newList = musicList
}

@OptIn(SavedStateHandleSaveableApi::class)
@HiltViewModel
class AudioViewModel @Inject constructor(
    private val audioServiceHandler: AudioServiceHandler,
    private val repository: AudioRepository,
    savedStateHandle: SavedStateHandle
):ViewModel() {
    private var duration by savedStateHandle.saveable { mutableLongStateOf(0L) }
    var progress by savedStateHandle.saveable { mutableFloatStateOf(0f) }
    private var progressString by savedStateHandle.saveable { mutableStateOf("00:00")}
    var isPlaying by savedStateHandle.saveable { mutableStateOf(false)}
    var currentSelectedAudio by savedStateHandle.saveable { mutableStateOf(audioDummy)}
    var audioList by savedStateHandle.saveable { mutableStateOf(listOf<Audio>())}

    private val _uiState : MutableStateFlow<UIState> = MutableStateFlow(UIState.Initial)


    //val uiState: StateFlow<UIState> = _uiState.asStateFlow()

    init {
        loadAudioData()

    }

    init {
        viewModelScope.launch {
            audioServiceHandler.audioState.collectLatest { mediaState ->
                when(mediaState){
                    AudioState.Initial -> _uiState.value = UIState.Initial
                    is AudioState.Buffering -> calculateProgressValue(mediaState.progress)
                    is AudioState.Playing -> isPlaying = mediaState.isPlaying
                    is AudioState.Progress -> calculateProgressValue(mediaState.progress)
                    is AudioState.CurrentPlaying -> {
                        currentSelectedAudio = audioList[mediaState.mediaItemIndex]
                    }
                    is AudioState.Ready -> {
                        duration = mediaState.duration
                        _uiState.value = UIState.Ready
                    }
                }
            }
        }
    }
    fun loadAudioData(){
        viewModelScope.launch {
            val audio = repository.getAudioData()
            audioList = audio
            setMediaItems()
        }
    }



    fun loadAudioListData(){
        viewModelScope.launch {
            val audio = newList
            audioList = audio
            setMediaItems()
        }
    }

    fun clearMediaItems(){
        audioServiceHandler.clearMediaItemList()
    }

    fun stopMedia(){
        audioServiceHandler.stopMedia()
    }

    private fun setMediaItems(){
        audioList.map{audio ->
            MediaItem.Builder()
                .setUri(audio.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setAlbumArtist(audio.artist)
                        .setDisplayTitle(audio.title)
                        .setSubtitle(audio.displayName)
                        .build()
                )
                .build()
        }.also {
            audioServiceHandler.setMediaItemList(it)
        }
    }

    private fun calculateProgressValue(currentProgress:Long){
        progress =
            if (currentProgress > 0) ((currentProgress.toFloat() / duration.toFloat()) * 100f)
            else 0f
        progressString = formatDuration(currentProgress)
    }

    fun onUiEvents(uiEvents: UIEvents) = viewModelScope.launch {
        when(uiEvents){
            UIEvents.BackWard -> audioServiceHandler.onPlayerEvents(PlayerEvent.BackWard)
            UIEvents.Forward -> audioServiceHandler.onPlayerEvents(PlayerEvent.Forward)
            UIEvents.SeekToNext -> audioServiceHandler.onPlayerEvents(PlayerEvent.SeekToNext)
            UIEvents.SeekToPrevious -> audioServiceHandler.onPlayerEvents(PlayerEvent.SeekToPrevious)

            is UIEvents.PlayPause -> {
                audioServiceHandler.onPlayerEvents(
                    PlayerEvent.PlayPause
                )
            }
            is UIEvents.SeekTo -> {
                audioServiceHandler.onPlayerEvents(
                    PlayerEvent.SeekTo,
                    seekPosition = ((duration * uiEvents.position) / 100f).toLong()
                )
            }
            is UIEvents.SelectedAudioChange -> {
                audioServiceHandler.onPlayerEvents(
                    PlayerEvent.SelectedAudioChange,
                    selectedAudioIndex = uiEvents.index
                )
            }
            is UIEvents.UpdateProgress -> {
                audioServiceHandler.onPlayerEvents(
                    PlayerEvent.UpdateProgress(
                        uiEvents.newProgress
                    )
                )
                progress = uiEvents.newProgress
            }
        }
    }

    private fun formatDuration(duration:Long):String{
        val minute = TimeUnit.MINUTES.convert(duration, TimeUnit.MILLISECONDS)
        val seconds = (minute) - minute * TimeUnit.SECONDS.convert(1, TimeUnit.MINUTES)
        return  String.format("%02d:%02d", minute, seconds)
    }

    override fun onCleared() {
        viewModelScope.launch {
            audioServiceHandler.onPlayerEvents(PlayerEvent.Stop)
        }
        super.onCleared()
    }
}

sealed class UIEvents{
    data object PlayPause:UIEvents()
    data class SelectedAudioChange(val index:Int):UIEvents()
    data class SeekTo(val position:Float):UIEvents()
    data object SeekToNext : UIEvents()
    data object SeekToPrevious:UIEvents()
    data object BackWard:UIEvents()
    data object Forward:UIEvents()
    data class UpdateProgress(val newProgress:Float):UIEvents()
}

sealed class UIState{
    data object Initial:UIState()
    data object Ready:UIState()
}

