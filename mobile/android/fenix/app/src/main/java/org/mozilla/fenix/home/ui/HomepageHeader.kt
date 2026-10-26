/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import mozilla.components.compose.base.button.IconButton
import mozilla.components.compose.base.theme.PreviewThemeProvider
import mozilla.components.compose.base.theme.Theme
import mozilla.components.ui.icons.R as iconsR
import org.mozilla.fenix.R
import org.mozilla.fenix.home.ui.HomepageTestTag.PRIVATE_BROWSING_HOMEPAGE_BUTTON
import org.mozilla.fenix.theme.FirefoxTheme
import org.mozilla.fenix.wallpapers.WallpaperTheme

/**
 * Header for the homepage.
 *
 * @param isPrivate Whether the homepage is private.
 * @param showStoriesButton Whether to show the stories button or not.
 * @param browsingModeChanged Callback for when the browsing mode is toggled, with whether it is now private.
 * @param onStoriesTapped Callback for when the stories button is tapped.
 */
@Composable
fun HomepageHeader(
    isPrivate: Boolean,
    showStoriesButton: Boolean,
    browsingModeChanged: (isPrivate: Boolean) -> Unit,
    onStoriesTapped: () -> Unit,
) {
    // In normal mode the wordmark and private-browsing button follow the wallpaper text color; in
    // private mode there is no wallpaper, so the wordmark is untinted and the button uses its
    // dedicated private-mode icon color.
    val wordmarkTextColor =
        if (isPrivate) {
            null
        } else {
            WallpaperTheme.onWallpaper
        }
    val privateBrowsingButtonColor =
        if (isPrivate) {
            colorResource(getAttr(iconsR.attr.mozac_ic_private_mode_circle_fill_icon_color))
        } else {
            WallpaperTheme.onWallpaper
        }

    Column(
        modifier =
            Modifier.fillMaxWidth()
                .wrapContentHeight()
                .padding(
                    bottom = FirefoxTheme.layout.space.static500,
                    top = FirefoxTheme.layout.space.static100,
                )
                .padding(horizontal = FirefoxTheme.layout.space.static100),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            verticalAlignment = Alignment.Top,
        ) {
            if (showStoriesButton) {
                StoriesButton(color = WallpaperTheme.onWallpaper, onClick = onStoriesTapped)
            }

            Spacer(modifier = Modifier.weight(1f))

            PrivateBrowsingButton(
                color = privateBrowsingButtonColor,
                isPrivate = isPrivate,
                browsingModeChanged = browsingModeChanged,
            )
        }

        if (!isPrivate) {
            WordmarkAndLogo(wordmarkTextColor = wordmarkTextColor)
        }
    }
}

@Composable
private fun StoriesButton(
    color: Color,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, contentDescription = null) {
        Icon(
            tint = color,
            painter = painterResource(iconsR.drawable.mozac_ic_reading_list_24),
            contentDescription = stringResource(R.string.homepage_all_stories),
        )
    }
}

@Composable
private fun PrivateBrowsingButton(
    color: Color,
    isPrivate: Boolean,
    browsingModeChanged: (isPrivate: Boolean) -> Unit,
) {
    IconToggleButton(
        modifier =
            Modifier.background(
                    color = colorResource(getAttr(iconsR.attr.mozac_ic_private_mode_circle_fill_background_color)),
                    shape = CircleShape,
                )
                .semantics {
                    role = Role.Switch
                    testTagsAsResourceId = true
                    testTag = PRIVATE_BROWSING_HOMEPAGE_BUTTON
                },
        checked = isPrivate,
        onCheckedChange = {
            browsingModeChanged(!isPrivate)
        },
    ) {
        Icon(
            tint = color,
            painter = painterResource(iconsR.drawable.mozac_ic_private_mode_24),
            contentDescription = stringResource(R.string.content_description_private_browsing),
        )
    }
}

@Composable
internal fun getAttr(resId: Int): Int {
    val typedArray = LocalContext.current.obtainStyledAttributes(intArrayOf(resId))
    val newResId = typedArray.getResourceId(0, 0)
    typedArray.recycle()

    return newResId
}

@Preview
@Composable
private fun HomepageHeaderPreview(@PreviewParameter(PreviewThemeProvider::class) theme: Theme) {
    FirefoxTheme(theme) {
        Surface {
            HomepageHeader(
                isPrivate = false,
                showStoriesButton = true,
                browsingModeChanged = {},
                onStoriesTapped = {},
            )
        }
    }
}
