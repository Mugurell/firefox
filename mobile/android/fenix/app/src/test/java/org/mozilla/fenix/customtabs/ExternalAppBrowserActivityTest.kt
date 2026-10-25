/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.customtabs

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import io.mockk.Called
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlin.test.assertNotNull
import mozilla.components.browser.state.state.ActiveOptionsPage
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.WebExtensionState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.browser.state.state.createTab
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.feature.intent.ext.putSessionId
import mozilla.components.support.test.robolectric.testContext
import mozilla.components.support.utils.toSafeIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.BrowserDirection
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.browser.browsingmode.BrowsingModeManager
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.getIntentSource
import org.mozilla.fenix.ext.getNavDirections
import org.mozilla.fenix.helpers.perf.TestStrictModeManager
import org.mozilla.fenix.utils.Settings
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExternalAppBrowserActivityTest {

    @Test
    fun getIntentSource() {
        val activity = ExternalAppBrowserActivity()

        val launcherIntent =
            Intent(Intent.ACTION_MAIN)
                .apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                .toSafeIntent()
        assertEquals("CUSTOM_TAB", activity.getIntentSource(launcherIntent))

        val viewIntent = Intent(Intent.ACTION_VIEW).toSafeIntent()
        assertEquals("CUSTOM_TAB", activity.getIntentSource(viewIntent))

        val otherIntent = Intent().toSafeIntent()
        assertEquals("CUSTOM_TAB", activity.getIntentSource(otherIntent))
    }

    @Test
    fun `navigateToBrowserOnColdStart does nothing for external app browser activity`() {
        val activity = spyk(ExternalAppBrowserActivity())
        val browsingModeManager: BrowsingModeManager = mockk()
        every { browsingModeManager.mode } returns BrowsingMode.Normal

        val settings: Settings = mockk()
        every { settings.shouldReturnToBrowser } returns true
        every { activity.components.settings.shouldReturnToBrowser } returns true
        every { activity.openToBrowser(any(), any()) } just Runs

        activity.browsingModeManager = browsingModeManager
        activity.navigateToBrowserOnColdStart()

        verify(exactly = 0) { activity.openToBrowser(BrowserDirection.FromGlobal, null) }
    }

    @Test
    fun `navigateToHome does nothing for external app browser activity`() {
        val activity = spyk(ExternalAppBrowserActivity())
        val navHostController: NavController = mockk()

        activity.navigateToHome(navHostController)
        verify { navHostController wasNot Called }
    }

    @Test
    fun `handleNewIntent does nothing for external app browser activity`() {
        val activity = spyk(ExternalAppBrowserActivity())
        val intent: Intent = mockk(relaxed = true)

        activity.handleNewIntent(intent)
        verify { intent wasNot Called }
    }

    @Test
    fun `WHEN addAboutHomeBinding is invoked THEN do nothing`() {
        val activity = spyk(ExternalAppBrowserActivity())
        val lifecycle: Lifecycle = mockk()

        activity.addAboutHomeBinding(lifecycle)
        verify { lifecycle wasNot Called }
    }

    @Test
    fun `getNavDirections finishes activity if session ID is null`() {
        val activity =
            spyk(
                object : ExternalAppBrowserActivity() {
                    override fun getIntent(): Intent {
                        val intent: Intent = mockk()
                        val bundle: Bundle = mockk()
                        every { bundle.getString(any()) } returns ""
                        every { intent.extras } returns bundle
                        every { intent.getBooleanExtra(any(), any()) } returns false
                        every { intent.dataString } returns null

                        return intent
                    }
                }
            )

        var directions = activity.getNavDirections(BrowserDirection.FromGlobal, "id")
        assertNotNull(directions)
        verify(exactly = 0) { activity.finishAndRemoveTask() }

        directions = activity.getNavDirections(BrowserDirection.FromGlobal, null)
        assertNull(directions)
        verify { activity.finishAndRemoveTask() }
    }

    @Test
    fun `GIVEN intent isSandboxCustomTab is true WHEN getNavDirections called THEN actionGlobalExternalAppBrowser isSandboxCustomTab is true`() {
        val activity =
            spyk(
                object : ExternalAppBrowserActivity() {
                    override fun getIntent(): Intent {
                        val intent: Intent = mockk()
                        val bundle: Bundle = mockk()
                        every { bundle.getString(any()) } returns ""
                        every { intent.getBooleanExtra(any(), any()) } returns true
                        every { intent.extras } returns bundle
                        every { intent.dataString } returns null

                        return intent
                    }
                }
            )

        val customTabSessionId = "id"
        val directions = activity.getNavDirections(BrowserDirection.FromGlobal, customTabSessionId)
        assertNotNull(directions)
        verify(exactly = 0) { activity.finishAndRemoveTask() }

        val expected =
            NavGraphDirections.actionGlobalExternalAppBrowser(
                activeSessionId = customTabSessionId,
                webAppManifestUrl = null,
                isSandboxCustomTab = true,
            )
        assertEquals(expected, directions)
    }

    @Test
    fun `GIVEN intent isSandboxCustomTab is false WHEN getNavDirections called THEN actionGlobalExternalAppBrowser isSandboxCustomTab is false`() {
        val activity =
            spyk(
                object : ExternalAppBrowserActivity() {
                    override fun getIntent(): Intent {
                        val intent: Intent = mockk()
                        val bundle: Bundle = mockk()
                        every { bundle.getString(any()) } returns ""
                        every { intent.getBooleanExtra(any(), any()) } returns false
                        every { intent.extras } returns bundle
                        every { intent.dataString } returns null

                        return intent
                    }
                }
            )

        val customTabSessionId = "id"
        val directions = activity.getNavDirections(BrowserDirection.FromGlobal, customTabSessionId)
        assertNotNull(directions)
        verify(exactly = 0) { activity.finishAndRemoveTask() }

        val expected =
            NavGraphDirections.actionGlobalExternalAppBrowser(
                activeSessionId = customTabSessionId,
                webAppManifestUrl = null,
                isSandboxCustomTab = false,
            )
        assertEquals(expected, directions)
    }

    @Test
    fun `ExternalAppBrowserActivity with matching external tab`() {
        val store =
            BrowserStore(
                BrowserState(
                    customTabs =
                        listOf(
                            createCustomTab(
                                url = "https://www.mozilla.org",
                                id = "mozilla",
                            )
                        )
                )
            )

        val intent = Intent(Intent.ACTION_VIEW).apply { putSessionId("mozilla") }

        val activity = spyk(ExternalAppBrowserActivity())
        every { activity.components.core.store } returns store
        every { activity.intent } returns intent

        assertTrue(activity.hasExternalTab())

        assertEquals("mozilla", activity.getExternalTabId())

        val tab = activity.getExternalTab()
        assertNotNull(tab!!)
        assertEquals("https://www.mozilla.org", tab.content.url)
    }

    @Test
    fun `ExternalAppBrowserActivity without matching external tab`() {
        val store = BrowserStore()

        val intent = Intent(Intent.ACTION_VIEW).apply { putSessionId("mozilla") }

        val activity = spyk(ExternalAppBrowserActivity())
        every { activity.components.core.store } returns store
        every { activity.intent } returns intent

        assertFalse(activity.hasExternalTab())
        assertEquals("mozilla", activity.getExternalTabId())
        assertNull(activity.getExternalTab())
    }

    @Test
    fun `ExternalAppBrowserActivity with matching regular tab`() {
        val store =
            BrowserStore(
                BrowserState(
                    tabs =
                        listOf(
                            createTab(
                                url = "https://www.mozilla.org",
                                id = "mozilla",
                            )
                        )
                )
            )

        val intent = Intent(Intent.ACTION_VIEW).apply { putSessionId("mozilla") }

        val activity = spyk(ExternalAppBrowserActivity())
        every { activity.components.core.store } returns store
        every { activity.intent } returns intent

        // Even though we have a matching regular tab we do not care about it in ExternalAppBrowserActivity
        assertFalse(activity.hasExternalTab())
        assertEquals("mozilla", activity.getExternalTabId())
        assertNull(activity.getExternalTab())
    }

    @Test
    fun `GIVEN a custom tab activity WHEN an extension opens its options page THEN the options page gets the custom tab ID`() {
        val activeOptionsPage =
            ActiveOptionsPage(
                instanceId = "instanceId",
                url = "moz-extension://extensionId/options.html",
                name = "Test extension",
            )
        val extension = WebExtensionState(id = "extensionId", activeOptionsPage = activeOptionsPage)
        every { testContext.components.core.store } returns
            BrowserStore(BrowserState(extensions = mapOf(extension.id to extension)))
        every { testContext.components.strictMode } returns TestStrictModeManager()
        val intent = Intent(Intent.ACTION_VIEW).apply { putSessionId("custom-tab-id") }
        val activity = Robolectric.buildActivity(ExternalAppBrowserActivity::class.java, intent).get()

        val directions = activity.createOpenOptionsPageDirections(activeOptionsPage)

        assertEquals("custom-tab-id", directions?.arguments?.getString("customTabSessionId"))
    }

    @Test
    fun `GIVEN a private custom tab, a normal intent and normal last known mode WHEN its activity starts THEN it is private and blocks screenshots`() {
        val activity = buildCustomTabActivity(customTabPrivate = true, intentPrivate = false)

        val browsingModeManager = activity.createBrowsingModeManager(activity.intent)

        assertEquals(BrowsingMode.Private, browsingModeManager.mode)
        assertTrue(activity.window.attributes.flags and FLAG_SECURE != 0)
    }

    @Test
    fun `GIVEN a normal custom tab and a private intent WHEN its activity starts THEN it is normal`() {
        val activity = buildCustomTabActivity(customTabPrivate = false, intentPrivate = true)

        assertEquals(BrowsingMode.Normal, activity.createBrowsingModeManager(activity.intent).mode)
    }

    @Test
    fun `GIVEN the custom tab is gone and a private intent WHEN its activity starts THEN it is private`() {
        val activity = buildCustomTabActivity(customTabPrivate = null, intentPrivate = true)

        assertEquals(BrowsingMode.Private, activity.createBrowsingModeManager(activity.intent).mode)
    }

    @Test
    fun `GIVEN the regular tabs activity is private WHEN a normal custom tab activity starts and changes its mode THEN the last known mode and the app state stay private`() {
        val settings = Settings(testContext).apply { lastKnownMode = BrowsingMode.Private }
        val appStore = AppStore(AppState(mode = BrowsingMode.Private))
        val activity =
            buildCustomTabActivity(
                customTabPrivate = false,
                intentPrivate = null,
                settings = settings,
                appStore = appStore,
            )

        val browsingModeManager = activity.createBrowsingModeManager(activity.intent)
        browsingModeManager.mode = BrowsingMode.Normal

        assertEquals(BrowsingMode.Private, settings.lastKnownMode)
        assertEquals(BrowsingMode.Private, appStore.state.mode)
    }

    private fun buildCustomTabActivity(
        customTabPrivate: Boolean?,
        intentPrivate: Boolean?,
        settings: Settings = Settings(testContext).apply { lastKnownMode = BrowsingMode.Normal },
        appStore: AppStore = AppStore(),
    ): ExternalAppBrowserActivity {
        val customTabs =
            listOfNotNull(
                customTabPrivate?.let {
                    createCustomTab(url = "https://www.mozilla.org", id = "mozilla", private = it)
                }
            )
        every { testContext.components.core.store } returns BrowserStore(BrowserState(customTabs = customTabs))
        every { testContext.components.settings } returns settings
        every { testContext.components.appStore } returns appStore
        val intent =
            Intent(Intent.ACTION_VIEW).apply {
                putSessionId("mozilla")
                intentPrivate?.let { putExtra(HomeActivity.PRIVATE_BROWSING_MODE, it) }
            }
        every { testContext.components.strictMode } returns TestStrictModeManager()
        return Robolectric.buildActivity(ExternalAppBrowserActivity::class.java, intent).get()
    }
}
