package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Compare
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.localization.LocalStrings
import com.example.ui.components.DolbyVisionHeaderBadge
import com.example.ui.components.LiquidGlassBackdrop
import com.example.ui.components.LiquidGlassCapsuleButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.LocalLiquidGlassColors

private data class OnboardingPageData(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val accentCyan: Boolean
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val strings = LocalStrings.current
    val glass = LocalLiquidGlassColors.current
    var page by remember { mutableIntStateOf(0) }

    val pages = listOf(
        OnboardingPageData(
            icon = Icons.Rounded.AutoAwesome,
            title = strings.onboardingTitle1,
            description = strings.onboardingDesc1,
            accentCyan = true
        ),
        OnboardingPageData(
            icon = Icons.Rounded.Compare,
            title = strings.onboardingTitle2,
            description = strings.onboardingDesc2,
            accentCyan = false
        ),
        OnboardingPageData(
            icon = Icons.Rounded.Verified,
            title = strings.onboardingTitle3,
            description = strings.onboardingDesc3,
            accentCyan = true
        )
    )

    LiquidGlassBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DolbyVisionHeaderBadge()
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text(
                        text = strings.skip,
                        style = MaterialTheme.typography.labelLarge,
                        color = glass.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    (slideInHorizontally(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) { it / 2 } + fadeIn())
                        .togetherWith(slideOutHorizontally { -it / 2 } + fadeOut())
                },
                label = "onboarding_page"
            ) { idx ->
                val item = pages[idx]
                val tint = if (item.accentCyan) glass.accentCyan else glass.accentGold
                LiquidGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp),
                    strong = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape)
                                .background(tint.copy(alpha = 0.16f))
                                .border(1.5.dp, tint.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = tint,
                                modifier = Modifier.size(38.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = glass.textPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = glass.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Capsule progress indicators
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { index ->
                    val active = index == page
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (active) 28.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (active) glass.accentCyan else glass.glassFillStrong)
                            .clickable { page = index }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            LiquidGlassCapsuleButton(
                onClick = {
                    if (page < pages.lastIndex) {
                        page += 1
                    } else {
                        onComplete()
                    }
                },
                accented = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .testTag("onboarding_next_button")
            ) {
                Text(
                    text = if (page < pages.lastIndex) strings.next else strings.getStarted,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF050912)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF050912),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
