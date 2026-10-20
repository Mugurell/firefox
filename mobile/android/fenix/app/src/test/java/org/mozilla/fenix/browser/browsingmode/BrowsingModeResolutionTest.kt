/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.browsingmode

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.fenix.HomeActivity.Companion.PRIVATE_BROWSING_MODE

@RunWith(AndroidJUnit4::class)
class BrowsingModeResolutionTest {

    @Test
    fun `GIVEN a private session and an intent setting normal mode WHEN resolving the browsing mode THEN return private`() {
        val intent = Intent().putExtra(PRIVATE_BROWSING_MODE, false)

        assertEquals(BrowsingMode.Private, resolveBrowsingMode(BrowsingMode.Private, intent, BrowsingMode.Normal))
    }

    @Test
    fun `GIVEN a normal session, an intent setting private mode and private last known mode WHEN resolving the browsing mode THEN return normal`() {
        val intent = Intent().putExtra(PRIVATE_BROWSING_MODE, true)

        assertEquals(BrowsingMode.Normal, resolveBrowsingMode(BrowsingMode.Normal, intent, BrowsingMode.Private))
    }

    @Test
    fun `GIVEN a private session, no intent and normal last known mode WHEN resolving the browsing mode THEN return private`() {
        assertEquals(BrowsingMode.Private, resolveBrowsingMode(BrowsingMode.Private, null, BrowsingMode.Normal))
    }

    @Test
    fun `GIVEN no session, an intent setting normal mode and private last known mode WHEN resolving the browsing mode THEN return normal`() {
        val intent = Intent().putExtra(PRIVATE_BROWSING_MODE, false)

        assertEquals(BrowsingMode.Normal, resolveBrowsingMode(null, intent, BrowsingMode.Private))
    }

    @Test
    fun `GIVEN no session, an intent setting private mode and normal last known mode WHEN resolving the browsing mode THEN return private`() {
        val intent = Intent().putExtra(PRIVATE_BROWSING_MODE, true)

        assertEquals(BrowsingMode.Private, resolveBrowsingMode(null, intent, BrowsingMode.Normal))
    }

    @Test
    fun `GIVEN no session and an intent without a browsing mode WHEN resolving the browsing mode THEN return the last known mode`() {
        assertEquals(BrowsingMode.Private, resolveBrowsingMode(null, Intent(), BrowsingMode.Private))
        assertEquals(BrowsingMode.Normal, resolveBrowsingMode(null, null, BrowsingMode.Normal))
    }
}
