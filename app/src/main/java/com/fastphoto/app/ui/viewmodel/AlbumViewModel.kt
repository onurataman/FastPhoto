package com.fastphoto.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastphoto.app.data.model.Album
import com.fastphoto.app.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Album list screen
 */
@HiltViewModel
class AlbumViewModel @Inject constructor(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    val uiState: StateFlow<AlbumUiState> = _uiState.asStateFlow()

    init {
        loadAlbums()
    }

    fun loadAlbums() {
        viewModelScope.launch {
            _uiState.value = AlbumUiState.Loading

            mediaRepository.loadAlbums()
                .onSuccess { albums ->
                    _uiState.value = if (albums.isEmpty()) {
                        AlbumUiState.Empty
                    } else {
                        AlbumUiState.Success(albums)
                    }
                }
                .onFailure { error ->
                    _uiState.value = AlbumUiState.Error(error.message ?: "Unknown error")
                }
        }
    }
}

sealed interface AlbumUiState {
    object Loading : AlbumUiState
    object Empty : AlbumUiState
    data class Success(val albums: List<Album>) : AlbumUiState
    data class Error(val message: String) : AlbumUiState
}
