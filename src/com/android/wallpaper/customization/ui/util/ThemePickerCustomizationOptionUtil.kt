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

package com.android.wallpaper.customization.ui.util

import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.android.wallpaper.R
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil
import com.android.wallpaper.picker.customization.ui.util.DefaultCustomizationOptionUtil
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject

@ActivityScoped
class ThemePickerCustomizationOptionUtil
@Inject
constructor(private val defaultCustomizationOptionUtil: DefaultCustomizationOptionUtil) :
    CustomizationOptionUtil {

    enum class ThemePickerLockCustomizationOption : CustomizationOptionUtil.CustomizationOption {
        CLOCK,
        SHORTCUTS,
        SHOW_NOTIFICATIONS,
        MORE_LOCK_SCREEN_SETTINGS,
    }

    enum class ThemePickerHomeCustomizationOption : CustomizationOptionUtil.CustomizationOption {
        COLORS,
        APP_GRID,
        APP_SHAPE,
        THEMED_ICONS,
    }

    private var viewMap: Map<CustomizationOptionUtil.CustomizationOption, View>? = null

    override fun initBottomSheetContent(
        bottomSheetContainer: FrameLayout,
        layoutInflater: LayoutInflater
    ) {
        defaultCustomizationOptionUtil.initBottomSheetContent(bottomSheetContainer, layoutInflater)
        viewMap = buildMap {
            put(
                ThemePickerLockCustomizationOption.CLOCK,
                createCustomizationPickerBottomSheetView(
                        ThemePickerLockCustomizationOption.CLOCK,
                        bottomSheetContainer,
                        layoutInflater,
                    )
                    .also { bottomSheetContainer.addView(it) }
            )
            put(
                ThemePickerLockCustomizationOption.SHORTCUTS,
                createCustomizationPickerBottomSheetView(
                        ThemePickerLockCustomizationOption.SHORTCUTS,
                        bottomSheetContainer,
                        layoutInflater,
                    )
                    .also { bottomSheetContainer.addView(it) }
            )
        }
    }

    override fun getBottomSheetContent(option: CustomizationOptionUtil.CustomizationOption): View? {
        return defaultCustomizationOptionUtil.getBottomSheetContent(option) ?: viewMap?.get(option)
    }

    override fun onDestroy() {
        viewMap = null
    }

    private fun createCustomizationPickerBottomSheetView(
        option: ThemePickerLockCustomizationOption,
        bottomSheetContainer: FrameLayout,
        layoutInflater: LayoutInflater,
    ): View =
        when (option) {
            ThemePickerLockCustomizationOption.CLOCK -> R.layout.bottom_sheet_clock
            ThemePickerLockCustomizationOption.SHORTCUTS -> R.layout.bottom_sheet_shortcut
            else ->
                throw IllegalStateException(
                    "Customization option $option does not have a bottom sheet view"
                )
        }.let { layoutInflater.inflate(it, bottomSheetContainer, false) }
}
