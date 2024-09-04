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

package com.android.wallpaper.di.modules

import com.android.customization.model.grid.FakeGridOptionsManager
import com.android.customization.model.grid.GridOptionsManager2
import com.android.wallpaper.picker.di.modules.ThemePickerSharedAppModule
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [ThemePickerSharedAppModule::class]
)
abstract class ThemePickerSharedAppTestModule {

    @Binds
    @Singleton
    abstract fun bindGridOptionsManager2(impl: FakeGridOptionsManager): GridOptionsManager2
}
