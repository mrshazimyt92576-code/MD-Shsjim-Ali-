package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.model.ThemeMode
import com.example.localization.AppLanguage
import com.example.localization.LocalStrings
import com.example.ui.components.DolbyVisionHeaderBadge
import com.example.ui.components.LiquidGlassBackdrop
import com.example.ui.components.LiquidGlassCapsuleButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.LocalLiquidGlassColors

private object SocialLinks {
    const val FACEBOOK = "https://www.facebook.com/share/1GrySiPu4e/"
    const val TIKTOK = "https://www.tiktok.com/@shajim92f_1k?_r=1&_t=ZS-9BZDiGTMxlP"
    const val TELEGRAM = "https://t.me/mrshazimff99"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    selectedLanguage: AppLanguage,
    selectedThemeMode: ThemeMode,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onReplayOnboarding: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalStrings.current
    val glass = LocalLiquidGlassColors.current

    fun openExternalSocial(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    LiquidGlassBackdrop {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                LiquidGlassCard(
                    shape = CircleShape,
                    padding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("settings_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = glass.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.settingsTitle,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = glass.textPrimary
                        )
                    }
                }

                // Instant Language Switcher
                LiquidGlassCard(shape = RoundedCornerShape(26.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = strings.languageSection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = glass.textPrimary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                val active = selectedLanguage == lang
                                LiquidGlassCapsuleButton(
                                    onClick = { onSelectLanguage(lang) },
                                    accented = active,
                                    height = 40.dp,
                                    horizontalPadding = 14.dp,
                                    modifier = Modifier.testTag("lang_${lang.code}")
                                ) {
                                    Text(
                                        text = lang.nativeLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (active) Color(0xFF050912) else glass.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Appearance / Dark Mode Selector
                LiquidGlassCard(shape = RoundedCornerShape(26.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = strings.themeSection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = glass.textPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                ThemeMode.OBSIDIAN to strings.themeObsidian,
                                ThemeMode.MIDNIGHT to strings.themeMidnight,
                                ThemeMode.SYSTEM to strings.themeSystem
                            ).forEach { (mode, label) ->
                                val active = selectedThemeMode == mode
                                LiquidGlassCapsuleButton(
                                    onClick = { onSelectThemeMode(mode) },
                                    accented = active,
                                    height = 42.dp,
                                    horizontalPadding = 12.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("theme_${mode.key}")
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (active) Color(0xFF050912) else glass.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }

                // Social Media Section — Official Icons Only, No Visible Links
                LiquidGlassCard(shape = RoundedCornerShape(26.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = strings.socialMediaSection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = glass.textPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SocialButtonCard(
                                iconRes = R.drawable.ic_social_facebook,
                                label = strings.socialFacebook,
                                onClick = { openExternalSocial(SocialLinks.FACEBOOK) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("social_facebook")
                            )
                            SocialButtonCard(
                                iconRes = R.drawable.ic_social_tiktok,
                                label = strings.socialTikTok,
                                onClick = { openExternalSocial(SocialLinks.TIKTOK) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("social_tiktok")
                            )
                            SocialButtonCard(
                                iconRes = R.drawable.ic_social_telegram,
                                label = strings.socialTelegram,
                                onClick = { openExternalSocial(SocialLinks.TELEGRAM) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("social_telegram")
                            )
                        }
                    }
                }

                // Studio Workspace & Tour (No Sign-In required)
                LiquidGlassCard(shape = RoundedCornerShape(26.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = strings.studioWorkspaceSection,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = glass.textPrimary
                        )
                        Text(
                            text = strings.localWorkspaceDesc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.textSecondary
                        )
                        LiquidGlassCapsuleButton(
                            onClick = onReplayOnboarding,
                            height = 44.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("replay_onboarding_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = strings.replayOnboarding,
                                tint = glass.accentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.replayOnboarding,
                                style = MaterialTheme.typography.labelLarge,
                                color = glass.textPrimary
                            )
                        }
                    }
                }

                // About Badge
                LiquidGlassCard(shape = RoundedCornerShape(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = strings.aboutSection,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = glass.textPrimary
                            )
                            Text(
                                text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.accentCyan
                            )
                        }
                        DolbyVisionHeaderBadge(showText = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialButtonCard(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalLiquidGlassColors.current
    LiquidGlassCard(
        onClick = onClick,
        strong = true,
        shape = RoundedCornerShape(22.dp),
        padding = PaddingValues(vertical = 14.dp, horizontal = 8.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = glass.textPrimary
            )
        }
    }
}
