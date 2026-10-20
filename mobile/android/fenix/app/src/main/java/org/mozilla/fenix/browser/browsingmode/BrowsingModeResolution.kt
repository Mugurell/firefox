/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.browser.browsingmode

import android.content.Intent
import mozilla.components.support.utils.toSafeIntent
import org.mozilla.fenix.HomeActivity.Companion.PRIVATE_BROWSING_MODE

/**
 * Returns the [BrowsingMode] of an activity: the one of the session it is bound to, else the one set by the [intent],
 * else the [lastKnownMode].
 *
 * @param sessionMode The [BrowsingMode] of the session the activity is bound to, if any.
 * @param intent The [Intent] that started the activity.
 * @param lastKnownMode The last known [BrowsingMode].
 */
fun resolveBrowsingMode(sessionMode: BrowsingMode?, intent: Intent?, lastKnownMode: BrowsingMode): BrowsingMode {
    sessionMode?.let {
        return it
    }

    intent?.toSafeIntent()?.let {
        if (it.hasExtra(PRIVATE_BROWSING_MODE)) {
            return BrowsingMode.fromBoolean(isPrivate = it.getBooleanExtra(PRIVATE_BROWSING_MODE, false))
        }
    }

    return lastKnownMode
}
