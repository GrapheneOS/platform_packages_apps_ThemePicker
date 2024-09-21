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

package com.android.customization.model.picker.settings.ui.viewmodel

import android.app.UiModeManager.ContrastUtils
import com.android.customization.picker.settings.ui.viewmodel.ColorContrastSectionDataViewModel
import com.android.customization.picker.settings.ui.viewmodel.ColorContrastSectionViewModel
import com.android.themepicker.R
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.testing.FakeUiModeManager
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ColorContrastSectionViewModelTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    private lateinit var viewModel: ColorContrastSectionViewModel

    @Inject lateinit var testDispatcher: TestDispatcher
    @Inject lateinit var uiModeManager: FakeUiModeManager
    @Inject lateinit var viewModelFactory: ColorContrastSectionViewModel.Factory

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        hiltRule.inject()
        Dispatchers.setMain(testDispatcher)
        viewModel = viewModelFactory.create(ColorContrastSectionViewModel::class.java)
    }

    @Test
    fun summaryEmitsCorrectDataValueForStandard() = runTest {
        uiModeManager.setContrast(
            ContrastUtils.fromContrastLevel(ContrastUtils.CONTRAST_LEVEL_STANDARD)
        )
        val expected =
            ColorContrastSectionDataViewModel(
                Text.Resource(R.string.color_contrast_default_title),
                Icon.Resource(res = R.drawable.ic_contrast_standard, contentDescription = null),
            )

        val result = viewModel.summary.first()

        assertEquals(expected, result)
    }

    @Test
    fun summaryEmitsCorrectDataValueForMedium() = runTest {
        uiModeManager.setContrast(
            ContrastUtils.fromContrastLevel(ContrastUtils.CONTRAST_LEVEL_MEDIUM)
        )
        val expected =
            ColorContrastSectionDataViewModel(
                Text.Resource(R.string.color_contrast_medium_title),
                Icon.Resource(res = R.drawable.ic_contrast_medium, contentDescription = null),
            )

        val result = viewModel.summary.first()

        assertEquals(expected, result)
    }

    @Test
    fun summaryEmitsCorrectDataValueForHigh() = runTest {
        uiModeManager.setContrast(
            ContrastUtils.fromContrastLevel(ContrastUtils.CONTRAST_LEVEL_HIGH)
        )
        val expected =
            ColorContrastSectionDataViewModel(
                Text.Resource(R.string.color_contrast_high_title),
                Icon.Resource(res = R.drawable.ic_contrast_high, contentDescription = null),
            )

        val result = viewModel.summary.first()

        assertEquals(expected, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun summaryThrowsIllegalArgumentExceptionForInvalidValue() = runTest {
        uiModeManager.setContrast(999f)

        viewModel.summary.collect() // This should throw an IllegalArgumentException
    }
}
