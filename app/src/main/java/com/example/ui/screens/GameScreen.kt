package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PlayerProfile
import com.example.game.BubbleColor
import com.example.game.BubbleGridEngine
import com.example.game.LevelGenerator
import com.example.ui.components.GlossyGameButton
import com.example.ui.components.WorldGameplayBackground
import com.example.ui.components.drawBubbleCannon
import com.example.ui.components.drawGlossyBubble
import com.example.ui.theme.DeepHudIndigo
import com.example.ui.theme.GoldStarLight
import com.example.ui.theme.GoldStarMid
import com.example.ui.theme.HudBarPurple
import com.example.ui.theme.HudPillDark
import com.example.ui.theme.LevelsBlueBottom
import com.example.ui.theme.LevelsBlueMid
import com.example.ui.theme.LevelsBlueTop
import com.example.ui.theme.PlayGreenBottom
import com.example.ui.theme.PlayGreenMid
import com.example.ui.theme.PlayGreenTop
import com.example.ui.theme.RibbonRedMid
import com.example.ui.theme.SettingsPurpleBottom
import com.example.ui.theme.SettingsPurpleMid
import com.example.ui.theme.SettingsPurpleTop
import com.example.viewmodel.ActiveGameState
import com.example.viewmodel.GameStatus
import com.example.viewmodel.GameViewModel
import kotlin.math.roundToInt

