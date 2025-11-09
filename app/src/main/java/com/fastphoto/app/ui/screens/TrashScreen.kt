package com.fastphoto.app.ui.screens

import androidx.compose.animation.*
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
import com.fastphoto.app.data.local.entity.TrashedPhoto
import com.fastphoto.app.ui.viewmodel.TrashEvent
import com.fastphoto.app.ui.viewmodel.TrashUiState
import com.fastphoto.app.ui.viewmodel.TrashViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TrashEvent.PhotoRestored -> {
                    snackbarHostState.showSnackbar(
                        message = "Fotoğraf geri yüklendi",
                        duration = SnackbarDuration.Short
                    )
                }
                is TrashEvent.PhotoDeletedPermanently -> {
                    snackbarHostState.showSnackbar(
                        message = "Fotoğraf kalıcı olarak silindi",
                        duration = SnackbarDuration.Short
                    )
                }
                is TrashEvent.TrashEmptied -> {
                    snackbarHostState.showSnackbar(
                        message = "Çöp kutusu boşaltıldı",
                        duration = SnackbarDuration.Short
                    )
                }
                is TrashEvent.Error -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        duration = SnackbarDuration.Long
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
                is TrashUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }

                is TrashUiState.Empty -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.trash_empty),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = onNavigateBack) {
                            Text("Ana Ekrana Dön")
                        }
                    }
                }

                is TrashUiState.Success -> {
                    TrashPagerWithGestures(
                        photos = state.photos,
                        onRestorePhoto = viewModel::restorePhoto,
                        onDeletePermanently = viewModel::deletePermanently,
                        onNavigateBack = onNavigateBack
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TrashPagerWithGestures(
    photos: List<TrashedPhoto>,
    onRestorePhoto: (TrashedPhoto) -> Unit,
    onDeletePermanently: (TrashedPhoto) -> Unit,
    onNavigateBack: () -> Unit
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
            SwipeableTrashPhotoPage(
                photo = photos[page],
                onSwipeUp = {
                    onDeletePermanently(photos[page])
                },
                onSwipeDown = {
                    onRestorePhoto(photos[page])
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
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Text(
                        text = "Çöp Kutusu",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(end = 16.dp)
                    )
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

        // Gesture hint overlay
        if (showControls) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
                    .background(
                        Color.Black.copy(alpha = 0.7f),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Yukarı: Kalıcı Sil",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Aşağı: Geri Yükle",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeableTrashPhotoPage(
    photo: TrashedPhoto,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
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
                            // Swiped up - delete permanently
                            onSwipeUp()
                        } else if (offsetY > threshold) {
                            // Swiped down - restore
                            onSwipeDown()
                        }
                        offsetY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetY += dragAmount.y
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = File(photo.trashFilePath),
            contentDescription = photo.displayName,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = offsetY
                    alpha = 1f - (kotlin.math.abs(offsetY) / threshold).coerceIn(0f, 0.7f)
                },
            contentScale = ContentScale.Fit
        )

        // Swipe indicator - up for delete
        if (offsetY < -50f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.DeleteForever,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier
                        .size(64.dp)
                        .alpha((kotlin.math.abs(offsetY) / threshold).coerceIn(0f, 1f))
                )
                Text(
                    "Kalıcı Sil",
                    color = Color.Red,
                    modifier = Modifier.alpha((kotlin.math.abs(offsetY) / threshold).coerceIn(0f, 1f))
                )
            }
        }

        // Swipe indicator - down for restore
        if (offsetY > 50f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.RestoreFromTrash,
                    contentDescription = null,
                    tint = Color.Green,
                    modifier = Modifier
                        .size(64.dp)
                        .alpha((offsetY / threshold).coerceIn(0f, 1f))
                )
                Text(
                    "Geri Yükle",
                    color = Color.Green,
                    modifier = Modifier.alpha((offsetY / threshold).coerceIn(0f, 1f))
                )
            }
        }
    }
}
