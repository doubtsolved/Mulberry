package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.UserProfile
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily

/**
 * Universal Material 3 Expressive Mulberry Logo.
 * Displays the exact violet/orchid leaf-and-book organic emblem directly derived
 * from material_3_expressive_logo.svg.
 */
@Composable
fun MulberryExpressiveLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    showContainer: Boolean = true,
    animated: Boolean = false
) {
    val scale = remember { Animatable(if (animated) 0.65f else 1f) }
    val glowAlpha = remember { Animatable(if (animated) 0f else 0.4f) }

    if (animated) {
        LaunchedEffect(Unit) {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
        }
        LaunchedEffect(Unit) {
            glowAlpha.animateTo(
                targetValue = 0.55f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    val glowBrush = Brush.radialGradient(
        colors = listOf(
            Color(0xFF9045FF).copy(alpha = glowAlpha.value * 0.4f),
            Color.Transparent
        )
    )

    Box(
        modifier = modifier
            .scale(scale.value)
            .size(size)
            .drawBehind {
                if (showContainer) {
                    drawCircle(brush = glowBrush, radius = this.size.maxDimension * 0.72f)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val drawableRes = if (showContainer) {
            R.drawable.ic_mulberry_logo
        } else {
            R.drawable.ic_mulberry_mark
        }

        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = "Mulberry Expressive Logo",
            modifier = Modifier.matchParentSize()
        )
    }
}

/**
 * Tier-outlined profile avatar:
 * - CORE_VIP (Core Architect): Golden outline
 * - AMBASSADOR (Ambassador): Silver outline
 * - STANDARD (Normal student): No outline
 */
@Composable
fun MulberryProfileAvatar(
    userProfile: UserProfile,
    onClick: (() -> Unit)? = null,
    size: Dp = 38.dp,
    cornerRadius: Dp = 12.dp,
    fontSize: TextUnit = 14.sp,
    avatarTestTag: String? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val shape = RoundedCornerShape(cornerRadius)

    val outlineBrush: Brush? = when (userProfile.vipTier.uppercase()) {
        "CORE_VIP" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFFFD700),
                Color(0xFFF59E0B),
                Color(0xFFFFE55C)
            )
        )
        "AMBASSADOR" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFE2E8F0),
                Color(0xFF94A3B8),
                Color(0xFFF1F5F9)
            )
        )
        else -> null
    }

    var boxModifier = modifier
        .size(size)
        .then(if (onClick != null) Modifier.fluidGlassPressEffect(onClick = onClick) else Modifier)
        .clip(shape)
        .background(colors.primary.copy(alpha = 0.14f))

    if (outlineBrush != null) {
        boxModifier = boxModifier.border(
            width = 2.dp,
            brush = outlineBrush,
            shape = shape
        )
    } else {
        boxModifier = boxModifier.border(
            width = 1.dp,
            color = colors.primary.copy(alpha = 0.28f),
            shape = shape
        )
    }

    if (avatarTestTag != null) {
        boxModifier = boxModifier.testTag(avatarTestTag)
    }

    Box(
        modifier = boxModifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = extractInitials(userProfile.displayName),
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            color = colors.primary
        )
    }
}

/**
 * Unified top header across Desk, Mulberry Shelf (Library), and Settings:
 * [Mulberry Icon] [Title]                        [Profile Icon]
 * Supports translucent refractive glass backdrop with caustic sheen & prismatic bottom border.
 */
@Composable
fun MulberryUnifiedHeader(
    title: String,
    userProfile: UserProfile,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 20.dp,
    verticalPadding: Dp = 12.dp,
    showGlassBackground: Boolean = true,
    titleTestTag: String? = null,
    avatarTestTag: String = "header_profile_avatar_button"
) {
    val colors = LocalMulberryColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (showGlassBackground) Modifier.refractiveHeaderGlass() else Modifier)
            .then(modifier)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MulberryExpressiveLogo(size = 32.dp, showContainer = false)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = colors.textPrimary,
                letterSpacing = (-0.5).sp,
                modifier = if (titleTestTag != null) Modifier.testTag(titleTestTag) else Modifier
            )
        }

        MulberryProfileAvatar(
            userProfile = userProfile,
            onClick = onProfileClick,
            size = 38.dp,
            cornerRadius = 12.dp,
            fontSize = 14.sp,
            avatarTestTag = avatarTestTag
        )
    }
}

private fun extractInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "M"
    val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
    return if (parts.size >= 2) {
        "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
    } else {
        trimmed.take(2).uppercase()
    }
}
