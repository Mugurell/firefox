/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components

import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.utils.Settings

@RunWith(AndroidJUnit4::class)
class ActivityPrivacyTest {
    private val privateTab = createTab(url = "https://www.mozilla.org", id = "private", private = true)
    private val normalCustomTab = createCustomTab(url = "https://www.mozilla.org", id = "custom")

    @Test
    fun `GIVEN a normal custom tab and a private selected tab WHEN asking for the custom tab's privacy THEN it is normal`() {
        val activityPrivacy = createActivityPrivacy(lastKnownMode = BrowsingMode.Private, homepageAsNewTab = true)

        assertFalse(activityPrivacy.isPrivate(customTabSessionId = normalCustomTab.id))
    }

    @Test
    fun `GIVEN the custom tab is gone and a private last known mode WHEN asking for its privacy THEN it is private`() {
        val activityPrivacy = createActivityPrivacy(lastKnownMode = BrowsingMode.Private, selectedTabId = null)

        assertTrue(activityPrivacy.isPrivate(customTabSessionId = "gone"))
    }

    @Test
    fun `GIVEN homepage as a new tab, a private selected tab and a normal last known mode WHEN asking for the regular tabs activity's privacy THEN it is private`() {
        val activityPrivacy = createActivityPrivacy(lastKnownMode = BrowsingMode.Normal, homepageAsNewTab = true)

        assertTrue(activityPrivacy.isPrivate(customTabSessionId = null))
    }

    @Test
    fun `GIVEN homepage as a new tab is disabled, a private selected tab and a normal last known mode WHEN asking for the regular tabs activity's privacy THEN it is normal`() {
        val activityPrivacy = createActivityPrivacy(lastKnownMode = BrowsingMode.Normal, homepageAsNewTab = false)

        assertFalse(activityPrivacy.isPrivate(customTabSessionId = null))
    }

    @Test
    fun `GIVEN homepage as a new tab, no selected tab and a private last known mode WHEN asking for the regular tabs activity's privacy THEN it is private`() {
        val activityPrivacy =
            createActivityPrivacy(lastKnownMode = BrowsingMode.Private, homepageAsNewTab = true, selectedTabId = null)

        assertTrue(activityPrivacy.isPrivate(customTabSessionId = null))
    }

    private fun createActivityPrivacy(
        lastKnownMode: BrowsingMode,
        homepageAsNewTab: Boolean = false,
        selectedTabId: String? = privateTab.id,
    ): ActivityPrivacy {
        val settings =
            Settings(testContext).apply {
                this.lastKnownMode = lastKnownMode
                enableHomepageAsNewTab = homepageAsNewTab
            }
        val browserStore =
            BrowserStore(
                BrowserState(
                    tabs = listOf(privateTab),
                    customTabs = listOf(normalCustomTab),
                    selectedTabId = selectedTabId,
                )
            )
        return ActivityPrivacy(browserStore, settings)
    }
}
