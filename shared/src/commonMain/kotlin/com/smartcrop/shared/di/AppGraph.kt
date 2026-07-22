package com.smartcrop.shared.di

import com.smartcrop.shared.data.repository.CharacterRepository
import com.smartcrop.shared.data.repository.PhotoRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * The application dependency graph. Metro generates the implementation at compile
 * time from the `@Inject` constructors of the types reachable from the accessors
 * below, plus the `@Provides` functions declared here.
 *
 * Created once at app startup via `createGraph<AppGraph>()` and shared through a
 * CompositionLocal (see `ui/App.kt`).
 */
@DependencyGraph(AppScope::class)
interface AppGraph {

    /** Accessor exposed to the UI layer; backing [RickAndMortyApi] + [HttpClient]
     *  are resolved and scoped by Metro. */
    val characterRepository: CharacterRepository

    /** Accessor for the Picsum feed; backing [PicsumApi] shares the [HttpClient]. */
    val photoRepository: PhotoRepository

    /**
     * The shared Ktor client. Uses the platform's auto-discovered engine
     * (OkHttp on Android, Darwin on iOS) with JSON content negotiation.
     */
    @Provides
    @SingleIn(AppScope::class)
    fun provideHttpClient(): HttpClient = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                }
            )
        }
    }
}
