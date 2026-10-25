/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.Preference
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.concept.engine.EngineSession
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.feature.tabs.TabsUseCases
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
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

        val settingsFragment =
            TrackingProtectionFragment().apply { arguments = TrackingProtectionFragmentArgs().toBundle() }
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        activity.supportFragmentManager.beginTransaction().add(android.R.id.content, settingsFragment).commitNow()

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
    fun `GIVEN the settings are shown from a custom tab WHEN opening the exceptions THEN they get the custom tab ID`() {
        every { testContext.components.analytics } returns mockk(relaxed = true)
        every { testContext.components.settings } returns Settings(testContext)
        every { testContext.components.core } returns mockk(relaxed = true)

        val customTabSessionId = "custom-tab-id"
        val settingsFragment =
            TrackingProtectionFragment().apply {
                arguments = TrackingProtectionFragmentArgs(customTabSessionId = customTabSessionId).toBundle()
            }
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        activity.supportFragmentManager.beginTransaction().add(android.R.id.content, settingsFragment).commitNow()

        val navController = mockk<NavController>(relaxed = true)
        Navigation.setViewNavController(settingsFragment.requireView(), navController)

        val exceptionsPref =
            settingsFragment.findPreference<Preference>(
                settingsFragment.getString(R.string.pref_key_tracking_protection_exceptions)
            )!!
        exceptionsPref.onPreferenceClickListener!!.onPreferenceClick(exceptionsPref)

        verify {
            navController.navigate(
                TrackingProtectionFragmentDirections.actionTrackingProtectionFragmentToExceptionsFragment(
                    customTabSessionId = customTabSessionId
                )
            )
        }
    }

    @Test
    fun `GIVEN the settings are shown from a custom tab WHEN tapping learn more THEN the help page loads in the custom tab`() {
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        every { testContext.components.analytics } returns mockk(relaxed = true)
        every { testContext.components.settings } returns Settings(testContext)
        every { testContext.components.core } returns mockk(relaxed = true)
        every { testContext.components.useCases.sessionUseCases } returns sessionUseCases

        val customTabSessionId = "custom-tab-id"
        val settingsFragment =
            TrackingProtectionFragment().apply {
                arguments = TrackingProtectionFragmentArgs(customTabSessionId = customTabSessionId).toBundle()
            }
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        activity.supportFragmentManager.beginTransaction().add(android.R.id.content, settingsFragment).commitNow()

        val navController = mockk<NavController>(relaxed = true)
        Navigation.setViewNavController(settingsFragment.requireView(), navController)

        val learnMorePref =
            settingsFragment.findPreference<Preference>(settingsFragment.getString(R.string.pref_key_etp_learn_more))!!
        learnMorePref.onPreferenceClickListener!!.onPreferenceClick(learnMorePref)

        val expectedUrl = SupportUtils.getGenericSumoURLForTopic(SupportUtils.SumoTopic.TRACKING_PROTECTION)
        verify {
            sessionUseCases.loadUrl(expectedUrl, customTabSessionId, any())
            navController.popBackStack(R.id.externalAppBrowserFragment, false)
        }
    }

    @Test
    fun `GIVEN the settings are shown from the regular tabs activity WHEN tapping learn more THEN the help page opens in a new tab`() {
        val tabsUseCases: TabsUseCases = mockk(relaxed = true)
        val fenixBrowserUseCases =
            FenixBrowserUseCases(
                appStore = AppStore(),
                tabsUseCases = tabsUseCases,
                loadUrlUseCase = mockk(relaxed = true),
                searchUseCases = mockk(relaxed = true),
                homepageTitle = "",
                profiler = null,
            )
        every { testContext.components.analytics } returns mockk(relaxed = true)
        every { testContext.components.settings } returns Settings(testContext)
        every { testContext.components.core } returns mockk(relaxed = true)
        every { testContext.components.useCases.fenixBrowserUseCases } returns fenixBrowserUseCases

        val settingsFragment =
            TrackingProtectionFragment().apply { arguments = TrackingProtectionFragmentArgs().toBundle() }
        val activity = Robolectric.buildActivity(AppCompatActivity::class.java).setup().get()
        activity.supportFragmentManager.beginTransaction().add(android.R.id.content, settingsFragment).commitNow()

        val navController = mockk<NavController>(relaxed = true)
        Navigation.setViewNavController(settingsFragment.requireView(), navController)

        val learnMorePref =
            settingsFragment.findPreference<Preference>(settingsFragment.getString(R.string.pref_key_etp_learn_more))!!
        learnMorePref.onPreferenceClickListener!!.onPreferenceClick(learnMorePref)

        val expectedUrl = SupportUtils.getGenericSumoURLForTopic(SupportUtils.SumoTopic.TRACKING_PROTECTION)
        verify {
            navController.navigate(R.id.browserFragment)
            tabsUseCases.addTab.invoke(
                url = expectedUrl,
                flags = EngineSession.LoadUrlFlags.none(),
                private = false,
                historyMetadata = null,
                originalInput = expectedUrl,
            )
        }
    }
}
