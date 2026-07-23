@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.smartcrop.shared.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

/**
 * Threads the [SharedTransitionScope] created by the top-level `SharedTransitionLayout`
 * (in `App`) down to leaf composables without changing every function signature.
 * Null when no layout is present (e.g. previews), in which case [sharedImage] is a no-op.
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * Threads the per-destination [AnimatedVisibilityScope] (the `AnimatedContentScope` each
 * NavHost `composable { }` block runs in) down to leaf composables. Null outside a nav
 * transition, in which case [sharedImage] is a no-op.
 */
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Marks this element as a shared element keyed by [key]. When the same [key] appears on
 * both the outgoing and incoming destinations of a nav transition, Compose morphs the
 * bounds of one into the other — so a smart-cropped feed image expands into the full
 * detail image (and back).
 *
 * No-op when either scope is absent (returns the receiver unchanged), so it is safe to
 * apply unconditionally.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedImage(key: String): Modifier {
    val sts = LocalSharedTransitionScope.current ?: return this
    val avs = LocalNavAnimatedVisibilityScope.current ?: return this
    return with(sts) {
        this@sharedImage.sharedBounds(
            rememberSharedContentState(key),
            animatedVisibilityScope = avs,
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(),
        )
    }
}
