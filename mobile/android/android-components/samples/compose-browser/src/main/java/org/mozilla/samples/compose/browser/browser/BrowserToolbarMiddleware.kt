/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.compose.browser.browser

import androidx.lifecycle.Lifecycle.State.RESUMED
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import mozilla.components.browser.state.selector.selectedTab
import mozilla.components.compose.browser.toolbar.concept.Action.ActionButtonRes
import mozilla.components.compose.browser.toolbar.concept.Action.TabCounterAction
import mozilla.components.compose.browser.toolbar.concept.PageOrigin
import mozilla.components.compose.browser.toolbar.store.BrowserDisplayToolbarAction.BrowserActionsEndUpdated
import mozilla.components.compose.browser.toolbar.store.BrowserDisplayToolbarAction.PageOriginUpdated
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarAction
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarAction.CommitUrl
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarAction.ToggleEditMode
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarInteraction.BrowserToolbarEvent
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarInteraction.BrowserToolbarMenu
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarMenuItem.BrowserToolbarMenuButton
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarMenuItem.BrowserToolbarMenuButton.ContentDescription
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarMenuItem.BrowserToolbarMenuButton.Icon
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarMenuItem.BrowserToolbarMenuButton.Text
import mozilla.components.compose.browser.toolbar.store.BrowserToolbarState
import mozilla.components.compose.browser.toolbar.store.DisplayState
import mozilla.components.compose.browser.toolbar.store.EditState
import mozilla.components.compose.browser.toolbar.store.Mode
import mozilla.components.compose.browser.toolbar.store.ToolbarGravity
import mozilla.components.lib.state.Action
import mozilla.components.lib.state.Middleware
import mozilla.components.lib.state.MiddlewareContext
import mozilla.components.lib.state.State
import mozilla.components.lib.state.Store
import mozilla.components.lib.state.ext.flow
import org.mozilla.samples.compose.browser.BrowserComposeActivity.Companion.ROUTE_SETTINGS
import org.mozilla.samples.compose.browser.Components
import org.mozilla.samples.compose.browser.R
import org.mozilla.samples.compose.browser.browser.DisplayBrowserActionsInteractions.TabCounterClicked
import org.mozilla.samples.compose.browser.browser.DisplayPageActionsEndInteractions.RefreshClicked
import org.mozilla.samples.compose.browser.browser.DisplayPageOriginInteractions.PageOriginClicked
import org.mozilla.samples.compose.browser.browser.EditActionsInteractions.ClearClicked
import mozilla.components.ui.icons.R as iconsR

private sealed class DisplayPageOriginInteractions : BrowserToolbarEvent {
    data object PageOriginClicked : DisplayPageOriginInteractions()
}

private sealed class DisplayPageActionsEndInteractions : BrowserToolbarEvent {
    data object RefreshClicked : DisplayPageActionsEndInteractions()
}

private sealed class DisplayBrowserActionsInteractions : BrowserToolbarEvent {
    data object TabCounterClicked : DisplayBrowserActionsInteractions()
}

private sealed class MenuInteractions : BrowserToolbarEvent {
    data object SettingsClicked : MenuInteractions()
}

private sealed class EditActionsInteractions : BrowserToolbarEvent {
    data object ClearClicked : EditActionsInteractions()
}

