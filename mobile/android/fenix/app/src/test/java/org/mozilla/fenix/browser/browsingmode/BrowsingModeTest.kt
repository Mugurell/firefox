/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.browsingmode

import android.content.Intent
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
import org.mozilla.fenix.HomeActivity.Companion.PRIVATE_BROWSING_MODE
import org.mozilla.fenix.utils.Settings

@RunWith(AndroidJUnit4::class)
class BrowsingModeTest {
    private val privateTab = createTab(url = "https://www.mozilla.org", id = "private", private = true)
    private val normalCustomTab = createCustomTab(url = "https://www.mozilla.org", id = "custom")

    @Test
    fun `GIVEN a normal custom tab and a private selected tab WHEN asking for the custom tab's privacy THEN it is normal`() {
        val browsingMode = createBrowsingMode(lastKnownModeIsPrivate = true, homepageAsNewTab = true)

        assertFalse(browsingMode.isPrivate(customTabSessionId = normalCustomTab.id))
    }

    @Test
    fun `GIVEN a normal custom tab and an intent requesting private mode WHEN asking for the custom tab's privacy THEN it is normal`() {
        val browsingMode = createBrowsingMode(lastKnownModeIsPrivate = true)

        assertFalse(
            browsingMode.isPrivate(
                customTabSessionId = normalCustomTab.id,
                intent = Intent().putExtra(PRIVATE_BROWSING_MODE, true),
            )
        )
    }

    @Test
    fun `GIVEN the custom tab is gone and a private last known mode WHEN asking for its privacy THEN it is private`() {
        val browsingMode = createBrowsingMode(lastKnownModeIsPrivate = true, selectedTabId = null)

        assertTrue(browsingMode.isPrivate(customTabSessionId = "gone"))
    }

    @Test
    fun `GIVEN homepage as a new tab, a private selected tab and a normal last known mode WHEN asking for the regular tabs activity's privacy THEN it is private`() {
        val browsingMode = createBrowsingMode(lastKnownModeIsPrivate = false, homepageAsNewTab = true)

        assertTrue(browsingMode.isPrivate(customTabSessionId = null))
    }

    @Test
    fun `GIVEN homepage as a new tab is disabled, a private selected tab and a normal last known mode WHEN asking for the regular tabs activity's privacy THEN it is normal`() {
        val browsingMode = createBrowsingMode(lastKnownModeIsPrivate = false, homepageAsNewTab = false)

        assertFalse(browsingMode.isPrivate(customTabSessionId = null))
    }

    @Test
    fun `GIVEN homepage as a new tab, no selected tab and a private last known mode WHEN asking for the regular tabs activity's privacy THEN it is private`() {
        val browsingMode =
            createBrowsingMode(lastKnownModeIsPrivate = true, homepageAsNewTab = true, selectedTabId = null)

        assertTrue(browsingMode.isPrivate(customTabSessionId = null))
    }

    @Test
    fun `GIVEN homepage as a new tab and a private selected tab WHEN an intent requests normal mode THEN it is normal`() {
        val browsingMode = createBrowsingMode(lastKnownModeIsPrivate = true, homepageAsNewTab = true)

        assertFalse(
            browsingMode.isPrivate(customTabSessionId = null, intent = Intent().putExtra(PRIVATE_BROWSING_MODE, false))
        )
    }

    @Test
    fun `GIVEN a normal last known mode WHEN an intent requests private mode THEN it is private`() {
        val browsingMode = createBrowsingMode(lastKnownModeIsPrivate = false)

        assertTrue(
            browsingMode.isPrivate(customTabSessionId = null, intent = Intent().putExtra(PRIVATE_BROWSING_MODE, true))
        )
    }

    @Test
    fun `GIVEN a private last known mode WHEN an intent requests no mode THEN it is private`() {
        val browsingMode = createBrowsingMode(lastKnownModeIsPrivate = true)

        assertTrue(browsingMode.isPrivate(customTabSessionId = null, intent = Intent()))
    }

    private fun createBrowsingMode(
        lastKnownModeIsPrivate: Boolean,
        homepageAsNewTab: Boolean = false,
        selectedTabId: String? = privateTab.id,
    ): BrowsingMode {
        val settings =
            Settings(testContext).apply {
                this.lastKnownModeIsPrivate = lastKnownModeIsPrivate
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
        return BrowsingMode(browserStore, settings)
    }
}
