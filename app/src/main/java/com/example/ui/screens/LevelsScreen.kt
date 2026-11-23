package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.LevelProgress
import com.example.data.PlayerProfile
import com.example.game.LevelGenerator
import com.example.ui.theme.DeepHudIndigo
import com.example.ui.theme.GoldStarLight
import com.example.ui.theme.GoldStarMid
import com.example.ui.theme.HudBarPurple
import com.example.ui.theme.LevelsBlueBottom
import com.example.ui.theme.LevelsBlueMid
import com.example.ui.theme.LevelsBlueTop
import com.example.ui.theme.PlayGreenMid
import com.example.ui.theme.PlayGreenTop

@Composable
fun LevelsScreen(
    profile: PlayerProfile,
    levelsByNumber: Map<Int, LevelProgress>,
    onLevelSelected: (Int) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val highestUnlocked = profile.highestUnlockedLevel.toInt().coerceIn(1, LevelGenerator.TOTAL_LEVELS)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF153B8B), Color(0xFF1E1B5E), Color(0xFF0F172A))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HudBarPurple.copy(alpha = 0.92f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("levels_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.btn_levels),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }

                // Total Stars Pill
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(DeepHudIndigo)
                        .border(1.5.dp, GoldStarMid.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = stringResource(R.string.cd_star_icon),
                        tint = GoldStarMid,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.label_stars_progress, profile.totalStars.toInt(), 300),
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldStarLight
                    )
                }
            }

            // 100-level world grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 68.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .testTag("levels_grid")
            ) {
                for (world in LevelGenerator.worlds) {
                    item(
                        key = "world_header_${world.index}",
                        span = { GridItemSpan(maxLineSpan) }
                    ) {
                        WorldSectionBanner(
                            worldIndex = world.index,
                            title = world.name,
                            subtitle = world.subtitle
                        )
                    }

                    for (levelNum in world.startLevel..world.endLevel) {
                        item(key = "level_$levelNum") {
                            val progress = levelsByNumber[levelNum]
                            val unlocked = levelNum <= highestUnlocked
                            val isCurrent = levelNum == highestUnlocked
                            val stars = progress?.stars?.toInt()?.coerceIn(0, 3) ?: 0

                            LevelTile(
                                levelNumber = levelNum,
                                stars = stars,
                                unlocked = unlocked,
                                isCurrent = isCurrent,
                                onClick = {
                                    if (unlocked) onLevelSelected(levelNum)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldSectionBanner(
    worldIndex: Int,
    title: String,
    subtitle: String
) {
    val gradientColors = when (worldIndex % 5) {
        0 -> listOf(Color(0xFF16A34A), Color(0xFF15803D))
        1 -> listOf(Color(0xFF0284C7), Color(0xFF1D4ED8))
        2 -> listOf(Color(0xFFEA580C), Color(0xFFBE185D))
        3 -> listOf(Color(0xFF0D9488), Color(0xFF312E81))
        else -> listOf(Color(0xFF9333EA), Color(0xFF4C1D95))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = if (worldIndex == 0) 0.dp else 8.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "World ${worldIndex + 1}: $title",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun LevelTile(
    levelNumber: Int,
    stars: Int,
    unlocked: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val bgBrush = when {
        isCurrent -> Brush.verticalGradient(listOf(PlayGreenTop, PlayGreenMid))
        unlocked -> Brush.verticalGradient(listOf(LevelsBlueTop, LevelsBlueMid))
        else -> Brush.verticalGradient(listOf(Color(0xFF334155), Color(0xFF1E293B)))
    }
    val borderColor = when {
        isCurrent -> GoldStarLight
        unlocked -> Color.White.copy(alpha = 0.55f)
        else -> Color.White.copy(alpha = 0.16f)
    }

    Column(
        modifier = Modifier
            .aspectRatio(0.92f)
            .shadow(if (unlocked) 6.dp else 2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(if (unlocked) LevelsBlueBottom else Color(0xFF0F172A))
            .padding(bottom = 3.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(bgBrush)
            .border(if (isCurrent) 2.5.dp else 1.5.dp, borderColor, RoundedCornerShape(15.dp))
            .clickable(enabled = unlocked, onClick = onClick)
            .testTag("level_tile_$levelNumber"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (unlocked) {
            Text(
                text = "$levelNumber",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                ),
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                for (i in 1..3) {
                    Icon(
                        imageVector = if (i <= stars) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = null,
                        tint = if (i <= stars) GoldStarLight else Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        } else {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$levelNumber",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF94A3B8)
            )
        }
    }
}
