/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar.chrome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Positioning primitive for the browser chrome overlaid on the engine view. It only decides *where*
 * content goes; *what* goes there — toolbar, navigation bar, find-in-page, autofill prompts,
 * search — is composed by the caller into [topBar] / [bottomBar].
 *
 * @param modifier [Modifier] to be applied to the layout.
 * @param topBar Chrome shown below the status-bar strip.
 * @param bottomBar Chrome shown above the navigation-bar strip.
 */
@Composable
fun BrowserChromeScaffold(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit) = {},
    bottomBar: (@Composable () -> Unit) = {},
) {
    Box(modifier = modifier.fillMaxSize()) {
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            topBar()
        }
        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            bottomBar()
        }
    }
}
