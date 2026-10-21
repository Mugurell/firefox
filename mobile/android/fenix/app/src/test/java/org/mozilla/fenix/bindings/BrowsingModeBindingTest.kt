/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.bindings

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState

@RunWith(AndroidJUnit4::class)
class BrowsingModeBindingTest {
    private val testDispatcher = StandardTestDispatcher()
    private val reportedModes = mutableListOf<BrowsingMode>()

    @Test
    fun `GIVEN the mode is already applied WHEN the binding starts THEN nothing is reported`() =
        runTest(testDispatcher) {
            val appStore = AppStore(AppState(mode = BrowsingMode.Private))

            createBinding(appStore, initialMode = BrowsingMode.Private).start()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(emptyList<BrowsingMode>(), reportedModes)
        }

    @Test
    fun `WHEN a new mode is published THEN it is reported once`() =
        runTest(testDispatcher) {
            val appStore = AppStore(AppState(mode = BrowsingMode.Normal))
            createBinding(appStore, initialMode = BrowsingMode.Normal).start()
            testDispatcher.scheduler.advanceUntilIdle()

            appStore.dispatch(AppAction.BrowsingModeChanged(BrowsingMode.Private))
            testDispatcher.scheduler.advanceUntilIdle()
            appStore.dispatch(AppAction.BrowsingModeChanged(BrowsingMode.Private))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf(BrowsingMode.Private), reportedModes)
        }

    @Test
    fun `WHEN another action changes the state THEN nothing is reported`() =
        runTest(testDispatcher) {
            val appStore = AppStore(AppState(mode = BrowsingMode.Normal, lastForegroundCustomTabId = "customTab"))
            createBinding(appStore, initialMode = BrowsingMode.Normal).start()
            testDispatcher.scheduler.advanceUntilIdle()

            appStore.dispatch(AppAction.MainBrowserMovedToForeground)
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(emptyList<BrowsingMode>(), reportedModes)
        }

    @Test
    fun `GIVEN the binding is stopped WHEN a new mode is published THEN it is reported on the next start`() =
        runTest(testDispatcher) {
            val appStore = AppStore(AppState(mode = BrowsingMode.Normal))
            val binding = createBinding(appStore, initialMode = BrowsingMode.Normal)
            binding.start()
            testDispatcher.scheduler.advanceUntilIdle()
            binding.stop()

            appStore.dispatch(AppAction.BrowsingModeChanged(BrowsingMode.Private))
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(emptyList<BrowsingMode>(), reportedModes)

            binding.start()
            testDispatcher.scheduler.advanceUntilIdle()

            assertEquals(listOf(BrowsingMode.Private), reportedModes)
        }

    @Test
    fun `WHEN a new mode is published on the main thread THEN the immediate dispatcher reports it right away`() {
        val appStore = AppStore(AppState(mode = BrowsingMode.Normal))
        createBinding(appStore, initialMode = BrowsingMode.Normal, mainDispatcher = Dispatchers.Main.immediate).start()

        appStore.dispatch(AppAction.BrowsingModeChanged(BrowsingMode.Private))

        assertEquals(listOf(BrowsingMode.Private), reportedModes)
    }

    private fun createBinding(
        appStore: AppStore,
        initialMode: BrowsingMode,
        mainDispatcher: CoroutineDispatcher = testDispatcher,
    ) =
        BrowsingModeBinding(
            appStore = appStore,
            initialMode = initialMode,
            onModeChanged = { reportedModes.add(it) },
            mainDispatcher = mainDispatcher,
        )
}
