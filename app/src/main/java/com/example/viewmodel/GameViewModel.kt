package com.example.viewmodel

import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameProgressRepository
import com.example.data.LevelProgress
import com.example.data.PlayerProfile
import com.example.data.UiState
import com.example.game.ActiveProjectile
import com.example.game.BubbleColor
import com.example.game.BubbleGridEngine
import com.example.game.FallingBubble
import com.example.game.FloatingScorePopup
import com.example.game.LevelDefinition
import com.example.game.LevelGenerator
import com.example.game.PopEffect
import com.example.game.SoundEffectsManager
import com.example.game.SparkleParticle
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class AppScreen {
    HOME,
    LEVELS,
    GAME,
    SETTINGS
}

enum class GameStatus {
    PLAYING,
    PAUSED,
    WON,
    LOST_OUT_OF_MOVES,
    LOST_DANGER_LINE
}

data class ActiveGameState(
    val level: LevelDefinition = LevelGenerator.generateLevel(1),
    val grid: List<List<BubbleColor?>> = LevelGenerator.generateLevel(1).initialRows,
    val currentBubble: BubbleColor = BubbleColor.RED,
    val nextBubble: BubbleColor = BubbleColor.BLUE,
    val movesRemaining: Int = 24,
    val score: Long = 0L,
    val moveBonus: Long = 0L,
    val starsEarned: Int = 0,
    val isNewHighScore: Boolean = false,
    val comboStreak: Int = 0,
    val aimAngleDeg: Float = -90f,
    val isAiming: Boolean = false,
    val projectile: ActiveProjectile? = null,
    val popEffects: List<PopEffect> = emptyList(),
    val particles: List<SparkleParticle> = emptyList(),
    val fallingBubbles: List<FallingBubble> = emptyList(),
    val scorePopups: List<FloatingScorePopup> = emptyList(),
    val status: GameStatus = GameStatus.PLAYING,
    val rainbowReady: Boolean = false
)

data class CloudProgressSnapshot(
    val profile: PlayerProfile,
    val levelsByNumber: Map<Int, LevelProgress>
)

