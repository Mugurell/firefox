/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix

import android.view.ContextThemeWrapper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mozilla.components.compose.base.theme.Theme
import mozilla.components.support.test.robolectric.testContext
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FenixApplicationThemeTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun `GIVEN a window with the private theme THEN the theme is private`() {
        assertEquals(Theme.Private, themeIn(R.style.PrivateTheme))
    }

    @Test
    fun `GIVEN a window with the normal theme THEN the theme is not private`() {
        assertEquals(Theme.Light, themeIn(R.style.NormalTheme))
    }

    private fun themeIn(windowTheme: Int): Theme? {
        val application = testContext.applicationContext as FenixApplication
        var theme: Theme? = null
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides ContextThemeWrapper(testContext, windowTheme)) {
                theme = application.provideTheme()
            }
        }
        return theme
    }
}
