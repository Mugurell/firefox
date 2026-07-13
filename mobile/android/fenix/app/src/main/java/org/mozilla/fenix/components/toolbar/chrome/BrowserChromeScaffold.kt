/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar.chrome

import androidx.annotation.Px
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsEndWidth
import androidx.compose.foundation.layout.windowInsetsStartWidth
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.LayoutDirection
import org.mozilla.fenix.components.toolbar.scroll.BrowserToolbarScrollState

@Composable
fun BrowserChromeScaffold(
    scrollState: BrowserToolbarScrollState,
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit) = {},
    bottomBar: (@Composable () -> Unit) = {},
    onNewChromeDimensions: ((BrowserChromeDimensions) -> Unit) = { _ -> },
) {
    // Reported to the Android host from a post-layout effect, never from inside the measure pass
    // below — the callback mutates the engine view's layout params, which must not happen during
    // Compose measurement. Nullable + guard so the initial pre-measure value isn't emitted.
    var dimensions by remember { mutableStateOf<BrowserChromeDimensions?>(null) }
    LaunchedEffect(dimensions) { dimensions?.let(onNewChromeDimensions) }

    val statusBars = WindowInsets.statusBars
    val navigationBars = WindowInsets.navigationBars
    val sideInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)

    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            // The side strips stay fixed (landscape nav bar / display cutout).
            Box(modifier = Modifier.layoutId("start")) { SideStrip(sideInsets, atStart = true) }
            Box(modifier = Modifier.layoutId("end")) { SideStrip(sideInsets, atStart = false) }

            // The whole top/bottom columns — system-bar background strip *and* bars — collapse
            // together and slide fully off screen, so the engine view shows edge to edge. Each
            // column reports its full height as that edge's collapse range and translates by that
            // edge's clamped offset (draw-phase, no relayout).
            //
            // Using fillMaxWidth here is safe because the custom layout will redefine
            // what "max width" means when passing down constraints to these columns.
            Column(
                modifier = Modifier
                    .layoutId("top")
                    .fillMaxWidth()
                    .onSizeChanged { scrollState.topBarHeight = it.height.toFloat() }
                    .graphicsLayer { translationY = scrollState.topOffset },
            ) {
                EdgeStrip(statusBars, navigationBars, atBottom = false)
                topBar()
            }

            Column(
                modifier = Modifier
                    .layoutId("bottom")
                    .fillMaxWidth()
                    .onSizeChanged { scrollState.bottomBarHeight = it.height.toFloat() }
                    .graphicsLayer { translationY = -scrollState.bottomOffset },
            ) {
                bottomBar()
                EdgeStrip(statusBars, navigationBars, atBottom = true)
            }
        },
    ) { measurables, constraints ->
        val startMeasurable = measurables.first { it.layoutId == "start" }
        val endMeasurable = measurables.first { it.layoutId == "end" }
        val topMeasurable = measurables.first { it.layoutId == "top" }
        val bottomMeasurable = measurables.first { it.layoutId == "bottom" }

        // 1. Measure Start and End first to establish horizontal boundaries.
        // minHeight = 0 allows them to size naturally, but they can still
        // fillMaxHeight() if your content requires it.
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val startPlaceable = startMeasurable.measure(looseConstraints)
        val endPlaceable = endMeasurable.measure(looseConstraints)

        val startWidth = startPlaceable.width
        val endWidth = endPlaceable.width

        // 2. Calculate the exact horizontal space left in the middle.
        val remainingWidth = (constraints.maxWidth - startWidth - endWidth).coerceAtLeast(0)

        // 3. Measure Top and Bottom, explicitly restricting their width
        // so they cannot bleed over the Start and End edges.
        val verticalConstraints = constraints.copy(
            minWidth = remainingWidth,
            maxWidth = remainingWidth,
            minHeight = 0,
        )
        val topPlaceable = topMeasurable.measure(verticalConstraints)
        val bottomPlaceable = bottomMeasurable.measure(verticalConstraints)

        val topHeight = topPlaceable.height
        val botHeight = bottomPlaceable.height

        val startInset = if (layoutDirection == LayoutDirection.Ltr) {
            sideInsets.getLeft(this, layoutDirection)
        } else {
            sideInsets.getRight(this, layoutDirection)
        }
        val endInset = if (layoutDirection == LayoutDirection.Ltr) {
            sideInsets.getRight(this, layoutDirection)
        } else {
            sideInsets.getLeft(this, layoutDirection)
        }

        val newDimensions = BrowserChromeDimensions(
            startWidth = startWidth,
            topHeight = topHeight,
            endWidth = endWidth,
            bottomHeight = botHeight,
            startInset = startInset,
            topInset = statusBars.getTop(this),
            endInset = endInset,
            bottomInset = navigationBars.getBottom(this),
        )
        // Only stash the measured result; the effect above runs the callback after the frame,
        // keeping the callback's view-layout side effects out of this measure pass.
        if (dimensions != newDimensions) {
            dimensions = newDimensions
        }

        // 4. Place elements explicitly so there is zero overlap.
        layout(constraints.maxWidth, constraints.maxHeight) {
            // Edges stay at the absolute X extremes
            startPlaceable.placeRelative(x = 0, y = 0)
            endPlaceable.placeRelative(x = constraints.maxWidth - endWidth, y = 0)

            // Top and Bottom are shifted by startW so they sit perfectly between the edges.
            topPlaceable.placeRelative(x = startWidth, y = 0)
            bottomPlaceable.placeRelative(x = startWidth, y = constraints.maxHeight - botHeight)
        }
    }
}

