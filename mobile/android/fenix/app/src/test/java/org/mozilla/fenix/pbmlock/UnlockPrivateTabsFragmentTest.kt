/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.pbmlock

import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import mozilla.components.browser.state.engine.EngineMiddleware
import mozilla.components.browser.state.selector.normalTabs
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.utils.ABOUT_HOME_URL
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mozilla.fenix.components.usecases.FenixBrowserUseCases

class UnlockPrivateTabsFragmentTest {

    @Test
    fun `GIVEN normal tabs WHEN leaving the private tabs THEN the most recently used normal tab is selected and no tab is added`() {
        val store =
            BrowserStore(
                BrowserState(
                    tabs =
                        listOf(
                            createTab(url = "https://www.mozilla.org", id = "older", lastAccess = 1),
                            createTab(url = "https://www.firefox.com", id = "recent", lastAccess = 2),
                            createTab(url = "https://example.com", id = "private", private = true, lastAccess = 3),
                        ),
                    selectedTabId = "private",
                )
            )

        leavePrivateTabs(store)

        assertEquals("recent", store.state.selectedTabId)
        assertEquals(3, store.state.tabs.size)
    }

    @Test
    fun `GIVEN only private tabs WHEN leaving the private tabs THEN a normal homepage tab is added and selected`() {
        val store =
            BrowserStore(
                BrowserState(
                    tabs = listOf(createTab(url = "https://example.com", id = "private", private = true)),
                    selectedTabId = "private",
                ),
                middleware = EngineMiddleware.create(engine = mockk(), scope = TestScope()),
            )

        leavePrivateTabs(store)

        val selectedTab = store.state.selectedTab!!
        assertEquals(2, store.state.tabs.size)
        assertEquals(ABOUT_HOME_URL, selectedTab.content.url)
        assertFalse(selectedTab.content.private)
    }

    private fun leavePrivateTabs(store: BrowserStore) {
        val tabsUseCases = TabsUseCases(store)
        showNormalTab(
            normalTabs = store.state.normalTabs,
            tabsUseCases = tabsUseCases,
            fenixBrowserUseCases =
                FenixBrowserUseCases(
                    isPrivate = { false },
                    tabsUseCases = tabsUseCases,
                    loadUrlUseCase = SessionUseCases(store).loadUrl,
                    searchUseCases = mockk(relaxed = true),
                    homepageTitle = "Homepage",
                    profiler = null,
                ),
        )
    }
}
