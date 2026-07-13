/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar.scroll

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import mozilla.components.support.base.log.logger.Logger
import kotlin.math.max

/**
 * State of the browser chrome's scroll collapse.
 *
 * The gesture feeds a single accumulator, [heightOffset]. Each edge is a *clamped projection* of it
 * ([topOffset] / [bottomOffset]), bounded by that edge's own collapsible height ([topBarHeight] /
 * [bottomBarHeight]). Because each projection is clamped at the source, an edge can never over- or
 * under-travel and the sum of the two obscured amounts can never exceed the total bar height — for
 * *any* combination of top/bottom heights, including a bottom taller than the top. Consumers derive
 * their pixels from the projections, so there are no bounds left to check downstream.
 *
 * @param initialHeightOffset The initial value for [heightOffset].
 * @param initialTopBarHeight The initial value for [topBarHeight].
 * @param initialBottomBarHeight The initial value for [bottomBarHeight].
 */
@Stable
class BrowserToolbarScrollState(
    initialHeightOffset: Float = 0f,
    initialTopBarHeight: Float = 0f,
    initialBottomBarHeight: Float = 0f,
) {
    private val _heightOffset = mutableFloatStateOf(initialHeightOffset)
    private val _topBarHeight = mutableFloatStateOf(initialTopBarHeight)
    private val _bottomBarHeight = mutableFloatStateOf(initialBottomBarHeight)

    private val changeListeners = mutableListOf<() -> Unit>()

    /**
     * The shared scroll accumulator in pixels, ranging from [heightOffsetLimit] (fully collapsed) to
     * 0 (fully expanded). Both edges read this through their clamped projections.
     *
     * Setting it notifies [changeListeners] synchronously, in the same call stack, so view-layer
     * consumers (the engine-view clipping / translation) stay in lockstep with the Compose
     * translation frame-for-frame. A `snapshotFlow` observer would emit a frame later, visibly
     * desynchronizing the engine view from the bars while scrolling.
     */
    var heightOffset: Float
        get() = _heightOffset.floatValue
        set(value) {
            if (!value.isFinite()) {
                // A non-finite offset can only come from a bug in animation/decay math. Keep the
                // last valid state, but surface it rather than silently masking the regression.
                logger.error("Ignoring non-finite browser chrome scroll offset: $value")
                return
            }
            val clamped = value.coerceIn(heightOffsetLimit, 0f)
            if (clamped != _heightOffset.floatValue) {
                _heightOffset.floatValue = clamped
                notifyChanged()
            }
        }

    /** Collapsible height of the top edge in pixels (the whole top chrome column). */
    var topBarHeight: Float
        get() = _topBarHeight.floatValue
        set(value) {
            if (value != _topBarHeight.floatValue) {
                setEdgeHeightPreservingCollapse { _topBarHeight.floatValue = value }
            }
        }

    /** Collapsible height of the bottom edge in pixels (the whole bottom chrome column). */
    var bottomBarHeight: Float
        get() = _bottomBarHeight.floatValue
        set(value) {
            if (value != _bottomBarHeight.floatValue) {
                setEdgeHeightPreservingCollapse { _bottomBarHeight.floatValue = value }
            }
        }

    /**
     * The minimum (most negative) value [heightOffset] can reach: the point at which the *taller*
     * edge is fully hidden (the shorter one saturated earlier).
     */
    val heightOffsetLimit: Float
        get() = -max(topBarHeight, bottomBarHeight)

    /** Top edge translation, clamped to the top edge's own height. Always in `[-topBarHeight, 0]`. */
    val topOffset: Float
        get() = heightOffset.coerceIn(-topBarHeight, 0f)

    /** Bottom edge translation magnitude, clamped to the bottom edge's own height. `[-bottomBarHeight, 0]`. */
    val bottomOffset: Float
        get() = heightOffset.coerceIn(-bottomBarHeight, 0f)

    /**
     * A fraction between 0 (fully expanded) and 1 (fully collapsed).
     * Returns 0 if [heightOffsetLimit] is 0 (no scroll possible).
     */
    val collapsedFraction: Float
        get() = if (heightOffsetLimit != 0f) heightOffset / heightOffsetLimit else 0f

    /** Register a [listener] invoked synchronously whenever the offset or an edge height changes. */
    fun addOnChangeListener(listener: () -> Unit) {
        changeListeners.add(listener)
    }

    /** Remove a listener previously registered with [addOnChangeListener]. */
    fun removeOnChangeListener(listener: () -> Unit) {
        changeListeners.remove(listener)
    }

    private fun notifyChanged() {
        changeListeners.forEach { it() }
    }

    /**
     * Apply an edge-height change ([updateHeight]) while keeping the collapse state stable across it:
     * a fully-expanded chrome stays expanded, a fully-collapsed chrome stays collapsed, and a partial
     * collapse keeps its fraction. Without this, *growing* an edge (e.g. the tab strip appearing)
     * would leave a fully-collapsed bar partly visible, and shrinking could leave the offset out of
     * range. (Preserving the fraction for partial collapses is a UX choice; the endpoints are not.)
     */
    private fun setEdgeHeightPreservingCollapse(updateHeight: () -> Unit) {
        val oldLimit = heightOffsetLimit
        val oldOffset = _heightOffset.floatValue

        updateHeight()

        val normalized = when {
            oldOffset == 0f -> 0f
            oldLimit != 0f && oldOffset == oldLimit -> heightOffsetLimit
            oldLimit != 0f -> heightOffsetLimit * (oldOffset / oldLimit)
            else -> oldOffset
        }
        _heightOffset.floatValue = normalized.coerceIn(heightOffsetLimit, 0f)
        notifyChanged()
    }

    /**
     * Animate the [heightOffset] to a specific [targetValue].
     */
    suspend fun animateTo(
        targetValue: Float,
        animationSpec: AnimationSpec<Float>,
    ) {
        animate(
            initialValue = heightOffset,
            targetValue = targetValue.coerceIn(heightOffsetLimit, 0f),
            animationSpec = animationSpec,
        ) { value, _ ->
            heightOffset = value
        }
    }

    companion object {
        private val logger = Logger("BrowserToolbarScrollState")

        /**
         * The default [Saver] implementation for [BrowserToolbarScrollState].
         */
        val Saver: Saver<BrowserToolbarScrollState, *> = listSaver(
            save = { listOf(it.heightOffset, it.topBarHeight, it.bottomBarHeight) },
            restore = {
                BrowserToolbarScrollState(
                    initialHeightOffset = it[0],
                    initialTopBarHeight = it[1],
                    initialBottomBarHeight = it[2],
                )
            },
        )
    }
}
