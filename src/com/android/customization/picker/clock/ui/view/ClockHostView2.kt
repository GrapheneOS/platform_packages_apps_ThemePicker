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
package com.android.customization.picker.clock.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.MeasureSpec.EXACTLY
import android.widget.FrameLayout
import com.android.customization.picker.clock.shared.ClockSize
import com.android.wallpaper.util.ScreenSizeCalculator

/**
 * Parent view for the clock view. We will calculate the current display size and the preview size
 * and scale down the clock view to fit in the preview.
 */
class ClockHostView2(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {

    var clockSize: ClockSize = ClockSize.DYNAMIC
        set(value) {
            if (field != value) {
                field = value
                updatePivotAndScale()
                invalidate()
            }
        }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        updatePivotAndScale()
    }

    override fun measureChildWithMargins(
        child: View?,
        parentWidthMeasureSpec: Int,
        widthUsed: Int,
        parentHeightMeasureSpec: Int,
        heightUsed: Int,
    ) {
        val screenSize = ScreenSizeCalculator.getInstance().getScreenSize(display)
        super.measureChildWithMargins(
            child,
            MeasureSpec.makeMeasureSpec(screenSize.x, EXACTLY),
            widthUsed,
            MeasureSpec.makeMeasureSpec(screenSize.y, EXACTLY),
            heightUsed,
        )
    }

    private fun updatePivotAndScale() {
        when (clockSize) {
            ClockSize.DYNAMIC -> {
                resetPivot()
            }
            ClockSize.SMALL -> {
                pivotX = getCenteredHostViewPivotX(this)
                pivotY = 0F
            }
        }
        val screenSize = ScreenSizeCalculator.getInstance().getScreenSize(display)
        val ratio = measuredWidth / screenSize.x.toFloat()
        scaleX = ratio
        scaleY = ratio
    }

    companion object {
        fun getCenteredHostViewPivotX(hostView: View): Float {
            return if (hostView.isLayoutRtl) hostView.width.toFloat() else 0F
        }
    }
}
