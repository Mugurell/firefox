/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.engine.gecko

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.zip
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.CompositorScrollDelegate
import org.mozilla.geckoview.GeckoSession.ScrollPositionUpdate

/**
 * Delegate for observing scroll related updates from a given [GeckoSession]
 * and exposing this data through [scrollYPosition] and [scrollYDeltas] flows.
 */
@OptIn(ExperimentalCoroutinesApi::class) // for flatMapLatest
internal class GeckoVerticalScrollListener(
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    private val updatesScope = CoroutineScope(dispatcher)
    private val currentGeckoSession = MutableStateFlow<GeckoSession?>(null)

    /**
     * Single source of truth for the scroll position in the current session.
     * It's a "hot" SharedFlow that resets its data stream on every new session.
     */
    private val sharedPositionFlow: SharedFlow<ScrollPositionUpdate> = currentGeckoSession
        .filterNotNull()
        .flatMapLatest { session ->
            callbackFlow {
                val delegate = CompositorScrollDelegate { trySend(it) }
                session.compositorScrollDelegate = delegate
                awaitClose { session.compositorScrollDelegate = null }
            }
        }
        .shareIn(updatesScope, SharingStarted.Eagerly)

    /**
     * Running flow of the current scroll position in pixels.
     */
    val scrollYPosition: StateFlow<Float> = sharedPositionFlow
        .map { it.scrollY * it.zoom }
        .stateIn(
            scope = updatesScope,
            started = SharingStarted.Eagerly,
            initialValue = 0f,
        )

    /**
     * Running flow of scroll deltas in pixels.
     */
    val scrollYDeltas: StateFlow<Float> = sharedPositionFlow
        .map { it.zoom to it.scrollY }
        // Wait until having 2 scroll values to calculate the delta between
        .zip(sharedPositionFlow.map { it.zoom to it.scrollY }.drop(1)) { old, new -> old to new }
        .map {
            when (it.second.first == it.first.first) { // check if currently zooming in/out
                true -> (it.second.second - it.first.second) * it.second.first
                false -> 0f
            }
        }
        .filter { it != 0f }
        .stateIn(
            scope = updatesScope,
            started = SharingStarted.Eagerly,
            initialValue = 0f,
        )

    /**
     * Start observing [geckoSession] for scroll related updates.
     */
    fun observe(geckoSession: GeckoSession) {
        currentGeckoSession.value = geckoSession
    }
}

private class CompositorScrollDelegate(
    private val scrollUpdatesCallback: (ScrollPositionUpdate) -> Unit,
) : CompositorScrollDelegate {
    override fun onScrollChanged(session: GeckoSession, update: ScrollPositionUpdate) {
        scrollUpdatesCallback(update)
    }
}
