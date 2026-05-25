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

    private val _recentAlbums = MutableStateFlow<List<Album>>(emptyList())
    val recentAlbums: StateFlow<List<Album>> = _recentAlbums.asStateFlow()

    private var currentBucketId: String? = null
    private var allAlbums: List<Album> = emptyList()
    private var trashedIds: Set<Long> = emptySet()
    private var trashCount: Int = 0
    private var rawPhotos: List<Photo> = emptyList()

    init {
        val bucketId: String? = savedStateHandle.get<String>("bucketId")
        currentBucketId = bucketId
        loadData()
        observeTrashState()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = MainPhotoUiState.Loading
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
                rawPhotos = photos
                publishSuccessState()
            }.onFailure { error ->
                _uiState.value = MainPhotoUiState.Error(error.message ?: "Failed to load photos")
            }
        }
    }

    private fun publishSuccessState() {
        val visible = rawPhotos.filter { it.id !in trashedIds }
        val currentAlbum = allAlbums.find { it.bucketId == currentBucketId }
        _uiState.value = if (visible.isEmpty()) {
            MainPhotoUiState.Empty
        } else {
            MainPhotoUiState.Success(
                photos = visible,
                currentAlbum = currentAlbum,
                allAlbums = allAlbums,
                trashCount = trashCount
            )
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
                    // Trash flow collect tetiklenince publishSuccessState çağrılır.
                }
                .onFailure { error ->
                    _events.emit(MainPhotoEvent.Error(error.message ?: "Failed to delete photo"))
                }
        }
    }

    fun undoLastAction() {
        viewModelScope.launch {
            val stack = _undoStack.value
            if (stack.isEmpty()) return@launch

            val photoToRestore = stack.last()
            val trashedList = trashRepository.getTrashedPhotos().first()
            val trashedPhoto = trashedList.find { it.originalPhotoId == photoToRestore.id }

            if (trashedPhoto == null) {
                _events.emit(MainPhotoEvent.Error("Could not restore ${photoToRestore.displayName} (Not found in trash)"))
                _undoStack.value = stack.dropLast(1)
                return@launch
            }

            trashRepository.restorePhoto(trashedPhoto)
                .onSuccess {
                    _events.emit(MainPhotoEvent.Message("${photoToRestore.displayName} restored!"))
                    _undoStack.value = stack.dropLast(1)
                }
                .onFailure { error ->
                    _events.emit(MainPhotoEvent.Error(error.message ?: "Restore failed"))
                }
        }
    }

    fun movePhotoToFolder(photo: Photo, targetBucketId: String?) {
        viewModelScope.launch {
            val targetAlbum = allAlbums.find { it.bucketId == targetBucketId }
            if (targetAlbum == null) {
                _events.emit(MainPhotoEvent.Error("Target folder not found!"))
                return@launch
            }
            if (targetAlbum.bucketId == photo.bucketId) {
                _events.emit(MainPhotoEvent.Error("Photo is already in ${targetAlbum.name}."))
                return@launch
            }

            val targetPath = targetAlbum.relativePath.ifBlank { "DCIM/${targetAlbum.name}/" }
            mediaRepository.copyPhotoToAlbum(photo, targetPath)
                .onSuccess {
                    // Original'i app içinden gizle (trash'e at). Sistemden silme
                    // toplu olarak çöp kutusundan tetiklenir.
                    trashRepository.moveToTrash(photo)
                        .onSuccess {
                            val currentStack = _undoStack.value.toMutableList()
                            currentStack.add(photo)
                            if (currentStack.size > 10) currentStack.removeAt(0)
                            _undoStack.value = currentStack

                            val currentRecents = _recentAlbums.value.toMutableList()
                            currentRecents.removeAll { it.bucketId == targetAlbum.bucketId }
                            currentRecents.add(0, targetAlbum)
                            if (currentRecents.size > 5) currentRecents.removeLast()
                            _recentAlbums.value = currentRecents

                            _events.emit(MainPhotoEvent.Message("📁 Moved to ${targetAlbum.name}"))
                        }
                        .onFailure { error ->
                            _events.emit(MainPhotoEvent.Error("Copied but could not hide original: ${error.message}"))
                        }
                }
                .onFailure { error ->
                    _events.emit(MainPhotoEvent.Error(error.message ?: "Move failed."))
                }
        }
    }

    fun commitPending() {
        viewModelScope.launch {
            val trashed = trashRepository.getTrashedPhotos().first()
            if (trashed.isEmpty()) return@launch
            trashRepository.bulkDeleteFromSystem(trashed)
                .onSuccess {
                    _events.emit(MainPhotoEvent.Message("Sent ${trashed.size} photo(s) for system approval"))
                }
                .onFailure { error ->
                    _events.emit(MainPhotoEvent.Error(error.message ?: "Commit failed"))
                }
        }
    }

    private fun observeTrashState() {
        viewModelScope.launch {
            trashRepository.getTrashedPhotos().collect { trashedPhotos ->
                trashCount = trashedPhotos.size
                trashedIds = trashedPhotos.map { it.originalPhotoId }.toSet()
                if (_uiState.value is MainPhotoUiState.Success || _uiState.value is MainPhotoUiState.Empty) {
                    publishSuccessState()
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
