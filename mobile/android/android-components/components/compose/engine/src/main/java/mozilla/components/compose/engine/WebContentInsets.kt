/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.compose.engine

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class WebContentInsets(
    val top: Dp = 0.dp,
    val visibleTop: Dp = top,
    val bottom: Dp = 0.dp,
    val visibleBottom: Dp = bottom,
)
