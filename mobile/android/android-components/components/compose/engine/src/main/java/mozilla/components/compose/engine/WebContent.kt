/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.compose.engine

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import mozilla.components.browser.state.action.EngineAction
import mozilla.components.browser.state.helper.Target
import mozilla.components.browser.state.store.BrowserStore
import mozilla.components.concept.engine.Engine
import mozilla.components.concept.engine.EngineView

/**
 * Composes an [EngineView] obtained from the given [Engine] and renders the web content of the
 * [target] from the [store] on it.
 */
@Composable
fun WebContent(
    engine: Engine,
    store: BrowserStore,
    insets: WebContentInsets,
    onScroll: (Float) -> Unit,
) {
    val scrollCallback by rememberUpdatedState(onScroll)
    var engineView by remember { mutableStateOf<EngineView?>(null) }

    val selectedTab = Target.SelectedTab.observeAsComposableStateFrom(
        store = store,
        observe = { tab ->
            // Render if the tab itself changed or when the state of the linked engine session changes
            arrayOf(
                tab?.id,
                tab?.engineState?.engineSession,
                tab?.engineState?.crashed,
                tab?.content?.firstContentfulPaint,
            )
        },
    )

    val density = LocalDensity.current
    LaunchedEffect(insets.bottom) {
        with(density) {
            engineView?.setDynamicToolbarMaxHeight(insets.bottom.roundToPx())
        }
    }
    LaunchedEffect(insets.visibleBottom) {
        with(density) {
            engineView?.setVerticalClipping(insets.visibleBottom.roundToPx())
        }
    }

    val currentEngineSession = remember(selectedTab.value) { selectedTab.value?.engineState?.engineSession }

    LaunchedEffect(engineView?.asView(), currentEngineSession) {
        val tab = selectedTab.value
        if (tab == null) {
            engineView?.release()
        } else {
            val session = tab.engineState.engineSession
            if (session == null) {
                // This tab does not have an EngineSession that we can render yet. Let's dispatch an
                // action to request creating one. Once one was created and linked to this session, this
                // method will get invoked again.
                store.dispatch(EngineAction.CreateEngineSessionAction(tab.id))
            } else {
                engineView?.render(session)
                launch {
                    engineView?.verticalScrollDelta?.collect {
                        scrollCallback(it)
                    }
                }
            }
        }
    }
    AndroidView(
        modifier = Modifier
            .fillMaxSize(),
        factory = { context ->
            engine.createView(context).also {
                engineView = it
            }.asView()
        },
    )
}
