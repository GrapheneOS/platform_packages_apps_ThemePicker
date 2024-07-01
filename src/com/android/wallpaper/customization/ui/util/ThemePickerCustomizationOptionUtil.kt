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
import android.widget.LinearLayout
import com.android.themepicker.R
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
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

    override fun getOptionEntries(
        screen: Screen,
        optionContainer: LinearLayout,
        layoutInflater: LayoutInflater,
    ): List<Pair<CustomizationOptionUtil.CustomizationOption, View>> {
        val defaultOptionEntries =
            defaultCustomizationOptionUtil.getOptionEntries(
                screen,
                optionContainer,
                layoutInflater,
            )
        return when (screen) {
            LOCK_SCREEN ->
                buildList {
                    addAll(defaultOptionEntries)
                    add(
                        ThemePickerLockCustomizationOption.CLOCK to
                            layoutInflater.inflate(
                                R.layout.customization_option_entry_clock,
                                optionContainer,
                                false,
                            )
                    )
                    add(
                        ThemePickerLockCustomizationOption.SHORTCUTS to
                            layoutInflater.inflate(
                                R.layout.customization_option_entry_keyguard_quick_affordance,
                                optionContainer,
                                false
                            )
                    )
                    add(
                        ThemePickerLockCustomizationOption.SHOW_NOTIFICATIONS to
                            layoutInflater.inflate(
                                R.layout.customization_option_entry_show_notifications,
                                optionContainer,
                                false,
                            )
                    )
                    add(
                        ThemePickerLockCustomizationOption.MORE_LOCK_SCREEN_SETTINGS to
                            layoutInflater.inflate(
                                R.layout.customization_option_entry_more_lock_settings,
                                optionContainer,
                                false,
                            )
                    )
                }
            HOME_SCREEN ->
                buildList {
                    addAll(defaultOptionEntries)
                    add(
                        ThemePickerHomeCustomizationOption.COLORS to
                            layoutInflater.inflate(
                                R.layout.customization_option_entry_colors,
                                optionContainer,
                                false,
                            )
                    )
                    add(
                        ThemePickerHomeCustomizationOption.APP_GRID to
                            layoutInflater.inflate(
                                R.layout.customization_option_entry_app_grid,
                                optionContainer,
                                false,
                            )
                    )
                    add(
                        ThemePickerHomeCustomizationOption.APP_SHAPE to
                            layoutInflater.inflate(
                                R.layout.customization_option_entry_app_shape,
                                optionContainer,
                                false,
                            )
                    )
                    add(
                        ThemePickerHomeCustomizationOption.THEMED_ICONS to
                            layoutInflater.inflate(
                                R.layout.customization_option_entry_themed_icons,
                                optionContainer,
                                false,
                            )
                    )
                }
        }
    }

    override fun initFloatingSheet(
        bottomSheetContainer: FrameLayout,
        layoutInflater: LayoutInflater
    ): Map<CustomizationOptionUtil.CustomizationOption, View> {
        val map =
            defaultCustomizationOptionUtil.initFloatingSheet(bottomSheetContainer, layoutInflater)
        return buildMap {
            putAll(map)
            put(
                ThemePickerLockCustomizationOption.CLOCK,
                inflateFloatingSheet(
                        ThemePickerLockCustomizationOption.CLOCK,
                        bottomSheetContainer,
                        layoutInflater,
                    )
                    .also { bottomSheetContainer.addView(it) }
            )
            put(
                ThemePickerLockCustomizationOption.SHORTCUTS,
                inflateFloatingSheet(
                        ThemePickerLockCustomizationOption.SHORTCUTS,
                        bottomSheetContainer,
                        layoutInflater,
                    )
                    .also { bottomSheetContainer.addView(it) }
            )
        }
    }

    private fun inflateFloatingSheet(
        option: ThemePickerLockCustomizationOption,
        bottomSheetContainer: FrameLayout,
        layoutInflater: LayoutInflater,
    ): View =
        when (option) {
            ThemePickerLockCustomizationOption.CLOCK -> R.layout.floating_sheet_clock
            ThemePickerLockCustomizationOption.SHORTCUTS -> R.layout.floating_sheet_shortcut
            else ->
                throw IllegalStateException(
                    "Customization option $option does not have a bottom sheet view"
                )
        }.let { layoutInflater.inflate(it, bottomSheetContainer, false) }
}
