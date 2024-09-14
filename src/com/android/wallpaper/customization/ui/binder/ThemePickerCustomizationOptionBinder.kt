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
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.ui.view.ClockHostView2
import com.android.customization.picker.clock.ui.view.ClockViewFactory
import com.android.customization.picker.grid.ui.binder.GridIconViewBinder
import com.android.themepicker.R
import com.android.wallpaper.config.BaseFlags
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerHomeCustomizationOption
import com.android.wallpaper.customization.ui.util.ThemePickerCustomizationOptionUtil.ThemePickerLockCustomizationOption
import com.android.wallpaper.customization.ui.viewmodel.ThemePickerCustomizationOptionsViewModel
import com.android.wallpaper.picker.common.icon.ui.viewbinder.IconViewBinder
import com.android.wallpaper.picker.common.text.ui.viewbinder.TextViewBinder
import com.android.wallpaper.picker.customization.ui.binder.CustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.binder.DefaultCustomizationOptionsBinder
import com.android.wallpaper.picker.customization.ui.util.CustomizationOptionUtil.CustomizationOption
import com.android.wallpaper.picker.customization.ui.viewmodel.ColorUpdateViewModel
import com.android.wallpaper.picker.customization.ui.viewmodel.CustomizationPickerViewModel2
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
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
        viewModel: CustomizationPickerViewModel2,
        colorUpdateViewModel: ColorUpdateViewModel,
        lifecycleOwner: LifecycleOwner,
    ) {
        defaultCustomizationOptionsBinder.bind(
            view,
            lockScreenCustomizationOptionEntries,
            homeScreenCustomizationOptionEntries,
            customizationOptionFloatingSheetViewMap,
            viewModel,
            colorUpdateViewModel,
            lifecycleOwner,
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

        val optionShapeAndGrid =
            homeScreenCustomizationOptionEntries
                .find { it.first == ThemePickerHomeCustomizationOption.APP_SHAPE_AND_GRID }
                ?.second
        val optionShapeAndGridDescription =
            optionShapeAndGrid?.findViewById<TextView>(R.id.option_entry_app_grid_description)
        val optionShapeAndGridIcon =
            optionShapeAndGrid?.findViewById<ImageView>(R.id.option_entry_app_grid_icon)

        val optionsViewModel =
            viewModel.customizationOptionsViewModel as ThemePickerCustomizationOptionsViewModel
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    optionsViewModel.onCustomizeClockClicked.collect {
                        optionClock?.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    optionsViewModel.onCustomizeShortcutClicked.collect {
                        optionShortcut?.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    optionsViewModel.keyguardQuickAffordancePickerViewModel2.summary.collect {
                        summary ->
                        optionShortcutDescription?.let {
                            TextViewBinder.bind(view = it, viewModel = summary.description)
                        }
                        summary.icon1?.let { icon ->
                            optionShortcutIcon1?.let {
                                IconViewBinder.bind(view = it, viewModel = icon)
                            }
                        }
                        optionShortcutIcon1?.isVisible = summary.icon1 != null

                        summary.icon2?.let { icon ->
                            optionShortcutIcon2?.let {
                                IconViewBinder.bind(view = it, viewModel = icon)
                            }
                        }
                        optionShortcutIcon2?.isVisible = summary.icon2 != null
                    }
                }

                launch {
                    optionsViewModel.onCustomizeColorsClicked.collect {
                        optionColors?.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    optionsViewModel.onCustomizeShapeAndGridClicked.collect {
                        optionShapeAndGrid?.setOnClickListener { _ -> it?.invoke() }
                    }
                }

                launch {
                    optionsViewModel.shapeAndGridPickerViewModel.selectedGridOption.collect {
                        gridOption ->
                        optionShapeAndGridDescription?.let {
                            TextViewBinder.bind(it, gridOption.text)
                        }
                        gridOption.payload?.let { gridIconViewModel ->
                            optionShapeAndGridIcon?.let {
                                GridIconViewBinder.bind(view = it, viewModel = gridIconViewModel)
                            }
                            // TODO(b/363018910): Use ColorUpdateBinder to update color
                            optionShapeAndGridIcon?.setColorFilter(
                                ContextCompat.getColor(
                                    view.context,
                                    com.android.wallpaper.R.color.system_on_surface_variant,
                                )
                            )
                        }
                    }
                }
            }
        }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerLockCustomizationOption.CLOCK)
            ?.let {
                ClockFloatingSheetBinder.bind(
                    it,
                    optionsViewModel,
                    colorUpdateViewModel,
                    lifecycleOwner,
                )
            }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerLockCustomizationOption.SHORTCUTS)
            ?.let {
                ShortcutFloatingSheetBinder.bind(
                    it,
                    optionsViewModel,
                    colorUpdateViewModel,
                    lifecycleOwner,
                )
            }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerHomeCustomizationOption.COLORS)
            ?.let {
                ColorsFloatingSheetBinder.bind(
                    it,
                    optionsViewModel,
                    colorUpdateViewModel,
                    lifecycleOwner,
                )
            }

        customizationOptionFloatingSheetViewMap
            ?.get(ThemePickerHomeCustomizationOption.APP_SHAPE_AND_GRID)
            ?.let {
                ShapeAndGridFloatingSheetBinder.bind(
                    it,
                    optionsViewModel.shapeAndGridPickerViewModel,
                    lifecycleOwner,
                    Dispatchers.IO,
                )
            }
    }

    override fun bindClockPreview(
        clockHostView: View,
        viewModel: CustomizationPickerViewModel2,
        lifecycleOwner: LifecycleOwner,
        clockViewFactory: ClockViewFactory,
    ) {
        clockHostView as ClockHostView2
        val clockPickerViewModel =
            (viewModel.customizationOptionsViewModel as ThemePickerCustomizationOptionsViewModel)
                .clockPickerViewModel

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    combine(
                            clockPickerViewModel.previewingClock.filterNotNull(),
                            clockPickerViewModel.previewingClockSize,
                        ) { clock, size ->
                            clock to size
                        }
                        .collect { (clock, size) ->
                            clockHostView.removeAllViews()
                            if (BaseFlags.get().isClockReactiveVariantsEnabled()) {
                                clockViewFactory.setReactiveTouchInteractionEnabled(
                                    clock.clockId,
                                    true,
                                )
                            }
                            val clockView =
                                when (size) {
                                    ClockSize.DYNAMIC ->
                                        clockViewFactory.getLargeView(clock.clockId)
                                    ClockSize.SMALL -> clockViewFactory.getSmallView(clock.clockId)
                                }
                            // The clock view might still be attached to an existing parent. Detach
                            // before adding to another parent.
                            (clockView.parent as? ViewGroup)?.removeView(clockView)
                            clockHostView.addView(clockView)
                            clockHostView.clockSize = size
                        }
                }

                launch {
                    combine(
                            clockPickerViewModel.previewingSeedColor,
                            clockPickerViewModel.previewingClock,
                        ) { color, clock ->
                            color to clock
                        }
                        .collect { (color, clock) ->
                            clockViewFactory.updateColor(clock.clockId, color)
                        }
                }
            }
        }
    }
}
