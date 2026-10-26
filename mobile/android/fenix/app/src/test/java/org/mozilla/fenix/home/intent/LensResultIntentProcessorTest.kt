/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.intent

import android.content.Intent
import androidx.navigation.NavController
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.EngineSession
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.utils.Settings
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LensResultIntentProcessorTest {

    private val activity: HomeActivity = mockk(relaxed = true)
    private val navController: NavController = mockk(relaxed = true)
    private val out: Intent = mockk(relaxed = true)
    private val settings: Settings = mockk()

    @Test
    fun `GIVEN no lens result extra WHEN process is called THEN it returns false and does nothing`() {
        val processed = LensResultIntentProcessor(activity).process(Intent(), navController, out, settings)

        assertFalse(processed)
        verify { activity wasNot Called }
        verify { out wasNot Called }
    }

    @Test
    fun `GIVEN open to browser without a lens result url WHEN process is called THEN it returns false`() {
        val intent = Intent().apply { putExtra(HomeActivity.OPEN_TO_BROWSER_AND_LOAD, true) }

        val processed = LensResultIntentProcessor(activity).process(intent, navController, out, settings)

        assertFalse(processed)
        verify { activity wasNot Called }
    }

    @Test
    fun `GIVEN the lens result url WHEN process is called THEN it loads the url in a new tab and clears the extra`() {
        val intent =
            Intent().apply {
                putExtra(HomeActivity.OPEN_TO_BROWSER_AND_LOAD, true)
                putExtra(HomeActivity.LENS_RESULT_URL, "https://lens.google.com/results")
            }
        every { activity.applicationContext } returns testContext

        val processed = LensResultIntentProcessor(activity).process(intent, navController, out, settings)

        assertTrue(processed)
        verify { out.putExtra(HomeActivity.OPEN_TO_BROWSER_AND_LOAD, false) }
        verify {
            @Suppress("DEPRECATION")
            activity.openToBrowserAndLoad(
                searchTermOrURL = "https://lens.google.com/results",
                newTab = true,
                from = BrowserDirection.FromGlobal,
                forceSearch = false,
                flags = EngineSession.LoadUrlFlags.external(),
                private = false,
            )
        }
    }

    @Test
    fun `GIVEN the lens result url and an intent asking for private mode WHEN process is called THEN it loads the url in a new private tab`() {
        val intent =
            Intent().apply {
                putExtra(HomeActivity.OPEN_TO_BROWSER_AND_LOAD, true)
                putExtra(HomeActivity.LENS_RESULT_URL, "https://lens.google.com/results")
                putExtra(HomeActivity.PRIVATE_BROWSING_MODE, true)
            }
        every { activity.applicationContext } returns testContext
        every { testContext.components.browsingMode } returns BrowsingMode(BrowserStore(), Settings(testContext))

        LensResultIntentProcessor(activity).process(intent, navController, out, settings)

        verify {
            @Suppress("DEPRECATION")
            activity.openToBrowserAndLoad(
                searchTermOrURL = "https://lens.google.com/results",
                newTab = true,
                from = BrowserDirection.FromGlobal,
                forceSearch = false,
                flags = EngineSession.LoadUrlFlags.external(),
                private = true,
            )
        }
    }
}
