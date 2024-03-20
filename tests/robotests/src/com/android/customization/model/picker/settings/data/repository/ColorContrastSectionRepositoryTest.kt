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

import android.app.UiModeManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import com.android.customization.picker.settings.data.repository.ColorContrastSectionRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.robolectric.RobolectricTestRunner

@SmallTest
@RunWith(RobolectricTestRunner::class)
class ColorContrastSectionRepositoryTest {
    private lateinit var underTest: ColorContrastSectionRepository

    private lateinit var context: Context
    private lateinit var bgDispatcher: TestCoroutineDispatcher

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        bgDispatcher = TestCoroutineDispatcher()
        underTest = ColorContrastSectionRepository(context, bgDispatcher)
    }

    @Test
    fun creationSucceeds() {
        assertThat(underTest).isNotNull()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun contrastFlowEmitsValues() = runBlockingTest {
        val mockUiModeManager = mock(UiModeManager::class.java)
        val contrastValues = listOf(0.5f, 0.7f, 0.8f)

        // Stub the initial contrast value before the flow starts collecting
        `when`(mockUiModeManager.contrast).thenReturn(contrastValues[0])

        // Assign the mockUiModeManager to the repository's uiModeManager
        underTest.uiModeManager = mockUiModeManager

        // Create a collector for the flow
        val flowCollector = mutableListOf<Float>()

        // Start collecting values from the flow
        val job = launch { underTest.contrast.collect { flowCollector.add(it) } }

        // Capture the ContrastChangeListener
        val listenerCaptor = argumentCaptor<UiModeManager.ContrastChangeListener>()
        verify(mockUiModeManager).addContrastChangeListener(any(), listenerCaptor.capture())

        // Simulate contrast changes after the initial value has been emitted
        contrastValues.drop(1).forEach { newValue ->
            listenerCaptor.firstValue.onContrastChanged(newValue)
        }

        assertThat(flowCollector).containsExactlyElementsIn(contrastValues)

        job.cancel()
    }

    @After
    fun tearDown() {
        bgDispatcher.cleanupTestCoroutines()
    }
}
