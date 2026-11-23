package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.MonetizationOn
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.PlayerProfile
import com.example.ui.components.BubbleShooterLogoCrest
import com.example.ui.components.CastleMeadowBackground
import com.example.ui.components.GlossyGameButton
import com.example.ui.theme.DeepHudIndigo
import com.example.ui.theme.GoldStarDark
import com.example.ui.theme.GoldStarLight
import com.example.ui.theme.GoldStarMid
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

@Composable
fun HomeScreen(
    profile: PlayerProfile,
    dailyRewardAvailable: Boolean,
    showDailyRewardDialog: Boolean,
    statusBanner: String?,
    onPlayClick: () -> Unit,
    onLevelsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDailyGiftClick: () -> Unit,
    onClaimDailyReward: () -> Unit,
    onDismissDailyReward: () -> Unit,
    onDismissBanner: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        CastleMeadowBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top HUD bar: Stars pill, Coins pill, Settings gear
            Row(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stars Earned Pill
                HudStatPill(
                    icon = Icons.Rounded.Star,
                    iconTint = GoldStarMid,
                    value = "${profile.totalStars}",
                    contentDescription = stringResource(R.string.cd_star_icon)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bonus Coins Pill
                    HudStatPill(
                        icon = Icons.Rounded.MonetizationOn,
                        iconTint = GoldStarLight,
                        value = "${profile.bonusCoins}",
                        contentDescription = null
                    )

                    // Top Settings Icon Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF9382FF), Color(0xFF5B45D6))
                                )
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("home_top_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.cd_settings),
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            if (statusBanner != null) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(DeepHudIndigo.copy(alpha = 0.92f))
                        .border(1.5.dp, GoldStarMid, RoundedCornerShape(20.dp))
                        .clickable(onClick = onDismissBanner)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = statusBanner,
                        style = MaterialTheme.typography.labelLarge,
                        color = GoldStarLight
                    )
                }
            }

            // Center Brand Logo + Menu Stack
            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BubbleShooterLogoCrest()

                Spacer(modifier = Modifier.height(22.dp))

                // Big Glossy Green PLAY Button
                GlossyGameButton(
                    text = stringResource(R.string.btn_play),
                    icon = Icons.Rounded.PlayArrow,
                    topColor = PlayGreenTop,
                    midColor = PlayGreenMid,
                    bottomColor = PlayGreenBottom,
                    height = 68.dp,
                    onClick = onPlayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("play_button")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Royal Blue LEVELS Button
                GlossyGameButton(
                    text = stringResource(R.string.btn_levels),
                    icon = Icons.AutoMirrored.Rounded.FormatListBulleted,
                    topColor = LevelsBlueTop,
                    midColor = LevelsBlueMid,
                    bottomColor = LevelsBlueBottom,
                    height = 58.dp,
                    onClick = onLevelsClick,
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .testTag("levels_button")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Vibrant Purple SETTINGS Button
                GlossyGameButton(
                    text = stringResource(R.string.btn_settings),
                    icon = Icons.Rounded.Settings,
                    topColor = SettingsPurpleTop,
                    midColor = SettingsPurpleMid,
                    bottomColor = SettingsPurpleBottom,
                    height = 58.dp,
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .testTag("settings_button")
                )
            }

            // Bottom bar: Best Score Crown Card (left) + Daily Gift Box (right)
            Row(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Best Score Badge
                Row(
                    modifier = Modifier
                        .shadow(8.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(HudPillDark.copy(alpha = 0.90f))
                        .border(2.dp, Color(0xFF818CF8), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.WorkspacePremium,
                        contentDescription = stringResource(R.string.cd_crown_icon),
                        tint = GoldStarMid,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.label_best_score),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White
                        )
                        Text(
                            text = "${profile.highScore}",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = GoldStarLight
                        )
                    }
                }

                // Interactive Daily Gift Box
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .testTag("daily_gift_button")
                        .clickable(onClick = onDailyGiftClick),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(GoldStarLight, GoldStarMid, GoldStarDark)
                                )
                            )
                            .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CardGiftcard,
                            contentDescription = stringResource(R.string.cd_daily_gift),
                            tint = RibbonRedMid,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    if (dailyRewardAvailable) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(RibbonRedMid)
                                .border(1.5.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "1",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        if (showDailyRewardDialog) {
            AlertDialog(
                onDismissRequest = onDismissDailyReward,
                containerColor = DeepHudIndigo,
                titleContentColor = GoldStarLight,
                textContentColor = Color.White,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = GoldStarMid,
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.daily_reward_title),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                text = {
                    Text(
                        text = if (dailyRewardAvailable) {
                            stringResource(R.string.daily_reward_ready)
                        } else {
                            stringResource(R.string.daily_reward_claimed)
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                confirmButton = {
                    if (dailyRewardAvailable) {
                        Button(
                            onClick = onClaimDailyReward,
                            colors = ButtonDefaults.buttonColors(containerColor = PlayGreenMid),
                            modifier = Modifier.testTag("claim_daily_reward_button")
                        ) {
                            Text(
                                text = stringResource(R.string.btn_claim_reward),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    } else {
                        TextButton(onClick = onDismissDailyReward) {
                            Text(
                                text = stringResource(R.string.btn_close),
                                color = GoldStarLight
                            )
                        }
                    }
                },
                dismissButton = {
                    if (dailyRewardAvailable) {
                        TextButton(onClick = onDismissDailyReward) {
                            Text(text = stringResource(R.string.btn_close), color = Color.White)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun HudStatPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    value: String,
    contentDescription: String?
) {
    Row(
        modifier = Modifier
            .shadow(6.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(HudPillDark.copy(alpha = 0.88f))
            .border(1.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            ),
            color = Color.White
        )
    }
}
