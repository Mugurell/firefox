/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.browsingmode

import android.content.Intent
import mozilla.components.browser.state.selector.findTabOrCustomTab
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.utils.toSafeIntent
import org.mozilla.fenix.HomeActivity.Companion.PRIVATE_BROWSING_MODE
import org.mozilla.fenix.utils.Settings

/**
 * Tells whether currently browsing in private mode, based on whether the tab it shows is private or not.
 *
 * @param browserStore The [BrowserStore] holding the tabs and custom tabs.
 * @param settings The [Settings] holding the last known browsing mode of the regular tabs activity.
 */
class BrowsingMode(
    private val browserStore: BrowserStore,
    private val settings: Settings,
) {
    /**
     * Whether currently showing a private tab, based on
     * - the private mode of the custom tab identified by [customTabSessionId], or
     * - the private mode requested by [intent], or
     * - the private mode of the currently selected tab when homepage as a new tab is enabled, or
     * - the last known mode if none of the above is known.
     *
     * @param customTabSessionId The ID of the custom tab shown or `null` when a regular tab is currently displayed.
     * @param intent The [Intent] that may request a private mode, or `null` to ignore any request.
     */
    fun isPrivate(customTabSessionId: String?, intent: Intent? = null): Boolean =
        customTabSessionId?.let { browserStore.state.findTabOrCustomTab(it) }?.content?.private
            ?: intent?.requestedPrivacy()
            ?: browserStore.state.selectedTab?.content?.private?.takeIf { settings.enableHomepageAsNewTab }
            ?: settings.lastKnownModeIsPrivate

    private fun Intent.requestedPrivacy(): Boolean? =
        toSafeIntent().takeIf { it.hasExtra(PRIVATE_BROWSING_MODE) }?.getBooleanExtra(PRIVATE_BROWSING_MODE, false)
}
