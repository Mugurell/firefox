/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.browsingmode

import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.Store
import org.mozilla.fenix.components.appstate.AppAction
import org.mozilla.fenix.components.appstate.AppState
import org.mozilla.fenix.utils.Settings

/**
 * [Middleware] that persists every browser mode change before the new mode reaches the [AppState].
 *
 * @param settings [Settings] used to persist the last known browsing mode.
 */
class BrowsingModePersistenceMiddleware(private val settings: Settings) : Middleware<AppState, AppAction> {

    override fun invoke(
        store: Store<AppState, AppAction>,
        next: (AppAction) -> Unit,
        action: AppAction,
    ) {
        if (action is AppAction.BrowsingModeChanged) {
            settings.lastKnownMode = action.mode
        }

        next(action)
    }
}
