/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.browsingmode

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.utils.Settings

@RunWith(AndroidJUnit4::class)
class BrowsingModePersistenceMiddlewareTest {

    private lateinit var settings: Settings
    private lateinit var store: AppStore

    @Before
    fun setup() {
        settings = mockk(relaxed = true)
        store =
            AppStore(
                initialState = AppState(mode = BrowsingMode.Normal),
                middlewares = listOf(BrowsingModePersistenceMiddleware(settings)),
            )
    }

    @Test
    fun `WHEN the browsing mode changes THEN it is saved as the last known mode before the state changes`() {
        var modeWhenSaved: BrowsingMode? = null
        every { settings.lastKnownMode = any() } answers { modeWhenSaved = store.state.mode }

        store.dispatch(AppAction.BrowsingModeChanged(BrowsingMode.Private))

        verify { settings.lastKnownMode = BrowsingMode.Private }
        assertEquals(BrowsingMode.Normal, modeWhenSaved)
        assertEquals(BrowsingMode.Private, store.state.mode)
    }

    @Test
    fun `WHEN the current browsing mode is published again THEN it is saved again`() {
        store.dispatch(AppAction.BrowsingModeChanged(BrowsingMode.Normal))
        store.dispatch(AppAction.BrowsingModeChanged(BrowsingMode.Normal))

        verify(exactly = 2) { settings.lastKnownMode = BrowsingMode.Normal }
    }

    @Test
    fun `WHEN another action is dispatched THEN the last known mode is not saved`() {
        store.dispatch(AppAction.MainBrowserMovedToForeground)

        verify(exactly = 0) { settings.lastKnownMode = any() }
    }
}
