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

import android.app.Dialog
import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.common.ui.view.DoubleRowListItemSpacing
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.viewmodel.KeyguardQuickAffordancePickerViewModel2
import com.android.wallpaper.picker.common.dialog.ui.viewbinder.DialogViewBinder
import com.android.wallpaper.picker.common.dialog.ui.viewmodel.DialogViewModel
import com.android.wallpaper.picker.common.icon.ui.viewbinder.IconViewBinder
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.customization.ui.view.FloatingTabToolbar
import com.android.wallpaper.picker.customization.ui.view.FloatingTabToolbar.Tab
import com.android.wallpaper.picker.customization.ui.view.FloatingTabToolbar.Tab.PRIMARY
import com.android.wallpaper.picker.customization.ui.view.FloatingTabToolbar.Tab.SECONDARY
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
object ShortcutFloatingSheetBinder {

    fun bind(
        view: View,
        viewModel: KeyguardQuickAffordancePickerViewModel2,
        lifecycleOwner: LifecycleOwner,
    ) {
        val quickAffordanceAdapter = createOptionItemAdapter(lifecycleOwner)
        val quickAffordanceList =
            view.requireViewById<RecyclerView>(R.id.quick_affordance_horizontal_list).also {
                it.initQuickAffordanceList(view.context.applicationContext, quickAffordanceAdapter)
            }

        val tabs = view.requireViewById<FloatingTabToolbar>(R.id.floating_bar_tabs)

        var dialog: Dialog? = null

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.slots
                        .map { slotById -> slotById.values }
                        .collect { slots ->
                            val list = slots.toList()
                            list.mapIndexed { index, slot ->
                                val tab = if (index == 0) PRIMARY else SECONDARY
                                tabs.setSelectedAffordances(tab, slot.selectedQuickAffordances)
                                tabs.setTabText(tab, slot.name)
                                tabs.setOnTabClick(tab, slot.onClicked)
                            }
                            list
                                .indexOfFirst { it.isSelected }
                                .let {
                                    if (it == 0) {
                                        tabs.setTabSelected(PRIMARY)
                                    } else if (it == 1) {
                                        tabs.setTabSelected(SECONDARY)
                                    }
                                }
                        }
                }

                launch {
                    viewModel.quickAffordances.collect { affordances ->
                        quickAffordanceAdapter.setItems(affordances)
                    }
                }

                launch {
                    viewModel.quickAffordances
                        .flatMapLatest { affordances ->
                            combine(affordances.map { affordance -> affordance.isSelected }) {
                                selectedFlags ->
                                selectedFlags.indexOfFirst { it }
                            }
                        }
                        .collectIndexed { index, selectedPosition ->
                            // Scroll the view to show the first selected affordance.
                            if (selectedPosition != -1) {
                                // We use "post" because we need to give the adapter item a pass to
                                // update the view.
                                quickAffordanceList.post {
                                    if (index == 0) {
                                        // don't animate on initial collection
                                        quickAffordanceList.scrollToPosition(selectedPosition)
                                    } else {
                                        quickAffordanceList.smoothScrollToPosition(selectedPosition)
                                    }
                                }
                            }
                        }
                }

                launch {
                    viewModel.dialog.distinctUntilChanged().collect { dialogRequest ->
                        dialog?.dismiss()
                        dialog =
                            if (dialogRequest != null) {
                                showDialog(
                                    context = view.context,
                                    request = dialogRequest,
                                    onDismissed = viewModel::onDialogDismissed
                                )
                            } else {
                                null
                            }
                    }
                }

                launch {
                    viewModel.activityStartRequests.collect { intent ->
                        if (intent != null) {
                            view.context.startActivity(intent)
                            viewModel.onActivityStarted()
                        }
                    }
                }
            }
        }
    }

    private fun showDialog(
        context: Context,
        request: DialogViewModel,
        onDismissed: () -> Unit,
    ): Dialog {
        return DialogViewBinder.show(
            context = context,
            viewModel = request,
            onDismissed = onDismissed,
        )
    }

    private fun createOptionItemAdapter(lifecycleOwner: LifecycleOwner): OptionItemAdapter<Icon> =
        OptionItemAdapter(
            layoutResourceId = R.layout.quick_affordance_list_item,
            lifecycleOwner = lifecycleOwner,
            bindIcon = { foregroundView: View, gridIcon: Icon ->
                val imageView = foregroundView as? ImageView
                imageView?.let { IconViewBinder.bind(imageView, gridIcon) }
            },
        )

    private fun RecyclerView.initQuickAffordanceList(
        context: Context,
        adapter: OptionItemAdapter<Icon>
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
            addItemDecoration(DoubleRowListItemSpacing(12))
        }
    }

    private fun FloatingTabToolbar.setSelectedAffordances(
        tab: Tab,
        selectedQuickAffordances: List<OptionItemViewModel<Icon>>,
    ) {
        val icon =
            selectedQuickAffordances.firstOrNull()?.payload
                ?: Icon.Resource(res = R.drawable.link_off, contentDescription = null)
        IconViewBinder.bind(
            if (tab == PRIMARY) {
                this.primaryIcon
            } else {
                this.secondaryIcon
            },
            icon,
        )
    }
}
