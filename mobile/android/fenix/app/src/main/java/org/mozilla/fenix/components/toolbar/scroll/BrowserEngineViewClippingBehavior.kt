/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar.scroll

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.coordinatorlayout.widget.CoordinatorLayout
import mozilla.components.concept.engine.EngineView
import mozilla.components.support.ktx.android.view.findViewInHierarchy
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A modification of [mozilla.components.ui.widgets.behavior.EngineViewClippingBehavior] that
 * observes a [BrowserToolbarScrollBehavior] instead of reading [View.translationY].
 *
 * Everything is derived from [BrowserToolbarScrollState] (the single source of truth): the reserve
 * reported to Gecko, the engine-view parent translation, and the vertical clipping. Because the
 * state exposes per-edge *clamped* offsets, the total clipping is a sum of two independently-bounded
 * terms and can never exceed the reserve, for any top/bottom bar heights — so no clamping or snap
 * guard is needed here.
 *
 * Updates are applied through a synchronous listener on the state (rather than a `snapshotFlow`) so
 * the engine-view stays in lockstep with the bars' Compose translation frame-for-frame.
 *
 * @param context [Context] for various Android interactions.
 * @param attrs XML attributes configuring this behavior.
 * @param engineViewParent The parent [View] of the [EngineView].
 * @param scrollBehavior The [BrowserToolbarScrollBehavior] to observe.
 */
class BrowserEngineViewClippingBehavior(
    context: Context,
    attrs: AttributeSet?,
    private val engineViewParent: View,
    private val scrollBehavior: BrowserToolbarScrollBehavior,
) : CoordinatorLayout.Behavior<View>(context, attrs) {

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var engineView = engineViewParent.findViewInHierarchy { it is EngineView } as EngineView?

    private val changeListener: () -> Unit = ::applyUpdates
    private var lastDynamicToolbarMaxHeight = Int.MIN_VALUE

    init {
        scrollBehavior.state.addOnChangeListener(changeListener)
        // Apply the current state immediately so the engine view is positioned correctly on attach.
        applyUpdates()
    }

    override fun onDetachedFromLayoutParams() {
        scrollBehavior.state.removeOnChangeListener(changeListener)
        super.onDetachedFromLayoutParams()
    }

    private fun applyUpdates() {
        val engine = engineView ?: return
        val state = scrollBehavior.state

        val topBarHeight = state.topBarHeight
        val bottomBarHeight = state.bottomBarHeight

        // Reserve = total collapsible chrome. Set only when it changes, not every scroll frame.
        val dynamicToolbarMaxHeight = (topBarHeight + bottomBarHeight).roundToInt()
        if (dynamicToolbarMaxHeight != lastDynamicToolbarMaxHeight) {
            engine.setDynamicToolbarMaxHeight(dynamicToolbarMaxHeight)
            lastDynamicToolbarMaxHeight = dynamicToolbarMaxHeight
        }

        // A top bar pushes the engine content down by its resting height; as it collapses (topOffset
        // goes negative) the content slides back up.
        if (topBarHeight > 0f) {
            engineViewParent.translationY = state.topOffset + topBarHeight
        }

        // Content obscured at the bottom = how much each edge is currently hidden. Each term is
        // clamped to its own bar's height by the state, so the sum is always within the reserve.
        val contentBottomClipping = -(abs(state.topOffset) + abs(state.bottomOffset)).roundToInt()
        engine.setVerticalClipping(contentBottomClipping)
    }
}
