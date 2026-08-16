package com.exposures.phone.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.exposures.phone.ui.camerabody.CameraBodyEditScreen
import com.exposures.phone.ui.camerabody.CameraBodyListScreen
import com.exposures.phone.ui.filmroll.FilmRollEditScreen
import com.exposures.phone.ui.filmroll.FilmRollListScreen
import com.exposures.phone.ui.home.HomeScreen
import com.exposures.phone.ui.lens.LensEditScreen
import com.exposures.phone.ui.lens.LensListScreen

@Composable
fun ExposuresNavHost() {
    val navController = rememberNavController()
    val idArgument = navArgument(Routes.ARG_ID) {
        type = NavType.StringType
        nullable = true
        defaultValue = null
    }

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenCameraBodies = { navController.navigate(Routes.CAMERA_BODY_LIST) },
                onOpenLenses = { navController.navigate(Routes.LENS_LIST) },
                onOpenFilmRolls = { navController.navigate(Routes.FILM_ROLL_LIST) },
            )
        }

        composable(Routes.CAMERA_BODY_LIST) {
            CameraBodyListScreen(
                onAdd = { navController.navigate(Routes.cameraBodyEdit()) },
                onEdit = { id -> navController.navigate(Routes.cameraBodyEdit(id)) },
            )
        }
        composable(Routes.CAMERA_BODY_EDIT, arguments = listOf(idArgument)) { backStackEntry ->
            CameraBodyEditScreen(
                id = backStackEntry.arguments?.getString(Routes.ARG_ID),
                onDone = { navController.popBackStack() },
            )
        }

        composable(Routes.LENS_LIST) {
            LensListScreen(
                onAdd = { navController.navigate(Routes.lensEdit()) },
                onEdit = { id -> navController.navigate(Routes.lensEdit(id)) },
            )
        }
        composable(Routes.LENS_EDIT, arguments = listOf(idArgument)) { backStackEntry ->
            LensEditScreen(
                id = backStackEntry.arguments?.getString(Routes.ARG_ID),
                onDone = { navController.popBackStack() },
            )
        }

        composable(Routes.FILM_ROLL_LIST) {
            FilmRollListScreen(
                onAdd = { navController.navigate(Routes.filmRollEdit()) },
                onEdit = { id -> navController.navigate(Routes.filmRollEdit(id)) },
            )
        }
        composable(Routes.FILM_ROLL_EDIT, arguments = listOf(idArgument)) { backStackEntry ->
            FilmRollEditScreen(
                id = backStackEntry.arguments?.getString(Routes.ARG_ID),
                onDone = { navController.popBackStack() },
            )
        }
    }
}
