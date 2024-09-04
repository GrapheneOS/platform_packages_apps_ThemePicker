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

import android.content.ContentValues
import android.content.Context
import com.android.wallpaper.R
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.util.PreviewUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class DefaultGridOptionsManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @BackgroundDispatcher private val bgDispatcher: CoroutineDispatcher,
) : GridOptionsManager2 {

    private val authorityMetadataKey: String =
        context.getString(R.string.grid_control_metadata_name)
    private val previewUtils: PreviewUtils = PreviewUtils(context, authorityMetadataKey)

    override suspend fun isGridOptionAvailable(): Boolean {
        return previewUtils.supportsPreview() && (getGridOptions()?.size ?: 0) > 1
    }

    override suspend fun getGridOptions(): List<GridOptionModel>? =
        withContext(bgDispatcher) {
            context.contentResolver
                .query(previewUtils.getUri(LIST_OPTIONS), null, null, null, null)
                ?.use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            val rows = cursor.getInt(cursor.getColumnIndex(COL_ROWS))
                            val cols = cursor.getInt(cursor.getColumnIndex(COL_COLS))
                            add(
                                GridOptionModel(
                                    key = cursor.getString(cursor.getColumnIndex(COL_NAME)),
                                    title =
                                        context.getString(
                                            com.android.themepicker.R.string.grid_title_pattern,
                                            cols,
                                            rows
                                        ),
                                    isCurrent =
                                        cursor
                                            .getString(cursor.getColumnIndex(COL_IS_DEFAULT))
                                            .toBoolean(),
                                    rows = rows,
                                    cols = cols,
                                )
                            )
                        }
                    }
                }
        }

    override fun applyGridOption(gridName: String): Int {
        return context.contentResolver.update(
            previewUtils.getUri(DEFAULT_GRID),
            ContentValues().apply { put("name", gridName) },
            null,
            null,
        )
    }

    companion object {
        const val LIST_OPTIONS: String = "list_options"
        const val DEFAULT_GRID: String = "default_grid"
        const val COL_NAME: String = "name"
        const val COL_ROWS: String = "rows"
        const val COL_COLS: String = "cols"
        const val COL_IS_DEFAULT: String = "is_default"
    }
}
