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
 *
 */

package com.android.customization.model.notifications.domain.interactor

import android.provider.Settings
import androidx.test.filters.SmallTest
import com.android.customization.picker.notifications.domain.interactor.NotificationsSnapshotRestorer
import com.android.systemui.shared.notifications.data.repository.NotificationSettingsRepository
import com.android.systemui.shared.notifications.domain.interactor.NotificationSettingsInteractor
import com.android.systemui.shared.settings.data.repository.FakeSecureSettingsRepository
import com.android.systemui.shared.settings.data.repository.FakeSystemSettingsRepository
import com.android.wallpaper.testing.FakeSnapshotStore
import com.android.wallpaper.testing.collectLastValue
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@SmallTest
@RunWith(RobolectricTestRunner::class)
class NotificationsSnapshotRestorerTest {

    private lateinit var underTest: NotificationsSnapshotRestorer
    private lateinit var fakeSecureSettingsRepository: FakeSecureSettingsRepository
    private lateinit var fakeSystemSettingsRepository: FakeSystemSettingsRepository
    private lateinit var interactor: NotificationSettingsInteractor

    private lateinit var testScope: TestScope

    @Before
    fun setUp() {
        val testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)
        fakeSecureSettingsRepository = FakeSecureSettingsRepository()
        fakeSystemSettingsRepository = FakeSystemSettingsRepository()
        interactor =
            NotificationSettingsInteractor(
                repository =
                    NotificationSettingsRepository(
                        backgroundScope = testScope.backgroundScope,
                        backgroundDispatcher = testDispatcher,
                        secureSettingsRepository = fakeSecureSettingsRepository,
                        systemSettingsRepository = fakeSystemSettingsRepository,
                    ),
            )
        underTest =
            NotificationsSnapshotRestorer(
                interactor = interactor,
                backgroundScope = testScope.backgroundScope
            )
    }

    @Test
    fun setUpAndRestore_Active() =
        testScope.runTest {
            fakeSecureSettingsRepository.setInt(Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 1)
            val showNotifs = collectLastValue(interactor.isShowNotificationsOnLockScreenEnabled())

            val store = FakeSnapshotStore()
            store.store(underTest.setUpSnapshotRestorer(store = store))
            val initialSnapshot = store.retrieve()
            underTest.restoreToSnapshot(snapshot = initialSnapshot)

            assertThat(showNotifs()).isTrue()
        }

    @Test
    fun setUpAndRestore_Inactive() =
        testScope.runTest {
            fakeSecureSettingsRepository.setInt(Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0)
            val showNotifs = collectLastValue(interactor.isShowNotificationsOnLockScreenEnabled())

            val store = FakeSnapshotStore()
            store.store(underTest.setUpSnapshotRestorer(store = store))
            val initialSnapshot = store.retrieve()
            underTest.restoreToSnapshot(snapshot = initialSnapshot)

            assertThat(showNotifs()).isFalse()
        }

    @Test
    fun setUp_deactivate_restoreToActive() = runTest {
        fakeSecureSettingsRepository.setInt(Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 1)
        val showNotifs = collectLastValue(interactor.isShowNotificationsOnLockScreenEnabled())
        val store = FakeSnapshotStore()
        store.store(underTest.setUpSnapshotRestorer(store = store))
        val initialSnapshot = store.retrieve()

        fakeSecureSettingsRepository.setInt(Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0)
        underTest.restoreToSnapshot(snapshot = initialSnapshot)

        assertThat(showNotifs()).isTrue()
    }

    @Test
    fun setUp_activate_restoreToInactive() = runTest {
        fakeSecureSettingsRepository.setInt(Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 0)
        val showNotifs = collectLastValue(interactor.isShowNotificationsOnLockScreenEnabled())
        val store = FakeSnapshotStore()
        store.store(underTest.setUpSnapshotRestorer(store = store))
        val initialSnapshot = store.retrieve()

        fakeSecureSettingsRepository.setInt(Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS, 1)
        underTest.restoreToSnapshot(snapshot = initialSnapshot)

        assertThat(showNotifs()).isFalse()
    }
}
