/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.store.BrowserStore
import org.mozilla.fenix.utils.Settings

/**
 * Tells whether currently browsing in private mode, based on whether the tab it shows is private or not.
 *
 * @param browserStore The [BrowserStore] holding the tabs and custom tabs.
 * @param settings The [Settings] holding the last known browsing mode of the regular tabs activity.
 */
class ActivityPrivacy(
    private val browserStore: BrowserStore,
    private val settings: Settings,
) {
    /**
     * Whether currently showing a private tab, based on
     * - the private mode of the currently selected tab or the custom tab identified by [customTabSessionId] or
     * - the last known mode if the tabs above could not be found.
     *
     * @param customTabSessionId The ID of the custom tab shown or `null` when a regular tab is currently displayed.
     */
    fun isPrivate(customTabSessionId: String?): Boolean =
        customTabSessionId?.let { browserStore.state.findTabOrCustomTab(it) }?.content?.private
            ?: settings.lastKnownMode.isPrivate
}
