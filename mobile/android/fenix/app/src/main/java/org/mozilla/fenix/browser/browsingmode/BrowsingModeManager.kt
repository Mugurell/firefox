/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.browsingmode

import android.content.Intent
import org.mozilla.fenix.components.AppStore
import org.mozilla.fenix.utils.Settings

/** Enum that represents whether or not private browsing is active. */
enum class BrowsingMode {
    Normal,
    Private;

    /** Returns true if the [BrowsingMode] is [Private] */
    val isPrivate
        get() = this == Private

    companion object {

        /** Convert a boolean into a [BrowsingMode]. True corresponds to [Private] and false corresponds to [Normal]. */
        fun fromBoolean(isPrivate: Boolean) = if (isPrivate) Private else Normal
    }
}

interface BrowsingModeManager {
    var mode: BrowsingMode

    /**
     * Updates the [BrowsingMode] based on the [Intent] that started the activity.
     *
     * @param intent The [Intent] that started the activity.
     */
    fun updateMode(intent: Intent? = null)
}

/**
 * Default implementation of [BrowsingModeManager] that tracks the current [BrowsingMode] and synchronizes it with
 * [AppStore].
 *
 * @param intent The [Intent] that started the activity.
 * @param settings [Settings] used to read the last known browsing mode.
 * @param getSessionMode Returns the [BrowsingMode] of the session this activity is bound to, if any. It takes
 *   precedence over the [Intent] and the last known [BrowsingMode].
 * @param onModeChange Callback invoked when the browsing mode changes.
 */
class DefaultBrowsingModeManager(
    intent: Intent?,
    private val settings: Settings,
    private val getSessionMode: () -> BrowsingMode? = { null },
    private val onModeChange: (BrowsingMode) -> Unit,
) : BrowsingModeManager {
    override var mode: BrowsingMode = BrowsingMode.Normal
        set(value) {
            field = value
            onModeChange(value)
        }

    init {
        mode = resolveMode(intent)
    }

    override fun updateMode(intent: Intent?) {
        mode = resolveMode(intent)
    }

    private fun resolveMode(intent: Intent?) = resolveBrowsingMode(getSessionMode(), intent, settings.lastKnownMode)
}
