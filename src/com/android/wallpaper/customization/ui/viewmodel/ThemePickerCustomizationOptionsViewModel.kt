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

package com.android.wallpaper.customization.ui.viewmodel

import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.DefaultCustomizationOptionsViewModel
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@ViewModelScoped
class ThemePickerCustomizationOptionsViewModel
@Inject
constructor(
    private val defaultCustomizationOptionsViewModel: DefaultCustomizationOptionsViewModel
) : CustomizationOptionsViewModel {

    override val selectedOption = defaultCustomizationOptionsViewModel.selectedOption

    override fun deselectOption(): Boolean = defaultCustomizationOptionsViewModel.deselectOption()

    val onCustomizeClockClicked: Flow<(() -> Unit)?> =
        selectedOption.map {
            if (it == null) {
                {
                    defaultCustomizationOptionsViewModel.selectOption(
                        ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption.CLOCK
                    )
                }
            } else {
                null
            }
        }

    val onCustomizeShortcutClicked: Flow<(() -> Unit)?> =
        selectedOption.map {
            if (it == null) {
                {
                    defaultCustomizationOptionsViewModel.selectOption(
                        ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption
                            .SHORTCUTS
                    )
                }
            } else {
                null
            }
        }
}
