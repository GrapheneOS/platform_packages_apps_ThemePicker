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
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.common.ui.view.SingleRowListItemSpacing
import com.android.customization.picker.grid.ui.binder.GridIconViewBinder
import com.android.customization.picker.grid.ui.viewmodel.GridIconViewModel
import com.android.wallpaper.R
import com.android.wallpaper.customization.ui.viewmodel.ShapeAndGridPickerViewModel
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter
import com.android.wallpaper.picker.option.ui.binder.OptionItemBinder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch

object ShapeAndGridFloatingSheetBinder {

    fun bind(
        view: View,
        viewModel: ShapeAndGridPickerViewModel,
        lifecycleOwner: LifecycleOwner,
        backgroundDispatcher: CoroutineDispatcher,
    ) {
        val adapter = createOptionItemAdapter(view.context, lifecycleOwner, backgroundDispatcher)
        val gridOptionList =
            view.requireViewById<RecyclerView>(R.id.options).also {
                it.initGridOptionList(view.context, adapter)
            }

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.optionItems.collect { options -> adapter.setItems(options) } }
            }
        }
    }

    private fun createOptionItemAdapter(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        backgroundDispatcher: CoroutineDispatcher,
    ): OptionItemAdapter<GridIconViewModel> =
        OptionItemAdapter(
            layoutResourceId = com.android.themepicker.R.layout.grid_option,
            lifecycleOwner = lifecycleOwner,
            backgroundDispatcher = backgroundDispatcher,
            foregroundTintSpec =
                OptionItemBinder.TintSpec(
                    selectedColor = context.getColor(R.color.system_on_surface),
                    unselectedColor = context.getColor(R.color.system_on_surface),
                ),
            bindIcon = { foregroundView: View, gridIcon: GridIconViewModel ->
                val imageView = foregroundView as? ImageView
                imageView?.let { GridIconViewBinder.bind(imageView, gridIcon) }
            }
        )

    private fun RecyclerView.initGridOptionList(
        context: Context,
        adapter: OptionItemAdapter<GridIconViewModel>,
    ) {
        apply {
            this.layoutManager =
                LinearLayoutManager(
                    context,
                    RecyclerView.HORIZONTAL,
                    false,
                )
            addItemDecoration(
                SingleRowListItemSpacing(
                    edgeItemSpacePx =
                        context.resources.getDimensionPixelSize(
                            com.android.themepicker.R.dimen
                                .floating_sheet_content_horizontal_padding
                        ),
                    itemHorizontalSpacePx =
                        context.resources.getDimensionPixelSize(
                            com.android.themepicker.R.dimen
                                .floating_sheet_list_item_horizontal_space
                        ),
                )
            )
            this.adapter = adapter
        }
    }
}