/**
 * Dimensions of the browser chrome in pixels.
 *
 * @param startWidth Start chrome width in pixels.
 * @param topHeight Top chrome height in pixels.
 * @param endWidth End chrome width in pixels.
 * @param bottomHeight Bottom chrome height in pixels.
 * @param startInset Start chrome inset in pixels.
 * @param topInset Top chrome inset in pixels.
 * @param endInset End chrome inset in pixels.
 * @param bottomInset Bottom chrome inset in pixels.
 */
data class BrowserChromeDimensions(
    @Px val startWidth: Int = 0,
    @Px val topHeight: Int = 0,
    @Px val endWidth: Int = 0,
    @Px val bottomHeight: Int = 0,
    @Px val startInset: Int = 0,
    @Px val topInset: Int = 0,
    @Px val endInset: Int = 0,
    @Px val bottomInset: Int = 0,
) {
    companion object {
        /**
         * Get the current chrome dimensions without the system insets.
         */
        fun BrowserChromeDimensions.withoutInsets(): BrowserChromeDimensions {
            return this.copy(
                startWidth = maxOf(0, startWidth - startInset),
                topHeight = maxOf(0, topHeight - topInset),
                endWidth = maxOf(0, endWidth - endInset),
                bottomHeight = maxOf(0, bottomHeight - bottomInset),
                // Zeroing out the insets in the returned object so it's clear they were stripped.
                startInset = 0, topInset = 0, endInset = 0, bottomInset = 0,
            )
        }
    }
}

@Composable
private fun EdgeStrip(
    statusBars: WindowInsets,
    navigationBars: WindowInsets,
    atBottom: Boolean,
) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when {
                    atBottom -> Modifier.windowInsetsBottomHeight(navigationBars)
                    else -> Modifier.windowInsetsTopHeight(statusBars)
                },
            )
            .background(MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun SideStrip(sideInsets: WindowInsets, atStart: Boolean) {
    Spacer(
        modifier = Modifier
            .fillMaxHeight()
            .then(
                when {
                    atStart -> Modifier.windowInsetsStartWidth(sideInsets)
                    else -> Modifier.windowInsetsEndWidth(sideInsets)
                },
            )
            .background(MaterialTheme.colorScheme.surface),
    )
}
