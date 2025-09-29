/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.samples.compose.browser.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import mozilla.components.support.ktx.android.view.createWindowInsetsController
import mozilla.components.support.utils.ext.getActivityWindow

/**
 * Screen displaying the settings of the browser.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val windowInsetsController = context.getActivityWindow()?.createWindowInsetsController()
        windowInsetsController?.isAppearanceLightStatusBars = true
        windowInsetsController?.isAppearanceLightNavigationBars = false
    }

    Scaffold(
        modifier = Modifier
            .clip(RoundedCornerShape(size = 32.dp)),
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        },
                    ) {
                        Icon(
                            painter = painterResource(android.R.drawable.arrow_up_float),
                            contentDescription = "Go back",
                            tint = Color.Black,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = Color.Black,
                    containerColor = Color.Transparent, // to show the scaffold color from underneath
                ),
            )
        },
        bottomBar = {
            Text("")
        },
        containerColor = Color.Yellow,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            Text(
                text = "This is just a sample edge-to-edge screen",
                modifier = Modifier
                    .align(Alignment.Center),
            )
        }
    }
}
