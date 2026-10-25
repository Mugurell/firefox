/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.compose.ext

import android.content.res.Resources.Theme
import android.util.TypedValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import org.mozilla.fenix.R

/** Whether the current activity has the private [Theme] applied or not. */
@Composable
@ReadOnlyComposable
fun isPrivateThemeApplied(): Boolean {
    val typedValue = TypedValue()
    return LocalContext.current.theme.resolveAttribute(R.attr.isPrivateTheme, typedValue, true) && typedValue.data != 0
}
