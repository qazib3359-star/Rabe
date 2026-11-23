package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PlayerProfile
import com.example.ui.theme.DeepHudIndigo
import com.example.ui.theme.GoldStarLight
import com.example.ui.theme.GoldStarMid
import com.example.ui.theme.HudBarPurple
import com.example.ui.theme.PlayGreenMid
import com.example.ui.theme.RibbonRedMid

@Composable
fun SettingsScreen(
    profile: PlayerProfile,
    userEmail: String,
    onUpdateSettings: (soundEnabled: Boolean, hapticsEnabled: Boolean, aimGuideExtended: Boolean) -> Unit,
    onSignOutClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1E3A8A), DeepHudIndigo, Color(0xFF0F172A))
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("settings_back_button")
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
                    text = stringResource(R.string.settings_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Audio & Gameplay Preferences Card
                SettingsCard {
                    SettingToggleRow(
                        icon = Icons.Rounded.VolumeUp,
                        title = stringResource(R.string.settings_sound),
                        subtitle = stringResource(R.string.settings_sound_desc),
                        checked = profile.soundEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateSettings(enabled, profile.hapticsEnabled, profile.aimGuideExtended)
                        },
                        testTag = "settings_sound_switch"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SettingToggleRow(
                        icon = Icons.Rounded.Vibration,
                        title = stringResource(R.string.settings_haptics),
                        subtitle = stringResource(R.string.settings_haptics_desc),
                        checked = profile.hapticsEnabled,
                        onCheckedChange = { enabled ->
                            onUpdateSettings(profile.soundEnabled, enabled, profile.aimGuideExtended)
                        },
                        testTag = "settings_haptics_switch"
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SettingToggleRow(
                        icon = Icons.Rounded.GpsFixed,
                        title = stringResource(R.string.settings_aim_guide),
                        subtitle = stringResource(R.string.settings_aim_guide_desc),
                        checked = profile.aimGuideExtended,
                        onCheckedChange = { enabled ->
                            onUpdateSettings(profile.soundEnabled, profile.hapticsEnabled, enabled)
                        },
                        testTag = "settings_aim_guide_switch"
                    )
                }

                // How to Play Card
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.HelpOutline,
                            contentDescription = null,
                            tint = GoldStarMid,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "How to Play",
                            style = MaterialTheme.typography.titleLarge,
                            color = GoldStarLight
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.settings_how_to_play),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFFE0E7FF)
                    )
                }

                // Cloud Account Card
                SettingsCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AccountCircle,
                            contentDescription = null,
                            tint = Color(0xFF93C5FD),
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_account),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF93C5FD)
                            )
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            if (userEmail.isNotBlank()) {
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFC7D2FE)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onSignOutClick,
                        colors = ButtonDefaults.buttonColors(containerColor = RibbonRedMid),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("settings_sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_sign_out),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .border(1.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        content()
    }
}

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = GoldStarLight,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFC7D2FE)
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PlayGreenMid,
                uncheckedThumbColor = Color(0xFFCBD5E1),
                uncheckedTrackColor = Color(0xFF334155)
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
