package com.fastphoto.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fastphoto.app.data.repository.PendingIntentBus
import com.fastphoto.app.ui.navigation.NavGraph
import com.fastphoto.app.ui.navigation.Screen
import com.fastphoto.app.ui.theme.FastPhotoTheme
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var pendingIntentBus: PendingIntentBus

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FastPhotoTheme {
                FastPhotoApp(pendingIntentBus = pendingIntentBus)
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FastPhotoApp(pendingIntentBus: PendingIntentBus) {
    val navController = rememberNavController()

    val systemActionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { /* result ignored — system handled the action */ }

    LaunchedEffect(pendingIntentBus) {
        pendingIntentBus.pending.collect { intentSender ->
            systemActionLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    val permissionsState = rememberMultiplePermissionsState(permissions)

    if (permissionsState.allPermissionsGranted) {
        NavGraph(
            navController = navController,
            startDestination = Screen.PhotoViewer.route
        )
    } else {
        PermissionRequestScreen(
            onRequestPermission = { permissionsState.launchMultiplePermissionRequest() }
        )
    }
}

@Composable
fun PermissionRequestScreen(
    onRequestPermission: () -> Unit
) {
    Scaffold { paddingValues ->
        Surface(
            modifier = Modifier.padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.permission_required),
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.permission_photos_rationale),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onRequestPermission) {
                    Text(stringResource(R.string.grant_permission))
                }
            }
        }
    }
}
