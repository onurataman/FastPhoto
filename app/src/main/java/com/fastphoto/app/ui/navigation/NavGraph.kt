package com.fastphoto.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.fastphoto.app.ui.screens.AlbumListScreen
import com.fastphoto.app.ui.screens.PhotoViewerScreen
import com.fastphoto.app.ui.screens.TrashScreen

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object Albums : Screen("albums")
    object Photos : Screen("photos/{bucketId}") {
        fun createRoute(bucketId: String) = "photos/$bucketId"
    }
    object Trash : Screen("trash")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Albums.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Albums screen
        composable(Screen.Albums.route) {
            AlbumListScreen(
                onAlbumClick = { album ->
                    navController.navigate(Screen.Photos.createRoute(album.bucketId))
                }
            )
        }

        // Photo viewer screen
        composable(
            route = Screen.Photos.route,
            arguments = listOf(
                navArgument("bucketId") {
                    type = NavType.StringType
                }
            )
        ) {
            PhotoViewerScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Trash screen
        composable(Screen.Trash.route) {
            TrashScreen()
        }
    }
}
