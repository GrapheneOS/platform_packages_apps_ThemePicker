/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.customization.picker.common.ui.view

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/** Item spacing used by the horizontal RecyclerView with 2 rows. */
class DoubleRowListItemSpacing(
    private val edgeItemSpacePx: Int,
    private val itemHorizontalSpacePx: Int,
    private val itemVerticalSpacePx: Int,
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val itemIndex = parent.getChildAdapterPosition(view)
        val columnIndex = itemIndex / 2
        val isRtl = parent.layoutManager?.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val density = parent.context.resources.displayMetrics.density

        val itemCount = parent.adapter?.itemCount ?: 0
        val columnCount = (itemCount + 1) / 2
        when {
            columnCount == 1 -> {
                outRect.left = edgeItemSpacePx
                outRect.right = edgeItemSpacePx
            }
            columnIndex > 0 && columnIndex < columnCount - 1 -> {
                outRect.left = itemHorizontalSpacePx
                outRect.right = itemHorizontalSpacePx
            }
            columnIndex == 0 -> {
                outRect.left = if (!isRtl) edgeItemSpacePx else itemHorizontalSpacePx
                outRect.right = if (isRtl) edgeItemSpacePx else itemHorizontalSpacePx
            }
            columnIndex == columnCount - 1 -> {
                outRect.right = if (!isRtl) edgeItemSpacePx else itemHorizontalSpacePx
                outRect.left = if (isRtl) edgeItemSpacePx else itemHorizontalSpacePx
            }
        }

        if (itemIndex % 2 == 0) {
            outRect.bottom = itemVerticalSpacePx
        }
    }
}
