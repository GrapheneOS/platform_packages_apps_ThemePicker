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
 *
 */

package com.android.customization.picker.grid.data.repository

import com.android.customization.model.grid.GridOptionModel
import com.android.customization.model.grid.GridOptionsManager2
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Singleton
class GridRepository2
@Inject
constructor(
    private val manager: GridOptionsManager2,
    @BackgroundDispatcher private val bgScope: CoroutineScope,
    @BackgroundDispatcher private val bgDispatcher: CoroutineDispatcher,
) {

    suspend fun isGridOptionAvailable(): Boolean =
        withContext(bgDispatcher) { manager.isGridOptionAvailable() }

    private val _gridOptions = MutableStateFlow<List<GridOptionModel>?>(null)

    init {
        bgScope.launch {
            val options = manager.getGridOptions()
            _gridOptions.value = options
        }
    }

    val gridOptions: StateFlow<List<GridOptionModel>?> = _gridOptions.asStateFlow()

    val selectedGridOption: Flow<GridOptionModel?> =
        gridOptions.map { gridOptions -> gridOptions?.firstOrNull { it.isCurrent } }

    suspend fun applySelectedOption(key: String) =
        withContext(bgDispatcher) {
            manager.applyGridOption(key)
            // After applying new grid option, we should query and update the grid options again.
            _gridOptions.value = manager.getGridOptions()
        }
}
