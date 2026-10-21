/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.browser.state.state.BrowserState
import mozilla.components.browser.state.state.createCustomTab
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.components.usecases.FenixBrowserUseCases
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.utils.Settings
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackingProtectionFragmentTest {

    @Test
    fun `UI component should match settings defaults`() {
        val settings = Settings(testContext)
        every { testContext.components.analytics } returns mockk(relaxed = true)
        every { testContext.components.settings } returns settings
        val settingsFragment = TrackingProtectionFragment()
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()

        activity.supportFragmentManager.beginTransaction().add(settingsFragment, "settingsFragment").commitNow()

        val customCookiesCheckBox = settingsFragment.customCookies.isChecked
        val customCookiesCheckBoxSettings = settings.blockCookiesInCustomTrackingProtection

        val customCookiesSelect = settingsFragment.customCookiesSelect.value
        val customCookiesSelectSettings = settings.blockCookiesSelectionInCustomTrackingProtection

        val customTrackingContentCheckBox = settingsFragment.customTracking.isChecked
        val customTrackingContentCheckBoxSettings = settings.blockTrackingContentInCustomTrackingProtection

        val customTrackingContentSelect = settingsFragment.customTrackingSelect.value
        val customTrackingContentSelectSettings = settings.blockTrackingContentSelectionInCustomTrackingProtection

        val customCryptominersCheckBox = settingsFragment.customCryptominers.isChecked
        val customCryptominersCheckBoxSettings = settings.blockCryptominersInCustomTrackingProtection

        val customFingerprintersCheckBox = settingsFragment.customFingerprinters.isChecked
        val customFingerprintersCheckBoxSettings = settings.blockFingerprintersInCustomTrackingProtection

        val customRedirectTrackersCheckBox = settingsFragment.customRedirectTrackers.isChecked
        val customRedirectTrackersCheckBoxSettings = settings.blockRedirectTrackersInCustomTrackingProtection

        val customSuspectedFingerprinters = settingsFragment.customSuspectedFingerprinters.isChecked
        val customSuspectedFingerprintersSetting = settings.blockSuspectedFingerprintersInCustomTrackingProtection

        val customSuspectedFingerprintersSelect = settingsFragment.customSuspectedFingerprintersSelect.value
        val customSuspectedFingerprintersSelectSetting =
            settings.blockSuspectedFingerprintersSelectionInCustomTrackingProtection

        assertEquals(customCookiesCheckBoxSettings, customCookiesCheckBox)
        assertEquals(customCookiesSelectSettings, customCookiesSelect)
        assertEquals(customTrackingContentCheckBoxSettings, customTrackingContentCheckBox)
        assertEquals(customTrackingContentSelect, customTrackingContentSelectSettings)
        assertEquals(customCryptominersCheckBoxSettings, customCryptominersCheckBox)
        assertEquals(customFingerprintersCheckBoxSettings, customFingerprintersCheckBox)
        assertEquals(customRedirectTrackersCheckBoxSettings, customRedirectTrackersCheckBox)
        assertEquals(customSuspectedFingerprinters, customSuspectedFingerprintersSetting)
        assertEquals(customSuspectedFingerprintersSelect, customSuspectedFingerprintersSelectSetting)
    }

    @Test
    fun `GIVEN baseline exception checkbox is true WHEN user clicks on baseline checkbox THEN warning dialog shows`() {
        val settings = Settings(testContext)
        every { testContext.components.analytics } returns mockk(relaxed = true)
        every { testContext.components.settings } returns settings
        every { testContext.components.core } returns mockk(relaxed = true)

        val settingsFragment = TrackingProtectionFragment()
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        activity.supportFragmentManager.beginTransaction().add(settingsFragment, "settingsFragment").commitNow()

        settings.strictAllowListBaselineTrackingProtection = true
        val baselineCheckbox = settingsFragment.strictAllowListBaselineTrackingProtection

        val result = baselineCheckbox.onPreferenceChangeListener?.onPreferenceChange(baselineCheckbox, false)

        assertEquals(true, settingsFragment.alertDialog.isShowing)
        assertEquals(false, result)
        assertEquals(true, settings.strictAllowListBaselineTrackingProtection)
        assertEquals(true, baselineCheckbox.isChecked)
    }

    @Test
    fun `GIVEN baseline uncheck warning dialog is showing WHEN user clicks confirm THEN baseline and convenience are false`() {
        val mockSettings = mockk<Settings>(relaxed = true)
        every { testContext.components.analytics } returns mockk(relaxed = true)
        every { testContext.components.settings } returns mockSettings
        every { testContext.components.core } returns mockk(relaxed = true)

        val settingsFragment = TrackingProtectionFragment()
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()

        // Properly attach the fragment to initialize lateinit properties
        activity.supportFragmentManager.beginTransaction().add(settingsFragment, "settingsFragment").commitNow()

        val fragmentSpy = spyk(settingsFragment)
        val dialog = mockk<AlertDialog>(relaxed = true)
        every { fragmentSpy.updateTrackingProtectionPolicy() } just Runs

        fragmentSpy.onDialogConfirm(true, dialog)

        verify { mockSettings.strictAllowListBaselineTrackingProtection = false }
    }

    @Test
    fun `GIVEN baseline uncheck warning dialog is showing WHEN user clicks cancel THEN baseline must be true and convenience does not change`() {
        val mockSettings = mockk<Settings>(relaxed = true)
        every { testContext.components.analytics } returns mockk(relaxed = true)
        every { testContext.components.settings } returns mockSettings
        every { testContext.components.core } returns mockk(relaxed = true)

        val settingsFragment = TrackingProtectionFragment()
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()

        // Properly attach the fragment to initialize lateinit properties
        activity.supportFragmentManager.beginTransaction().add(settingsFragment, "settingsFragment").commitNow()

        val fragmentSpy = spyk(settingsFragment)
        val dialog = mockk<AlertDialog>(relaxed = true)
        every { fragmentSpy.updateTrackingProtectionPolicy() } just Runs

        fragmentSpy.onDialogCancel(true, dialog)

        verify { mockSettings.strictAllowListBaselineTrackingProtection = true }
    }

    @Test
    fun `GIVEN a private custom tab is focused in normal mode WHEN the allow list learn more is clicked THEN open it in a private tab`() {
        val fenixBrowserUseCases = setUpLearnMore(mode = BrowsingMode.Normal, customTabPrivate = true)

        clickAllowListLearnMore()

        verifyLearnMoreOpened(fenixBrowserUseCases, private = true)
    }

    @Test
    fun `GIVEN a normal custom tab is focused in private mode WHEN the allow list learn more is clicked THEN open it in a normal tab`() {
        val fenixBrowserUseCases = setUpLearnMore(mode = BrowsingMode.Private, customTabPrivate = false)

        clickAllowListLearnMore()

        verifyLearnMoreOpened(fenixBrowserUseCases, private = false)
    }

    @Test
    fun `GIVEN no custom tab is focused in private mode WHEN the allow list learn more is clicked THEN open it in a private tab`() {
        val fenixBrowserUseCases = setUpLearnMore(mode = BrowsingMode.Private, customTabPrivate = null)

        clickAllowListLearnMore()

        verifyLearnMoreOpened(fenixBrowserUseCases, private = true)
    }

    private fun setUpLearnMore(mode: BrowsingMode, customTabPrivate: Boolean?): FenixBrowserUseCases {
        val fenixBrowserUseCases: FenixBrowserUseCases = mockk(relaxed = true)
        every { testContext.components.analytics } returns mockk(relaxed = true)
        every { testContext.components.settings } returns Settings(testContext)
        every { testContext.components.useCases.fenixBrowserUseCases } returns fenixBrowserUseCases
        every { testContext.components.appStore.state } returns
            AppState(
                mode = mode,
                lastForegroundCustomTabId = customTabPrivate?.let { "customTab" },
            )
        every { testContext.components.core.store.state } returns
            BrowserState(
                customTabs =
                    listOfNotNull(
                        customTabPrivate?.let {
                            createCustomTab(url = "https://mozilla.org", id = "customTab", private = it)
                        }
                    )
            )
        return fenixBrowserUseCases
    }

    private fun clickAllowListLearnMore() {
        val settingsFragment = TrackingProtectionFragment()
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        activity.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, settingsFragment, "settingsFragment")
            .commitNow()
        Navigation.setViewNavController(settingsFragment.requireView(), mockk<NavController>(relaxed = true))

        val subheader = settingsFragment.strictAllowListTrackingProtectionSubheader
        subheader.onPreferenceClickListener?.onPreferenceClick(subheader)
    }

    private fun verifyLearnMoreOpened(fenixBrowserUseCases: FenixBrowserUseCases, private: Boolean) {
        verify {
            fenixBrowserUseCases.loadUrlOrSearch(
                searchTermOrURL = any(),
                newTab = true,
                private = private,
                forceSearch = any(),
                searchEngine = any(),
                flags = any(),
                historyMetadata = any(),
                additionalHeaders = any(),
            )
        }
    }
}
