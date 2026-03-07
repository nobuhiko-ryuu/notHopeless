package com.nothopeless.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class DataStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val onboardingKey = booleanPreferencesKey("onboarding_completed")

    val onboardingCompleted: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[onboardingKey] ?: false }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs -> prefs[onboardingKey] = true }
    }
}
