/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.browsingmode

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.HomeActivity.Companion.PRIVATE_BROWSING_MODE
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.utils.Settings

@RunWith(AndroidJUnit4::class)
class AppStoreBrowsingModeManagerTest {
    private val dispatchedActions = mutableListOf<AppAction>()
    private val recordActionsMiddleware =
        object : Middleware<AppState, AppAction> {
            override fun invoke(store: Store<AppState, AppAction>, next: (AppAction) -> Unit, action: AppAction) {
                dispatchedActions.add(action)
                next(action)
            }
        }
    private val settings: Settings = mockk { every { lastKnownMode } returns BrowsingMode.Normal }

    @Test
    fun `WHEN reading the mode THEN the current mode of the app state is returned`() {
        val appStore = AppStore(AppState(mode = BrowsingMode.Private))
        val manager = createManager(appStore)

        assertEquals(BrowsingMode.Private, manager.mode)

        appStore.dispatch(AppAction.BrowsingModeChanged(BrowsingMode.Normal))

        assertEquals(BrowsingMode.Normal, manager.mode)
    }

    @Test
    fun `WHEN setting the mode THEN it is dispatched to the app state even if it did not change`() {
        val appStore = AppStore(AppState(mode = BrowsingMode.Normal), listOf(recordActionsMiddleware))
        val manager = createManager(appStore)

        manager.mode = BrowsingMode.Private
        manager.mode = BrowsingMode.Private

        assertEquals(BrowsingMode.Private, appStore.state.mode)
        assertEquals(
            listOf(
                AppAction.BrowsingModeChanged(BrowsingMode.Private),
                AppAction.BrowsingModeChanged(BrowsingMode.Private),
            ),
            dispatchedActions,
        )
    }

    @Test
    fun `GIVEN an intent asking for private browsing WHEN updating the mode THEN private mode is dispatched`() {
        val appStore = AppStore(AppState(mode = BrowsingMode.Normal))
        val manager = createManager(appStore)

        manager.updateMode(Intent().putExtra(PRIVATE_BROWSING_MODE, true))

        assertEquals(BrowsingMode.Private, appStore.state.mode)
    }

    @Test
    fun `GIVEN an intent asking for normal browsing WHEN updating the mode THEN normal mode is dispatched`() {
        every { settings.lastKnownMode } returns BrowsingMode.Private
        val appStore = AppStore(AppState(mode = BrowsingMode.Private))
        val manager = createManager(appStore)

        manager.updateMode(Intent().putExtra(PRIVATE_BROWSING_MODE, false))

        assertEquals(BrowsingMode.Normal, appStore.state.mode)
    }

    @Test
    fun `GIVEN no intent and a private last known mode WHEN updating the mode THEN private mode is dispatched`() {
        every { settings.lastKnownMode } returns BrowsingMode.Private
        val appStore = AppStore(AppState(mode = BrowsingMode.Normal))
        val manager = createManager(appStore)

        manager.updateMode(null)

        assertEquals(BrowsingMode.Private, appStore.state.mode)
    }

    private fun createManager(appStore: AppStore) =
        AppStoreBrowsingModeManager(appStore = appStore, settings = settings)
}
