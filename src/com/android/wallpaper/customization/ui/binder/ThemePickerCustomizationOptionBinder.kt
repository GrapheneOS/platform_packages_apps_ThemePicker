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

package com.android.wallpaper.customization.ui.binder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.common.icon.ui.viewbinder.IconViewBinder
import com.android.wallpaper.picker.common.text.ui.viewbinder.TextViewBinder
import com.android.wallpaper.picker.customization.ui.binder.CustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.DefaultCustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch

@Singleton
class ThemePickerCustomizationOptionsBinder
@Inject
constructor(private val defaultCustomizationOptionsBinder: DefaultCustomizationOptionsBinder) :
    CustomizationOptionsBinder {

    override fun bind(
        view: View,
        lockScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        homeScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        customizationOptionFloatingSheetViewMap: Map<CustomizationOption, View>?,
        viewModel: CustomizationOptionsViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        defaultCustomizationOptionsBinder.bind(
            view,
            lockScreenCustomizationOptionEntries,
            homeScreenCustomizationOptionEntries,
            customizationOptionFloatingSheetViewMap,
            viewModel,
            lifecycleOwner
        )

        val optionClock =
            lockScreenCustomizationOptionEntries
                .find { it.first == ThemePickerLockCustomizationOption.CLOCK }
                ?.second

        val optionShortcut =
            lockScreenCustomizationOptionEntries
                .find { it.first == ThemePickerLockCustomizationOption.SHORTCUTS }
                ?.second
        val optionShortcutDescription =
            optionShortcut?.findViewById<TextView>(
                R.id.option_entry_keyguard_quick_affordance_description
            )
        val optionShortcutIcon1 =
            optionShortcut?.findViewById<ImageView>(
                R.id.option_entry_keyguard_quick_affordance_icon_1
            )
        val optionShortcutIcon2 =
            optionShortcut?.findViewById<ImageView>(
                R.id.option_entry_keyguard_quick_affordance_icon_2
            )

        val optionColors =
            homeScreenCustomizationOptionEntries
                .find { it.first == ThemePickerHomeCustomizationOption.COLORS }
                ?.second

        viewModel as ThemePickerCustomizationOptionsViewModel
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.onCustomizeClockClicked.collect {
                        optionClock?.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    viewModel.onCustomizeShortcutClicked.collect {
                        optionShortcut?.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    viewModel.keyguardQuickAffordancePickerViewModel2.summary.collect { summary ->
                        optionShortcutDescription?.let {
                            TextViewBinder.bind(
                                view = it,
                                viewModel = summary.description,
                            )
                        }
                        summary.icon1?.let { icon ->
                            optionShortcutIcon1?.let {
                                IconViewBinder.bind(
                                    view = it,
                                    viewModel = icon,
                                )
                            }
                        }
                        optionShortcutIcon1?.isVisible = summary.icon1 != null

                        summary.icon2?.let { icon ->
                            optionShortcutIcon2?.let {
                                IconViewBinder.bind(
                                    view = it,
                                    viewModel = icon,
                                )
                            }
                        }
                        optionShortcutIcon2?.isVisible = summary.icon2 != null
                    }
                }

                launch {
                    viewModel.onCustomizeColorsClicked.collect {
                        optionColors?.setOnClickListener { _ -> it?.invoke() }
                    }
                }
            }
        }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerLockCustomizationOption.CLOCK)
            ?.let {
                ClockFloatingSheetBinder.bind(
                    it,
                    viewModel.clockPickerViewModel,
                    lifecycleOwner,
                )
            }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerLockCustomizationOption.SHORTCUTS)
            ?.let {
                ShortcutFloatingSheetBinder.bind(
                    it,
                    viewModel.keyguardQuickAffordancePickerViewModel2,
                    lifecycleOwner,
                )
            }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerHomeCustomizationOption.COLORS)
            ?.let {
                ColorsFloatingSheetBinder.bind(
                    it,
                    viewModel.colorPickerViewModel2,
                    lifecycleOwner,
                )
            }
    }
}
