package com.fastphoto.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastphoto.app.data.local.entity.TrashedPhoto
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
 * ViewModel for Trash screen
 */
@HiltViewModel
class TrashViewModel @Inject constructor(
    private val trashRepository: TrashRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<TrashUiState>(TrashUiState.Loading)
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<TrashEvent>()
    val events: SharedFlow<TrashEvent> = _events.asSharedFlow()

    init {
        loadTrashedPhotos()
    }

    private fun loadTrashedPhotos() {
        viewModelScope.launch {
            trashRepository.getTrashedPhotos().collect { photos ->
                _uiState.value = if (photos.isEmpty()) {
                    TrashUiState.Empty
                } else {
                    TrashUiState.Success(photos)
                }
            }
        }
    }

    fun restorePhoto(photo: TrashedPhoto) {
        viewModelScope.launch {
            trashRepository.restorePhoto(photo)
                .onSuccess {
                    _events.emit(TrashEvent.PhotoRestored)
                }
                .onFailure { error ->
                    _events.emit(TrashEvent.Error(error.message ?: "Failed to restore photo"))
                }
        }
    }

    fun deletePermanently(photo: TrashedPhoto) {
        viewModelScope.launch {
            trashRepository.deletePermanently(photo)
                .onSuccess {
                    _events.emit(TrashEvent.PhotoDeletedPermanently)
                }
                .onFailure { error ->
                    _events.emit(TrashEvent.Error(error.message ?: "Failed to delete photo"))
                }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            trashRepository.emptyTrash()
                .onSuccess {
                    _events.emit(TrashEvent.TrashEmptied)
                }
                .onFailure { error ->
                    _events.emit(TrashEvent.Error(error.message ?: "Failed to empty trash"))
                }
        }
    }
}

sealed interface TrashUiState {
    object Loading : TrashUiState
    object Empty : TrashUiState
    data class Success(val photos: List<TrashedPhoto>) : TrashUiState
}

sealed interface TrashEvent {
    object PhotoRestored : TrashEvent
    object PhotoDeletedPermanently : TrashEvent
    object TrashEmptied : TrashEvent
    data class Error(val message: String) : TrashEvent
}