@Composable
fun GameScreen(
    gameState: ActiveGameState,
    profile: PlayerProfile,
    statusBanner: String?,
    onAimUpdate: (cannonPos: Offset, touchPos: Offset) -> Unit,
    onFire: (cannonPos: Offset) -> Unit,
    onCancelAim: () -> Unit,
    onSwapBubbles: () -> Unit,
    onActivateRainbow: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onRestartClick: () -> Unit,
    onNextLevelClick: () -> Unit,
    onLevelsClick: () -> Unit,
    onHomeClick: () -> Unit,
    onToggleSound: () -> Unit,
    onDismissBanner: () -> Unit,
    onTickFrame: (dtSeconds: Float, boardHeightRatio: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        when (gameState.status) {
            GameStatus.PLAYING -> onPauseClick()
            GameStatus.PAUSED -> onResumeClick()
            else -> onLevelsClick()
        }
    }

    var boardHeightRatio by remember { mutableFloatStateOf(1.48f) }

    // Smooth 60fps Compose frame loop
    LaunchedEffect(gameState.status) {
        var lastFrameNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastFrameNanos != 0L) {
                    val dt = (frameNanos - lastFrameNanos) / 1_000_000_000f
                    onTickFrame(dt, boardHeightRatio)
                }
                lastFrameNanos = frameNanos
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // World-themed scenic backdrop
        WorldGameplayBackground(worldIndex = gameState.level.world.index)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top glossy HUD Bar (Level badge, Score + Star bar, Restart + Pause)
            GameTopHudBar(
                levelNumber = gameState.level.levelNumber,
                score = gameState.score,
                threeStarTarget = gameState.level.threeStarScore,
                onRestartClick = onRestartClick,
                onPauseClick = onPauseClick
            )

            // Main Playfield Container (fluid width, centered on tablets/foldables)
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 500.dp)
                    .fillMaxWidth()
            ) {
                val density = LocalDensity.current
                val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
                val heightPx = with(density) { maxHeight.toPx() }.coerceAtLeast(1f)
                val heightRatio = heightPx / widthPx
                boardHeightRatio = heightRatio

                val bubbleRadiusPx = widthPx * BubbleGridEngine.BUBBLE_RADIUS
                // Cannon positioned cleanly above the bottom controls
                val cannonYRatio = (heightRatio - 0.135f).coerceAtLeast(BubbleGridEngine.ROW_HEIGHT * 12.2f)
                val cannonNorm = Offset(0.5f, cannonYRatio)

                val aimDots = remember(
                    gameState.grid,
                    gameState.aimAngleDeg,
                    gameState.isAiming,
                    gameState.projectile,
                    profile.aimGuideExtended,
                    cannonNorm
                ) {
                    if (gameState.status == GameStatus.PLAYING && gameState.projectile == null) {
                        BubbleGridEngine.computeAimTrajectory(
                            grid = gameState.grid,
                            cannonPos = cannonNorm,
                            aimAngleDeg = gameState.aimAngleDeg,
                            extendedBounce = profile.aimGuideExtended
                        )
                    } else {
                        emptyList()
                    }
                }

                // Interactive Bubble Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("game_playfield_canvas")
                        .pointerInput(cannonNorm, widthPx, gameState.status) {
                            awaitEachGesture {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                // Ignore taps directly on bottom HUD corners (handled by buttons)
                                val downYRatio = down.position.y / widthPx
                                val downXRatio = down.position.x / widthPx
                                val inBottomCorner = downYRatio > cannonYRatio - 0.08f &&
                                    (downXRatio < 0.28f || downXRatio > 0.72f)
                                if (inBottomCorner) return@awaitEachGesture

                                onAimUpdate(cannonNorm, Offset(downXRatio, downYRatio))
                                var lastTouch = Offset(downXRatio, downYRatio)
                                var releasedCleanly = false

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    if (!change.pressed) {
                                        releasedCleanly = true
                                        break
                                    }
                                    lastTouch = Offset(change.position.x / widthPx, change.position.y / widthPx)
                                    onAimUpdate(cannonNorm, lastTouch)
                                }

                                if (releasedCleanly) {
                                    onAimUpdate(cannonNorm, lastTouch)
                                    onFire(cannonNorm)
                                } else {
                                    onCancelAim()
                                }
                            }
                        }
                ) {
                    val scale = size.width

                    // Danger line indicator at DANGER_ROW
                    val dangerY = (BubbleGridEngine.BUBBLE_RADIUS + LevelGenerator.DANGER_ROW * BubbleGridEngine.ROW_HEIGHT) * scale
                    val hasLowBubbles = gameState.grid
                        .drop(LevelGenerator.DANGER_ROW - 3)
                        .any { row -> row.any { it != null } }
                    drawLine(
                        color = if (hasLowBubbles) Color(0xFFFF4D6D).copy(alpha = 0.75f) else Color.White.copy(alpha = 0.22f),
                        start = Offset(12f, dangerY),
                        end = Offset(size.width - 12f, dangerY),
                        strokeWidth = if (hasLowBubbles) 3.5f else 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))
                    )

                    // Draw hexagonal grid bubbles
                    for (r in gameState.grid.indices) {
                        val row = gameState.grid[r]
                        for (c in row.indices) {
                            val bubbleColor = row[c] ?: continue
                            val normCenter = BubbleGridEngine.cellCenter(r, c)
                            drawGlossyBubble(
                                center = Offset(normCenter.x * scale, normCenter.y * scale),
                                radius = bubbleRadiusPx,
                                color = bubbleColor
                            )
                        }
                    }

                    // Draw aiming trajectory dots (color-matched to loaded bubble)
                    if (aimDots.isNotEmpty() && gameState.status == GameStatus.PLAYING) {
                        val dotColor = if (gameState.currentBubble == BubbleColor.RAINBOW) {
                            GoldStarLight
                        } else {
                            gameState.currentBubble.baseColor
                        }
                        val dotRadius = bubbleRadiusPx * 0.24f
                        for (idx in aimDots.indices) {
                            val pt = aimDots[idx]
                            val alpha = (1f - (idx.toFloat() / (aimDots.size + 4))).coerceIn(0.35f, 1f)
                            val pxCenter = Offset(pt.x * scale, pt.y * scale)
                            // White glow outline
                            drawCircle(
                                color = Color.White.copy(alpha = 0.92f * alpha),
                                radius = dotRadius * 1.38f,
                                center = pxCenter
                            )
                            // Vivid core dot
                            drawCircle(
                                color = dotColor.copy(alpha = alpha),
                                radius = dotRadius,
                                center = pxCenter
                            )
                        }
                    }

                    // Draw pop burst rings
                    for (pop in gameState.popEffects) {
                        val cx = pop.x * scale
                        val cy = pop.y * scale
                        val ringRadius = bubbleRadiusPx * (0.8f + pop.progress * 1.15f)
                        val ringAlpha = (1f - pop.progress).coerceIn(0f, 1f)
                        drawCircle(
                            color = pop.color.highlightColor.copy(alpha = ringAlpha * 0.85f),
                            radius = ringRadius,
                            center = Offset(cx, cy),
                            style = Stroke(width = bubbleRadiusPx * 0.22f * ringAlpha)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = ringAlpha * 0.45f),
                            radius = ringRadius * 0.6f,
                            center = Offset(cx, cy)
                        )
                    }

                    // Draw sparkle particles
                    for (pt in gameState.particles) {
                        drawCircle(
                            color = pt.color.highlightColor.copy(alpha = pt.life.coerceIn(0f, 1f)),
                            radius = pt.radius * scale,
                            center = Offset(pt.x * scale, pt.y * scale)
                        )
                    }

                    // Draw falling disconnected bubbles
                    for (fb in gameState.fallingBubbles) {
                        drawGlossyBubble(
                            center = Offset(fb.x * scale, fb.y * scale),
                            radius = bubbleRadiusPx * 0.92f,
                            color = fb.color
                        )
                    }

                    // Draw flying active projectile
                    val proj = gameState.projectile
                    if (proj != null) {
                        drawGlossyBubble(
                            center = Offset(proj.x * scale, proj.y * scale),
                            radius = bubbleRadiusPx,
                            color = proj.color
                        )
                    }

                    // Draw ornate purple & gold shooter cannon at bottom center
                    drawBubbleCannon(
                        center = Offset(cannonNorm.x * scale, cannonNorm.y * scale),
                        aimAngleDeg = gameState.aimAngleDeg,
                        loadedBubble = gameState.currentBubble,
                        bubbleRadius = bubbleRadiusPx
                    )
                }

                // Floating score popups
                for (popup in gameState.scorePopups) {
                    val pxX = (popup.x * widthPx).roundToInt()
                    val pxY = (popup.y * widthPx).roundToInt()
                    Text(
                        text = popup.text,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = if (popup.isCombo) 22.sp else 18.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = if (popup.isCombo) GoldStarLight else Color.White,
                        modifier = Modifier.offset { IntOffset(pxX - 60, pxY - 24) }
                    )
                }

                // Optional short status toast banner (e.g. booster feedback)
                if (statusBanner != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(DeepHudIndigo.copy(alpha = 0.92f))
                            .border(1.5.dp, GoldStarMid, RoundedCornerShape(18.dp))
                            .clickable(onClick = onDismissBanner)
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = statusBanner,
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldStarLight
                        )
                    }
                }

                // Bottom-left: Circular Swap Bubble button (shows Next Bubble)
                SwapBubbleControl(
                    nextBubble = gameState.nextBubble,
                    onClick = onSwapBubbles,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 14.dp)
                )

                // Bottom-right: Moves Counter Badge + Rainbow Booster trigger
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Rainbow Wildcard Booster Pill
                    val canAffordRainbow = profile.bonusCoins >= GameViewModel.RAINBOW_BOOSTER_COST &&
                        gameState.currentBubble != BubbleColor.RAINBOW
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (canAffordRainbow) {
                                    Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)))
                                } else {
                                    Brush.horizontalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
                                }
                            )
                            .border(
                                1.5.dp,
                                if (canAffordRainbow) GoldStarLight else Color.White.copy(alpha = 0.25f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable(onClick = onActivateRainbow)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("rainbow_booster_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = stringResource(R.string.cd_rainbow_booster),
                            tint = GoldStarLight,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${GameViewModel.RAINBOW_BOOSTER_COST}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                    }

                    // Moves Remaining Badge (matching mockup "17 Moves")
                    val lowMoves = gameState.movesRemaining <= 5
                    Column(
                        modifier = Modifier
                            .shadow(8.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                if (lowMoves) Color(0xFF7F1D1D).copy(alpha = 0.94f)
                                else HudPillDark.copy(alpha = 0.92f)
                            )
                            .border(
                                2.dp,
                                if (lowMoves) Color(0xFFFCA5A5) else Color(0xFF818CF8),
                                RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("moves_counter_badge"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${gameState.movesRemaining}",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = if (lowMoves) GoldStarLight else Color.White
                        )
                        Text(
                            text = stringResource(R.string.label_moves),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFE0E7FF)
                        )
                    }
                }
            }
        }

        // Modal Overlays: Pause, Victory, Game Over
        when (gameState.status) {
            GameStatus.PAUSED -> {
                PauseOverlay(
                    levelNumber = gameState.level.levelNumber,
                    soundEnabled = profile.soundEnabled,
                    onResume = onResumeClick,
                    onRestart = onRestartClick,
                    onToggleSound = onToggleSound,
                    onLevels = onLevelsClick,
                    onHome = onHomeClick
                )
            }
            GameStatus.WON -> {
                VictoryOverlay(
                    gameState = gameState,
                    onNextLevel = onNextLevelClick,
                    onRestart = onRestartClick,
                    onLevels = onLevelsClick
                )
            }
            GameStatus.LOST_OUT_OF_MOVES, GameStatus.LOST_DANGER_LINE -> {
                GameOverOverlay(
                    isDangerLine = gameState.status == GameStatus.LOST_DANGER_LINE,
                    score = gameState.score,
                    onRestart = onRestartClick,
                    onLevels = onLevelsClick
                )
            }
            GameStatus.PLAYING -> Unit
        }
    }
}

