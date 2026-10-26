/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Context
import androidx.annotation.VisibleForTesting
import mozilla.components.concept.menu.candidate.DividerMenuCandidate
import mozilla.components.concept.menu.candidate.MenuCandidate
import mozilla.components.ui.tabcounter.TabCounterMenu

class FenixTabCounterMenu(
    context: Context,
    onItemTapped: (Item) -> Unit,
    iconColor: Int? = null,
) : TabCounterMenu(context, onItemTapped, iconColor) {

    @VisibleForTesting internal fun menuItems(): List<MenuCandidate> = listOf(newTabItem, newPrivateTabItem)

    @VisibleForTesting
    internal fun menuItems(showOnlyPrivate: Boolean): List<MenuCandidate> {
        return if (showOnlyPrivate) listOf(newPrivateTabItem) else listOf(newTabItem)
    }

    @VisibleForTesting
    internal fun menuItems(toolbarPosition: ToolbarPosition): List<MenuCandidate> {
        val items =
            listOf(
                newTabItem,
                newPrivateTabItem,
                DividerMenuCandidate(),
                closeTabItem,
            )

        return when (toolbarPosition) {
            ToolbarPosition.BOTTOM -> items.reversed()
            ToolbarPosition.TOP -> items
        }
    }

    /** Update the displayed menu items with the new tab and new private tab menu items. */
    fun updateMenu() {
        menuController.submitList(menuItems())
    }

    /**
     * Update the displayed menu items.
     *
     * @param showOnlyPrivate Show only the new private tab item if true, else only the new tab item.
     */
    fun updateMenu(showOnlyPrivate: Boolean) {
        val items = menuItems(showOnlyPrivate)
        menuController.submitList(items)
    }

    /**
     * Update the displayed menu items.
     *
     * @param toolbarPosition Return a list that is ordered based on the given [ToolbarPosition].
     */
    fun updateMenu(toolbarPosition: ToolbarPosition) {
        menuController.submitList(menuItems(toolbarPosition))
    }
}