class GameViewModel(
    private val repository: GameProgressRepository,
    val soundManager: SoundEffectsManager,
    private val currentUserId: String,
    private val userDisplayName: String,
    val userEmail: String
) : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _gameState = MutableStateFlow(ActiveGameState())
    val gameState: StateFlow<ActiveGameState> = _gameState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _showDailyRewardDialog = MutableStateFlow(false)
    val showDailyRewardDialog: StateFlow<Boolean> = _showDailyRewardDialog.asStateFlow()

    val cloudState: StateFlow<UiState<CloudProgressSnapshot>> = combine(
        repository.observePlayerProfile(currentUserId),
        repository.observeAllLevelProgress(currentUserId)
    ) { profile, levels ->
        val resolvedProfile = profile ?: PlayerProfile(
            id = currentUserId,
            userId = currentUserId,
            displayName = userDisplayName.ifBlank { "Bubble Player" }
        )
        soundManager.soundEnabled = resolvedProfile.soundEnabled
        soundManager.hapticsEnabled = resolvedProfile.hapticsEnabled
        CloudProgressSnapshot(
            profile = resolvedProfile,
            levelsByNumber = levels.associateBy { it.levelNumber.toInt() }
        )
    }
        .map<CloudProgressSnapshot, UiState<CloudProgressSnapshot>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing cloud progress", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to sync game progress"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private var lastBoardHeightRatio: Float = 1.48f

    init {
        viewModelScope.launch {
            val initResult = repository.ensureProfileInitialized(userDisplayName.ifBlank { "Bubble Player" })
            if (initResult.isFailure) {
                Log.w(TAG, "Profile initialization deferred", initResult.exceptionOrNull())
            }
        }
    }

    fun todayIsoDate(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return formatter.format(Date())
    }

    fun isDailyRewardAvailable(profile: PlayerProfile): Boolean {
        return profile.lastClaimedDailyRewardDay != todayIsoDate()
    }

    fun openDailyRewardDialog() {
        soundManager.playButton()
        _showDailyRewardDialog.value = true
    }

    fun dismissDailyRewardDialog() {
        _showDailyRewardDialog.value = false
    }

    fun claimDailyReward() {
        val snapshot = (cloudState.value as? UiState.Success)?.data ?: return
        if (!isDailyRewardAvailable(snapshot.profile)) return
        soundManager.playWin()
        viewModelScope.launch {
            val result = repository.claimDailyReward(
                todayIso = todayIsoDate(),
                currentCoins = snapshot.profile.bonusCoins,
                rewardAmount = DAILY_REWARD_COINS
            )
            if (result.isSuccess) {
                _statusBanner.value = "+$DAILY_REWARD_COINS Bonus Coins claimed!"
                _showDailyRewardDialog.value = false
            } else {
                _statusBanner.value = "Could not claim gift right now. Please try again."
            }
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun navigateTo(screen: AppScreen) {
        soundManager.playButton()
        _currentScreen.value = screen
    }

    fun playNextUnlockedLevel() {
        val unlocked = (cloudState.value as? UiState.Success)?.data?.profile?.highestUnlockedLevel?.toInt() ?: 1
        startLevel(unlocked.coerceIn(1, LevelGenerator.TOTAL_LEVELS))
    }

    fun startLevel(levelNumber: Int) {
        soundManager.playButton()
        val levelDef = LevelGenerator.generateLevel(levelNumber)
        val firstColor = BubbleGridEngine.pickNextBubbleColor(levelDef.initialRows, levelDef.availableColors)
        val secondColor = BubbleGridEngine.pickNextBubbleColor(levelDef.initialRows, levelDef.availableColors)
        _gameState.value = ActiveGameState(
            level = levelDef,
            grid = levelDef.initialRows,
            currentBubble = firstColor,
            nextBubble = secondColor,
            movesRemaining = levelDef.movesLimit,
            score = 0L,
            moveBonus = 0L,
            starsEarned = 0,
            isNewHighScore = false,
            comboStreak = 0,
            aimAngleDeg = -90f,
            isAiming = false,
            projectile = null,
            popEffects = emptyList(),
            particles = emptyList(),
            fallingBubbles = emptyList(),
            scorePopups = emptyList(),
            status = GameStatus.PLAYING,
            rainbowReady = false
        )
        _currentScreen.value = AppScreen.GAME
    }

    fun restartCurrentLevel() {
        val currentLevelNum = _gameState.value.level.levelNumber
        startLevel(currentLevelNum)
    }

    fun startNextLevel() {
        val nextNum = (_gameState.value.level.levelNumber + 1).coerceAtMost(LevelGenerator.TOTAL_LEVELS)
        startLevel(nextNum)
    }

    fun pauseGame() {
        soundManager.playButton()
        _gameState.update { state ->
            if (state.status == GameStatus.PLAYING) state.copy(status = GameStatus.PAUSED, isAiming = false) else state
        }
    }

    fun resumeGame() {
        soundManager.playButton()
        _gameState.update { state ->
            if (state.status == GameStatus.PAUSED) state.copy(status = GameStatus.PLAYING) else state
        }
    }

    fun updateAimFromTouch(cannonPos: Offset, touchPos: Offset) {
        val state = _gameState.value
        if (state.status != GameStatus.PLAYING || state.projectile != null) return
        val angle = BubbleGridEngine.angleFromTouch(cannonPos, touchPos)
        _gameState.update { it.copy(aimAngleDeg = angle, isAiming = true) }
    }

    fun cancelAiming() {
        _gameState.update { it.copy(isAiming = false) }
    }

    fun fireBubble(cannonPos: Offset) {
        val state = _gameState.value
        if (state.status != GameStatus.PLAYING || state.projectile != null || state.movesRemaining <= 0) return

        val rad = state.aimAngleDeg * PI / 180.0
        val vx = (cos(rad) * BubbleGridEngine.PROJECTILE_SPEED).toFloat()
        val vy = (sin(rad) * BubbleGridEngine.PROJECTILE_SPEED).toFloat()

        soundManager.playShoot()
        _gameState.update { current ->
            val nextReplacement = BubbleGridEngine.pickNextBubbleColor(
                current.grid,
                current.level.availableColors
            )
            current.copy(
                projectile = ActiveProjectile(
                    x = cannonPos.x,
                    y = cannonPos.y,
                    vx = vx,
                    vy = vy,
                    color = current.currentBubble
                ),
                currentBubble = current.nextBubble,
                nextBubble = nextReplacement,
                movesRemaining = (current.movesRemaining - 1).coerceAtLeast(0),
                isAiming = false,
                rainbowReady = false
            )
        }
    }

    fun swapBubbles() {
        val state = _gameState.value
        if (state.status != GameStatus.PLAYING || state.projectile != null) return
        soundManager.playSwap()
        _gameState.update { current ->
            current.copy(
                currentBubble = current.nextBubble,
                nextBubble = current.currentBubble
            )
        }
    }

    fun activateRainbowBooster() {
        val state = _gameState.value
        if (state.status != GameStatus.PLAYING || state.projectile != null || state.currentBubble == BubbleColor.RAINBOW) {
            return
        }
        val snapshot = (cloudState.value as? UiState.Success)?.data ?: return
        if (snapshot.profile.bonusCoins < RAINBOW_BOOSTER_COST) {
            _statusBanner.value = "Need $RAINBOW_BOOSTER_COST coins for a Rainbow Wildcard!"
            return
        }
        soundManager.playSwap()
        _gameState.update { current ->
            current.copy(
                currentBubble = BubbleColor.RAINBOW,
                rainbowReady = true
            )
        }
        viewModelScope.launch {
            repository.spendBonusCoins(snapshot.profile.bonusCoins, RAINBOW_BOOSTER_COST)
        }
    }

    fun updateSettings(soundEnabled: Boolean, hapticsEnabled: Boolean, aimGuideExtended: Boolean) {
        soundManager.soundEnabled = soundEnabled
        soundManager.hapticsEnabled = hapticsEnabled
        if (soundEnabled) soundManager.playButton()
        viewModelScope.launch {
            val result = repository.updateSettings(soundEnabled, hapticsEnabled, aimGuideExtended)
            if (result.isFailure) {
                _statusBanner.value = "Unable to sync settings right now."
            }
        }
    }

    /**
     * Advances physics, particles, pop effects, and falling bubbles on each animation frame.
     */
    fun tickFrame(dtSeconds: Float, boardHeightRatio: Float) {
        lastBoardHeightRatio = boardHeightRatio
        val state = _gameState.value
        if (state.status == GameStatus.PAUSED) return

        val hasWork = state.projectile != null ||
            state.popEffects.isNotEmpty() ||
            state.particles.isNotEmpty() ||
            state.fallingBubbles.isNotEmpty() ||
            state.scorePopups.isNotEmpty()
        if (!hasWork) return

        val dt = dtSeconds.coerceIn(0.001f, 0.045f)

        // Update pop rings
        val nextPops = ArrayList<PopEffect>(state.popEffects.size)
        for (p in state.popEffects) {
            val np = p.progress + dt * 3.8f
            if (np < 1f) nextPops.add(p.copy(progress = np))
        }

        // Update sparkle particles (bounded for low RAM)
        val nextParticles = ArrayList<SparkleParticle>(state.particles.size)
        for (pt in state.particles) {
            val nl = pt.life - dt * 2.1f
            if (nl > 0f) {
                nextParticles.add(
                    pt.copy(
                        x = pt.x + pt.vx * dt,
                        y = pt.y + pt.vy * dt,
                        vy = pt.vy + 1.8f * dt,
                        life = nl
                    )
                )
            }
        }

        // Update falling disconnected bubbles
        val nextFalling = ArrayList<FallingBubble>(state.fallingBubbles.size)
        for (fb in state.fallingBubbles) {
            val ny = fb.y + fb.vy * dt
            if (ny < boardHeightRatio + 0.25f) {
                nextFalling.add(
                    fb.copy(
                        x = fb.x + fb.vx * dt,
                        y = ny,
                        vy = fb.vy + 3.4f * dt
                    )
                )
            }
        }

        // Update floating score popups
        val nextPopups = ArrayList<FloatingScorePopup>(state.scorePopups.size)
        for (sp in state.scorePopups) {
            val nl = sp.life - dt * 1.15f
            if (nl > 0f) {
                nextPopups.add(sp.copy(y = sp.y - 0.16f * dt, life = nl))
            }
        }

        // Advance active projectile with 10 sub-steps to prevent tunneling
        val proj = state.projectile
        if (proj != null && state.status == GameStatus.PLAYING) {
            val subSteps = 10
            val subDt = dt / subSteps
            var px = proj.x
            var py = proj.y
            var pvx = proj.vx
            val pvy = proj.vy
            var bounced = false
            var impacted = false

            for (step in 0 until subSteps) {
                px += pvx * subDt
                py += pvy * subDt

                if (px <= BubbleGridEngine.BUBBLE_RADIUS) {
                    px = BubbleGridEngine.BUBBLE_RADIUS + (BubbleGridEngine.BUBBLE_RADIUS - px)
                    pvx = abs(pvx)
                    bounced = true
                } else if (px >= BubbleGridEngine.BOARD_WIDTH - BubbleGridEngine.BUBBLE_RADIUS) {
                    px = (BubbleGridEngine.BOARD_WIDTH - BubbleGridEngine.BUBBLE_RADIUS) -
                        (px - (BubbleGridEngine.BOARD_WIDTH - BubbleGridEngine.BUBBLE_RADIUS))
                    pvx = -abs(pvx)
                    bounced = true
                }

                if (py <= BubbleGridEngine.BUBBLE_RADIUS) {
                    py = BubbleGridEngine.BUBBLE_RADIUS
                    impacted = true
                    break
                }

                if (BubbleGridEngine.collidesWithGrid(state.grid, px, py)) {
                    impacted = true
                    break
                }
            }

            if (bounced) soundManager.playBounce()

            if (impacted) {
                resolveProjectileImpact(
                    state = state,
                    impactX = px,
                    impactY = py,
                    color = proj.color,
                    existingPops = nextPops,
                    existingParticles = nextParticles,
                    existingFalling = nextFalling,
                    existingPopups = nextPopups
                )
            } else {
                _gameState.value = state.copy(
                    projectile = proj.copy(x = px, y = py, vx = pvx, vy = pvy),
                    popEffects = nextPops,
                    particles = nextParticles,
                    fallingBubbles = nextFalling,
                    scorePopups = nextPopups
                )
            }
        } else {
            _gameState.value = state.copy(
                popEffects = nextPops,
                particles = nextParticles,
                fallingBubbles = nextFalling,
                scorePopups = nextPopups
            )
        }
    }

    private fun resolveProjectileImpact(
        state: ActiveGameState,
        impactX: Float,
        impactY: Float,
        color: BubbleColor,
        existingPops: MutableList<PopEffect>,
        existingParticles: MutableList<SparkleParticle>,
        existingFalling: MutableList<FallingBubble>,
        existingPopups: MutableList<FloatingScorePopup>
    ) {
        val mutableGrid = state.grid.map { it.toMutableList() }.toMutableList()
        val nextCombo = state.comboStreak + 1
        val outcome = BubbleGridEngine.placeAndResolve(
            mutableGrid = mutableGrid,
            impactX = impactX,
            impactY = impactY,
            color = color,
            comboMultiplier = nextCombo.coerceAtMost(4)
        )

        val poppedAny = outcome.poppedCells.isNotEmpty()
        val finalCombo = if (poppedAny) nextCombo else 0

        if (poppedAny) {
            soundManager.playPop(finalCombo)
            val rng = Random.Default
            for (cell in outcome.poppedCells) {
                val center = BubbleGridEngine.cellCenter(cell.row, cell.col)
                val popColor = state.grid[cell.row][cell.col] ?: color
                existingPops.add(PopEffect(center.x, center.y, popColor, 0f))
                // Bounded particle bursts for low-RAM devices
                val burstCount = if (existingParticles.size < MAX_PARTICLES - 6) 5 else 2
                for (i in 0 until burstCount) {
                    if (existingParticles.size >= MAX_PARTICLES) break
                    val angle = rng.nextFloat() * (2f * PI.toFloat())
                    val speed = 0.25f + rng.nextFloat() * 0.55f
                    existingParticles.add(
                        SparkleParticle(
                            x = center.x,
                            y = center.y,
                            vx = cos(angle) * speed,
                            vy = sin(angle) * speed - 0.15f,
                            color = popColor,
                            radius = 0.008f + rng.nextFloat() * 0.009f,
                            life = 0.85f + rng.nextFloat() * 0.25f
                        )
                    )
                }
            }
            val anchorCenter = BubbleGridEngine.cellCenter(outcome.placedCell.row, outcome.placedCell.col)
            val label = if (finalCombo >= 2) "+${outcome.scoreGained}  x$finalCombo!" else "+${outcome.scoreGained}"
            existingPopups.add(
                FloatingScorePopup(
                    x = anchorCenter.x.coerceIn(0.18f, 0.82f),
                    y = anchorCenter.y,
                    text = label,
                    isCombo = finalCombo >= 2 || outcome.droppedCells.isNotEmpty()
                )
            )
        }

        if (outcome.droppedCells.isNotEmpty()) {
            soundManager.playDrop()
            val rng = Random.Default
            for ((cell, droppedColor) in outcome.droppedCells) {
                val center = BubbleGridEngine.cellCenter(cell.row, cell.col)
                existingFalling.add(
                    FallingBubble(
                        x = center.x,
                        y = center.y,
                        vx = (rng.nextFloat() - 0.5f) * 0.45f,
                        vy = -0.25f - rng.nextFloat() * 0.2f,
                        color = droppedColor
                    )
                )
            }
        }

        val updatedGrid = mutableGrid.map { it.toList() }
        val updatedScore = state.score + outcome.scoreGained

        // Refresh shooter colors if current/next bubble color no longer exists on board
        val remainingPalette = BubbleGridEngine.remainingColorsOnBoard(updatedGrid, state.level.availableColors)
        val refreshedCurrent = if (state.currentBubble == BubbleColor.RAINBOW || state.currentBubble in remainingPalette) {
            state.currentBubble
        } else {
            remainingPalette.first()
        }
        val refreshedNext = if (state.nextBubble in remainingPalette) {
            state.nextBubble
        } else {
            remainingPalette.last()
        }

        var finalStatus = GameStatus.PLAYING
        var moveBonus = 0L
        var finalTotalScore = updatedScore
        var stars = 0
        var newBest = false

        when {
            outcome.boardCleared -> {
                finalStatus = GameStatus.WON
                moveBonus = state.movesRemaining * 150L
                finalTotalScore = updatedScore + moveBonus
                stars = when {
                    finalTotalScore >= state.level.threeStarScore -> 3
                    finalTotalScore >= state.level.twoStarScore -> 2
                    else -> 1
                }
                val currentBest = (cloudState.value as? UiState.Success)?.data?.profile?.highScore ?: 0L
                newBest = finalTotalScore > currentBest
                soundManager.playWin()
                persistLevelOutcome(
                    levelNumber = state.level.levelNumber,
                    score = finalTotalScore,
                    starsEarned = stars,
                    won = true
                )
            }
            outcome.dangerReached -> {
                finalStatus = GameStatus.LOST_DANGER_LINE
                soundManager.playLose()
                persistLevelOutcome(
                    levelNumber = state.level.levelNumber,
                    score = updatedScore,
                    starsEarned = 0,
                    won = false
                )
            }
            state.movesRemaining <= 0 -> {
                finalStatus = GameStatus.LOST_OUT_OF_MOVES
                soundManager.playLose()
                persistLevelOutcome(
                    levelNumber = state.level.levelNumber,
                    score = updatedScore,
                    starsEarned = 0,
                    won = false
                )
            }
        }

        _gameState.value = state.copy(
            grid = updatedGrid,
            currentBubble = refreshedCurrent,
            nextBubble = refreshedNext,
            score = finalTotalScore,
            moveBonus = moveBonus,
            starsEarned = stars,
            isNewHighScore = newBest,
            comboStreak = finalCombo,
            projectile = null,
            popEffects = existingPops,
            particles = existingParticles,
            fallingBubbles = existingFalling,
            scorePopups = existingPopups,
            status = finalStatus
        )
    }

    private fun persistLevelOutcome(
        levelNumber: Int,
        score: Long,
        starsEarned: Int,
        won: Boolean
    ) {
        val snapshot = (cloudState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val result = repository.recordLevelResult(
                levelNumber = levelNumber,
                score = score,
                starsEarned = starsEarned,
                won = won,
                currentProfile = snapshot.profile,
                existingLevel = snapshot.levelsByNumber[levelNumber],
                allExistingProgress = snapshot.levelsByNumber
            )
            if (result.isFailure) {
                Log.w(TAG, "Failed to persist level $levelNumber result", result.exceptionOrNull())
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }

    companion object {
        private const val TAG = "GameViewModel"
        private const val STOP_TIMEOUT_MILLIS = 5000L
        private const val MAX_PARTICLES = 48
        const val RAINBOW_BOOSTER_COST = 25L
        const val DAILY_REWARD_COINS = 50L
    }
}
