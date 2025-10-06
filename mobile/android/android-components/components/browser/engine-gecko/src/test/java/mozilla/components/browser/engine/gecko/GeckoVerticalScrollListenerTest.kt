/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mozilla.components.browser.engine.gecko.GeckoVerticalScrollListener
import mozilla.components.support.test.argumentCaptor
import mozilla.components.support.test.mock
import org.junit.After
import org.junit.Test
import org.mockito.Mockito.isNull
import org.mockito.Mockito.verify
import org.mozilla.geckoview.GeckoSession

@ExperimentalCoroutinesApi
class GeckoVerticalScrollListenerTest {
    private val geckoSession: GeckoSession = mock()
    private val delegateCaptor = argumentCaptor<GeckoSession.CompositorScrollDelegate>()
    private var scrollUpdatesDispatcher: CoroutineDispatcher = StandardTestDispatcher()

    @After
    fun teardown() {
        scrollUpdatesDispatcher.cancel()
    }

    @Test
    fun `WHEN there is no scroll data THEN return 0 for scroll and delta`() = runTest {
        scrollUpdatesDispatcher = StandardTestDispatcher(testScheduler)
        val scrollListener = GeckoVerticalScrollListener(scrollUpdatesDispatcher)

        assertEquals(0f, scrollListener.scrollYPosition.value)
        assertEquals(0f, scrollListener.scrollYDeltas.value)
    }

    @Test
    fun `GIVEN multiple scroll and zoom updates in a GeckoSession WHEN observing them THEN get the scroll values and scroll deltas only if not zooming`() = runTest {
        val scope = this + Job()
        scrollUpdatesDispatcher = StandardTestDispatcher(testScheduler)
        val scrollListener = GeckoVerticalScrollListener(scrollUpdatesDispatcher)

        val positions = mutableListOf<Float>()
        val deltas = mutableListOf<Float>()
        scrollListener.scrollYPosition.onEach { positions.add(it) }.launchIn(scope)
        scrollListener.scrollYDeltas.onEach { deltas.add(it) }.launchIn(scope)

        scrollListener.observe(geckoSession)
        advanceUntilIdle()
        verify(geckoSession).compositorScrollDelegate = delegateCaptor.capture()
        val delegate = delegateCaptor.value
        delegate.onScrollChanged(geckoSession, buildScrollUpdate(0f))
        advanceUntilIdle()
        delegate.onScrollChanged(geckoSession, buildScrollUpdate(50f))
        advanceUntilIdle()
        delegate.onScrollChanged(geckoSession, buildScrollUpdate(120f))
        advanceUntilIdle()
        delegate.onScrollChanged(geckoSession, buildScrollUpdate(70f, 2f))
        advanceUntilIdle()
        delegate.onScrollChanged(geckoSession, buildScrollUpdate(160f, -1f))
        advanceUntilIdle()
        delegate.onScrollChanged(geckoSession, buildScrollUpdate(180f))
        advanceUntilIdle()
        delegate.onScrollChanged(geckoSession, buildScrollUpdate(150f))
        advanceUntilIdle()

        assertEquals(listOf(0f, 50f, 120f, 140f, -160f, 180f, 150f), positions)
        assertEquals(listOf(0f, 50f, 70f, -30f), deltas)
        assertEquals(150f, scrollListener.scrollYPosition.value)
        assertEquals(-30f, scrollListener.scrollYDeltas.value)
    }

    @Test
    fun `GIVEN a new GeckoSession WHEN observing it for scroll details THEN resets reset current data and start observing the new session`() = runTest {
        val scope = this + Job()
        scrollUpdatesDispatcher = StandardTestDispatcher(testScheduler)
        val scrollListener = GeckoVerticalScrollListener(scrollUpdatesDispatcher)
        val positions = mutableListOf<Float>()
        scrollListener.scrollYPosition.onEach { positions.add(it) }.launchIn(scope)

        scrollListener.observe(geckoSession)
        advanceUntilIdle()
        verify(geckoSession).compositorScrollDelegate = delegateCaptor.capture()
        val initialDelegate = delegateCaptor.value
        initialDelegate.onScrollChanged(geckoSession, buildScrollUpdate(100f))
        advanceUntilIdle()
        assertEquals(100f, scrollListener.scrollYPosition.value)

        val otherGeckoSession: GeckoSession = mock()
        scrollListener.observe(otherGeckoSession)
        advanceUntilIdle()
        verify(geckoSession).compositorScrollDelegate = isNull()
        verify(otherGeckoSession).compositorScrollDelegate = delegateCaptor.capture()

        val newDelegate = delegateCaptor.value
        newDelegate.onScrollChanged(otherGeckoSession, buildScrollUpdate(200f))
        advanceUntilIdle()
        assertEquals(listOf(0f, 100f, 200f), positions)
        assertEquals(200f, scrollListener.scrollYPosition.value)
    }

    private fun buildScrollUpdate(
        scrollY: Float,
        zoom: Float = 1f,
    ) = mock<GeckoSession>().ScrollPositionUpdate().apply {
        this.scrollY = scrollY
        this.zoom = zoom
    }
}
