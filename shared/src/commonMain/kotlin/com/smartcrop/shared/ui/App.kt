package com.smartcrop.shared.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.smartcrop.shared.ui.detail.DetailScreen
import com.smartcrop.shared.ui.home.HomeScreen

@Composable
fun App() {
    MaterialTheme {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = HomeRoute) {
            composable<HomeRoute> {
                HomeScreen(
                    onCharacterClick = { id ->
                        navController.navigate(DetailRoute(characterId = id))
                    }
                )
            }
            composable<DetailRoute> { backStackEntry ->
                val route = backStackEntry.toRoute<DetailRoute>()
                DetailScreen(
                    characterId = route.characterId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
