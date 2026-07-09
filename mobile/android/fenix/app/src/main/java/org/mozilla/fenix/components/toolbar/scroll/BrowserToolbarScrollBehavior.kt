/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar.scroll

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.job
import kotlin.math.abs

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
     * An [AnimationSpec] that will be used to snap the toolbar to its edges.
     */
    val snapAnimationSpec: AnimationSpec<Float>?

    /**
     * A [DecayAnimationSpec] used to settle the toolbar after a fling.
     */
    val flingAnimationSpec: DecayAnimationSpec<Float>?

    /**
     * The [NestedScrollConnection] that view-side scroll sources call directly to feed scroll
     * deltas, flings and post-fling settles into this behavior.
     */
    val nestedScrollConnection: NestedScrollConnection

    /**
     * Settle the toolbar to its nearest edge (fully expanded or fully collapsed).
     */
    suspend fun settle()
}

/**
 * Default implementation of [BrowserToolbarScrollBehavior].
 *
 * @param state The [BrowserToolbarScrollState] to use.
 * @param snapAnimationSpec The [AnimationSpec] to use for snapping.
 * @param flingAnimationSpec The [DecayAnimationSpec] used to settle the toolbar after a fling.
 */
@Stable
class DefaultBrowserToolbarScrollBehavior(
    override val state: BrowserToolbarScrollState,
    override val snapAnimationSpec: AnimationSpec<Float>? = spring(stiffness = Spring.StiffnessMediumLow),
    override var flingAnimationSpec: DecayAnimationSpec<Float>? = null,
) : BrowserToolbarScrollBehavior {
    override val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y == 0f) return Offset.Zero

            // A new scroll gesture takes over: cancel any in-flight settle/fling animation so the
            // user's drag and the animation don't fight over heightOffset.
            settleJob?.cancel()

            val prevHeightOffset = state.heightOffset
            state.heightOffset = (prevHeightOffset + available.y).coerceIn(state.heightOffsetLimit, 0f)
            return Offset(0f, state.heightOffset - prevHeightOffset)
        }

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            val decaySpec = flingAnimationSpec
            if (available.y != 0f && decaySpec != null) {
                // Decay then snap as a single exclusive animation so a concurrent settle()
                // (from onStopNestedScroll) cannot interleave writes to heightOffset.
                runSingleAnimation {
                    val animatable = Animatable(state.heightOffset)
                    animatable.animateDecay(
                        initialVelocity = available.y,
                        animationSpec = decaySpec,
                    ) {
                        state.heightOffset = value.coerceIn(state.heightOffsetLimit, 0f)
                    }
                    snapToClosestEdge()
                }
            } else {
                settle()
            }
            return super.onPostFling(consumed, available)
        }
    }

    private var settleJob: Job? = null

    override suspend fun settle() {
        runSingleAnimation {
            snapToClosestEdge()
        }
    }

    /**
     * Run [block] as the single active toolbar animation, cancelling any previously running one
     * first. This serializes [settle] and the post-fling settle so they never write
     * [BrowserToolbarScrollState.heightOffset] concurrently.
     */
    private suspend fun runSingleAnimation(block: suspend () -> Unit) = coroutineScope {
        // cancelAndJoin on a completed (or null) job is a no-op.
        settleJob?.cancelAndJoin()
        settleJob = coroutineContext.job
        block()
    }

    /**
     * Snap [BrowserToolbarScrollState.heightOffset] to whichever edge (fully expanded or fully
     * collapsed) is closer. No-op when already at an edge or when there is no snap spec.
     *
     * Must be called from within [runSingleAnimation] so it is serialized with other animations.
     */
    private suspend fun snapToClosestEdge() {
        val snapSpec = snapAnimationSpec ?: return
        if (state.heightOffset == 0f || state.heightOffset == state.heightOffsetLimit) return

        val targetValue = if (abs(state.heightOffset) < abs(state.heightOffsetLimit) / 2) {
            0f
        } else {
            state.heightOffsetLimit
        }
        state.animateTo(targetValue, snapSpec)
    }
}
