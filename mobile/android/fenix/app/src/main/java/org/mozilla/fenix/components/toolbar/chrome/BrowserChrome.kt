/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar.chrome

import android.view.View
import mozilla.components.concept.toolbar.ScrollableToolbar
import org.mozilla.fenix.FeatureFlags.USE_NEW_CHROME_HOST
import org.mozilla.fenix.components.toolbar.ToolbarPosition
import org.mozilla.fenix.components.toolbar.scroll.BrowserToolbarScrollBehavior

/**
 * The browser chrome as the fragment sees it: the toolbar (and navigation bar) plus their scroll
 * behavior and visibility, regardless of whether it is realized as the separate per-bar
 * ComposeViews or the single [BrowserChromeHost] scaffold.
 *
 * Both implementations expose this so the hosting fragment can be written against one type and the
 * chrome structure can be swapped behind [USE_NEW_CHROME_HOST].
 */
interface BrowserChrome : ScrollableToolbar {
    /**
     * The root [View] added to the browser layout.
     */
    val layout: View

    /**
     * Shared scroll behavior / offset driving the dynamic bars.
     */
    val scrollBehavior: BrowserToolbarScrollBehavior?

    /**
     * Make the chrome visible.
     */
    fun visible()

    /**
     * Hide the chrome (e.g. entering fullscreen).
     */
    fun gone()

    /**
     * (Re)configure how the chrome responds to scrolling for the given [toolbarPosition], optionally
     * forcing scrolling off via [shouldDisableScroll].
     */
    fun setToolbarBehavior(toolbarPosition: ToolbarPosition, shouldDisableScroll: Boolean = false)
}
