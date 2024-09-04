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
package com.android.customization.picker.common.ui.view

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/** Item spacing used by the horizontal RecyclerView with only 1 row. */
class SingleRowListItemSpacing(
    private val edgeItemSpacePx: Int,
    private val itemHorizontalSpacePx: Int,
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State,
    ) {
        val itemIndex = parent.getChildAdapterPosition(view)
        val itemCount = parent.adapter?.itemCount ?: 0
        val isRtl = parent.layoutManager?.layoutDirection == View.LAYOUT_DIRECTION_RTL
        when (itemIndex) {
            0 -> {
                outRect.left = if (!isRtl) edgeItemSpacePx else itemHorizontalSpacePx
                outRect.right = if (isRtl) edgeItemSpacePx else itemHorizontalSpacePx
            }
            itemCount - 1 -> {
                outRect.right = if (!isRtl) edgeItemSpacePx else itemHorizontalSpacePx
                outRect.left = if (isRtl) edgeItemSpacePx else itemHorizontalSpacePx
            }
            else -> {
                outRect.left = itemHorizontalSpacePx
                outRect.right = itemHorizontalSpacePx
            }
        }
    }
}
