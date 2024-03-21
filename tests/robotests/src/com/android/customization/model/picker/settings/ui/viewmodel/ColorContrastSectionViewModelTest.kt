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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.customization.picker.settings.domain.interactor.ColorContrastSectionInteractor
import com.android.customization.picker.settings.ui.viewmodel.ColorContrastSectionDataViewModel
import com.android.customization.picker.settings.ui.viewmodel.ColorContrastSectionViewModel
import com.android.themepicker.R
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ColorContrastSectionViewModelTest {
    private lateinit var bgDispatcher: TestCoroutineDispatcher
    private lateinit var context: Context
    private lateinit var viewModel: ColorContrastSectionViewModel
    private lateinit var interactor: ColorContrastSectionInteractor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        bgDispatcher = TestCoroutineDispatcher()
    }

    @Test
    fun summaryEmitsCorrectDataValueForStandard() = runBlockingTest {
        interactor = mock {
            on { contrast } doReturn
                flowOf(ColorContrastSectionViewModel.ContrastValue.STANDARD.value)
        }
        val factory = ColorContrastSectionViewModel.Factory(interactor)
        viewModel = factory.create(ColorContrastSectionViewModel::class.java)

        val expected =
            ColorContrastSectionDataViewModel(
                Text.Resource(R.string.color_contrast_default_title),
                Icon.Resource(res = R.drawable.ic_contrast_standard, contentDescription = null)
            )

        val result = viewModel.summary.first()
        assertEquals(expected, result)
    }

    @Test
    fun summaryEmitsCorrectDataValueForMedium() = runBlockingTest {
        interactor = mock {
            on { contrast } doReturn
                flowOf(ColorContrastSectionViewModel.ContrastValue.MEDIUM.value)
        }
        val factory = ColorContrastSectionViewModel.Factory(interactor)
        viewModel = factory.create(ColorContrastSectionViewModel::class.java)

        val expected =
            ColorContrastSectionDataViewModel(
                Text.Resource(R.string.color_contrast_medium_title),
                Icon.Resource(res = R.drawable.ic_contrast_medium, contentDescription = null)
            )

        val result = viewModel.summary.first()
        assertEquals(expected, result)
    }

    @Test
    fun summaryEmitsCorrectDataValueForHigh() = runBlockingTest {
        interactor = mock {
            on { contrast } doReturn flowOf(ColorContrastSectionViewModel.ContrastValue.HIGH.value)
        }
        val factory = ColorContrastSectionViewModel.Factory(interactor)
        viewModel = factory.create(ColorContrastSectionViewModel::class.java)

        val expected =
            ColorContrastSectionDataViewModel(
                Text.Resource(R.string.color_contrast_high_title),
                Icon.Resource(res = R.drawable.ic_contrast_high, contentDescription = null)
            )

        val result = viewModel.summary.first()
        assertEquals(expected, result)
    }

    @Test(expected = IllegalArgumentException::class)
    fun summaryThrowsIllegalArgumentExceptionForInvalidValue() = runBlockingTest {
        interactor = mock { on { contrast } doReturn flowOf(999f) }
        val factory = ColorContrastSectionViewModel.Factory(interactor)
        viewModel = factory.create(ColorContrastSectionViewModel::class.java)
        viewModel.summary.collect() // This should throw an IllegalArgumentException
    }
}
