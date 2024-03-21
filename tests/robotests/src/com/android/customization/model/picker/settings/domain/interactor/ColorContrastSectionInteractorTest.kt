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

package com.android.customization.model.picker.settings.domain.interactor

import androidx.test.filters.SmallTest
import com.android.customization.picker.settings.data.repository.ColorContrastSectionRepository
import com.android.customization.picker.settings.domain.interactor.ColorContrastSectionInteractor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@SmallTest
@RunWith(RobolectricTestRunner::class)
class ColorContrastSectionInteractorTest {

    @Test
    fun contrastEmitCorrectValuesFromRepository() = runBlockingTest {
        val mockRepository: ColorContrastSectionRepository = mock()
        val expectedContrast = 1.5f
        whenever(mockRepository.contrast).thenReturn(flowOf(expectedContrast))
        val interactor = ColorContrastSectionInteractor(mockRepository)

        val result = interactor.contrast.first()

        assertEquals(expectedContrast, result)
    }
}
