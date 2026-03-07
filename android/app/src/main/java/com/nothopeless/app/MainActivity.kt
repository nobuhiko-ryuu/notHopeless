package com.nothopeless.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.nothopeless.app.data.repository.SettingsRepository
import com.nothopeless.app.ui.AppRoot
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var auth: FirebaseAuth
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnFailureListener { e ->
                android.util.Log.e("MainActivity", "Anonymous sign-in failed", e)
            }
        }
        lifecycleScope.launch {
            val completed = settingsRepository.onboardingCompleted.first()
            val start = if (completed) "home" else "onboarding"
            setContent { AppRoot(startDestination = start) }
        }
    }
}
