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

/** Item spacing used by the RecyclerView. */
class KeyguardQuickAffordanceItemSpacing() : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val itemIndex = parent.getChildAdapterPosition(view)
        val columnIndex = itemIndex / 2
        val isRtl = parent.layoutManager?.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val density = parent.context.resources.displayMetrics.density

        val itemCount = parent.adapter?.itemCount ?: 0
        val columnCount = (itemCount + 1) / 2
        when {
            columnCount == 1 -> {
                outRect.left = EDGE_ITEM_HORIZONTAL_SPACING_DP.toPx(density)
                outRect.right = EDGE_ITEM_HORIZONTAL_SPACING_DP.toPx(density)
            }
            columnIndex > 0 && columnIndex < columnCount - 1 -> {
                outRect.left = COMMON_HORIZONTAL_SPACING_DP.toPx(density)
                outRect.right = COMMON_HORIZONTAL_SPACING_DP.toPx(density)
            }
            columnIndex == 0 -> {
                outRect.left =
                    if (!isRtl) EDGE_ITEM_HORIZONTAL_SPACING_DP.toPx(density)
                    else COMMON_HORIZONTAL_SPACING_DP.toPx(density)
                outRect.right =
                    if (isRtl) EDGE_ITEM_HORIZONTAL_SPACING_DP.toPx(density)
                    else COMMON_HORIZONTAL_SPACING_DP.toPx(density)
            }
            columnIndex == columnCount - 1 -> {
                outRect.right =
                    if (!isRtl) EDGE_ITEM_HORIZONTAL_SPACING_DP.toPx(density)
                    else COMMON_HORIZONTAL_SPACING_DP.toPx(density)
                outRect.left =
                    if (isRtl) EDGE_ITEM_HORIZONTAL_SPACING_DP.toPx(density)
                    else COMMON_HORIZONTAL_SPACING_DP.toPx(density)
            }
        }

        if (itemIndex % 2 == 0) {
            outRect.top = FIRST_ROW_TOP_SPACING_DP.toPx(density)
            outRect.bottom = FIRST_ROW_BOTTOM_SPACING_DP.toPx(density)
        } else {
            outRect.bottom = SECOND_ROW_BOTTOM_SPACING_DP.toPx(density)
        }
    }

    private fun Int.toPx(density: Float): Int {
        return (this * density).toInt()
    }

    companion object {
        const val EDGE_ITEM_HORIZONTAL_SPACING_DP = 20
        const val COMMON_HORIZONTAL_SPACING_DP = 9
        const val FIRST_ROW_TOP_SPACING_DP = 20
        const val FIRST_ROW_BOTTOM_SPACING_DP = 8
        const val SECOND_ROW_BOTTOM_SPACING_DP = 24
    }
}
