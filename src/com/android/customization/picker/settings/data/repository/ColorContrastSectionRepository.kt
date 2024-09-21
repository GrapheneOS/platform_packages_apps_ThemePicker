/*
 * Copyright (C) 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.customization.picker.settings.data.repository

import android.app.UiModeManager
import android.app.UiModeManager.ContrastUtils
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.system.UiModeManagerWrapper
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Singleton
class ColorContrastSectionRepository
@Inject
constructor(
    uiModeManager: UiModeManagerWrapper,
    @BackgroundDispatcher bgDispatcher: CoroutineDispatcher,
) {
    var contrast: Flow<Int> = callbackFlow {
        val executor: Executor = bgDispatcher.asExecutor()
        val listener =
            UiModeManager.ContrastChangeListener { contrast ->
                // Emit the new contrast value whenever it changes
                trySend(ContrastUtils.toContrastLevel(contrast))
            }

        // Emit the current contrast value immediately
        uiModeManager.getContrast()?.let { currentContrast ->
            trySend(ContrastUtils.toContrastLevel(currentContrast))
        }

        uiModeManager.addContrastChangeListener(executor, listener)

        awaitClose {
            // Unregister the listener when the flow collection is cancelled or no longer in use
            uiModeManager.removeContrastChangeListener(listener)
        }
    }
}
