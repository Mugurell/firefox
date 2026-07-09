/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar.chrome

import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import kotlinx.coroutines.CoroutineScope
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.toolbar.ToolbarBehaviorController
import mozilla.components.support.utils.KeyboardState
import mozilla.components.support.utils.keyboardAsState
import org.mozilla.fenix.R
import org.mozilla.fenix.components.toolbar.ToolbarPosition
import org.mozilla.fenix.components.toolbar.scroll.BrowserToolbarScrollBehavior
import org.mozilla.fenix.components.toolbar.scroll.BrowserToolbarScrollGesturesBehavior
import org.mozilla.fenix.components.toolbar.scroll.DefaultBrowserToolbarScrollBehavior
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.utils.Settings

/**
 * Single parent for the browser chrome: one full-screen overlay [ComposeView] added over the engine
 * view that hosts every chrome surface (toolbar, navigation bar, and the system-bar background
 * strips) in a [BrowserChromeScaffold].
 *
 * @param container The [CoordinatorLayout] the host is added to (a sibling of the engine view).
 * @param scrollBehavior Shared scroll behavior/state driving all dynamic bars.
 * @param settings Used to decide whether the chrome should actually scroll (dynamic toolbar) or stay
 * fixed — mirroring the per-bar path so the overlay and the engine view agree.
 * @param coroutineScope Scope used by the scroll-gesture behavior for settle/fling animations.
 * @param topBar Dynamic content anchored to the top (e.g. tab strip + toolbar), or `null`.
 * @param bottomBar Dynamic content anchored to the bottom (e.g. navigation bar), or `null`.
 */
class BrowserChromeHost(
    private val container: ViewGroup,
    private val browserStore: BrowserStore,
    override val scrollBehavior: BrowserToolbarScrollBehavior,
    private val customTabId: String?,
    private val settings: Settings,
    coroutineScope: CoroutineScope,
    private val topBar: (@Composable () -> Unit) = {},
    private val bottomBar: (@Composable () -> Unit) = {},
    private val onNewChromeDimensions: ((BrowserChromeDimensions) -> Unit) = { _ -> },
) : BrowserChrome {
    private val scrollGesturesBehavior = BrowserToolbarScrollGesturesBehavior<View>(
        context = container.context,
        scrollBehavior = scrollBehavior,
        coroutineScope = coroutineScope,
    )

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
                    onNewChromeDimensions = onNewChromeDimensions,
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

        // Attach the scroll-gesture behavior only when the toolbar is actually dynamic; otherwise the
        // chrome stays fixed and expanded so it matches the engine view's fixed-toolbar layout.
        setToolbarBehavior(settings.toolbarPosition)
    }

    @Composable
    private fun ChromeContent() {
        // Fling decay for the scroll behavior (snap alone doesn't carry momentum after a fling).
        val flingSpec = rememberSplineBasedDecay<Float>()
        LaunchedEffect(flingSpec) {
            (scrollBehavior as? DefaultBrowserToolbarScrollBehavior)?.flingAnimationSpec = flingSpec
        }

        // Show/hide the chrome based on page load, fullscreen and security state, and re-enable
        // scrolling appropriately — the same controller the per-bar path uses.
        DisposableEffect(Unit) {
            val controller = ToolbarBehaviorController(
                toolbar = this@BrowserChromeHost,
                store = browserStore,
                customTabId = customTabId,
            )
            controller.start()
            onDispose { controller.stop() }
        }

        // Re-expand the chrome once the keyboard is dismissed.
        if (!settings.shouldUseMinimalBottomToolbarWhenEnteringText) {
            val keyboardState by keyboardAsState()
            LaunchedEffect(keyboardState) {
                if (keyboardState == KeyboardState.Closed) {
                    expand()
                }
            }
        }

        FirefoxTheme {
            BrowserChromeScaffold(
                topBar = topBar,
                bottomBar = bottomBar,
                onNewChromeDimensions = onNewChromeDimensions,
            )
        }
    }

    override fun gone() {
        layout.isVisible = false
    }

    override fun visible() {
        layout.isVisible = true
    }

    override fun setToolbarBehavior(toolbarPosition: ToolbarPosition, shouldDisableScroll: Boolean) {
        // Mirrors BrowserToolbarComposable: the chrome is only dynamic (scrollable) when the dynamic
        // toolbar is enabled and no accessibility service forces a fixed toolbar. Otherwise detach the
        // scroll behavior and stay expanded, so the bars don't scroll off a fixed-height engine view.
        val isDynamic = when (toolbarPosition) {
            ToolbarPosition.BOTTOM ->
                settings.isDynamicToolbarEnabled && !settings.shouldUseFixedTopToolbar
            ToolbarPosition.TOP ->
                settings.isDynamicToolbarEnabled && !settings.shouldUseFixedTopToolbar && !shouldDisableScroll
        }

        val params = layout.layoutParams as? CoordinatorLayout.LayoutParams ?: return
        if (isDynamic) {
            params.behavior = scrollGesturesBehavior
        } else {
            expand()
            params.behavior = null
        }
        layout.requestLayout()
    }

    override fun enableScrolling() {
        scrollGesturesBehavior.enableScrolling()
    }

    override fun disableScrolling() {
        scrollGesturesBehavior.disableScrolling()
    }

    override fun expand() {
        scrollGesturesBehavior.forceExpand()
    }

    override fun collapse() {
        scrollGesturesBehavior.forceCollapse()
    }
}
