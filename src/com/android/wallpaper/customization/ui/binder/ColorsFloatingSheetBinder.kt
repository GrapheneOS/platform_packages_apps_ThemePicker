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

import android.content.Context
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.color.shared.model.ColorType
import com.android.customization.picker.color.ui.binder.ColorOptionIconBinder
import com.android.customization.picker.color.ui.view.ColorOptionIconView
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.customization.picker.color.ui.viewmodel.ColorTypeTabViewModel
import com.android.customization.picker.common.ui.view.DoubleRowListItemSpacing
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.viewmodel.ColorPickerViewModel2
import com.android.wallpaper.picker.customization.ui.view.FloatingTabToolbar
import com.android.wallpaper.picker.customization.ui.view.FloatingTabToolbar.Tab.PRIMARY
import com.android.wallpaper.picker.customization.ui.view.FloatingTabToolbar.Tab.SECONDARY
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter
import kotlinx.coroutines.launch

object ColorsFloatingSheetBinder {

    fun bind(
        view: View,
        viewModel: ColorPickerViewModel2,
        lifecycleOwner: LifecycleOwner,
    ) {
        val subhead = view.requireViewById<TextView>(R.id.color_type_tab_subhead)

        val colorsAdapter =
            createOptionItemAdapter(view.resources.configuration.uiMode, lifecycleOwner)
        val colorsList =
            view.requireViewById<RecyclerView>(R.id.colors_horizontal_list).also {
                it.initColorsList(view.context.applicationContext, colorsAdapter)
            }

        val tabs = view.requireViewById<FloatingTabToolbar>(R.id.floating_bar_tabs)

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.colorTypeTabs.collect { colorTypes ->
                        colorTypes.forEach { (colorType, tabViewModel) ->
                            bindTab(tabs, colorType, tabViewModel)
                        }
                        colorTypes
                            .filterValues { it.isSelected }
                            .keys
                            .firstOrNull()
                            ?.let {
                                when (it) {
                                    ColorType.WALLPAPER_COLOR -> tabs.setTabSelected(PRIMARY)
                                    ColorType.PRESET_COLOR -> tabs.setTabSelected(SECONDARY)
                                }
                            }
                    }
                }

                launch { viewModel.colorTypeTabSubheader.collect { subhead.text = it } }

                launch {
                    viewModel.colorOptions.collect { colorOptions ->
                        colorsAdapter.setItems(colorOptions) {
                            var indexToFocus = colorOptions.indexOfFirst { it.isSelected.value }
                            indexToFocus = if (indexToFocus < 0) 0 else indexToFocus
                            (colorsList.layoutManager as LinearLayoutManager)
                                .scrollToPositionWithOffset(indexToFocus, 0)
                        }
                    }
                }
            }
        }
    }

    private fun createOptionItemAdapter(
        uiMode: Int,
        lifecycleOwner: LifecycleOwner
    ): OptionItemAdapter<ColorOptionIconViewModel> =
        OptionItemAdapter(
            layoutResourceId = R.layout.color_option,
            lifecycleOwner = lifecycleOwner,
            bindIcon = { foregroundView: View, colorIcon: ColorOptionIconViewModel ->
                val colorOptionIconView = foregroundView as? ColorOptionIconView
                val night = uiMode and UI_MODE_NIGHT_MASK == UI_MODE_NIGHT_YES
                colorOptionIconView?.let { ColorOptionIconBinder.bind(it, colorIcon, night) }
            }
        )

    private fun RecyclerView.initColorsList(
        context: Context,
        adapter: OptionItemAdapter<ColorOptionIconViewModel>,
    ) {
        apply {
            this.adapter = adapter
            layoutManager =
                GridLayoutManager(
                    context,
                    2,
                    GridLayoutManager.HORIZONTAL,
                    false,
                )
            addItemDecoration(
                DoubleRowListItemSpacing(
                    context.resources.getDimensionPixelSize(
                        R.dimen.floating_sheet_content_horizontal_padding
                    ),
                    context.resources.getDimensionPixelSize(
                        R.dimen.floating_sheet_list_item_horizontal_space
                    ),
                    context.resources.getDimensionPixelSize(
                        R.dimen.floating_sheet_list_item_vertical_space
                    ),
                )
            )
        }
    }

    private fun bindTab(
        tabs: FloatingTabToolbar,
        colorType: ColorType,
        viewModel: ColorTypeTabViewModel
    ) {
        val tab =
            when (colorType) {
                ColorType.WALLPAPER_COLOR -> PRIMARY
                ColorType.PRESET_COLOR -> SECONDARY
            }
        val iconDrawable =
            ResourcesCompat.getDrawable(
                tabs.resources,
                when (colorType) {
                    ColorType.WALLPAPER_COLOR ->
                        com.android.wallpaper.R.drawable.ic_baseline_wallpaper_24
                    ColorType.PRESET_COLOR -> R.drawable.ic_colors
                },
                null,
            )
        when (colorType) {
            ColorType.WALLPAPER_COLOR -> tabs.primaryIcon.setImageDrawable(iconDrawable)
            ColorType.PRESET_COLOR -> tabs.secondaryIcon.setImageDrawable(iconDrawable)
        }
        tabs.setTabText(tab, viewModel.name)
        tabs.setOnTabClick(tab, viewModel.onClick)
    }
}
