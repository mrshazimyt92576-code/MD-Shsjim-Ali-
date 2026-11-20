package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalLiquidGlassColors

@Composable
fun LiquidGlassBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val transition = rememberInfiniteTransition(label = "liquid_caustics")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caustic_drift"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(glass.backdropBase)
    ) {
        if (glass.isDark) {
            Image(
                painter = painterResource(id = R.drawable.img_liquid_backdrop),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.36f)
            )
        }

        // Animated molten-glass light refraction orbs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cyanCenter = Offset(
                x = size.width * (0.18f + 0.16f * drift),
                y = size.height * (0.12f + 0.08f * drift)
            )
            val goldCenter = Offset(
                x = size.width * (0.85f - 0.15f * drift),
                y = size.height * (0.78f - 0.10f * drift)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.accentCyan.copy(alpha = if (glass.isDark) 0.16f else 0.14f),
                        Color.Transparent
                    ),
                    center = cyanCenter,
                    radius = size.maxDimension * 0.48f
                ),
                radius = size.maxDimension * 0.48f,
                center = cyanCenter
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glass.accentGold.copy(alpha = if (glass.isDark) 0.14f else 0.12f),
                        Color.Transparent
                    ),
                    center = goldCenter,
                    radius = size.maxDimension * 0.44f
                ),
                radius = size.maxDimension * 0.44f,
                center = goldCenter
            )
        }

        content()
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(26.dp),
    strong: Boolean = false,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && onClick != null) 0.975f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_press"
    )

    val fillBrush = remember(glass, strong) {
        val base = if (strong) glass.glassFillStrong else glass.glassFill
        Brush.linearGradient(
            colors = listOf(
                base.copy(alpha = (base.alpha + 0.08f).coerceAtMost(0.95f)),
                base,
                base.copy(alpha = (base.alpha * 0.72f))
            )
        )
    }

    val borderBrush = remember(glass) {
        Brush.linearGradient(
            colors = listOf(
                glass.borderHighlight,
                glass.borderSubtle,
                glass.accentCyan.copy(alpha = 0.25f)
            )
        )
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(fillBrush, shape)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.material3.ripple(color = glass.accentCyan),
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(padding),
        content = content
    )
}

@Composable
fun LiquidGlassCapsuleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accented: Boolean = false,
    height: Dp = 50.dp,
    horizontalPadding: Dp = 20.dp,
    content: @Composable RowScope.() -> Unit
) {
    val glass = LocalLiquidGlassColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "capsule_spring"
    )

    val bgBrush = if (accented) {
        Brush.horizontalGradient(
            colors = listOf(
                glass.accentCyan.copy(alpha = if (enabled) 0.92f else 0.4f),
                glass.accentGold.copy(alpha = if (enabled) 0.85f else 0.35f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                glass.glassFillStrong,
                glass.glassFill
            )
        )
    }

    val borderColor = if (accented) {
        Color.White.copy(alpha = 0.55f)
    } else {
        glass.borderHighlight
    }

    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .defaultMinSize(minHeight = height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            }
            .clip(CircleShape)
            .drawBehind {
                drawRect(brush = bgBrush)
            }
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(color = Color.White),
                onClick = onClick
            ),
        color = Color.Transparent,
        shape = CircleShape
    ) {
        Row(
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
fun DolbyVisionHeaderBadge(
    modifier: Modifier = Modifier,
    showText: Boolean = true
) {
    val glass = LocalLiquidGlassColors.current
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(glass.glassFillStrong)
            .border(1.dp, glass.borderHighlight.copy(alpha = 0.45f), CircleShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_dolby_emblem),
            contentDescription = "Dolby Vision HDR emblem",
            modifier = Modifier.size(width = 22.dp, height = 12.dp),
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(glass.textPrimary)
        )
        if (showText) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "DOLBY VISION",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = glass.accentCyan
            )
        }
    }
}
