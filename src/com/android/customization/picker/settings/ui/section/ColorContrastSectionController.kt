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

package com.android.customization.picker.settings.ui.section

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.LayoutInflater
import androidx.lifecycle.LifecycleOwner
import com.android.customization.picker.settings.ui.binder.ColorContrastSectionViewBinder
import com.android.customization.picker.settings.ui.view.ColorContrastSectionView
import com.android.customization.picker.settings.ui.viewmodel.ColorContrastSectionViewModel
import com.android.themepicker.R
import com.android.wallpaper.model.CustomizationSectionController

class ColorContrastSectionController(
    private val viewModel: ColorContrastSectionViewModel,
    private val lifecycleOwner: LifecycleOwner,
) : CustomizationSectionController<ColorContrastSectionView> {

    // TODO (b/330381229): Check for whether the color contrast activity intent
    // resolves to something or not before marking the feature as available
    override fun isAvailable(context: Context): Boolean {
        return true
    }

    @SuppressLint("InflateParams") // We're okay not providing a parent view.
    override fun createView(context: Context): ColorContrastSectionView {
        val view =
            LayoutInflater.from(context)
                .inflate(
                    R.layout.color_contrast_section_view,
                    null,
                ) as ColorContrastSectionView
        ColorContrastSectionViewBinder.bind(
            view = view,
            lifecycleOwner = lifecycleOwner,
            viewModel = viewModel
        ) {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_COLOR_CONTRAST_SETTINGS))
        }
        return view
    }
}
