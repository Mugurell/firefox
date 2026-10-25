/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.ext

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigation
import io.mockk.Runs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import mozilla.components.concept.engine.EngineSession.LoadUrlFlags
import mozilla.components.feature.session.SessionUseCases
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.R
import org.mozilla.fenix.navigation.NavControllerProvider
import org.mozilla.fenix.utils.Settings
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FragmentTest {

    private val navDirections: NavDirections = mockk(relaxed = true)
    private val mockDestination = spyk(NavDestination("hi"))
    private val mockId = 4
    private val navController: NavController = mockk(relaxed = true)
    private val mockOptions: NavOptions = mockk(relaxed = true)
    private val navControllerProvider: NavControllerProvider = mockk(relaxed = true)
    private val settings: Settings = spyk(Settings(testContext))
    private lateinit var mockContext: Context
    private lateinit var fragment: Fragment

    @Before
    fun setup() {
        mockContext =
            mockk(relaxed = true) {
                every { components.settings } returns settings
            }
        fragment =
            spyk(Fragment()).apply {
                every { context } returns mockContext
            }

        every { navControllerProvider.getNavController(fragment) } returns navController
        every { navController.currentDestination } returns mockDestination
        every { mockDestination.id } returns mockId
    }

    @Test
    fun `Test nav fun with ID and directions`() {
        every { navController.navigate(navDirections, null) } just Runs

        fragment.nav(mockId, navDirections, navControllerProvider = navControllerProvider)
        verify { navController.currentDestination }
        verify { navController.navigate(navDirections, null) }
        confirmVerified(fragment)
    }

    @Test
    fun `Test nav fun with ID, directions, and options`() {
        every { navController.navigate(navDirections, mockOptions) } just Runs

        fragment.nav(mockId, navDirections, mockOptions, navControllerProvider = navControllerProvider)
        verify { navController.currentDestination }
        verify { navController.navigate(navDirections, mockOptions) }
        confirmVerified(fragment)
    }

    @Test
    fun `WHEN a link is opened in a custom tab THEN it loads in that custom tab and the screens above it close`() {
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        every { testContext.components.useCases.sessionUseCases } returns sessionUseCases
        val shownFragment = showFragment()

        shownFragment.loadInCustomTab(
            url = "https://example.com",
            customTabSessionId = "custom-tab-id",
        )

        verify {
            sessionUseCases.loadUrl("https://example.com", "custom-tab-id", LoadUrlFlags.none())
            navController.popBackStack(R.id.externalAppBrowserFragment, false)
        }
    }

    @Test
    fun `WHEN a link is opened in a custom tab with load flags THEN it loads with those flags`() {
        val sessionUseCases: SessionUseCases = mockk(relaxed = true)
        every { testContext.components.useCases.sessionUseCases } returns sessionUseCases
        val shownFragment = showFragment()

        val flags = LoadUrlFlags.select(LoadUrlFlags.ALLOW_JAVASCRIPT_URL)
        shownFragment.loadInCustomTab(
            url = "javascript:alert(1)",
            customTabSessionId = "custom-tab-id",
            flags = flags,
        )

        verify {
            sessionUseCases.loadUrl("javascript:alert(1)", "custom-tab-id", flags)
            navController.popBackStack(R.id.externalAppBrowserFragment, false)
        }
    }

    private fun showFragment(): Fragment {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()
        val shownFragment = ViewFragment()
        activity.supportFragmentManager.beginTransaction().add(android.R.id.content, shownFragment).commitNow()
        Navigation.setViewNavController(shownFragment.requireView(), navController)
        return shownFragment
    }

    class ViewFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
            View(inflater.context)
    }

    @Test
    fun `WHEN allowScreenCaptureInSecureScreens is true and a fragment is secured THEN FLAG_SECURE flag is added to the LayoutParams`() {
        every { settings.allowScreenCaptureInSecureScreens } returns false

        val testFragment = TestFragment(mockContext)

        startFragment(testFragment)

        testFragment.secure()

        val flags = testFragment.requireActivity().window.attributes.flags
        assertTrue(flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
    }

    @Test
    fun `WHEN allowScreenCaptureInSecureScreens is false and a fragment is secured THEN no window flags are added to the LayoutParams`() {
        every { settings.allowScreenCaptureInSecureScreens } returns true

        val testFragment = TestFragment(mockContext)

        startFragment(testFragment)

        testFragment.secure()

        val flags = testFragment.requireActivity().window.attributes.flags
        assertFalse(flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
    }

    class TestFragment(private val mockContext: Context) : Fragment() {
        override fun getContext(): Context? {
            return mockContext
        }
    }

    private fun startFragment(fragment: Fragment) {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().start().resume().get()
        activity.supportFragmentManager.beginTransaction().add(fragment, null).commitNow()
    }
}
