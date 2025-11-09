package com.fastphoto.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastphoto.app.data.model.Photo
import com.fastphoto.app.data.repository.MediaRepository
import com.fastphoto.app.data.repository.TrashRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Photo viewer screen
 */
@HiltViewModel
class PhotoViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val trashRepository: TrashRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val bucketId: String = savedStateHandle.get<String>("bucketId") ?: ""

    private val _uiState = MutableStateFlow<PhotoUiState>(PhotoUiState.Loading)
    val uiState: StateFlow<PhotoUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<PhotoEvent>()
    val events: SharedFlow<PhotoEvent> = _events.asSharedFlow()

    init {
        loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch {
            _uiState.value = PhotoUiState.Loading

            if (bucketId.isEmpty()) {
                mediaRepository.loadAllPhotos()
            } else {
                mediaRepository.loadPhotosFromAlbum(bucketId)
            }.onSuccess { photos ->
                _uiState.value = if (photos.isEmpty()) {
                    PhotoUiState.Empty
                } else {
                    PhotoUiState.Success(photos)
                }
            }.onFailure { error ->
                _uiState.value = PhotoUiState.Error(error.message ?: "Unknown error")
            }
        }
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch {
            trashRepository.moveToTrash(photo)
                .onSuccess {
                    _events.emit(PhotoEvent.PhotoDeleted)
                    // Reload photos to update the list
                    loadPhotos()
                }
                .onFailure { error ->
                    _events.emit(PhotoEvent.Error(error.message ?: "Failed to delete photo"))
                }
        }
    }
}

sealed interface PhotoUiState {
    object Loading : PhotoUiState
    object Empty : PhotoUiState
    data class Success(val photos: List<Photo>) : PhotoUiState
    data class Error(val message: String) : PhotoUiState
}

sealed interface PhotoEvent {
    object PhotoDeleted : PhotoEvent
    data class Error(val message: String) : PhotoEvent
}
