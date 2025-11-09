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
    object PhotoViewer : Screen("photoViewer?bucketId={bucketId}") {
        fun createRoute(bucketId: String? = null) =
            if (bucketId != null) "photoViewer?bucketId=$bucketId"
            else "photoViewer"
    }
    object Trash : Screen("trash")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.PhotoViewer.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Main Photo viewer screen
        composable(
            route = Screen.PhotoViewer.route,
            arguments = listOf(
                navArgument("bucketId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            PhotoViewerScreen(
                onNavigateToTrash = {
                    navController.navigate(Screen.Trash.route)
                }
            )
        }

        // Trash screen
        composable(Screen.Trash.route) {
            TrashScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
