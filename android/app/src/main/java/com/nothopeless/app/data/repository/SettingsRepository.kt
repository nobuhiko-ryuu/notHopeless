package com.nothopeless.app.data.repository

import com.nothopeless.app.data.local.DataStoreDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(private val dataStore: DataStoreDataSource) {
    val onboardingCompleted: Flow<Boolean> = dataStore.onboardingCompleted
    suspend fun completeOnboarding() = dataStore.setOnboardingCompleted()
}
