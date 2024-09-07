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

import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toolbar
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.customization.ui.binder.DefaultToolbarBinder
import com.android.wallpaper.picker.customization.ui.binder.ToolbarBinder
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch

@Singleton
class ThemePickerToolbarBinder
@Inject
constructor(private val defaultToolbarBinder: DefaultToolbarBinder) : ToolbarBinder {

    override fun bind(
        navButton: FrameLayout,
        toolbar: Toolbar,
        applyButton: Button,
        viewModel: CustomizationOptionsViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        defaultToolbarBinder.bind(navButton, toolbar, applyButton, viewModel, lifecycleOwner)

        if (viewModel !is ThemePickerCustomizationOptionsViewModel) {
            throw IllegalArgumentException(
                "viewModel $viewModel is not a ThemePickerCustomizationOptionsViewModel."
            )
        }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.onApplyButtonClicked.collect { onApplyButtonClicked ->
                        applyButton.setOnClickListener {
                            onApplyButtonClicked?.invoke()?.let { viewModel.deselectOption() }
                        }
                    }
                }

                launch { viewModel.isOnApplyVisible.collect { applyButton.isVisible = it } }

                launch { viewModel.isOnApplyEnabled.collect { applyButton.isEnabled = it } }
            }
        }
    }
}
