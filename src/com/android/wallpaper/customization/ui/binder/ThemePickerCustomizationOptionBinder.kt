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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.binder.CustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.DefaultCustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ThemePickerCustomizationOptionsBinder
@Inject
constructor(private val defaultCustomizationOptionsBinder: DefaultCustomizationOptionsBinder) :
    CustomizationOptionsBinder {

    override fun bind(
        view: View,
        lockScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        homeScreenCustomizationOptionEntries: List<Pair<CustomizationOption, View>>,
        viewModel: CustomizationOptionsViewModel,
        lifecycleOwner: LifecycleOwner
    ) {
        defaultCustomizationOptionsBinder.bind(
            view,
            lockScreenCustomizationOptionEntries,
            homeScreenCustomizationOptionEntries,
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
            }
        }

        ShortcutFloatingSheetBinder.bind(
            view,
            viewModel.keyguardQuickAffordancePickerViewModel2,
            lifecycleOwner,
        )
    }
}
