/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose.ext

import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import org.mozilla.fenix.R

/** Whether the current context uses the theme of a private browsing window. */
@Composable
@ReadOnlyComposable
fun isPrivateTheme(): Boolean {
    val typedValue = TypedValue()
    return LocalContext.current.theme.resolveAttribute(R.attr.isPrivateTheme, typedValue, true) && typedValue.data != 0
}
