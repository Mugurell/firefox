/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.home.privatebrowsing.interactor

/** Interface for private browsing mode related actions. */
interface PrivateBrowsingInteractor {
    /**
     * Shows the Private Browsing Learn More page in a new tab. Called when a user clicks on the "Common myths about
     * private browsing" link in private mode.
     */
    fun onLearnMoreClicked()

    /**
     * Called when a user clicks on the Private Mode button on the homescreen.
     *
     * @param isPrivate Whether the user switches to private mode.
     */
    fun onPrivateModeButtonClicked(isPrivate: Boolean)
}
