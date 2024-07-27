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
import android.stats.style.StyleEnums
import androidx.test.filters.SmallTest
import com.android.customization.module.logging.TestThemesUserEventLogger
import com.android.customization.picker.clock.data.repository.FakeClockPickerRepository
import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import com.android.customization.picker.clock.domain.interactor.ClockPickerSnapshotRestorer
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.shared.model.ClockMetadataModel
import com.android.customization.picker.clock.ui.viewmodel.ClockColorViewModel
import com.android.customization.picker.clock.ui.viewmodel.ClockSettingsViewModel
import com.android.customization.picker.color.data.repository.FakeColorPickerRepository
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.domain.interactor.ColorPickerSnapshotRestorer
import com.android.wallpaper.testing.FakeSnapshotStore
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(RobolectricTestRunner::class)
class ClockPickerViewModelTest {

    private val logger = TestThemesUserEventLogger()

    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject @ApplicationContext lateinit var context: Context
    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject lateinit var testScope: TestScope

    private lateinit var colorMap: Map<String, ClockColorViewModel>
    private lateinit var underTest: ClockPickerViewModel

    @Before
    fun setUp() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        val repository = FakeClockPickerRepository()
        val clockPickerInteractor =
            ClockPickerInteractor(
                repository = repository,
                snapshotRestorer =
                    ClockPickerSnapshotRestorer(repository = repository).apply {
                        runBlocking { setUpSnapshotRestorer(store = FakeSnapshotStore()) }
                    },
            )
        val colorPickerRepository = FakeColorPickerRepository(context = context)
        val colorPickerInteractor =
            ColorPickerInteractor(
                repository = colorPickerRepository,
                snapshotRestorer =
                    ColorPickerSnapshotRestorer(repository = colorPickerRepository).apply {
                        runBlocking { setUpSnapshotRestorer(store = FakeSnapshotStore()) }
                    },
            )
        colorMap = ClockColorViewModel.getPresetColorMap(context.resources)
        underTest =
            ClockPickerViewModel(
                context = context,
                resources = context.resources,
                clockPickerInteractor = clockPickerInteractor,
                colorPickerInteractor = colorPickerInteractor,
                logger = logger,
                backgroundDispatcher = testDispatcher,
                viewModelScope = testScope,
            )

