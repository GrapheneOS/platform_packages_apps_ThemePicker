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

import android.app.UiModeManager.ContrastUtils
import androidx.test.filters.SmallTest
import com.android.customization.picker.settings.domain.interactor.ColorContrastSectionInteractor
import com.android.wallpaper.testing.FakeUiModeManager
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@SmallTest
@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
class ColorContrastSectionInteractorTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var uiModeManager: FakeUiModeManager
    @Inject lateinit var interactor: ColorContrastSectionInteractor

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun contrastEmitCorrectValuesFromRepository() = runTest {
        val contrastVal = 0.5f
        val expectedContrastVal = ContrastUtils.toContrastLevel(contrastVal)
        uiModeManager.setContrast(contrastVal)

        val result = interactor.contrast.first()

        assertThat(result).isEqualTo(expectedContrastVal)
    }
}
