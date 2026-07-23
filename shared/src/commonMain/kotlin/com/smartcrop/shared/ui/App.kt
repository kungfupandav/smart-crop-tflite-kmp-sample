package com.smartcrop.shared.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.smartcrop.shared.di.AppGraph
import com.smartcrop.shared.ui.detail.DetailScreen
import com.smartcrop.shared.ui.entry.EntryScreen
import com.smartcrop.shared.ui.home.HomeScreen
import com.smartcrop.shared.ui.picsum.PicsumDetailScreen
import com.smartcrop.shared.ui.picsum.PicsumFeedScreen
import com.smartcrop.shared.ui.theme.NeoColors
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

@OptIn(ExperimentalSharedTransitionApi::class)
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

            // Cream fills the whole window (behind the status/navigation bars);
            // screen content is inset to the safe area so nothing draws under the
            // status bar, home indicator, or display cutout.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NeoColors.Cream),
            ) {
                // A single SharedTransitionLayout hosts the whole NavHost so images can
                // morph across destinations. The scope is threaded down via
                // LocalSharedTransitionScope, and each destination's AnimatedContentScope
                // via LocalNavAnimatedVisibilityScope, so leaf composables opt in with
                // Modifier.sharedImage(key) without signature changes.
                SharedTransitionLayout {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                        NavHost(
                            navController = navController,
                            startDestination = EntryRoute,
                            modifier = Modifier
                                .fillMaxSize()
                                .safeDrawingPadding(),
                        ) {
                            // Landing: choose a feed.
                            composable<EntryRoute> {
                                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                                    EntryScreen(
                                        onRickAndMorty = { navController.navigate(HomeRoute) },
                                        onPicsum = { navController.navigate(PicsumFeedRoute) },
                                    )
                                }
                            }

                            // --- Rick & Morty flow ---
                            composable<HomeRoute> {
                                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                                    HomeScreen(
                                        onCharacterClick = { id ->
                                            navController.navigate(DetailRoute(characterId = id))
                                        }
                                    )
                                }
                            }
                            composable<DetailRoute> { backStackEntry ->
                                val route = backStackEntry.toRoute<DetailRoute>()
                                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                                    DetailScreen(
                                        characterId = route.characterId,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }

                            // --- Picsum flow ---
                            composable<PicsumFeedRoute> {
                                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                                    PicsumFeedScreen(
                                        onPhotoClick = { id ->
                                            navController.navigate(PicsumDetailRoute(photoId = id))
                                        }
                                    )
                                }
                            }
                            composable<PicsumDetailRoute> { backStackEntry ->
                                val route = backStackEntry.toRoute<PicsumDetailRoute>()
                                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                                    PicsumDetailScreen(
                                        photoId = route.photoId,
                                        onBack = { navController.popBackStack() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
