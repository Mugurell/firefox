/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar.chrome

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId

@Composable
fun BrowserChromeScaffold(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit) = {},
    bottomBar: (@Composable () -> Unit) = {},
    onDimensionsCalculated: ((BrowserChromeDimensions) -> Unit) = { _ -> }
) {
    var lastDimensions = remember { BrowserChromeDimensions() }
    LaunchedEffect(lastDimensions) { onDimensionsCalculated(lastDimensions) }

    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            Box(modifier = Modifier.layoutId("start")) { SideStrip(atStart = true) }
            Box(modifier = Modifier.layoutId("end")) { SideStrip(atStart = false) }

            // Using fillMaxWidth here is safe because the custom layout will redefine
            // what "max width" means when passing down constraints to these columns.
            Column(modifier = Modifier.layoutId("top").fillMaxWidth()) {
                EdgeStrip(atBottom = false)
                topBar()
            }

            Column(modifier = Modifier.layoutId("bottom").fillMaxWidth()) {
                bottomBar()
                EdgeStrip(atBottom = true)
            }
        }
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
            minHeight = 0
        )
        val topPlaceable = topMeasurable.measure(verticalConstraints)
        val bottomPlaceable = bottomMeasurable.measure(verticalConstraints)

        val topHeight = topPlaceable.height
        val botHeight = bottomPlaceable.height
        val newDimensions = BrowserChromeDimensions(startWidth, topHeight, endWidth, botHeight)
        if (lastDimensions != newDimensions) {
            onDimensionsCalculated(newDimensions)
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
 * Dimensions of the browser chrome in pixels.
 *
 * @param startWidth Start chrome width in pixels.
 * @param topHeight Top chrome height in pixels.
 * @param endWidth End chrome width in pixels.
 * @param bottomHeight Bottom chrome height in pixels.
 */
data class BrowserChromeDimensions(
    val startWidth: Int = 0,
    val topHeight: Int = 0,
    val endWidth: Int = 0,
    val bottomHeight: Int = 0,
)

@Composable
private fun EdgeStrip(atBottom: Boolean) {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                when {
                    atBottom -> Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
                    else -> Modifier.windowInsetsTopHeight(WindowInsets.statusBars)
                },
            )
            .background(MaterialTheme.colorScheme.surface),
    )
}

@Composable
private fun SideStrip(atStart: Boolean) {
    val sideInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
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
