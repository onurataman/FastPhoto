package com.fastphoto.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.fastphoto.app.R
import com.fastphoto.app.data.model.Album
import com.fastphoto.app.data.model.Photo
import com.fastphoto.app.ui.viewmodel.MainPhotoEvent
import com.fastphoto.app.ui.viewmodel.MainPhotoUiState
import com.fastphoto.app.ui.viewmodel.MainPhotoViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotoViewerScreen(
    onNavigateToTrash: () -> Unit,
    viewModel: MainPhotoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showFolderPicker by remember { mutableStateOf(false) }

    // Collect events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MainPhotoEvent.PhotoDeleted -> {
                    snackbarHostState.showSnackbar(
                        message = "Fotoğraf çöp kutusuna taşındı",
                        duration = SnackbarDuration.Short
                    )
                }
                is MainPhotoEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            when (val state = uiState) {
                is MainPhotoUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }

                is MainPhotoUiState.Empty -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Fotoğraf bulunamadı",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                is MainPhotoUiState.Success -> {
                    PhotoPagerWithGestures(
                        photos = state.photos,
                        currentAlbum = state.currentAlbum,
                        allAlbums = state.allAlbums,
                        trashCount = state.trashCount,
                        onDeletePhoto = { photo ->
                            scope.launch {
                                viewModel.deletePhoto(photo)
                            }
                        },
                        onNavigateToTrash = onNavigateToTrash,
                        onAlbumSelected = { album ->
                            viewModel.selectAlbum(album.bucketId)
                        },
                        showFolderPicker = showFolderPicker,
                        onShowFolderPicker = { showFolderPicker = it }
                    )
                }

                is MainPhotoUiState.Error -> {
                    Column(
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
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PhotoPagerWithGestures(
    photos: List<Photo>,
    currentAlbum: Album?,
    allAlbums: List<Album>,
    trashCount: Int,
    onDeletePhoto: (Photo) -> Unit,
    onNavigateToTrash: () -> Unit,
    onAlbumSelected: (Album) -> Unit,
    showFolderPicker: Boolean,
    onShowFolderPicker: (Boolean) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { photos.size })
    var showControls by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // Photo Pager with swipe gestures
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            SwipeablePhotoPage(
                photo = photos[page],
                onSwipeUp = {
                    onDeletePhoto(photos[page])
                },
                onTap = {
                    showControls = !showControls
                }
            )
        }

        // Top Bar
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
                            text = "${pagerState.currentPage + 1}/${photos.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onShowFolderPicker(true) }
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = "Klasör",
                            tint = Color.White,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentAlbum?.name ?: "Tüm Fotoğraflar",
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
                    BadgedBox(
                        badge = {
                            if (trashCount > 0) {
                                Badge {
                                    Text(trashCount.toString())
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onNavigateToTrash) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Çöp Kutusu",
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

        // Navigation Arrows
        AnimatedVisibility(
            visible = showControls && photos.size > 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            IconButton(
                onClick = {
                    scope.launch {
                        if (pagerState.currentPage > 0) {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Önceki",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = showControls && photos.size > 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            IconButton(
                onClick = {
                    scope.launch {
                        if (pagerState.currentPage < photos.size - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    Icons.Default.ArrowForward,
                    contentDescription = "Sonraki",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }

    // Folder Picker Dialog
    if (showFolderPicker) {
        FolderPickerDialog(
            albums = allAlbums,
            currentAlbum = currentAlbum,
            onAlbumSelected = { album ->
                onAlbumSelected(album)
                onShowFolderPicker(false)
            },
            onDismiss = { onShowFolderPicker(false) }
        )
    }
}

@Composable
private fun SwipeablePhotoPage(
    photo: Photo,
    onSwipeUp: () -> Unit,
    onTap: () -> Unit
) {
    var offsetY by remember { mutableStateOf(0f) }
    val threshold = 300f // Swipe threshold in pixels

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            ) {
                onTap()
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (offsetY < -threshold) {
                            // Swiped up - delete
                            onSwipeUp()
                        }
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        // Only allow upward swipes
                        if (dragAmount.y < 0) {
                            offsetY += dragAmount.y
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = photo.uri,
            contentDescription = photo.displayName,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = offsetY
                    alpha = 1f - (kotlin.math.abs(offsetY) / threshold).coerceIn(0f, 0.7f)
                },
            contentScale = ContentScale.Fit
        )

        // Swipe indicator
        if (offsetY < -50f) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(64.dp)
                    .alpha((kotlin.math.abs(offsetY) / threshold).coerceIn(0f, 1f))
            )
        }
    }
}

@Composable
private fun FolderPickerDialog(
    albums: List<Album>,
    currentAlbum: Album?,
    onAlbumSelected: (Album) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Klasör Seç") },
        text = {
            Column {
                albums.forEach { album ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAlbumSelected(album) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = album.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (album.bucketId == currentAlbum?.bucketId)
                                    MaterialTheme.colorScheme.primary
                                else
                                    Color.Unspecified
                            )
                            Text(
                                text = "${album.photoCount} fotoğraf",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (album.bucketId == currentAlbum?.bucketId) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Kapat")
            }
        }
    )
}
