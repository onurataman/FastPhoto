package com.fastphoto.app.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastphoto.app.data.model.Album
import com.fastphoto.app.data.model.Photo
import com.fastphoto.app.data.repository.MediaRepository
import com.fastphoto.app.data.repository.TrashRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main ViewModel for photo viewer
 */
@HiltViewModel
class MainPhotoViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val trashRepository: TrashRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<MainPhotoUiState>(MainPhotoUiState.Loading)
    val uiState: StateFlow<MainPhotoUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MainPhotoEvent>()
    val events: SharedFlow<MainPhotoEvent> = _events.asSharedFlow()

    private val _undoStack = MutableStateFlow<List<Photo>>(emptyList())
    val undoStack: StateFlow<List<Photo>> = _undoStack.asStateFlow()

    private var currentBucketId: String? = null
    private var allAlbums: List<Album> = emptyList()
    private var trashCount: Int = 0

    init {
        val bucketId: String? = savedStateHandle.get<String>("bucketId")
        currentBucketId = bucketId
        loadData()
        observeTrashCount()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = MainPhotoUiState.Loading

            // Load albums
            mediaRepository.loadAlbums()
                .onSuccess { albums ->
                    allAlbums = albums
                    loadPhotos()
                }
                .onFailure { error ->
                    _uiState.value = MainPhotoUiState.Error(error.message ?: "Failed to load albums")
                }
        }
    }

    fun loadPhotos() {
        viewModelScope.launch {
            val result = if (currentBucketId != null) {
                mediaRepository.loadPhotosFromAlbum(currentBucketId!!)
            } else {
                mediaRepository.loadAllPhotos()
            }

            result.onSuccess { photos ->
                val currentAlbum = allAlbums.find { it.bucketId == currentBucketId }
                _uiState.value = if (photos.isEmpty()) {
                    MainPhotoUiState.Empty
                } else {
                    MainPhotoUiState.Success(
                        photos = photos,
                        currentAlbum = currentAlbum,
                        allAlbums = allAlbums,
                        trashCount = trashCount
                    )
                }
            }.onFailure { error ->
                _uiState.value = MainPhotoUiState.Error(error.message ?: "Failed to load photos")
            }
        }
    }

    fun selectAlbum(bucketId: String?) {
        currentBucketId = bucketId
        loadPhotos()
    }

    fun deletePhoto(photo: Photo) {
        viewModelScope.launch {
            trashRepository.moveToTrash(photo)
                .onSuccess {
                    val currentStack = _undoStack.value.toMutableList()
                    currentStack.add(photo)
                    if (currentStack.size > 10) currentStack.removeAt(0)
                    _undoStack.value = currentStack
                    
                    _events.emit(MainPhotoEvent.PhotoDeleted)
                    loadPhotos() // Reload to update the list
                }
                .onFailure { error ->
                    _events.emit(MainPhotoEvent.Error(error.message ?: "Failed to delete photo"))
                }
        }
    }

    fun undoLastAction() {
        viewModelScope.launch {
            val stack = _undoStack.value
            if (stack.isNotEmpty()) {
                val photoToRestore = stack.last()
                
                // Arka planda veritabanından çöp kutusundaki halini bul ve orijinal yerine iade et
                val trashedList = trashRepository.getTrashedPhotos().firstOrNull() ?: emptyList()
                val trashedPhoto = trashedList.find { it.originalPhotoId == photoToRestore.id }
                
                if (trashedPhoto != null) {
                    trashRepository.restorePhoto(trashedPhoto)
                }

                _events.emit(MainPhotoEvent.Message("${photoToRestore.displayName} Geri Alındı!"))
                _undoStack.value = stack.dropLast(1)
                loadPhotos()
            }
        }
    }

    fun movePhotoToFolder(photo: Photo, targetBucketId: String?) {
        viewModelScope.launch {
            val targetAlbum = allAlbums.find { it.bucketId == targetBucketId }
            if (targetAlbum == null) {
                _events.emit(MainPhotoEvent.Error("Hedef klasör bulunamadı!"))
                return@launch
            }

            mediaRepository.movePhotoToAlbum(photo, targetAlbum.name)
                .onSuccess {
                    val currentStack = _undoStack.value.toMutableList()
                    currentStack.add(photo)
                    if (currentStack.size > 10) currentStack.removeAt(0)
                    _undoStack.value = currentStack
                    
                    _events.emit(MainPhotoEvent.Message("${photo.displayName} 📁 ${targetAlbum.name} klasörüne taşındı!"))
                    loadPhotos() // Ekranı güncelle
                }
                .onFailure { error ->
                    _events.emit(MainPhotoEvent.Error(error.message ?: "Taşıma başarısız oldu."))
                }
        }
    }

    private fun observeTrashCount() {
        viewModelScope.launch {
            trashRepository.getTrashedPhotos().collect { trashedPhotos ->
                trashCount = trashedPhotos.size
                // Update UI state with new trash count
                val currentState = _uiState.value
                if (currentState is MainPhotoUiState.Success) {
                    _uiState.value = currentState.copy(trashCount = trashCount)
                }
            }
        }
    }
}

sealed interface MainPhotoUiState {
    object Loading : MainPhotoUiState
    object Empty : MainPhotoUiState
    data class Success(
        val photos: List<Photo>,
        val currentAlbum: Album?,
        val allAlbums: List<Album>,
        val trashCount: Int
    ) : MainPhotoUiState
    data class Error(val message: String) : MainPhotoUiState
}

sealed interface MainPhotoEvent {
    object PhotoDeleted : MainPhotoEvent
    data class Error(val message: String) : MainPhotoEvent
    data class Message(val text: String) : MainPhotoEvent
}
