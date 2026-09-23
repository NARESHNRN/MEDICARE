package com.example

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel
import com.example.ui.ScreenState
import com.example.ui.components.UniversalVoiceCommander
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if launched via Google Assistant or Voice Command
        val action = intent?.action
        if (action == Intent.ACTION_VOICE_COMMAND || action == Intent.ACTION_ASSIST) {
            viewModel.navigateTo(ScreenState.SCAN)
        }

        setContent {
            MyApplicationTheme {
                // Request critical camera and microphone permissions smoothly on launch
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    // Handled gracefully in app flows
                }

                LaunchedEffect(Unit) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                        )
                    )
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        MediVoiceApp(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Omnipresent Voice Assistant Floating Controller & Feedback
                        UniversalVoiceCommander(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun MediVoiceApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    Crossfade(
        targetState = currentScreen,
        modifier = modifier.fillMaxSize(),
        label = "screen_transition"
    ) { screen ->
        when (screen) {
            ScreenState.LANGUAGE_SELECT -> LanguageScreen(viewModel)
            ScreenState.USER_SETUP -> UserSetupScreen(viewModel)
            ScreenState.PRIVACY_CONSENT -> PrivacyScreen(viewModel)
            ScreenState.HOME -> HomeScreen(viewModel)
            ScreenState.SCAN -> ScanScreen(viewModel)
            ScreenState.MEDICINE_RESULT -> MedicineResultScreen(viewModel)
            ScreenState.HISTORY -> HistoryScreen(viewModel)
            ScreenState.CAREGIVER -> CaregiverScreen(viewModel)
        }
    }
}
