/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar.scroll

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job

/**
 * Interface that defines how the browser toolbar should respond to scrolling.
 */
@Stable
interface BrowserToolbarScrollBehavior {
    /**
     * The [BrowserToolbarScrollState] associated with this behavior.
     */
    val state: BrowserToolbarScrollState

    /**
     * An [AnimationSpec] that will be used to animate the toolbar back to shown when scrolling stops.
     */
    val snapAnimationSpec: AnimationSpec<Float>?

    /**
     * The [NestedScrollConnection] that view-side scroll sources call directly to feed scroll deltas
     * into this behavior.
     */
    val nestedScrollConnection: NestedScrollConnection

    /**
     * Settle the chrome once scrolling stops — animates it back to fully shown.
     */
    suspend fun settle()

    /** Animate the chrome to fully shown, serialized with [settle] and [collapse]. */
    suspend fun expand()

    /** Animate the chrome to fully collapsed, serialized with [settle] and [expand]. */
    suspend fun collapse()
}

/**
 * Default implementation of [BrowserToolbarScrollBehavior]. The chrome collapses only while the user
 * is actively scrolling down; once scrolling stops it is animated back to fully shown by [settle].
 *
 * @param state The [BrowserToolbarScrollState] to use.
 * @param snapAnimationSpec The [AnimationSpec] to use when animating back to shown.
 */
@Stable
class DefaultBrowserToolbarScrollBehavior(
    override val state: BrowserToolbarScrollState,
    override val snapAnimationSpec: AnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
) : BrowserToolbarScrollBehavior {
    override val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y == 0f) return Offset.Zero

            // A new scroll gesture takes over: cancel any in-flight settle animation so the user's
            // drag and the animation don't fight over heightOffset.
            settleJob?.cancel()

            val prevHeightOffset = state.heightOffset
            state.heightOffset = (prevHeightOffset + available.y).coerceIn(state.heightOffsetLimit, 0f)
            return Offset(0f, state.heightOffset - prevHeightOffset)
        }
    }

    private var settleJob: Job? = null

    override suspend fun settle() {
        runSingleAnimation { animateToOffset(0f) }
    }

    override suspend fun expand() {
        runSingleAnimation { animateToOffset(0f) }
    }

    override suspend fun collapse() {
        runSingleAnimation { animateToOffset(state.heightOffsetLimit) }
    }

    /**
     * Run [block] as the single active toolbar animation, cancelling any previously running one
     * first, so repeated settles never write [BrowserToolbarScrollState.heightOffset] concurrently.
     */
    private suspend fun runSingleAnimation(block: suspend () -> Unit) = coroutineScope {
        // cancelAndJoin on a completed (or null) job is a no-op.
        settleJob?.cancelAndJoin()
        settleJob = coroutineContext.job
        block()
    }

    /**
     * Animate [BrowserToolbarScrollState.heightOffset] to [target] (0 = fully shown,
     * [BrowserToolbarScrollState.heightOffsetLimit] = fully collapsed). No-op when already there or
     * without a snap spec.
     *
     * Must be called from within [runSingleAnimation] so every settle/expand/collapse is serialized
     * through the one job and can't fight over the offset.
     */
    private suspend fun animateToOffset(target: Float) {
        if (state.heightOffset == target) return
        val snapSpec = snapAnimationSpec ?: run {
            state.heightOffset = target
            return
        }
        state.animateTo(target, snapSpec)
    }
}
