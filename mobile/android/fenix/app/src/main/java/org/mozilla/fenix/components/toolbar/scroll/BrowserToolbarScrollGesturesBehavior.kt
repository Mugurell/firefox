/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar.scroll

import android.content.Context
import android.view.View
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mozilla.components.concept.engine.EngineView
import mozilla.components.support.ktx.android.view.findViewInHierarchy

/**
 * A [CoordinatorLayout.Behavior] implementation that bridges view-layer nested scrolling into a
 * [BrowserToolbarScrollBehavior].
 *
 * Forked and adapted from [mozilla.components.ui.widgets.behavior.EngineViewScrollingGesturesBehavior].
 *
 * @param scrollBehavior The [BrowserToolbarScrollBehavior] to bridge to.
 * @param coroutineScope The [CoroutineScope] to use for starting animations.
 */
internal class BrowserToolbarScrollGesturesBehavior<V : View>(
    context: Context,
    private val scrollBehavior: BrowserToolbarScrollBehavior,
    private val coroutineScope: CoroutineScope,
) : CoordinatorLayout.Behavior<V>(context, null) {

    var isScrollEnabled: Boolean = true

    // True between a fling being started and its momentum settling. Used to defer showing the bars
    // until the fling finishes, so they don't pop back mid-fling on the touch-up stop.
    private var awaitingFlingSettle = false

    // Cache engineView reference. Stale references are unlikely as the whole toolbar is
    // recreated on major layout changes, but we check each scroll start.
    private var engineView: EngineView? = null

    private val shouldScroll: Boolean
        get() = engineView?.getInputResultDetail()?.let {
            it.canScrollToBottom() || it.canScrollToTop()
        } ?: false

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int,
    ): Boolean {
        if (engineView == null) {
            engineView = coordinatorLayout.findViewInHierarchy { it is EngineView } as? EngineView
        }
        // A fresh touch scroll (e.g. grabbing the page mid-fling) clears any pending fling wait so
        // its stop reveals the bars.
        if (type == ViewCompat.TYPE_TOUCH) {
            awaitingFlingSettle = false
        }
        return isScrollEnabled && shouldScroll && (axes and ViewCompat.SCROLL_AXIS_VERTICAL != 0)
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int,
    ) {
        if (!isScrollEnabled || !shouldScroll) return

        val available = Offset(0f, -dy.toFloat())
        val source = if (type == ViewCompat.TYPE_TOUCH) {
            NestedScrollSource.UserInput
        } else {
            NestedScrollSource.SideEffect
        }

        val consumedByCompose = scrollBehavior.nestedScrollConnection.onPreScroll(available, source)
        consumed[1] = -consumedByCompose.y.toInt()
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        type: Int,
    ) {
        if (!isScrollEnabled) return

        // A fling produces a touch-up stop followed by a momentum (non-touch) stop. Wait for the
        // momentum to finish before showing the bars, otherwise they'd pop back mid-fling.
        if (type == ViewCompat.TYPE_TOUCH && awaitingFlingSettle) return
        awaitingFlingSettle = false

        coroutineScope.launch {
            scrollBehavior.settle()
        }
    }

    override fun onNestedPreFling(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        velocityX: Float,
        velocityY: Float,
    ): Boolean {
        // Let the page consume the fling; the bars stay collapsed through the momentum and are shown
        // again once it settles (the following non-touch onStopNestedScroll).
        awaitingFlingSettle = true
        return false
    }

    /**
     * Allow scroll gestures to translate the toolbar.
     */
    fun enableScrolling() {
        isScrollEnabled = true
    }

    /**
     * Prevent scroll gestures from translating the toolbar.
     */
    fun disableScrolling() {
        isScrollEnabled = false
    }

    /**
     * Force the toolbar to expand. Routed through [BrowserToolbarScrollBehavior.expand] so it is
     * serialized with settle/collapse rather than racing them on the shared offset.
     */
    fun forceExpand() {
        coroutineScope.launch { scrollBehavior.expand() }
    }

    /**
     * Force the toolbar to collapse. Routed through [BrowserToolbarScrollBehavior.collapse] for the
     * same serialization.
     */
    fun forceCollapse() {
        coroutineScope.launch { scrollBehavior.collapse() }
    }
}