        testScope.launch {
            clockPickerInteractor.setSelectedClock(FakeClockPickerRepository.CLOCK_ID_0)
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun setTab() = runTest {
        val tabs = collectLastValue(underTest.tabs)
        assertThat(tabs()?.get(0)?.isSelected).isTrue()
        tabs()?.get(1)?.onClick?.invoke()
        assertThat(tabs()?.get(1)?.isSelected).isTrue()
        tabs()?.get(2)?.onClick?.invoke()
        assertThat(tabs()?.get(2)?.isSelected).isTrue()
    }

    @Test
    fun setClockStyle() = runTest {
        val clockStyleOptions = collectLastValue(underTest.clockStyleOptions)
        // Advance CLOCKS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from clockStyleOptions
        advanceTimeBy(ClockPickerViewModel.CLOCKS_EVENT_UPDATE_DELAY_MILLIS)
        val option0IsSelected = collectLastValue(clockStyleOptions()!![0].isSelected)
        val option0OnClicked = collectLastValue(clockStyleOptions()!![0].onClicked)
        assertThat(option0IsSelected()).isTrue()
        assertThat(option0OnClicked()).isNull()

        val option1OnClickedBefore = collectLastValue(clockStyleOptions()!![1].onClicked)
        option1OnClickedBefore()?.invoke()
        // Advance CLOCKS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from clockColorOptions
        advanceTimeBy(ClockPickerViewModel.CLOCKS_EVENT_UPDATE_DELAY_MILLIS)
        val option1IsSelected = collectLastValue(clockStyleOptions()!![1].isSelected)
        val option1OnClickedAfter = collectLastValue(clockStyleOptions()!![1].onClicked)
        assertThat(option0IsSelected()).isFalse()
        assertThat(option1IsSelected()).isTrue()
        assertThat(option1OnClickedAfter()).isNull()
    }

    @Test
    fun setSelectedColor() = runTest {
        val clockColorOptions = collectLastValue(underTest.clockColorOptions)
        val observedSliderProgress = collectLastValue(underTest.sliderProgress)
        val observedSeedColor = collectLastValue(underTest.seedColor)
        // Advance COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from
        // clockColorOptions
        advanceTimeBy(ClockPickerViewModel.COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
        val option0IsSelected = collectLastValue(clockColorOptions()!![0].isSelected)
        val option0OnClicked = collectLastValue(clockColorOptions()!![0].onClicked)
        assertThat(option0IsSelected()).isTrue()
        assertThat(option0OnClicked()).isNull()

        val option1OnClickedBefore = collectLastValue(clockColorOptions()!![1].onClicked)
        option1OnClickedBefore()?.invoke()
        // Advance COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from
        // clockColorOptions
        advanceTimeBy(ClockPickerViewModel.COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
        val option1IsSelected = collectLastValue(clockColorOptions()!![1].isSelected)
        val option1OnClickedAfter = collectLastValue(clockColorOptions()!![1].onClicked)
        assertThat(option0IsSelected()).isFalse()
        assertThat(option1IsSelected()).isTrue()
        assertThat(option1OnClickedAfter()).isNull()
        assertThat(observedSliderProgress())
            .isEqualTo(ClockMetadataModel.DEFAULT_COLOR_TONE_PROGRESS)
        val expectedSelectedColorModel = colorMap.values.first() // RED
        assertThat(observedSeedColor())
            .isEqualTo(
                ClockSettingsViewModel.blendColorWithTone(
                    expectedSelectedColorModel.color,
                    expectedSelectedColorModel.getColorTone(
                        ClockMetadataModel.DEFAULT_COLOR_TONE_PROGRESS
                    ),
                )
            )
    }

    @Test
    fun setColorTone() = runTest {
        val clockColorOptions = collectLastValue(underTest.clockColorOptions)
        val observedIsSliderEnabled = collectLastValue(underTest.isSliderEnabled)
        val observedSliderProgress = collectLastValue(underTest.sliderProgress)
        val observedSeedColor = collectLastValue(underTest.seedColor)
        // Advance COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from
        // clockColorOptions
        advanceTimeBy(ClockPickerViewModel.COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
        val option0IsSelected = collectLastValue(clockColorOptions()!![0].isSelected)
        assertThat(option0IsSelected()).isTrue()
        assertThat(observedIsSliderEnabled()).isFalse()

        val option1OnClicked = collectLastValue(clockColorOptions()!![1].onClicked)
        option1OnClicked()?.invoke()

        // Advance COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from
        // clockColorOptions
        advanceTimeBy(ClockPickerViewModel.COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
        assertThat(observedIsSliderEnabled()).isTrue()
        val targetProgress1 = 99
        underTest.onSliderProgressChanged(targetProgress1)
        assertThat(observedSliderProgress()).isEqualTo(targetProgress1)
        val targetProgress2 = 55
        testScope.launch { underTest.onSliderProgressStop(targetProgress2) }
        assertThat(observedSliderProgress()).isEqualTo(targetProgress2)
        val expectedSelectedColorModel = colorMap.values.first() // RED
        assertThat(observedSeedColor())
            .isEqualTo(
                ClockSettingsViewModel.blendColorWithTone(
                    expectedSelectedColorModel.color,
                    expectedSelectedColorModel.getColorTone(targetProgress2),
                )
            )
    }

    @Test
    fun getIsReactiveToTone() = runTest {
        val clockColorOptions = collectLastValue(underTest.clockColorOptions)
        val isSliderEnabled = collectLastValue(underTest.isSliderEnabled)
        // Advance COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS since there is a delay from
        // clockColorOptions
        advanceTimeBy(ClockPickerViewModel.COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
        val option1OnClicked = collectLastValue(clockColorOptions()!![1].onClicked)
        option1OnClicked()?.invoke()

        underTest.setSelectedClock(FakeClockPickerRepository.CLOCK_ID_0)
        assertThat(isSliderEnabled()).isTrue()

        underTest.setSelectedClock(FakeClockPickerRepository.CLOCK_ID_3)
        assertThat(isSliderEnabled()).isFalse()
    }

    @Test
    fun setClockSize() = runTest {
        val selectedClockSize = collectLastValue(underTest.selectedClockSize)
        underTest.setClockSize(ClockSize.DYNAMIC)
        assertThat(selectedClockSize()).isEqualTo(ClockSize.DYNAMIC)
        assertThat(logger.getLoggedClockSize()).isEqualTo(StyleEnums.CLOCK_SIZE_DYNAMIC)

        underTest.setClockSize(ClockSize.SMALL)
        assertThat(selectedClockSize()).isEqualTo(ClockSize.SMALL)
        assertThat(logger.getLoggedClockSize()).isEqualTo(StyleEnums.CLOCK_SIZE_SMALL)
    }
}