internal class BrowserToolbarMiddleware(
    initialDependencies: Dependencies,
) : Middleware<BrowserToolbarState, BrowserToolbarAction> {
    var dependencies = initialDependencies

    override fun invoke(
        context: MiddlewareContext<BrowserToolbarState, BrowserToolbarAction>,
        next: (BrowserToolbarAction) -> Unit,
        action: BrowserToolbarAction,
    ) {
        when (action) {
            is BrowserToolbarAction.Init -> {
                next(buildInitialState())
                observePageOriginUpdates(context)
                observeTabCounterUpdates(context)
            }

            is PageOriginClicked -> {
                next(ToggleEditMode(true))
            }

            is TabCounterClicked -> {
                dependencies.browserScreenStore.dispatch(BrowserScreenAction.ShowTabs)
            }

            is MenuInteractions.SettingsClicked -> {
                dependencies.navController.navigate(ROUTE_SETTINGS)
            }

            is CommitUrl -> {
                dependencies.components.sessionUseCases.loadUrl(action.text)
                next(ToggleEditMode(false))
            }

            else -> {
                next(action)
            }
        }
    }

    private fun buildInitialState() = BrowserToolbarAction.Init(
        mode = Mode.DISPLAY,
        displayState = DisplayState(
            pageOrigin = PageOrigin(
                hint = R.string.toolbar_search_hint,
                title = null,
                url = null,
                onClick = PageOriginClicked,
            ),
            pageActionsEnd = buildDisplayPageActions(),
            browserActionsEnd = buildDisplayBrowserActions(),
        ),
        editState = EditState(
            editActionsEnd = buildEditPageActionsEnd(),
        ),
        gravity = ToolbarGravity.Bottom,
    )

    private fun buildDisplayPageActions() = listOf(
        ActionButtonRes(
            drawableResId = iconsR.drawable.mozac_ic_arrow_clockwise_24,
            contentDescription = R.string.page_action_refresh_description,
            onClick = RefreshClicked,
        ),
    )

    private fun buildDisplayBrowserActions() = listOf(
        TabCounterAction(
            count = dependencies.components.store.state.tabs.size,
            contentDescription = "Tab counter",
            showPrivacyMask = false,
            onClick = TabCounterClicked,
        ),

        ActionButtonRes(
            drawableResId = iconsR.drawable.mozac_ic_ellipsis_vertical_24,
            contentDescription = R.string.menu_button_description,
            onClick = BrowserToolbarMenu {
                listOf(
                    BrowserToolbarMenuButton(
                        icon = Icon.DrawableResIcon(iconsR.drawable.mozac_ic_settings_24),
                        text = Text.StringResText(R.string.menu_item_settings),
                        contentDescription = ContentDescription.StringResContentDescription(
                            R.string.menu_item_settings_description,
                        ),
                        onClick = MenuInteractions.SettingsClicked,
                    ),
                )
            },
        ),
    )

    private fun buildEditPageActionsEnd() = listOf(
        ActionButtonRes(
            drawableResId = iconsR.drawable.mozac_ic_stop,
            contentDescription = R.string.clear_input_description,
            onClick = ClearClicked,
        ),
    )

    private fun observePageOriginUpdates(
        context: MiddlewareContext<BrowserToolbarState, BrowserToolbarAction>,
    ) {
        dependencies.components.store.observeWhileActive {
            distinctUntilChangedBy { it.selectedTab?.content?.url }
                .collect {
                    context.dispatch(
                        PageOriginUpdated(
                            context.state.displayState.pageOrigin.copy(
                                url = (it.selectedTab?.content?.url ?: ""),
                            ),
                        ),
                    )
                }
        }
    }

    private fun observeTabCounterUpdates(
        context: MiddlewareContext<BrowserToolbarState, BrowserToolbarAction>,
    ) {
        dependencies.components.store.observeWhileActive {
            distinctUntilChangedBy { it.tabs.size }
                .collect {
                    context.dispatch(BrowserActionsEndUpdated(buildDisplayBrowserActions()))
                }
        }
    }

    private inline fun <S : State, A : Action> Store<S, A>.observeWhileActive(
        crossinline observe: suspend (Flow<S>.() -> Unit),
    ): Job? = dependencies.lifecycleOwner.run {
        lifecycleScope.launch {
            repeatOnLifecycle(RESUMED) {
                flow().observe()
            }
        }
    }

    companion object {
        data class Dependencies(
            val lifecycleOwner: LifecycleOwner,
            val components: Components,
            val navController: NavController,
            val browserScreenStore: Store<BrowserScreenState, BrowserScreenAction>,
        )
    }
}
