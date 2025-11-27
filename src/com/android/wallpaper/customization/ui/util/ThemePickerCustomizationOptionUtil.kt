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

import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.SHORTCUTS
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.util.DefaultCustomizationOptionUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePickerCustomizationOptionUtil
@Inject
constructor(private val defaultCustomizationOptionUtil: DefaultCustomizationOptionUtil) :
    CustomizationOptionUtil {

    enum class ThemePickerLockCustomizationOption : CustomizationOption {
        CLOCK,
        SHORTCUTS,
        LOCK_SCREEN_NOTIFICATIONS,
        MORE_LOCK_SCREEN_SETTINGS,
    }

    enum class ThemePickerHomeCustomizationOption : CustomizationOption {
        PACK_THEME,
        COLORS,
        COLOR_CONTRAST,
        APP_ICONS,
        GRID,
        ICON_SHAPE,
    }

    override fun getCustomizationOptionFromDestination(destination: String): CustomizationOption? {
        return defaultCustomizationOptionUtil.getCustomizationOptionFromDestination(destination)
            ?: when (destination) {
                DESTINATION_QUICK_AFFORDANCES -> SHORTCUTS
                else -> null
            }
    }

    companion object {
        const val DESTINATION_QUICK_AFFORDANCES = "quick_affordances"
    }
}
