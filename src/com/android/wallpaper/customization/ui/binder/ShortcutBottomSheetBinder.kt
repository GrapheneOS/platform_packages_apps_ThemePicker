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
import android.view.ViewGroup
import android.view.accessibility.AccessibilityEvent
import android.widget.ImageView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.customization.picker.common.ui.view.ItemSpacing
import com.android.customization.picker.common.ui.view.KeyguardQuickAffordanceItemSpacing
import com.android.customization.picker.quickaffordance.ui.adapter.SlotTabAdapter
import com.android.themepicker.R
import com.android.wallpaper.customization.ui.viewmodel.KeyguardQuickAffordancePickerViewModel2
import com.android.wallpaper.picker.common.dialog.ui.viewbinder.DialogViewBinder
import com.android.wallpaper.picker.common.dialog.ui.viewmodel.DialogViewModel
import com.android.wallpaper.picker.common.icon.ui.viewbinder.IconViewBinder
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.option.ui.adapter.OptionItemAdapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
object ShortcutBottomSheetBinder {

    fun bind(
        view: View,
        viewModel: KeyguardQuickAffordancePickerViewModel2,
        lifecycleOwner: LifecycleOwner,
    ) {
        val quickAffordanceAdapter =
            OptionItemAdapter(
                layoutResourceId = R.layout.quick_affordance_list_item,
                lifecycleOwner = lifecycleOwner,
                bindIcon = { foregroundView: View, gridIcon: Icon ->
                    val imageView = foregroundView as? ImageView
                    imageView?.let { IconViewBinder.bind(imageView, gridIcon) }
                },
            )
        val quickAffordanceList =
            view.requireViewById<RecyclerView>(R.id.quick_affordance_horizontal_list).apply {
                adapter = quickAffordanceAdapter
                layoutManager =
                    GridLayoutManager(
                        view.context.applicationContext,
                        2,
                        GridLayoutManager.HORIZONTAL,
                        false
                    )
                addItemDecoration(KeyguardQuickAffordanceItemSpacing())
            }
        val slotTabAdapter = SlotTabAdapter()
        val slotTabView: RecyclerView =
            view.requireViewById<RecyclerView>(R.id.slot_tabs).apply {
                adapter = slotTabAdapter
                layoutManager = LinearLayoutManager(view.context, RecyclerView.HORIZONTAL, false)
                addItemDecoration(ItemSpacing(ItemSpacing.TAB_ITEM_SPACING_DP))
            }
        // Setting a custom accessibility delegate so that the default content descriptions
        // for items in a list aren't announced (for left & right shortcuts). We populate
        // the content description for these shortcuts later on with the right (expected)
        // values.
        val slotTabViewDelegate: AccessibilityDelegateCompat =
            object : AccessibilityDelegateCompat() {
                override fun onRequestSendAccessibilityEvent(
                    host: ViewGroup,
                    child: View,
                    event: AccessibilityEvent
                ): Boolean {
                    if (event.eventType != AccessibilityEvent.TYPE_VIEW_FOCUSED) {
                        child.contentDescription = null
                    }
                    return super.onRequestSendAccessibilityEvent(host, child, event)
                }
            }
        ViewCompat.setAccessibilityDelegate(slotTabView, slotTabViewDelegate)

        var dialog: Dialog? = null

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.slots
                        .map { slotById -> slotById.values }
                        .collect { slots -> slotTabAdapter.setItems(slots.toList()) }
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
}
