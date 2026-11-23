package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.GameProgressRepository
import com.example.data.PlayerProfile
import com.example.data.UiState
import com.example.data.signOutFromGoogle
import com.example.game.SoundEffectsManager
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LevelsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BubbleShooterProTheme
import com.example.ui.theme.DeepHudIndigo
import com.example.ui.theme.GoldStarMid
import com.example.viewmodel.AppScreen
import com.example.viewmodel.CloudProgressSnapshot
import com.example.viewmodel.GameViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BubbleShooterProTheme {
                BubbleShooterAppContent()
            }
        }
    }
}

@Composable
private fun BubbleShooterAppContent() {
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = user.uid
        val displayName = user.displayName ?: "Bubble Player"
        val email = user.email ?: ""
        key(uid) {
            AuthenticatedGameHost(
                currentUserId = uid,
                displayName = displayName,
                userEmail = email,
                onSignedOut = { currentUser = null }
            )
        }
    }
}

@Composable
private fun AuthenticatedGameHost(
    currentUserId: String,
    displayName: String,
    userEmail: String,
    onSignedOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: GameViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repository = GameProgressRepository(db)
                val soundManager = SoundEffectsManager(app.applicationContext)
                GameViewModel(
                    repository = repository,
                    soundManager = soundManager,
                    currentUserId = currentUserId,
                    userDisplayName = displayName,
                    userEmail = userEmail
                )
            }
        }
    )

    val cloudState by viewModel.cloudState.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBanner.collectAsStateWithLifecycle()
    val showDailyRewardDialog by viewModel.showDailyRewardDialog.collectAsStateWithLifecycle()

    when (val state = cloudState) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepHudIndigo),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = GoldStarMid,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Loading Bubble Shooter Pro…",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }

        is UiState.Error -> {
            // Gracefully allow fallback play / retry or sign out without crashing
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DeepHudIndigo)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cloud Sync Error",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFCA5A5),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = {
                            signOutFromGoogle(credentialManager, scope, onSignedOut)
                        }
                    ) {
                        Text(text = "Sign Out & Retry")
                    }
                }
            }
        }

        is UiState.Success<CloudProgressSnapshot> -> {
            val profile: PlayerProfile = state.data.profile
            val levelsByNumber = state.data.levelsByNumber

            when (currentScreen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        profile = profile,
                        dailyRewardAvailable = viewModel.isDailyRewardAvailable(profile),
                        showDailyRewardDialog = showDailyRewardDialog,
                        statusBanner = statusBanner,
                        onPlayClick = { viewModel.playNextUnlockedLevel() },
                        onLevelsClick = { viewModel.navigateTo(AppScreen.LEVELS) },
                        onSettingsClick = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        onDailyGiftClick = { viewModel.openDailyRewardDialog() },
                        onClaimDailyReward = { viewModel.claimDailyReward() },
                        onDismissDailyReward = { viewModel.dismissDailyRewardDialog() },
                        onDismissBanner = { viewModel.clearStatusBanner() }
                    )
                }

                AppScreen.LEVELS -> {
                    LevelsScreen(
                        profile = profile,
                        levelsByNumber = levelsByNumber,
                        onLevelSelected = { levelNum -> viewModel.startLevel(levelNum) },
                        onBackClick = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                }

                AppScreen.GAME -> {
                    GameScreen(
                        gameState = gameState,
                        profile = profile,
                        statusBanner = statusBanner,
                        onAimUpdate = { cannonPos, touchPos ->
                            viewModel.updateAimFromTouch(cannonPos, touchPos)
                        },
                        onFire = { cannonPos -> viewModel.fireBubble(cannonPos) },
                        onCancelAim = { viewModel.cancelAiming() },
                        onSwapBubbles = { viewModel.swapBubbles() },
                        onActivateRainbow = { viewModel.activateRainbowBooster() },
                        onPauseClick = { viewModel.pauseGame() },
                        onResumeClick = { viewModel.resumeGame() },
                        onRestartClick = { viewModel.restartCurrentLevel() },
                        onNextLevelClick = { viewModel.startNextLevel() },
                        onLevelsClick = { viewModel.navigateTo(AppScreen.LEVELS) },
                        onHomeClick = { viewModel.navigateTo(AppScreen.HOME) },
                        onToggleSound = {
                            viewModel.updateSettings(
                                soundEnabled = !profile.soundEnabled,
                                hapticsEnabled = profile.hapticsEnabled,
                                aimGuideExtended = profile.aimGuideExtended
                            )
                        },
                        onDismissBanner = { viewModel.clearStatusBanner() },
                        onTickFrame = { dt, ratio -> viewModel.tickFrame(dt, ratio) }
                    )
                }

                AppScreen.SETTINGS -> {
                    SettingsScreen(
                        profile = profile,
                        userEmail = viewModel.userEmail,
                        onUpdateSettings = { sound, haptics, aimGuide ->
                            viewModel.updateSettings(sound, haptics, aimGuide)
                        },
                        onSignOutClick = {
                            signOutFromGoogle(credentialManager, scope, onSignedOut)
                        },
                        onBackClick = { viewModel.navigateTo(AppScreen.HOME) }
                    )
                }
            }
        }
    }
}
