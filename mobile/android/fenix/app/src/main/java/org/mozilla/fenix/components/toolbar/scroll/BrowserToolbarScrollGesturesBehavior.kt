/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar.scroll

import android.content.Context
import android.view.View
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
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
        coroutineScope.launch {
            val available = Velocity(0f, -velocityY)
            val consumed = scrollBehavior.nestedScrollConnection.onPreFling(available)
            scrollBehavior.nestedScrollConnection.onPostFling(consumed, available - consumed)
        }
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
     * Force the toolbar to expand.
     */
    fun forceExpand() {
        val spec = scrollBehavior.snapAnimationSpec ?: run {
            scrollBehavior.state.heightOffset = 0f
            return
        }
        coroutineScope.launch {
            scrollBehavior.state.animateTo(0f, spec)
        }
    }

    /**
     * Force the toolbar to collapse.
     */
    fun forceCollapse() {
        val spec = scrollBehavior.snapAnimationSpec ?: run {
            scrollBehavior.state.heightOffset = scrollBehavior.state.heightOffsetLimit
            return
        }
        coroutineScope.launch {
            scrollBehavior.state.animateTo(scrollBehavior.state.heightOffsetLimit, spec)
        }
    }
}
