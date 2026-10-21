/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.intent

import android.content.Intent
import androidx.navigation.NavController
import org.mozilla.fenix.HomeActivity
import org.mozilla.fenix.NavGraphDirections
import org.mozilla.fenix.browser.browsingmode.resolveBrowsingMode
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.usecases.FenixBrowserUseCases
import org.mozilla.fenix.ext.nav
import org.mozilla.fenix.home.intent.StartSearchIntentProcessor.Companion.PRIVATE_BROWSING_PINNED_SHORTCUT
import org.mozilla.fenix.utils.Settings

/**
 * Responds to [HomeActivity.OPEN_TO_HOME] flag when the browser should open the homepage in response to an intent.
 *
 * Also responds to [PRIVATE_BROWSING_PINNED_SHORTCUT] when homepage as a new tab is enabled, so that shortcuts pinned
 * before that setting existed open the private homepage rather than focusing the address bar.
 *
 * @param fenixBrowserUseCases [FenixBrowserUseCases] used to add a new homepage tab.
 * @param appStore [AppStore] used to set the browsing mode.
 */
class OpenHomeIntentProcessor(
    private val fenixBrowserUseCases: FenixBrowserUseCases,
    private val appStore: AppStore,
) : HomeIntentProcessor {
    override fun process(intent: Intent, navController: NavController, out: Intent, settings: Settings): Boolean {
        return if (
            intent.extras?.getBoolean(HomeActivity.OPEN_TO_HOME) == true ||
                (intent.extras?.getString(HomeActivity.OPEN_TO_SEARCH) == PRIVATE_BROWSING_PINNED_SHORTCUT &&
                    settings.enableHomepageAsNewTab)
        ) {
            out.putExtra(HomeActivity.OPEN_TO_HOME, false)
            out.removeExtra(HomeActivity.OPEN_TO_SEARCH)

            val mode = resolveBrowsingMode(null, intent, settings.lastKnownMode)
            appStore.dispatch(AppAction.BrowsingModeChanged(mode = mode))
            fenixBrowserUseCases.addNewHomepageTab(private = mode.isPrivate)
            navController.nav(
                null,
                directions = NavGraphDirections.actionGlobalHome(),
            )
            true
        } else {
            false
        }
    }
}
