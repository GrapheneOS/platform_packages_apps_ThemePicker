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
import android.content.res.Resources
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import com.android.customization.model.color.ColorOptionImpl
import com.android.customization.module.logging.ThemesUserEventLogger
import com.android.customization.picker.clock.domain.interactor.ClockPickerInteractor
import com.android.customization.picker.clock.shared.ClockSize
import com.android.customization.picker.clock.shared.model.ClockMetadataModel
import com.android.customization.picker.clock.ui.viewmodel.ClockColorViewModel
import com.android.customization.picker.color.domain.interactor.ColorPickerInteractor
import com.android.customization.picker.color.shared.model.ColorOptionModel
import com.android.customization.picker.color.shared.model.ColorType
import com.android.customization.picker.color.ui.viewmodel.ColorOptionIconViewModel
import com.android.themepicker.R
import com.android.wallpaper.picker.common.icon.ui.viewmodel.Icon
import com.android.wallpaper.picker.common.text.ui.viewmodel.Text
import com.android.wallpaper.picker.customization.ui.viewmodel.FloatingToolbarTabViewModel
import com.android.wallpaper.picker.di.modules.BackgroundDispatcher
import com.android.wallpaper.picker.option.ui.viewmodel.OptionItemViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

/** View model for the clock customization screen. */
class ClockPickerViewModel
@AssistedInject
constructor(
    @ApplicationContext context: Context,
    resources: Resources,
    private val clockPickerInteractor: ClockPickerInteractor,
    colorPickerInteractor: ColorPickerInteractor,
    private val logger: ThemesUserEventLogger,
    @BackgroundDispatcher private val backgroundDispatcher: CoroutineDispatcher,
    @Assisted private val viewModelScope: CoroutineScope,
) {

    enum class Tab {
        STYLE,
        COLOR,
        SIZE,
    }

    private val colorMap = ClockColorViewModel.getPresetColorMap(context.resources)

    // Tabs
    private val _selectedTab = MutableStateFlow(Tab.STYLE)
    val selectedTab: StateFlow<Tab> = _selectedTab.asStateFlow()
    val tabs: Flow<List<FloatingToolbarTabViewModel>> =
        _selectedTab.asStateFlow().map {
            listOf(
                FloatingToolbarTabViewModel(
                    Icon.Resource(
                        res = R.drawable.ic_style_filled_24px,
                        contentDescription = Text.Resource(R.string.clock_style),
                    ),
                    context.getString(R.string.clock_style),
                    it == Tab.STYLE,
                ) {
                    _selectedTab.value = Tab.STYLE
                },
                FloatingToolbarTabViewModel(
                    Icon.Resource(
                        res = R.drawable.ic_palette_filled_24px,
                        contentDescription = Text.Resource(R.string.clock_color),
                    ),
                    context.getString(R.string.clock_color),
                    it == Tab.COLOR,
                ) {
                    _selectedTab.value = Tab.COLOR
                },
                FloatingToolbarTabViewModel(
                    Icon.Resource(
                        res = R.drawable.ic_open_in_full_24px,
                        contentDescription = Text.Resource(R.string.clock_size),
                    ),
                    context.getString(R.string.clock_size),
                    it == Tab.SIZE,
                ) {
                    _selectedTab.value = Tab.SIZE
                },
            )
        }

    // Clock style
    private val overridingClock = MutableStateFlow<ClockMetadataModel?>(null)
    val previewingClock =
        combine(overridingClock, clockPickerInteractor.selectedClock) {
            overridingClock,
            selectedClock ->
            overridingClock ?: selectedClock
        }
    @OptIn(ExperimentalCoroutinesApi::class)
    val clockStyleOptions: StateFlow<List<OptionItemViewModel<Drawable>>> =
        clockPickerInteractor.allClocks
            .mapLatest { allClocks ->
                // Delay to avoid the case that the full list of clocks is not initiated.
                delay(CLOCKS_EVENT_UPDATE_DELAY_MILLIS)
                allClocks.map { clockModel ->
                    val isSelectedFlow =
                        previewingClock
                            .map { it.clockId == clockModel.clockId }
                            .stateIn(viewModelScope)
                    val contentDescription =
                        resources.getString(
                            R.string.select_clock_action_description,
                            clockModel.description,
                        )
                    OptionItemViewModel<Drawable>(
                        key = MutableStateFlow(clockModel.clockId) as StateFlow<String>,
                        payload = clockModel.thumbnail,
                        text = Text.Loaded(contentDescription),
                        isTextUserVisible = false,
                        isSelected = isSelectedFlow,
                        onClicked =
                            isSelectedFlow.map { isSelected ->
                                if (isSelected) {
                                    null
                                } else {
                                    { overridingClock.value = clockModel }
                                }
                            },
                    )
                }
            }
            // makes sure that the operations above this statement are executed on I/O dispatcher
            // while parallelism limits the number of threads this can run on which makes sure that
            // the flows run sequentially
            .flowOn(backgroundDispatcher.limitedParallelism(1))
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // Clock size
    private val overridingClockSize = MutableStateFlow<ClockSize?>(null)
    val previewingClockSize =
        combine(overridingClockSize, clockPickerInteractor.selectedClockSize) {
            overridingClockSize,
            selectedClockSize ->
            overridingClockSize ?: selectedClockSize
        }
    val sizeOptions = flow {
        emit(
            listOf(
                ClockSizeOptionViewModel(
                    ClockSize.DYNAMIC,
                    previewingClockSize.map { it == ClockSize.DYNAMIC }.stateIn(viewModelScope),
                    previewingClockSize
                        .map {
                            if (it == ClockSize.DYNAMIC) {
                                null
                            } else {
                                { overridingClockSize.value = ClockSize.DYNAMIC }
                            }
                        }
                        .stateIn(viewModelScope),
                ),
                ClockSizeOptionViewModel(
                    ClockSize.SMALL,
                    previewingClockSize.map { it == ClockSize.SMALL }.stateIn(viewModelScope),
                    previewingClockSize
                        .map {
                            if (it == ClockSize.SMALL) {
                                null
                            } else {
                                { overridingClockSize.value = ClockSize.SMALL }
                            }
                        }
                        .stateIn(viewModelScope),
                ),
            )
        )
    }

    // Clock color
    // 0 - 100
    private val overridingClockColorId = MutableStateFlow<String?>(null)
    private val previewingClockColorId =
        combine(overridingClockColorId, clockPickerInteractor.selectedColorId) {
            overridingClockColorId,
            selectedColorId ->
            overridingClockColorId ?: selectedColorId
        }

    private val overridingSliderProgress = MutableStateFlow<Int?>(null)
    val previewingSliderProgress: Flow<Int> =
        combine(overridingSliderProgress, clockPickerInteractor.colorToneProgress) {
            overridingSliderProgress,
            colorToneProgress ->
            overridingSliderProgress ?: colorToneProgress
        }
    val isSliderEnabled: Flow<Boolean> =
        combine(previewingClock, previewingClockColorId) { clock, clockColorId ->
                // clockColorId null means clock color is the system theme color, thus no slider
                clock.isReactiveToTone && clockColorId != null
            }
            .distinctUntilChanged()

    fun onSliderProgressChanged(progress: Int) {
        overridingSliderProgress.value = progress
    }

    val previewingSeedColor: Flow<Int?> =
        combine(previewingClockColorId, previewingSliderProgress) { clockColorId, sliderProgress ->
            val clockColorViewModel = if (clockColorId == null) null else colorMap[clockColorId]
            if (clockColorViewModel == null) {
                null
            } else {
                blendColorWithTone(
                    color = clockColorViewModel.color,
                    colorTone = clockColorViewModel.getColorTone(sliderProgress),
                )
            }
        }

    val clockColorOptions: Flow<List<OptionItemViewModel<ColorOptionIconViewModel>>> =
        colorPickerInteractor.colorOptions.map { colorOptions ->
            // Use mapLatest and delay(100) here to prevent too many selectedClockColor update
            // events from ClockRegistry upstream, caused by sliding the saturation level bar.
            delay(COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS)
            buildList {
                val defaultThemeColorOptionViewModel =
                    (colorOptions[ColorType.WALLPAPER_COLOR]?.find { it.isSelected })
                        ?.toOptionItemViewModel(context)
                        ?: (colorOptions[ColorType.PRESET_COLOR]?.find { it.isSelected })
                            ?.toOptionItemViewModel(context)
                if (defaultThemeColorOptionViewModel != null) {
                    add(defaultThemeColorOptionViewModel)
                }

                colorMap.values.forEachIndexed { index, colorModel ->
                    val isSelectedFlow =
                        previewingClockColorId
                            .map { colorMap.keys.indexOf(it) == index }
                            .stateIn(viewModelScope)
                    add(
                        OptionItemViewModel<ColorOptionIconViewModel>(
                            key = MutableStateFlow(colorModel.colorId) as StateFlow<String>,
                            payload =
                                ColorOptionIconViewModel(
                                    lightThemeColor0 = colorModel.color,
                                    lightThemeColor1 = colorModel.color,
                                    lightThemeColor2 = colorModel.color,
                                    lightThemeColor3 = colorModel.color,
                                    darkThemeColor0 = colorModel.color,
                                    darkThemeColor1 = colorModel.color,
                                    darkThemeColor2 = colorModel.color,
                                    darkThemeColor3 = colorModel.color,
                                ),
                            text =
                                Text.Loaded(
                                    context.getString(
                                        R.string.content_description_color_option,
                                        index,
                                    )
                                ),
                            isTextUserVisible = false,
                            isSelected = isSelectedFlow,
                            onClicked =
                                isSelectedFlow.map { isSelected ->
                                    if (isSelected) {
                                        null
                                    } else {
                                        {
                                            overridingClockColorId.value = colorModel.colorId
                                            overridingSliderProgress.value =
                                                ClockMetadataModel.DEFAULT_COLOR_TONE_PROGRESS
                                        }
                                    }
                                },
                        )
                    )
                }
            }
        }

    private suspend fun ColorOptionModel.toOptionItemViewModel(
        context: Context
    ): OptionItemViewModel<ColorOptionIconViewModel> {
        val lightThemeColors =
            (colorOption as ColorOptionImpl)
                .previewInfo
                .resolveColors(
                    /** darkTheme= */
                    false
                )
        val darkThemeColors =
            colorOption.previewInfo.resolveColors(
                /** darkTheme= */
                true
            )
        val isSelectedFlow = previewingClockColorId.map { it == null }.stateIn(viewModelScope)
        return OptionItemViewModel<ColorOptionIconViewModel>(
            key = MutableStateFlow(key) as StateFlow<String>,
            payload =
                ColorOptionIconViewModel(
                    lightThemeColor0 = lightThemeColors[0],
                    lightThemeColor1 = lightThemeColors[1],
                    lightThemeColor2 = lightThemeColors[2],
                    lightThemeColor3 = lightThemeColors[3],
                    darkThemeColor0 = darkThemeColors[0],
                    darkThemeColor1 = darkThemeColors[1],
                    darkThemeColor2 = darkThemeColors[2],
                    darkThemeColor3 = darkThemeColors[3],
                ),
            text = Text.Loaded(context.getString(R.string.default_theme_title)),
            isTextUserVisible = true,
            isSelected = isSelectedFlow,
            onClicked =
                isSelectedFlow.map { isSelected ->
                    if (isSelected) {
                        null
                    } else {
                        {
                            overridingClockColorId.value = null
                            overridingSliderProgress.value =
                                ClockMetadataModel.DEFAULT_COLOR_TONE_PROGRESS
                        }
                    }
                },
        )
    }

    val onApply: Flow<(suspend () -> Unit)?> =
        combine(
            previewingClock,
            previewingClockSize,
            previewingClockColorId,
            previewingSliderProgress,
        ) { clock, size, colorId, progress ->
            {
                val clockColorViewModel = colorMap[colorId]
                val seedColor =
                    if (clockColorViewModel != null) {
                        blendColorWithTone(
                            color = clockColorViewModel.color,
                            colorTone = clockColorViewModel.getColorTone(progress),
                        )
                    } else {
                        null
                    }
                clockPickerInteractor.applyClock(
                    clockId = clock.clockId,
                    size = size,
                    selectedColorId = colorId,
                    colorToneProgress = progress,
                    seedColor = seedColor,
                )
            }
        }

    fun resetPreview() {
        overridingClock.value = null
        overridingClockSize.value = null
        overridingClockColorId.value = null
        overridingSliderProgress.value = null
        _selectedTab.value = Tab.STYLE
    }

    companion object {
        private val helperColorLab: DoubleArray by lazy { DoubleArray(3) }

        fun blendColorWithTone(color: Int, colorTone: Double): Int {
            ColorUtils.colorToLAB(color, helperColorLab)
            return ColorUtils.LABToColor(colorTone, helperColorLab[1], helperColorLab[2])
        }

        const val COLOR_OPTIONS_EVENT_UPDATE_DELAY_MILLIS: Long = 100
        const val CLOCKS_EVENT_UPDATE_DELAY_MILLIS: Long = 100
    }

    @ViewModelScoped
    @AssistedFactory
    interface Factory {
        fun create(viewModelScope: CoroutineScope): ClockPickerViewModel
    }
}
