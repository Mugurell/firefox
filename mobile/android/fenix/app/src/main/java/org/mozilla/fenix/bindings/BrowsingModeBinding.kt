/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.bindings

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import mozilla.components.lib.state.helpers.AbstractBinding
import org.mozilla.fenix.browser.browsingmode.BrowsingMode
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.components.appstate.AppState

/**
 * A binding that reports every new browsing mode of the regular tabs window, so that the window can apply it. A mode
 * published while the binding is stopped is reported once it starts again.
 *
 * @param appStore The [AppStore] to observe the browsing mode from.
 * @param initialMode The browsing mode the window already applied.
 * @param onModeChanged Invoked with every browsing mode that differs from the last one applied.
 * @param mainDispatcher The [CoroutineDispatcher] used to observe the [appStore].
 */
class BrowsingModeBinding(
    appStore: AppStore,
    initialMode: BrowsingMode,
    private val onModeChanged: (BrowsingMode) -> Unit,
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : AbstractBinding<AppState>(appStore, mainDispatcher) {

    private var appliedMode = initialMode

    override suspend fun onState(flow: Flow<AppState>) {
        flow
            .map { it.mode }
            .filter { it != appliedMode }
            .collect { mode ->
                appliedMode = mode
                onModeChanged(mode)
            }
    }
}