@Composable
private fun GameTopHudBar(
    levelNumber: Int,
    score: Long,
    threeStarTarget: Long,
    onRestartClick: () -> Unit,
    onPauseClick: () -> Unit
) {
    val progress = (score.toFloat() / threeStarTarget.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DeepHudIndigo, HudBarPurple.copy(alpha = 0.94f))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left: Level Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF7C3AED), Color(0xFF4C1D95))
                    )
                )
                .border(1.5.dp, Color(0xFFC4B5FD), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("hud_level_badge")
        ) {
            Text(
                text = stringResource(R.string.label_level_format, levelNumber),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White
            )
        }

        // Center: Score + 3-Star Progress Bar
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${stringResource(R.string.label_score)} $score",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                ),
                color = Color.White,
                modifier = Modifier.testTag("hud_score_text")
            )
            Spacer(modifier = Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = GoldStarMid,
                trackColor = Color.White.copy(alpha = 0.22f)
            )
        }

        // Right: Restart + Pause Buttons
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF4C1D95))
                    .border(1.5.dp, Color(0xFFC4B5FD), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onRestartClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("game_restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.btn_restart),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF9382FF), Color(0xFF5B45D6))
                        )
                    )
                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("game_pause_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pause,
                        contentDescription = stringResource(R.string.cd_pause),
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SwapBubbleControl(
    nextBubble: BubbleColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(72.dp)
            .shadow(8.dp, CircleShape)
            .clip(CircleShape)
            .background(HudPillDark.copy(alpha = 0.86f))
            .border(2.5.dp, Color(0xFFC4B5FD), CircleShape)
            .clickable(onClick = onClick)
            .testTag("swap_bubble_button"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(44.dp)) {
            drawGlossyBubble(
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.minDimension * 0.44f,
                color = nextBubble
            )
        }
        Icon(
            imageVector = Icons.Rounded.SwapHoriz,
            contentDescription = stringResource(R.string.cd_swap_bubble),
            tint = Color.White.copy(alpha = 0.92f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(Color(0xFF4C1D95))
                .padding(2.dp)
        )
    }
}

@Composable
private fun PauseOverlay(
    levelNumber: Int,
    soundEnabled: Boolean,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onToggleSound: () -> Unit,
    onLevels: () -> Unit,
    onHome: () -> Unit
) {
    OverlayBackdrop {
        Text(
            text = stringResource(R.string.dialog_paused),
            style = MaterialTheme.typography.headlineLarge,
            color = GoldStarLight
        )
        Text(
            text = stringResource(R.string.label_level_format, levelNumber),
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFFE0E7FF)
        )
        Spacer(modifier = Modifier.height(18.dp))

        GlossyGameButton(
            text = stringResource(R.string.btn_resume),
            icon = Icons.Rounded.PlayArrow,
            topColor = PlayGreenTop,
            midColor = PlayGreenMid,
            bottomColor = PlayGreenBottom,
            onClick = onResume,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pause_resume_button")
        )
        Spacer(modifier = Modifier.height(12.dp))
        GlossyGameButton(
            text = stringResource(R.string.btn_restart),
            icon = Icons.Rounded.Refresh,
            topColor = LevelsBlueTop,
            midColor = LevelsBlueMid,
            bottomColor = LevelsBlueBottom,
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pause_restart_button")
        )
        Spacer(modifier = Modifier.height(12.dp))
        GlossyGameButton(
            text = if (soundEnabled) "Sound: ON" else "Sound: OFF",
            icon = if (soundEnabled) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeOff,
            topColor = SettingsPurpleTop,
            midColor = SettingsPurpleMid,
            bottomColor = SettingsPurpleBottom,
            onClick = onToggleSound,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pause_sound_toggle_button")
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallOverlayActionButton(
                text = stringResource(R.string.btn_levels),
                icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                onClick = onLevels,
                modifier = Modifier
                    .weight(1f)
                    .testTag("pause_levels_button")
            )
            SmallOverlayActionButton(
                text = stringResource(R.string.btn_home),
                icon = Icons.Rounded.Home,
                onClick = onHome,
                modifier = Modifier
                    .weight(1f)
                    .testTag("pause_home_button")
            )
        }
    }
}

