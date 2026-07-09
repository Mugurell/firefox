/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser

import android.view.View
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Observes system-bar insets on a fragment root view while the fragment renders edge-to-edge
 * and notifies a callback whenever they change.
 *
 * Callers use this to recompute engine-view margins, toolbar heights and other chrome positions
 * when the available inset area changes (rotation, fold, gesture-nav swap, immersive toggle).
 *
 * @param rootView The fragment root view to observe.
 * @param onInsetsChanged Invoked on the main thread whenever the cached system bar insets differ
 * from the previously observed value.
 */
class BrowserFragmentInsetsController(
    private val rootView: View,
    private val onInsetsChanged: () -> Unit,
) {
    private var lastSystemBars: Insets = Insets.NONE

    fun start() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            if (systemBars != lastSystemBars) {
                lastSystemBars = systemBars
                onInsetsChanged()
            }
            insets
        }
        ViewCompat.requestApplyInsets(rootView)
    }

    fun stop() {
        ViewCompat.setOnApplyWindowInsetsListener(rootView, null)
    }
}
