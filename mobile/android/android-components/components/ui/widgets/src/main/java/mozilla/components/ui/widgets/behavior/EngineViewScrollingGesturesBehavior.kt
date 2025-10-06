/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.ui.widgets.behavior

import android.view.MotionEvent
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import mozilla.components.concept.base.crash.CrashReporting
import mozilla.components.concept.engine.EngineView

/**
 * A [CoordinatorLayout.Behavior] implementation to be used for moving [dependency] up/down
 * depending on scroll events in [engineView].
 *
 * This is safe to use even if [dependency] has it's visibility modified.
 *
 * This implementation will:
 * - Show/Hide the [View] automatically when scrolling vertically.
 * - Snap the [View] to be hidden or visible when the user stops scrolling.
 */
class EngineViewScrollingGesturesBehavior(
    private val engineView: EngineView,
    private val dependency: View,
    dependencyGravity: DependencyGravity,
    private val crashReporting: CrashReporting? = null,
) : EngineViewScrollingBehavior(engineView, dependency, dependencyGravity) {
    // This implementation is heavily based on this blog article:
    // https://android.jlelse.eu/scroll-your-bottom-navigation-view-away-with-10-lines-of-code-346f1ed40e9e

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var shouldSnapAfterScroll: Boolean = false

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var startedScroll = false

    /**
     * Depending on how user's touch was consumed by EngineView / current website,
     *
     * we will animate the dynamic navigation bar if:
     * - touches were used for zooming / panning operations in the website.
     *
     * We will do nothing if:
     * - the website is not scrollable
     * - the website handles the touch events itself through it's own touch event listeners.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal val shouldScroll: Boolean
        get() = engineView.getInputResultDetail().let {
            (it.canScrollToBottom() || it.canScrollToTop()) && isScrollEnabled
        }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal var gesturesDetector: BrowserGestureDetector = createGestureDetector()

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int,
    ): Boolean = when (dependency.isVisible) {
        true -> startNestedScroll(axes, type, child)
        false -> false // not interested in subsequent scroll events
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: View,
        target: View,
        type: Int,
    ) {
        if (dependency.isVisible) {
            stopNestedScroll(type, child)
        }
    }

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: View,
        ev: MotionEvent,
    ): Boolean {
        if (dependency.isVisible) {
            gesturesDetector.handleTouchEvent(ev)
        }

        return false // allow events to be passed to below listeners
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun tryToScrollVertically(distance: Float) {
        if (shouldScroll && startedScroll) {
            yTranslator.translate(dependency, distance)
        } else if (!engineView.getInputResultDetail().isTouchHandlingUnknown()) {
            // Force expand the view if the user scrolled up, it is not already expanded and
            // an animation to expand it is not already in progress,
            // otherwise the user could get stuck in a state where they cannot show the view
            // See https://github.com/mozilla-mobile/android-components/issues/7101
            yTranslator.forceExpandIfNotAlready(dependency, distance)
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun createGestureDetector() =
        BrowserGestureDetector(
            engineView.asView().context,
            BrowserGestureDetector.GesturesListener(
                onVerticalScroll = ::tryToScrollVertically,
                onScaleBegin = {
                    // Scale shouldn't animate the view but a small y translation is still possible
                    // because of a previous scroll. Try to be swift about such an in progress animation.
                    yTranslator.snapImmediately(dependency)
                },
            ),
            crashReporting = crashReporting,
        )

    @VisibleForTesting
    internal fun startNestedScroll(axes: Int, type: Int, view: View): Boolean {
        return if (shouldScroll && axes == ViewCompat.SCROLL_AXIS_VERTICAL) {
            startedScroll = true
            shouldSnapAfterScroll = type == ViewCompat.TYPE_TOUCH
            yTranslator.cancelInProgressTranslation()
            true
        } else if (engineView.getInputResultDetail().isTouchUnhandled()) {
            // Force expand the view if event is unhandled, otherwise user could get stuck in a
            // state where they cannot show the view
            yTranslator.cancelInProgressTranslation()
            yTranslator.expandWithAnimation(view)
            false
        } else {
            false
        }
    }

    @VisibleForTesting
    internal fun stopNestedScroll(type: Int, view: View) {
        startedScroll = false
        if (shouldSnapAfterScroll || type == ViewCompat.TYPE_NON_TOUCH) {
            yTranslator.snapWithAnimation(view)
        }
    }
}
