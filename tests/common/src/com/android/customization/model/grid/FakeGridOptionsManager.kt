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

package com.android.customization.model.grid

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeGridOptionsManager @Inject constructor() : GridOptionsManager2 {

    var isGridOptionAvailable: Boolean = true

    private var gridOptions: List<GridOptionModel>? = DEFAULT_GRID_OPTION_LIST

    override suspend fun isGridOptionAvailable(): Boolean = isGridOptionAvailable

    override suspend fun getGridOptions(): List<GridOptionModel>? = gridOptions

    override fun applyGridOption(gridName: String): Int {
        gridOptions = gridOptions?.map { it.copy(isCurrent = it.key == gridName) }
        return 0
    }

    companion object {
        val DEFAULT_GRID_OPTION_LIST =
            listOf(
                GridOptionModel(
                    key = "normal",
                    title = "5x5",
                    isCurrent = true,
                    rows = 5,
                    cols = 5,
                ),
                GridOptionModel(
                    key = "practical",
                    title = "4x5",
                    isCurrent = false,
                    rows = 5,
                    cols = 4,
                ),
            )
    }
}
