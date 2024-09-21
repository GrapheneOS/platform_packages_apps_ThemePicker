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

package com.android.customization.model.picker.settings.data.repository

import android.app.UiModeManager.ContrastUtils
import androidx.test.filters.SmallTest
import com.android.customization.picker.settings.data.repository.ColorContrastSectionRepository
import com.android.wallpaper.testing.FakeUiModeManager
import com.google.common.truth.Truth.assertThat
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@HiltAndroidTest
@SmallTest
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ColorContrastSectionRepositoryTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)

    @Inject lateinit var uiModeManager: FakeUiModeManager
    @Inject lateinit var underTest: ColorContrastSectionRepository
    @Inject lateinit var testScope: TestScope

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun creationSucceeds() {
        assertThat(underTest).isNotNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun contrastFlowEmitsValues() =
        testScope.runTest {
            val nextContrastValues = listOf(0.5f, 0.7f, 0.8f)
            val expectedContrastValues =
                nextContrastValues.map { ContrastUtils.toContrastLevel(it) }
            // Set up a flow to collect all contrast values
            val flowCollector = mutableListOf<Int>()
            // Start collecting values from the flow, using an unconfined dispatcher to start
            // collecting from the flow right away (rather than explicitly calling `runCurrent`)
            // See https://developer.android.com/kotlin/flow/test#continuous-collection
            backgroundScope.launch(UnconfinedTestDispatcher()) {
                underTest.contrast.toList(flowCollector)
            }

            nextContrastValues.forEach { uiModeManager.setContrast(it) }

            // Ignore the first contrast value from constructing the repository
            val collectedValues = flowCollector.drop(1)
            assertThat(collectedValues).containsExactlyElementsIn(expectedContrastValues)
        }
}
