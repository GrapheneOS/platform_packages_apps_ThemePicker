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

import android.content.Context
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.compose.ui.platform.ComposeView
import com.android.customization.picker.mode.shared.util.DarkModeLifecycleUtil
import com.android.themepicker.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.customization.ui.compose.ColorFloatingSheet
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.APP_ICONS
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.COLORS
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.COLOR_CONTRAST
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.GRID
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.PACK_THEME
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.CLOCK
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.LOCK_SCREEN_NOTIFICATIONS
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.MORE_LOCK_SCREEN_SETTINGS
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.SHORTCUTS
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsData
import com.android.wallpaper.model.Screen
import com.android.wallpaper.model.Screen.HOME_SCREEN
import com.android.wallpaper.model.Screen.LOCK_SCREEN
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionViewUtil
import com.android.wallpaper.picker.customization.ui.util.DefaultCustomizationOptionViewUtil
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsData
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import javax.inject.Inject
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption.ICON_SHAPE

@ActivityScoped
class ThemePickerCustomizationOptionViewUtil
@Inject
constructor(
    private val defaultCustomizationOptionUtil: DefaultCustomizationOptionViewUtil,
    @ActivityContext private val context: Context,
) : CustomizationOptionViewUtil {

    // Instantiate DarkModeLifecycleUtil for it to observe lifecycle and update DarkModeRepository
    @Inject lateinit var darkModeLifecycleUtil: DarkModeLifecycleUtil

    override fun getOptionEntries(
        customizationOptionsData: CustomizationOptionsData,
        screen: Screen,
        optionContainer: LinearLayout,
        layoutInflater: LayoutInflater,
    ): List<Pair<CustomizationOptionUtil.CustomizationOption, View>> {
        customizationOptionsData as ThemePickerCustomizationOptionsData
        val isKeyguardQuickAffordanceEnabled =
            BaseFlags.get().isKeyguardQuickAffordanceEnabled(optionContainer.context)
        val showPackEntry =
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.PACK_THEME_FEATURE_ENABLED,
                /* def= */ 0,
            ) == 1
        val defaultOptionEntries =
            defaultCustomizationOptionUtil.getOptionEntries(
                customizationOptionsData = customizationOptionsData,
                screen = screen,
                optionContainer = optionContainer,
                layoutInflater = layoutInflater,
            )
        return when (screen) {
            LOCK_SCREEN ->
                buildList {
                    addAll(defaultOptionEntries)
                    if (BaseFlags.get().isPackThemeEnabled() && showPackEntry) {
                        add(
                            PACK_THEME to
                                layoutInflater.inflate(
                                    R.layout.customization_option_entry_pack_theme,
                                    optionContainer,
                                    false,
                                )
                        )
                    }
                    add(
                        CLOCK to
                            layoutInflater.inflate(
                                R.layout.customization_option_entry_clock,
                                optionContainer,
                                false,
                            )
                    )
                    if (isKeyguardQuickAffordanceEnabled) {
                        add(
                            SHORTCUTS to
                                layoutInflater.inflate(
                                    R.layout.customization_option_entry_keyguard_quick_affordance,
                                    optionContainer,
                                    false,
                                )
                        )
                    }
                    add(
                        LOCK_SCREEN_NOTIFICATIONS to
                            layoutInflater.inflate(
                                R.layout.customization_option_entry_lock_screen_notifications,
                                optionContainer,
                                false,
                            )
                    )
                    add(
                        MORE_LOCK_SCREEN_SETTINGS to
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

                    if (BaseFlags.get().isPackThemeEnabled() && showPackEntry) {
                        add(
                            PACK_THEME to
                                layoutInflater.inflate(
                                    R.layout.customization_option_entry_pack_theme,
                                    optionContainer,
                                    false,
                                )
                        )
                    }
                    add(
                        COLORS to
                            layoutInflater.inflate(
                                R.layout.customization_option_entry_colors,
                                optionContainer,
                                false,
                            )
                    )
                    add(
                        COLOR_CONTRAST to
                            layoutInflater.inflate(
                                R.layout.customization_option_entry_color_contrast,
                                optionContainer,
                                false,
                            )
                    )
                    if (
                        customizationOptionsData.isIconStyleAvailable ||
                            customizationOptionsData.isShapeAvailable
                    )
                        add(
                            APP_ICONS to
                                layoutInflater.inflate(
                                    R.layout.customization_option_entry_app_icons,
                                    optionContainer,
                                    false,
                                )
                        )
                    if (customizationOptionsData.isGridCustomizationAvailable) {
                        add(
                            GRID to
                                layoutInflater.inflate(
                                    R.layout.customization_option_entry_grid,
                                    optionContainer,
                                    false,
                                )
                        )
                    }
                    // [ADD THIS BLOCK] ----------------------------------------
                    // Manually create the Icon Shape button view
                    // We assume ICON_SHAPE is available (or you can check a flag if you have one)
                    add(
                        ICON_SHAPE to
                            layoutInflater.inflate(
                                // We reuse the Grid layout because it's just a standard "Icon + Text" button
                                R.layout.customization_option_entry_grid, 
                                optionContainer,
                                false,
                            )
                    )
                    // ---------------------------------------------------------
                }
        }
    }

    override fun initFloatingSheet(
        customizationOptionsData: CustomizationOptionsData,
        bottomSheetContainer: FrameLayout,
        layoutInflater: LayoutInflater,
    ): Map<CustomizationOptionUtil.CustomizationOption, View> {
        customizationOptionsData as ThemePickerCustomizationOptionsData
        val map =
            defaultCustomizationOptionUtil.initFloatingSheet(
                customizationOptionsData = customizationOptionsData,
                bottomSheetContainer = bottomSheetContainer,
                layoutInflater = layoutInflater,
            )
        val isComposeRefactorEnabled = BaseFlags.get().isComposeRefactorEnabled()
        val isKeyguardQuickAffordanceEnabled =
            BaseFlags.get().isKeyguardQuickAffordanceEnabled(bottomSheetContainer.context)
        return buildMap {
            putAll(map)
            put(
                CLOCK,
                inflateFloatingSheet(CLOCK, bottomSheetContainer, layoutInflater).also {
                    bottomSheetContainer.addView(it)
                },
            )
            if (isKeyguardQuickAffordanceEnabled) {
                put(
                    SHORTCUTS,
                    if (isComposeRefactorEnabled) {
                            ComposeView(context)
                        } else {
                            inflateFloatingSheet(SHORTCUTS, bottomSheetContainer, layoutInflater)
                        }
                        .also { bottomSheetContainer.addView(it) },
                )
            }

            put(
                COLORS,
                if (isComposeRefactorEnabled) {
                        ComposeView(context).apply { setContent { ColorFloatingSheet() } }
                    } else {
                        inflateFloatingSheet(COLORS, bottomSheetContainer, layoutInflater)
                    }
                    .also { bottomSheetContainer.addView(it) },
            )
            put(APP_ICONS, inflateFloatingSheet(APP_ICONS, bottomSheetContainer, layoutInflater))
            if (customizationOptionsData.isGridCustomizationAvailable) {
                put(
                    GRID,
                    inflateFloatingSheet(GRID, bottomSheetContainer, layoutInflater).also {
                        bottomSheetContainer.addView(it)
                    },
                )
            }
        }
    }

    override fun createClockPreviewAndAddToParent(
        parentView: ViewGroup,
        layoutInflater: LayoutInflater,
    ): View? {
        val clockHostView = layoutInflater.inflate(R.layout.clock_host_view, parentView, false)
        parentView.addView(clockHostView)
        return clockHostView
    }

    private fun inflateFloatingSheet(
        option: CustomizationOptionUtil.CustomizationOption,
        bottomSheetContainer: FrameLayout,
        layoutInflater: LayoutInflater,
    ): View =
        when (option) {
            CLOCK -> R.layout.floating_sheet_clock
            SHORTCUTS -> R.layout.floating_sheet_shortcut
            COLORS -> R.layout.floating_sheet_colors
            APP_ICONS -> R.layout.floating_sheet_app_icon
            GRID -> R.layout.floating_sheet_grid
            else ->
                throw IllegalStateException(
                    "Customization option $option does not have a bottom sheet view"
                )
        }.let { layoutInflater.inflate(it, bottomSheetContainer, false) }
}
