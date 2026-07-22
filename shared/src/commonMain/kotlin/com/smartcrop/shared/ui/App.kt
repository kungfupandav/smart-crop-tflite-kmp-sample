package com.smartcrop.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.smartcrop.shared.di.AppGraph
import com.smartcrop.shared.ui.detail.DetailScreen
import com.smartcrop.shared.ui.home.HomeScreen
import com.smartcrop.shared.ui.theme.SmartCropTheme
import dev.zacsweers.metro.createGraph

/**
 * Provides the application [AppGraph] to the composition. Screens read their
 * dependencies from `LocalAppGraph.current` instead of constructing services
 * directly.
 */
val LocalAppGraph = staticCompositionLocalOf<AppGraph> {
    error("AppGraph not provided — wrap content in App()")
}

@Composable
fun App() {
    // Build the DI graph once for the app's lifetime.
    val appGraph = remember { createGraph<AppGraph>() }

    // Wire Coil's singleton image loader with a Ktor-backed network fetcher so
    // https:// character images load on both Android and iOS.
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .build()
    }

    CompositionLocalProvider(LocalAppGraph provides appGraph) {
        SmartCropTheme {
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
}