@Composable
private fun VictoryOverlay(
    gameState: ActiveGameState,
    onNextLevel: () -> Unit,
    onRestart: () -> Unit,
    onLevels: () -> Unit
) {
    val isFinalLevel = gameState.level.levelNumber >= LevelGenerator.TOTAL_LEVELS

    OverlayBackdrop {
        Text(
            text = if (isFinalLevel) stringResource(R.string.dialog_all_cleared) else stringResource(R.string.dialog_victory),
            style = MaterialTheme.typography.headlineLarge,
            color = GoldStarLight,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(10.dp))

        // Animated 3-Star Row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 1..3) {
                val earned = i <= gameState.starsEarned
                Icon(
                    imageVector = if (earned) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = null,
                    tint = if (earned) GoldStarMid else Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(if (i == 2) 54.dp else 44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "${stringResource(R.string.label_score)} ${gameState.score}",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        if (gameState.moveBonus > 0L) {
            Text(
                text = "${stringResource(R.string.label_move_bonus)}: +${gameState.moveBonus}",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF86EFAC)
            )
        }
        if (gameState.isNewHighScore) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.label_new_high_score),
                style = MaterialTheme.typography.labelLarge,
                color = GoldStarLight
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!isFinalLevel) {
            GlossyGameButton(
                text = stringResource(R.string.btn_next_level),
                icon = Icons.Rounded.PlayArrow,
                topColor = PlayGreenTop,
                midColor = PlayGreenMid,
                bottomColor = PlayGreenBottom,
                onClick = onNextLevel,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("victory_next_level_button")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmallOverlayActionButton(
                text = stringResource(R.string.btn_restart),
                icon = Icons.Rounded.Refresh,
                onClick = onRestart,
                modifier = Modifier
                    .weight(1f)
                    .testTag("victory_restart_button")
            )
            SmallOverlayActionButton(
                text = stringResource(R.string.btn_levels),
                icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                onClick = onLevels,
                modifier = Modifier
                    .weight(1f)
                    .testTag("victory_levels_button")
            )
        }
    }
}

