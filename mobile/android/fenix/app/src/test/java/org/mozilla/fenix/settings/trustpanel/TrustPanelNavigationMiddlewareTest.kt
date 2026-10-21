/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.settings.trustpanel

import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.state.state.SessionState
import mozilla.components.feature.tabs.TabsUseCases
import org.junit.Test
import org.mozilla.fenix.R
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.settings.PhoneFeature
import org.mozilla.fenix.settings.trustpanel.middleware.TrustPanelNavigationMiddleware
import org.mozilla.fenix.settings.trustpanel.store.TrustPanelAction
import org.mozilla.fenix.settings.trustpanel.store.TrustPanelState
import org.mozilla.fenix.settings.trustpanel.store.TrustPanelStore
import org.mozilla.fenix.settings.trustpanel.store.WebsiteInfoState

class TrustPanelNavigationMiddlewareTest {

    private val navController: NavController =
        mockk(relaxed = true) {
            every { navigate(any<NavDirections>(), any<NavOptions>()) } just runs
            every { currentDestination?.id } returns R.id.trustPanelFragment
        }
    private val appStore: AppStore = mockk(relaxed = true)
    private val tabsUseCases: TabsUseCases = mockk(relaxed = true)

    @Test
    fun `WHEN navigate to privacy security settings action is dispatched THEN navigate to privacy and security settings`() =
        runTest {
            val privacySecurityPrefKey = "pref_key_privacy_security_category"
            val store = createStore(privacySecurityPrefKey = privacySecurityPrefKey, scope = this)
            store.dispatch(TrustPanelAction.Navigate.PrivacySecuritySettings)
            testScheduler.advanceUntilIdle()

            verify {
                navController.navigate(
                    TrustPanelFragmentDirections.actionGlobalTrackingProtectionFragment(
                        preferenceToScrollTo = privacySecurityPrefKey
                    ),
                    null,
                )
            }
        }

    @Test
    fun `WHEN navigate to manage phone feature is dispatched THEN navigate to manage phone feature`() = runTest {
        val store = createStore(scope = this)
        store.dispatch(TrustPanelAction.Navigate.ManagePhoneFeature(PhoneFeature.CAMERA))
        testScheduler.advanceUntilIdle()

        verify {
            navController.navigate(
                TrustPanelFragmentDirections.actionGlobalSitePermissionsManagePhoneFeature(PhoneFeature.CAMERA),
                null,
            )
        }
    }

    @Test
    fun `GIVEN a normal tab in private mode WHEN security certificate action is dispatched THEN open the certificate in a normal tab`() =
        runTest {
            every { appStore.state.mode.isPrivate } returns true

            dispatchSecurityCertificate(sessionState = createSessionState(isPrivate = false))

            verify { navController.navigate(R.id.browserFragment) }
            verify {
                tabsUseCases.addTab(
                    url = "about:certificate?cert=null",
                    parentId = "session-id",
                    contextId = "context-id",
                    private = false,
                )
            }
        }

    @Test
    fun `GIVEN a private tab in normal mode WHEN security certificate action is dispatched THEN open the certificate in a private tab`() =
        runTest {
            every { appStore.state.mode.isPrivate } returns false

            dispatchSecurityCertificate(sessionState = createSessionState(isPrivate = true))

            verify { navController.navigate(R.id.browserFragment) }
            verify {
                tabsUseCases.addTab(
                    url = "about:certificate?cert=null",
                    parentId = "session-id",
                    contextId = "context-id",
                    private = true,
                )
            }
        }

    @Test
    fun `GIVEN no tab in private mode WHEN security certificate action is dispatched THEN open the certificate in a private tab`() =
        runTest {
            every { appStore.state.mode.isPrivate } returns true

            dispatchSecurityCertificate(sessionState = null)

            verify { navController.navigate(R.id.browserFragment) }
            verify {
                tabsUseCases.addTab(
                    url = "about:certificate?cert=null",
                    parentId = null,
                    contextId = null,
                    private = true,
                )
            }
        }

    private fun createSessionState(isPrivate: Boolean): SessionState = mockk {
        every { id } returns "session-id"
        every { contextId } returns "context-id"
        every { content.private } returns isPrivate
    }

    private fun TestScope.dispatchSecurityCertificate(sessionState: SessionState?) {
        val websiteInfoState: WebsiteInfoState = mockk {
            every { certificate } returns mockk(relaxed = true)
        }
        val store =
            createStore(
                trustPanelState =
                    TrustPanelState(
                        sessionState = sessionState,
                        websiteInfoState = websiteInfoState,
                    ),
                scope = this,
            )

        store.dispatch(TrustPanelAction.Navigate.SecurityCertificate)
        testScheduler.advanceUntilIdle()
    }

    private fun createStore(
        trustPanelState: TrustPanelState = TrustPanelState(),
        privacySecurityPrefKey: String = "",
        scope: CoroutineScope,
    ) =
        TrustPanelStore(
            initialState = trustPanelState,
            middleware =
                listOf(
                    TrustPanelNavigationMiddleware(
                        navController = navController,
                        privacySecurityPrefKey = privacySecurityPrefKey,
                        appStore = appStore,
                        tabsUseCases = tabsUseCases,
                        scope = scope,
                    )
                ),
        )
}
