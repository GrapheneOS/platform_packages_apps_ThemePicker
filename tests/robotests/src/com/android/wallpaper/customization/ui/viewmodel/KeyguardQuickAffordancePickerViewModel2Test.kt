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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import com.android.customization.module.logging.TestThemesUserEventLogger
import com.android.customization.picker.quickaffordance.data.repository.KeyguardQuickAffordancePickerRepository
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordancePickerInteractor
import com.android.customization.picker.quickaffordance.domain.interactor.KeyguardQuickAffordanceSnapshotRestorer
import com.android.systemui.shared.customization.data.content.CustomizationProviderClient
import com.android.systemui.shared.customization.data.content.FakeCustomizationProviderClient
import com.android.systemui.shared.keyguard.shared.model.KeyguardQuickAffordanceSlots
import com.android.themepicker.R
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.customization.ui.viewmodel.FloatingToolbarTabViewModel
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(RobolectricTestRunner::class)
class KeyguardQuickAffordancePickerViewModel2Test {

    private val logger = TestThemesUserEventLogger()

    private lateinit var underTest: KeyguardQuickAffordancePickerViewModel2

    private lateinit var context: Context
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var client: FakeCustomizationProviderClient

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        testDispatcher = UnconfinedTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)
        client = FakeCustomizationProviderClient()
        val quickAffordanceInteractor =
            KeyguardQuickAffordancePickerInteractor(
                repository =
                    KeyguardQuickAffordancePickerRepository(
                        client = client,
                        mainScope = testScope.backgroundScope,
                    ),
                client = client,
                snapshotRestorer = KeyguardQuickAffordanceSnapshotRestorer(client),
            )
        underTest =
            KeyguardQuickAffordancePickerViewModel2(
                applicationContext = context,
                quickAffordanceInteractor = quickAffordanceInteractor,
                logger = logger,
                viewModelScope = testScope.backgroundScope,
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun selectedSlotIdUpdates_whenClickingOnTabsAndCallingResetPreview() =
        testScope.runTest {
            val selectedSlotId = collectLastValue(underTest.selectedSlotId)

            val tabs = collectLastValue(underTest.tabs)

            // Default selected slot ID is bottom_start
            assertThat(selectedSlotId())
                .isEqualTo(KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START)

            // Click on tab1
            val tab1 = tabs()?.get(1) ?: throw NullPointerException("secondTab should not be null.")
            tab1.onClick?.invoke()
            assertThat(selectedSlotId()).isEqualTo(KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END)

            underTest.resetPreview()
            assertThat(selectedSlotId())
                .isEqualTo(KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START)
        }

    @Test
    fun selectedQuickAffordancesMapUpdates_whenClickingOnQuickAffordanceOptionsAndCallingResetPreview() =
        testScope.runTest {
            val selectedQuickAffordances = collectLastValue(underTest.selectedQuickAffordances)

            val tabs = collectLastValue(underTest.tabs)
            val quickAffordances = collectLastValue(underTest.quickAffordances)

            // Default selectedQuickAffordances is an empty map
            assertThat(selectedQuickAffordances()).isEqualTo(emptyMap<String, String>())

            // Click on quick affordance 1 when selected slot ID is bottom_start
            val onClickAffordance1 =
                collectLastValue(quickAffordances()?.get(1)?.onClicked ?: emptyFlow())
            onClickAffordance1()?.invoke()
            assertThat(selectedQuickAffordances())
                .isEqualTo(
                    mapOf(
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START to
                            FakeCustomizationProviderClient.AFFORDANCE_1
                    )
                )

            // Click on tab 1 to change the selected slot ID to bottom_end and click on quick
            // affordance 2
            tabs()?.get(1)?.onClick?.invoke()
            val onClickAffordance2 =
                collectLastValue(quickAffordances()?.get(2)?.onClicked ?: emptyFlow())
            onClickAffordance2()?.invoke()
            assertThat(selectedQuickAffordances())
                .isEqualTo(
                    mapOf(
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START to
                            FakeCustomizationProviderClient.AFFORDANCE_1,
                        KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END to
                            FakeCustomizationProviderClient.AFFORDANCE_2
                    )
                )

            underTest.resetPreview()
            assertThat(selectedQuickAffordances()).isEqualTo(emptyMap<String, String>())
        }

    @Test
    fun tabsUpdates_whenClickingOnTabsAndQuickAffordanceOptions() =
        testScope.runTest {
            val tabs = collectLastValue(underTest.tabs)

            val quickAffordances = collectLastValue(underTest.quickAffordances)

            // Default state of the 2 tabs
            assertTabUiState(
                tab = tabs()?.get(0),
                icon = Icon.Resource(R.drawable.link_off, null),
                text = "Left shortcut",
                isSelected = true,
            )
            assertTabUiState(
                tab = tabs()?.get(1),
                icon = Icon.Resource(R.drawable.link_off, null),
                text = "Right shortcut",
                isSelected = false,
            )

            // Click on tab 1
            tabs()?.get(1)?.onClick?.invoke()
            assertTabUiState(
                tab = tabs()?.get(0),
                icon = Icon.Resource(R.drawable.link_off, null),
                text = "Left shortcut",
                isSelected = false,
            )
            val tab1 = tabs()?.get(1)
            assertTabUiState(
                tab = tab1,
                icon = Icon.Resource(R.drawable.link_off, null),
                text = "Right shortcut",
                isSelected = true,
            )

            // Click on quick affordance 1 when tab 1 is selected. Icon should change
            val clickOnQuickAffordance1 =
                collectLastValue(quickAffordances()?.get(1)?.onClicked ?: emptyFlow())
            clickOnQuickAffordance1()?.invoke()
            assertTabUiState(
                tab = tabs()?.get(1),
                icon =
                    Icon.Loaded(
                        FakeCustomizationProviderClient.ICON_1,
                        Text.Loaded("Right shortcut")
                    ),
                text = "Right shortcut",
                isSelected = true,
            )
        }

    @Test
    fun quickAffordancesUpdates_whenClickingOnTabsAndQuickAffordanceOptions() =
        testScope.runTest {
            val quickAffordances = collectLastValue(underTest.quickAffordances)

            val tabs = collectLastValue(underTest.tabs)

            // The default quickAffordances snapshot
            assertThat(quickAffordances()?.size).isEqualTo(4)
            assertQuickAffordance(
                testScope = this,
                quickAffordance = quickAffordances()?.get(0),
                key = "${KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START}::none",
                icon = Icon.Resource(R.drawable.link_off, null),
                text = Text.Resource(R.string.keyguard_affordance_none),
                isSelected = true,
            )
            assertQuickAffordance(
                testScope = this,
                quickAffordance = quickAffordances()?.get(1),
                key =
                    "${KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START}::${FakeCustomizationProviderClient.AFFORDANCE_1}",
                icon = Icon.Loaded(FakeCustomizationProviderClient.ICON_1, null),
                text = Text.Loaded(FakeCustomizationProviderClient.AFFORDANCE_1),
                isSelected = false,
            )
            assertQuickAffordance(
                testScope = this,
                quickAffordance = quickAffordances()?.get(2),
                key =
                    "${KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START}::${FakeCustomizationProviderClient.AFFORDANCE_2}",
                icon = Icon.Loaded(FakeCustomizationProviderClient.ICON_2, null),
                text = Text.Loaded(FakeCustomizationProviderClient.AFFORDANCE_2),
                isSelected = false,
            )
            assertQuickAffordance(
                testScope = this,
                quickAffordance = quickAffordances()?.get(3),
                key =
                    "${KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START}::${FakeCustomizationProviderClient.AFFORDANCE_3}",
                icon = Icon.Loaded(FakeCustomizationProviderClient.ICON_3, null),
                text = Text.Loaded(FakeCustomizationProviderClient.AFFORDANCE_3),
                isSelected = false,
            )

            // Click on quick affordance 2. Quick affordance 0 will be unselected and quick
            // affordance 2 will be selected.
            val onClickQuickAffordance2 =
                collectLastValue(quickAffordances()?.get(2)?.onClicked ?: emptyFlow())
            onClickQuickAffordance2()?.invoke()
            assertQuickAffordance(
                testScope = this,
                quickAffordance = quickAffordances()?.get(0),
                key = "${KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START}::none",
                icon = Icon.Resource(R.drawable.link_off, null),
                text = Text.Resource(R.string.keyguard_affordance_none),
                isSelected = false,
            )
            assertQuickAffordance(
                testScope = this,
                quickAffordance = quickAffordances()?.get(2),
                key =
                    "${KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START}::${FakeCustomizationProviderClient.AFFORDANCE_2}",
                icon = Icon.Loaded(FakeCustomizationProviderClient.ICON_2, null),
                text = Text.Loaded(FakeCustomizationProviderClient.AFFORDANCE_2),
                isSelected = true,
            )

            tabs()?.get(1)?.onClick?.invoke()
            assertQuickAffordance(
                testScope = this,
                quickAffordance = quickAffordances()?.get(0),
                key = "${KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END}::none",
                icon = Icon.Resource(R.drawable.link_off, null),
                text = Text.Resource(R.string.keyguard_affordance_none),
                isSelected = true,
            )
            assertQuickAffordance(
                testScope = this,
                quickAffordance = quickAffordances()?.get(1),
                key =
                    "${KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END}::${FakeCustomizationProviderClient.AFFORDANCE_1}",
                icon = Icon.Loaded(FakeCustomizationProviderClient.ICON_1, null),
                text = Text.Loaded(FakeCustomizationProviderClient.AFFORDANCE_1),
                isSelected = false,
            )
            assertQuickAffordance(
                testScope = this,
                quickAffordance = quickAffordances()?.get(2),
                key =
                    "${KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END}::${FakeCustomizationProviderClient.AFFORDANCE_2}",
                icon = Icon.Loaded(FakeCustomizationProviderClient.ICON_2, null),
                text = Text.Loaded(FakeCustomizationProviderClient.AFFORDANCE_2),
                isSelected = false,
            )
            assertQuickAffordance(
                testScope = this,
                quickAffordance = quickAffordances()?.get(3),
                key =
                    "${KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END}::${FakeCustomizationProviderClient.AFFORDANCE_3}",
                icon = Icon.Loaded(FakeCustomizationProviderClient.ICON_3, null),
                text = Text.Loaded(FakeCustomizationProviderClient.AFFORDANCE_3),
                isSelected = false,
            )

            // When tab 1 is selected, click on quick affordance 3. Quick affordance 0 will be
            // unselected and quick affordance 3 will be selected.
            val onClickQuickAffordance3 =
                collectLastValue(quickAffordances()?.get(3)?.onClicked ?: emptyFlow())
            onClickQuickAffordance3()?.invoke()
            assertQuickAffordance(
                testScope = this,
                quickAffordance = quickAffordances()?.get(0),
                key = "${KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END}::none",
                icon = Icon.Resource(R.drawable.link_off, null),
                text = Text.Resource(R.string.keyguard_affordance_none),
                isSelected = false,
            )
            assertQuickAffordance(
                testScope = this,
                quickAffordance = quickAffordances()?.get(3),
                key =
                    "${KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END}::${FakeCustomizationProviderClient.AFFORDANCE_3}",
                icon = Icon.Loaded(FakeCustomizationProviderClient.ICON_3, null),
                text = Text.Loaded(FakeCustomizationProviderClient.AFFORDANCE_3),
                isSelected = true,
            )
        }

    @Test
    fun loggerShouldLogAndClientShouldUpdate_whenOnApply() =
        testScope.runTest {
            val onApply = collectLastValue(underTest.onApply)

            val tabs = collectLastValue(underTest.tabs)
            val quickAffordances = collectLastValue(underTest.quickAffordances)

            // Select the preview quick affordances
            val onClickAffordance1 =
                collectLastValue(quickAffordances()?.get(1)?.onClicked ?: emptyFlow())
            onClickAffordance1()?.invoke()
            tabs()?.get(1)?.onClick?.invoke()
            val onClickAffordance2 =
                collectLastValue(quickAffordances()?.get(2)?.onClicked ?: emptyFlow())
            onClickAffordance2()?.invoke()

            onApply()?.invoke()
            assertThat(client.querySelections())
                .isEqualTo(
                    listOf(
                        CustomizationProviderClient.Selection(
                            slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START,
                            affordanceId = FakeCustomizationProviderClient.AFFORDANCE_1,
                            affordanceName = FakeCustomizationProviderClient.AFFORDANCE_1,
                        ),
                        CustomizationProviderClient.Selection(
                            slotId = KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END,
                            affordanceId = FakeCustomizationProviderClient.AFFORDANCE_2,
                            affordanceName = FakeCustomizationProviderClient.AFFORDANCE_2,
                        ),
                    )
                )
            assertThat(logger.shortcutLogs)
                .isEqualTo(
                    listOf(
                        FakeCustomizationProviderClient.AFFORDANCE_1 to
                            KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_START,
                        FakeCustomizationProviderClient.AFFORDANCE_2 to
                            KeyguardQuickAffordanceSlots.SLOT_ID_BOTTOM_END,
                    )
                )
        }

    private fun assertTabUiState(
        tab: FloatingToolbarTabViewModel?,
        icon: Icon?,
        text: String,
        isSelected: Boolean,
    ) {
        if (tab == null) {
            throw NullPointerException("tab is null.")
        }
        assertThat(tab.icon).isEqualTo(icon)
        assertThat(tab.text).isEqualTo(text)
        assertThat(tab.isSelected).isEqualTo(isSelected)
    }

    private fun assertQuickAffordance(
        testScope: TestScope,
        quickAffordance: OptionItemViewModel<Icon>?,
        key: String,
        icon: Icon,
        text: Text,
        isSelected: Boolean,
    ) {
        if (quickAffordance == null) {
            throw NullPointerException("quickAffordance is null.")
        }
        assertThat(testScope.collectLastValue(quickAffordance.key)()).isEqualTo(key)
        assertThat(quickAffordance.payload).isEqualTo(icon)
        assertThat(quickAffordance.text).isEqualTo(text)
        assertThat(quickAffordance.isTextUserVisible).isEqualTo(true)
        assertThat(testScope.collectLastValue(quickAffordance.isSelected)()).isEqualTo(isSelected)
        assertThat(quickAffordance.isEnabled).isEqualTo(true)
    }
}
