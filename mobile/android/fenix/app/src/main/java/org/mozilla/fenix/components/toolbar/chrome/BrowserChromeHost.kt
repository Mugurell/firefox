/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar.chrome

import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import org.mozilla.fenix.R
import org.mozilla.fenix.components.toolbar.ToolbarPosition

/**
 * Single parent for the browser chrome: one full-screen overlay [ComposeView] added over the engine
 * view that hosts every chrome surface (toolbar, navigation bar, and the system-bar background
 * strips) in a [BrowserChromeScaffold].
 *
 * @param container The [CoordinatorLayout] the host is added to (a sibling of the engine view).
 * @param topBar Dynamic content anchored to the top (e.g. tab strip + toolbar), or `null`.
 * @param bottomBar Dynamic content anchored to the bottom (e.g. navigation bar), or `null`.
 */
class BrowserChromeHost(
    private val container: ViewGroup,
    private val topBar: (@Composable () -> Unit) = {},
    private val bottomBar: (@Composable () -> Unit) = {},
    onDimensionsCalculated: ((BrowserChromeDimensions) -> Unit) = { _ -> },
) : BrowserChrome {
    override val layout: View = ComposeView(container.context).apply {
        id = R.id.browser_chrome_host
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    surface = Color.Red,
                ),
            ) {
                BrowserChromeScaffold(
                    topBar = topBar,
                    bottomBar = bottomBar,
                    onDimensionsCalculated = onDimensionsCalculated,
                )
            }
        }
    }

    init {
        container.addView(
            layout,
            CoordinatorLayout.LayoutParams(
                CoordinatorLayout.LayoutParams.MATCH_PARENT,
                CoordinatorLayout.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    override fun gone() {
        layout.isVisible = false
    }

    override fun visible() {
        layout.isVisible = true
    }

    override fun setToolbarBehavior(toolbarPosition: ToolbarPosition, shouldDisableScroll: Boolean) {
        if (shouldDisableScroll) disableScrolling() else enableScrolling()
    }

    override fun enableScrolling() { }

    override fun disableScrolling() { }

    override fun expand() { }

    override fun collapse() { }
}
