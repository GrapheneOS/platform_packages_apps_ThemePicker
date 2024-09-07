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

package com.android.wallpaper.picker.common.preview.ui.binder

import android.os.Bundle
import android.os.Message
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.systemui.shared.keyguard.shared.model.KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END
import com.android.systemui.shared.keyguard.shared.model.KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.KEY_INITIALLY_SELECTED_SLOT_ID
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.KEY_QUICK_AFFORDANCE_ID
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.KEY_SLOT_ID
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.MESSAGE_ID_DEFAULT_PREVIEW
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.MESSAGE_ID_PREVIEW_QUICK_AFFORDANCE_SELECTED
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.MESSAGE_ID_SLOT_SELECTED
import com.android.systemui.shared.quickaffordance.shared.model.KeyguardPreviewConstants.MESSAGE_ID_START_CUSTOMIZING_QUICK_AFFORDANCES
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.model.Screen
import com.android.wallpaper.picker.common.preview.ui.binder.WorkspaceCallbackBinder.Companion.sendMessage
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationOptionsViewModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.launch

@Singleton
class ThemePickerWorkspaceCallbackBinder
@Inject
constructor(private val defaultWorkspaceCallbackBinder: DefaultWorkspaceCallbackBinder) :
    WorkspaceCallbackBinder {

    override fun bind(
        workspaceCallback: Message,
        viewModel: CustomizationOptionsViewModel,
        screen: Screen,
        lifecycleOwner: LifecycleOwner,
    ) {
        defaultWorkspaceCallbackBinder.bind(
            workspaceCallback = workspaceCallback,
            viewModel = viewModel,
            screen = screen,
            lifecycleOwner = lifecycleOwner,
        )

        if (viewModel !is ThemePickerCustomizationOptionsViewModel) {
            throw IllegalArgumentException(
                "viewModel $viewModel is not a ThemePickerCustomizationOptionsViewModel."
            )
        }

        when (screen) {
            Screen.LOCK_SCREEN ->
                lifecycleOwner.lifecycleScope.launch {
                    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        launch {
                            viewModel.selectedOption.collect {
                                when (it) {
                                    ThemePickerLockCustomizationOption.SHORTCUTS ->
                                        workspaceCallback.sendMessage(
                                            MESSAGE_ID_START_CUSTOMIZING_QUICK_AFFORDANCES,
                                            Bundle().apply {
                                                putString(
                                                    KEY_INITIALLY_SELECTED_SLOT_ID,
                                                    SLOT_ID_BOTTOM_START,
                                                )
                                            }
                                        )
                                    else ->
                                        workspaceCallback.sendMessage(
                                            MESSAGE_ID_DEFAULT_PREVIEW,
                                            Bundle.EMPTY,
                                        )
                                }
                            }
                        }

                        launch {
                            viewModel.keyguardQuickAffordancePickerViewModel2.selectedSlotId
                                .collect {
                                    workspaceCallback.sendMessage(
                                        MESSAGE_ID_SLOT_SELECTED,
                                        Bundle().apply { putString(KEY_SLOT_ID, it) },
                                    )
                                }
                        }

                        launch {
                            viewModel.keyguardQuickAffordancePickerViewModel2
                                .previewingQuickAffordances
                                .collect {
                                    it[SLOT_ID_BOTTOM_START]?.let {
                                        workspaceCallback.sendMessage(
                                            MESSAGE_ID_PREVIEW_QUICK_AFFORDANCE_SELECTED,
                                            Bundle().apply {
                                                putString(KEY_SLOT_ID, SLOT_ID_BOTTOM_START)
                                                putString(KEY_QUICK_AFFORDANCE_ID, it)
                                            },
                                        )
                                    }
                                    it[SLOT_ID_BOTTOM_END]?.let {
                                        workspaceCallback.sendMessage(
                                            MESSAGE_ID_PREVIEW_QUICK_AFFORDANCE_SELECTED,
                                            Bundle().apply {
                                                putString(KEY_SLOT_ID, SLOT_ID_BOTTOM_END)
                                                putString(KEY_QUICK_AFFORDANCE_ID, it)
                                            },
                                        )
                                    }
                                }
                        }
                    }
                }
            Screen.HOME_SCREEN ->
                lifecycleOwner.lifecycleScope.launch {
                    lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        launch {
                            viewModel.shapeAndGridPickerViewModel.previewingGridOptionKey.collect {
                                workspaceCallback.sendMessage(
                                    MESSAGE_ID_UPDATE_GRID,
                                    bundleOf(KEY_GRID_NAME to it)
                                )
                            }
                        }
                    }
                }
        }
    }

    companion object {
        const val MESSAGE_ID_UPDATE_GRID = 7414
        const val KEY_GRID_NAME = "grid_name"
    }
}
