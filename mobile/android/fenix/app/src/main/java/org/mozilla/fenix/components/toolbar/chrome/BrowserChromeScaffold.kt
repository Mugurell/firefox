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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.LayoutDirection
import org.mozilla.fenix.components.toolbar.scroll.BrowserToolbarScrollState

@Composable
fun BrowserChromeScaffold(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit) = {},
    bottomBar: (@Composable () -> Unit) = {},
    onNewChromeDimensions: ((BrowserChromeDimensions) -> Unit) = { _ -> },
) {
    var lastDimensions = remember { BrowserChromeDimensions() }
    LaunchedEffect(lastDimensions) { onNewChromeDimensions(lastDimensions) }

    val statusBars = WindowInsets.statusBars
    val navigationBars = WindowInsets.navigationBars
    val sideInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)

    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            Box(modifier = Modifier.layoutId("start")) { SideStrip(sideInsets, atStart = true) }
            Box(modifier = Modifier.layoutId("end")) { SideStrip(sideInsets, atStart = false) }

            // Using fillMaxWidth here is safe because the custom layout will redefine
            // what "max width" means when passing down constraints to these columns.
            Column(modifier = Modifier.layoutId("top").fillMaxWidth()) {
                EdgeStrip(statusBars, navigationBars, atBottom = false)
                topBar()
            }

            Column(modifier = Modifier.layoutId("bottom").fillMaxWidth()) {
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
        if (lastDimensions != newDimensions) {
            onNewChromeDimensions(newDimensions)
            lastDimensions = newDimensions
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
 * Which edge a piece of chrome is anchored to.
 */
enum class ChromeEdge { Top, Bottom }

/**
 * Wraps chrome [content] that should collapse/expand with scrolling. The whole [content] translates
 * with this [edge]'s offset (up for [ChromeEdge.Top], down for [ChromeEdge.Bottom]) inside a fixed
 * clip region, so it slides *behind* the fixed background strip rather than over it.
 *
 * The translation is the edge's *clamped* offset — bounded by that edge's collapsible height — so it
 * stops exactly when the edge is hidden regardless of the other edge's height. This only translates;
 * it does *not* define the collapse range. Mark the subtree whose height should bound this edge's
 * collapse (the toolbar, stacked with the navbar in bottom-toolbar mode) with
 * [Modifier.collapseRange] and the matching [edge]. Content left unmarked (an autofill bar stacked
 * above the toolbar) rides the same offset but doesn't extend the range, so it parks flush when the
 * toolbar is hidden instead of sliding off.
 *
 * @param scrollState Shared scroll state driving the translation.
 * @param edge Which edge this chrome is anchored to (sets the translation direction and which
 * clamped offset it reads).
 */
@Composable
fun CollapsingChrome(
    scrollState: BrowserToolbarScrollState,
    edge: ChromeEdge,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.clipToBounds()) {
        Box(
            modifier = Modifier.graphicsLayer {
                translationY = when (edge) {
                    ChromeEdge.Top -> scrollState.topOffset
                    ChromeEdge.Bottom -> -scrollState.bottomOffset
                }
            },
        ) {
            content()
        }
    }
}

/**
 * Marks a subtree inside a [CollapsingChrome] as the one whose measured height defines how far the
 * given [edge] can collapse ([BrowserToolbarScrollState.topBarHeight] / [bottomBarHeight]). Apply it
 * to the toolbar (stacked with the navbar in bottom-toolbar mode); leave secondary content that only
 * mirrors the offset (an autofill bar) unmarked. The edge's height is reset to 0 when this subtree
 * leaves composition, so removing a bar shrinks the collapse range instead of leaving it stale.
 */
fun Modifier.collapseRange(
    scrollState: BrowserToolbarScrollState,
    edge: ChromeEdge,
): Modifier = composed {
    DisposableEffect(scrollState, edge) {
        onDispose {
            when (edge) {
                ChromeEdge.Top -> scrollState.topBarHeight = 0f
                ChromeEdge.Bottom -> scrollState.bottomBarHeight = 0f
            }
        }
    }
    onSizeChanged {
        when (edge) {
            ChromeEdge.Top -> scrollState.topBarHeight = it.height.toFloat()
            ChromeEdge.Bottom -> scrollState.bottomBarHeight = it.height.toFloat()
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
