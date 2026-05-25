package com.fastphoto.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.fastphoto.app.R
import com.fastphoto.app.data.model.Album
import com.fastphoto.app.data.model.Photo
import com.fastphoto.app.ui.viewmodel.MainPhotoEvent
import com.fastphoto.app.ui.viewmodel.MainPhotoUiState
import com.fastphoto.app.ui.viewmodel.MainPhotoViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotoViewerScreen(
    onNavigateToTrash: () -> Unit,
    viewModel: MainPhotoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAlbumPicker by remember { mutableStateOf(false) }
    var showMovePicker by remember { mutableStateOf<Photo?>(null) }
    var showCommitConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainPhotoEvent.PhotoDeleted -> snackbarHostState.showSnackbar(
                    message = "Photo moved to trash",
                    duration = SnackbarDuration.Short
                )
                is MainPhotoEvent.Error -> snackbarHostState.showSnackbar(
                    message = event.message,
                    duration = SnackbarDuration.Short
                )
                is MainPhotoEvent.Message -> snackbarHostState.showSnackbar(
                    message = event.text,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            when (val state = uiState) {
                is MainPhotoUiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )

                is MainPhotoUiState.Empty -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No photos found",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                is MainPhotoUiState.Success -> PhotoSwipeStack(
                    photos = state.photos,
                    currentAlbum = state.currentAlbum,
                    trashCount = state.trashCount,
                    undoStackSize = viewModel.undoStack.collectAsStateWithLifecycle().value.size,
                    recentAlbums = viewModel.recentAlbums.collectAsStateWithLifecycle().value,
                    onUndo = viewModel::undoLastAction,
                    onDeletePhoto = viewModel::deletePhoto,
                    onRequestMove = { photo -> showMovePicker = photo },
                    onNavigateToTrash = onNavigateToTrash,
                    onShowAlbumPicker = { showAlbumPicker = true },
                    onMoveToRecent = viewModel::movePhotoToFolder,
                    onRequestCommit = { showCommitConfirm = true }
                )

                is MainPhotoUiState.Error -> Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadPhotos() }) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }
        }
    }

    val success = uiState as? MainPhotoUiState.Success
    if (showAlbumPicker && success != null) {
        AlbumPickerSheet(
            title = "Select Folder",
            albums = success.allAlbums,
            currentBucketId = success.currentAlbum?.bucketId,
            onSelect = { album ->
                viewModel.selectAlbum(album.bucketId)
                showAlbumPicker = false
            },
            onShowAll = {
                viewModel.selectAlbum(null)
                showAlbumPicker = false
            },
            onDismiss = { showAlbumPicker = false }
        )
    }

    val moveTarget = showMovePicker
    if (moveTarget != null && success != null) {
        AlbumPickerSheet(
            title = "Select Target Folder",
            albums = success.allAlbums.filter { it.bucketId != moveTarget.bucketId },
            currentBucketId = null,
            onSelect = { album ->
                viewModel.movePhotoToFolder(moveTarget, album.bucketId)
                showMovePicker = null
            },
            onShowAll = null,
            onDismiss = { showMovePicker = null }
        )
    }

    if (showCommitConfirm && success != null && success.trashCount > 0) {
        AlertDialog(
            onDismissRequest = { showCommitConfirm = false },
            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Commit Pending Changes") },
            text = {
                Text(
                    "${success.trashCount} photo(s) are waiting for system approval to be removed from your gallery. " +
                    "Tap Confirm to send a single Android approval dialog for all of them."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCommitConfirm = false
                    viewModel.commitPending()
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCommitConfirm = false
                    onNavigateToTrash()
                }) { Text("Open Trash") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSwipeStack(
    photos: List<Photo>,
    currentAlbum: Album?,
    trashCount: Int,
    undoStackSize: Int,
    recentAlbums: List<Album>,
    onUndo: () -> Unit,
    onDeletePhoto: (Photo) -> Unit,
    onRequestMove: (Photo) -> Unit,
    onNavigateToTrash: () -> Unit,
    onShowAlbumPicker: () -> Unit,
    onMoveToRecent: (Photo, String) -> Unit,
    onRequestCommit: () -> Unit
) {
    var currentIndex by remember(photos.firstOrNull()?.id) { mutableStateOf(0) }
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(photos.size) {
        if (currentIndex >= photos.size) currentIndex = (photos.size - 1).coerceAtLeast(0)
    }

    val current = photos.getOrNull(currentIndex)
    val next = photos.getOrNull(currentIndex + 1)

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenWidthPx = with(density) { config.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }
    val threshold = with(density) { 100.dp.toPx() }

    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        if (next != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(next.uri)
                    .crossfade(false)
                    .build(),
                contentDescription = next.displayName,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val progress = (abs(offsetX.value) / screenWidthPx + abs(offsetY.value) / screenHeightPx)
                            .coerceIn(0f, 1f)
                        scaleX = 0.92f + 0.08f * progress
                        scaleY = 0.92f + 0.08f * progress
                        alpha = 0.4f + 0.6f * progress
                    },
                contentScale = ContentScale.Fit
            )
        }

        if (current != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(current.uri)
                    .crossfade(false)
                    .build(),
                contentDescription = current.displayName,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offsetX.value
                        translationY = offsetY.value
                        rotationZ = (offsetX.value / 40f).coerceIn(-12f, 12f)
                    }
                    .pointerInput(currentIndex, photos.size) {
                        detectTapGestures(onTap = { showControls = !showControls })
                    }
                    .pointerInput(currentIndex, photos.size) {
                        detectDragGestures(
                            onDragEnd = {
                                val dx = offsetX.value
                                val dy = offsetY.value
                                val absDx = abs(dx)
                                val absDy = abs(dy)
                                val photo = current
                                scope.launch {
                                    when {
                                        absDy > absDx && dy < -threshold -> {
                                            offsetY.animateTo(-screenHeightPx * 1.2f, tween(200))
                                            onDeletePhoto(photo)
                                            offsetX.snapTo(0f); offsetY.snapTo(0f)
                                        }
                                        absDy > absDx && dy > threshold -> {
                                            offsetX.animateTo(0f, spring())
                                            offsetY.animateTo(0f, spring())
                                            onRequestMove(photo)
                                        }
                                        absDx > absDy && dx < -threshold && currentIndex < photos.size - 1 -> {
                                            offsetX.animateTo(-screenWidthPx * 1.2f, tween(180))
                                            currentIndex += 1
                                            offsetX.snapTo(0f); offsetY.snapTo(0f)
                                        }
                                        absDx > absDy && dx > threshold && currentIndex > 0 -> {
                                            offsetX.animateTo(screenWidthPx * 1.2f, tween(180))
                                            currentIndex -= 1
                                            offsetX.snapTo(0f); offsetY.snapTo(0f)
                                        }
                                        else -> {
                                            offsetX.animateTo(0f, spring())
                                            offsetY.animateTo(0f, spring())
                                        }
                                    }
                                }
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                scope.launch {
                                    offsetX.snapTo(offsetX.value + drag.x)
                                    offsetY.snapTo(offsetY.value + drag.y)
                                }
                            }
                        )
                    },
                contentScale = ContentScale.Fit
            )

            SwipeHints(offsetX = offsetX.value, offsetY = offsetY.value, threshold = threshold)
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${currentIndex + 1}/${photos.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onShowAlbumPicker() }
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = "Folder",
                            tint = Color.White,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentAlbum?.name ?: "All Photos",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    val pending = trashCount > 0
                    val commitTint = if (pending) Color(0xFFFFC107) else Color(0xFF4CAF50)
                    val commitDesc = if (pending) "Pending: $trashCount" else "All synced"
                    BadgedBox(
                        badge = {
                            if (pending) Badge(containerColor = Color(0xFFFFC107)) {
                                Text(trashCount.toString(), color = Color.Black)
                            }
                        }
                    ) {
                        IconButton(onClick = { if (pending) onRequestCommit() }) {
                            Icon(
                                imageVector = if (pending) Icons.Default.HourglassTop else Icons.Default.CheckCircle,
                                contentDescription = commitDesc,
                                tint = commitTint
                            )
                        }
                    }
                    BadgedBox(
                        badge = {
                            if (trashCount > 0) Badge { Text(trashCount.toString()) }
                        }
                    ) {
                        IconButton(onClick = onNavigateToTrash) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Trash",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            )
        }

        AnimatedVisibility(
            visible = showControls && currentIndex > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            IconButton(
                onClick = {
                    if (currentIndex > 0) currentIndex -= 1
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Previous",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        AnimatedVisibility(
            visible = showControls && currentIndex < photos.size - 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            IconButton(
                onClick = {
                    if (currentIndex < photos.size - 1) currentIndex += 1
                },
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Next",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showControls && current != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 32.dp)
        ) {
            FloatingActionButton(
                onClick = { current?.let(onRequestMove) },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    Icons.Default.DriveFileMove,
                    contentDescription = "Move to Folder",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        AnimatedVisibility(
            visible = showControls && recentAlbums.isNotEmpty(),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(recentAlbums) { album ->
                    SuggestionChip(
                        onClick = { current?.let { onMoveToRecent(it, album.bucketId) } },
                        label = { Text(album.name) },
                        icon = { Icon(Icons.Default.Folder, null) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = undoStackSize > 0 && showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 32.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = onUndo,
                icon = { Icon(Icons.Default.Undo, contentDescription = "Undo") },
                text = { Text("Undo ($undoStackSize)") },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun BoxScope.SwipeHints(offsetX: Float, offsetY: Float, threshold: Float) {
    val absX = abs(offsetX); val absY = abs(offsetY)
    if (absY > absX) {
        if (offsetY < -50f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 100.dp)
                    .alpha((absY / threshold).coerceIn(0f, 1f))
            ) {
                Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(64.dp))
                Text("Delete", color = Color.Red, style = MaterialTheme.typography.titleLarge)
            }
        } else if (offsetY > 50f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
                    .alpha((absY / threshold).coerceIn(0f, 1f))
            ) {
                Icon(Icons.Default.DriveFileMove, null, tint = Color.Cyan, modifier = Modifier.size(64.dp))
                Text("Move to Folder", color = Color.Cyan, style = MaterialTheme.typography.titleLarge)
            }
        }
    } else {
        if (offsetX < -50f) {
            Icon(
                Icons.Default.ArrowForward,
                null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 32.dp)
                    .size(72.dp)
                    .alpha((absX / threshold).coerceIn(0f, 1f))
            )
        } else if (offsetX > 50f) {
            Icon(
                Icons.Default.ArrowBack,
                null,
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 32.dp)
                    .size(72.dp)
                    .alpha((absX / threshold).coerceIn(0f, 1f))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumPickerSheet(
    title: String,
    albums: List<Album>,
    currentBucketId: String?,
    onSelect: (Album) -> Unit,
    onShowAll: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
            LazyColumn {
                if (onShowAll != null) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onShowAll() }
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "All Photos",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (currentBucketId == null) MaterialTheme.colorScheme.primary else Color.Unspecified,
                                modifier = Modifier.weight(1f)
                            )
                            if (currentBucketId == null) {
                                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Divider()
                    }
                }
                items(albums) { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(album) }
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (album.bucketId == currentBucketId) MaterialTheme.colorScheme.primary else Color.Unspecified
                            )
                            Text(
                                text = "${album.photoCount} photos",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (album.bucketId == currentBucketId) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