@Composable
private fun GameOverOverlay(
    isDangerLine: Boolean,
    score: Long,
    onRestart: () -> Unit,
    onLevels: () -> Unit
) {
    OverlayBackdrop {
        Text(
            text = if (isDangerLine) {
                stringResource(R.string.dialog_danger_over)
            } else {
                stringResource(R.string.dialog_game_over)
            },
            style = MaterialTheme.typography.headlineLarge,
            color = Color(0xFFFCA5A5),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${stringResource(R.string.label_score)} $score",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(20.dp))

        GlossyGameButton(
            text = stringResource(R.string.btn_restart),
            icon = Icons.Rounded.Refresh,
            topColor = PlayGreenTop,
            midColor = PlayGreenMid,
            bottomColor = PlayGreenBottom,
            onClick = onRestart,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gameover_restart_button")
        )
        Spacer(modifier = Modifier.height(12.dp))
        SmallOverlayActionButton(
            text = stringResource(R.string.btn_levels),
            icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
            onClick = onLevels,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gameover_levels_button")
        )
    }
}

@Composable
private fun OverlayBackdrop(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .shadow(20.dp, RoundedCornerShape(28.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF312E81), DeepHudIndigo)
                    )
                )
                .border(2.5.dp, Color(0xFFC4B5FD), RoundedCornerShape(28.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}

@Composable
private fun SmallOverlayActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.14f))
            .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = Color.White
        )
    }
}
