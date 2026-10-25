package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import kotlinx.coroutines.launch

/**
 * Visual glass variants for standardized styling across Mulberry.
 */
enum class GlassVariant {
    PRIMARY,    // Strong blur, ~90-94% opaque with primary tint, rim light glow, high elevation
    SECONDARY,  // Less blur, ~45% translucent surface, subtle rim, low elevation
    DOCK,       // 70% translucent frosted glass surface
    CHIP        // Distinct tinted frosted chip glass with crisp visible border
}

/**
 * Universal interactive modifier providing:
 * - Spring-loaded bouncy scale-down to 0.94f on touch down
 * - Dual-stage tactile haptic feedback (light tick on down, confirmation click on select)
 * - Elastic spring snap back to 1.0f with medium bouncy overshoot on release
 * - Uses rememberUpdatedState so stateful click lambdas (such as filter chips) never go stale
 */
fun Modifier.fluidGlassPressEffect(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    if (!enabled) return@composed this

    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1.0f) }
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnLongClick by rememberUpdatedState(onLongClick)

    this
        .scale(scaleAnim.value)
        .pointerInput(enabled) {
            detectTapGestures(
                onPress = {
                    // 1. Touch Down: Light tick haptic & spring compress to 0.94f
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val pressJob = scope.launch {
                        scaleAnim.animateTo(
                            targetValue = pressedScale,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    }

                    val success = tryAwaitRelease()
                    pressJob.cancel()

                    // 2. Touch Up / Release: Spring snap back to 1.0f with bouncy overshoot
                    scope.launch {
                        scaleAnim.animateTo(
                            targetValue = 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    }

                    if (success) {
                        // Confirm haptic & perform click with latest state
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnClick()
                    }
                },
                onLongPress = if (currentOnLongClick != null) {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnLongClick?.invoke()
                    }
                } else null
            )
        }
}

/**
 * Centralized GlassModifier applying glass translucency, specular rim sheen, and borders.
 */
fun Modifier.glassEffect(
    variant: GlassVariant = GlassVariant.SECONDARY,
    shape: Shape = RoundedCornerShape(14.dp),
    borderWidth: Dp = 1.dp
): Modifier = composed {
    val colors = LocalMulberryColors.current

    when (variant) {
        GlassVariant.PRIMARY -> {
            val primaryBrush = Brush.verticalGradient(
                colors = listOf(
                    colors.primary.copy(alpha = 0.96f),
                    colors.primary.copy(alpha = 0.90f)
                )
            )
            val borderBrush = FluidGlassDefaults.activePillBorderBrush(colors.isDark, colors.primary)
            val rimBrush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = if (colors.isDark) 0.16f else 0.22f),
                    Color.Transparent
                )
            )

            this
                .shadow(
                    elevation = 6.dp,
                    shape = shape,
                    spotColor = colors.primary.copy(alpha = 0.40f),
                    ambientColor = Color.Black.copy(alpha = 0.15f)
                )
                .clip(shape)
                .background(primaryBrush, shape)
                .drawBehind {
                    val r = size.height / 2f
                    drawRoundRect(
                        brush = rimBrush,
                        size = Size(size.width, size.height * 0.48f),
                        cornerRadius = CornerRadius(r, r)
                    )
                }
                .border(borderWidth, borderBrush, shape)
        }
        GlassVariant.SECONDARY -> {
            val secondaryBg = Brush.verticalGradient(
                colors = listOf(
                    colors.surfaceCard.copy(alpha = if (colors.isDark) 0.62f else 0.80f),
                    colors.surfaceTint.copy(alpha = if (colors.isDark) 0.42f else 0.58f)
                )
            )
            val borderBrush = FluidGlassDefaults.glassBorderBrush(colors.isDark, colors.borderSubtle)
            val rimBrush = Brush.verticalGradient(
                colors = listOf(
                    if (colors.isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.35f),
                    Color.Transparent
                )
            )

            this
                .shadow(
                    elevation = 2.dp,
                    shape = shape,
                    spotColor = colors.borderSubtle.copy(alpha = 0.15f),
                    ambientColor = Color.Transparent
                )
                .clip(shape)
                .background(secondaryBg, shape)
                .drawBehind {
                    val r = size.height / 2f
                    drawRoundRect(
                        brush = rimBrush,
                        size = Size(size.width, size.height * 0.45f),
                        cornerRadius = CornerRadius(r, r)
                    )
                }
                .border(borderWidth, borderBrush, shape)
        }
        GlassVariant.DOCK -> {
            val dockBg = colors.surface.copy(alpha = 0.72f)
            val borderBrush = FluidGlassDefaults.glassBorderBrush(colors.isDark, colors.borderSubtle)
            val rimBrush = FluidGlassDefaults.rimLightBrush(colors.isDark)

            this
                .shadow(
                    elevation = 12.dp,
                    shape = shape,
                    spotColor = colors.primary.copy(alpha = 0.20f),
                    ambientColor = Color.Black.copy(alpha = 0.20f)
                )
                .clip(shape)
                .background(dockBg, shape)
                .drawBehind {
                    drawRoundRect(
                        brush = rimBrush,
                        size = Size(size.width, size.height * 0.5f),
                        cornerRadius = CornerRadius(size.height / 2f, size.height / 2f)
                    )
                }
                .border(borderWidth, borderBrush, shape)
        }
        GlassVariant.CHIP -> {
            val chipBrush = Brush.verticalGradient(
                colors = listOf(
                    if (colors.isDark) colors.surfaceCard.copy(alpha = 0.82f)
                    else colors.surfaceTint.copy(alpha = 0.68f),
                    if (colors.isDark) colors.surface.copy(alpha = 0.75f)
                    else colors.surfaceCard.copy(alpha = 0.90f)
                )
            )
            val chipBorderBrush = Brush.verticalGradient(
                colors = listOf(
                    if (colors.isDark) Color.White.copy(alpha = 0.22f)
                    else colors.primary.copy(alpha = 0.28f),
                    colors.borderSubtle.copy(alpha = if (colors.isDark) 0.65f else 0.95f)
                )
            )

            this
                .clip(shape)
                .background(chipBrush, shape)
                .border(borderWidth, chipBorderBrush, shape)
        }
    }
}

/**
 * Translucent, blurred, refractive glass surface modifier for Top Headers / App Bars.
 * Renders a frosted translucent backdrop, subtle internal caustic refraction sheen,
 * and a multi-stop prismatic dispersion bottom hairline border.
 */
fun Modifier.refractiveHeaderGlass(): Modifier = composed {
    val colors = LocalMulberryColors.current

    val baseSurfaceBrush = Brush.verticalGradient(
        colors = listOf(
            colors.surface.copy(alpha = if (colors.isDark) 0.76f else 0.78f),
            colors.surfaceCard.copy(alpha = if (colors.isDark) 0.66f else 0.68f)
        )
    )

    val causticSheenBrush = Brush.linearGradient(
        colors = listOf(
            if (colors.isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.42f),
            colors.primary.copy(alpha = if (colors.isDark) 0.09f else 0.08f),
            Color.Transparent
        )
    )

    val prismBottomBorderBrush = Brush.horizontalGradient(
        colors = listOf(
            colors.borderSubtle.copy(alpha = 0.45f),
            if (colors.isDark) Color.White.copy(alpha = 0.32f) else colors.primary.copy(alpha = 0.38f),
            Color(0xFF38BDF8).copy(alpha = if (colors.isDark) 0.28f else 0.22f),
            colors.primary.copy(alpha = if (colors.isDark) 0.35f else 0.32f),
            colors.borderSubtle.copy(alpha = 0.45f)
        )
    )

    this
        .background(baseSurfaceBrush)
        .drawWithContent {
            // Top refractive caustic sheen
            drawRect(
                brush = causticSheenBrush,
                size = Size(size.width, size.height * 0.68f)
            )
            drawContent()
            // Bottom prismatic dispersion hairline
            val strokePx = 1.dp.toPx()
            drawLine(
                brush = prismBottomBorderBrush,
                start = Offset(0f, size.height - strokePx / 2f),
                end = Offset(size.width, size.height - strokePx / 2f),
                strokeWidth = strokePx
            )
        }
}

/**
 * Standardized High-Fidelity Primary Glass Button.
 * Replaces any legacy Button / Surface double-containers with a single, pristine frosted glass component.
 */
@Composable
fun FluidGlassPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    testTag: String? = null
) {
    val colors = LocalMulberryColors.current

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .height(50.dp)
            .minimumInteractiveComponentSize()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .fluidGlassPressEffect(enabled = enabled, onClick = onClick)
            .glassEffect(
                variant = if (enabled) GlassVariant.PRIMARY else GlassVariant.SECONDARY,
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (enabled) Color.White else colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = text,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (enabled) Color.White else colors.textMuted
            )

            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = if (enabled) Color.White else colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Standardized High-Fidelity Secondary Glass Button.
 * Clean, translucent glass container with subtle border and crisp secondary text.
 */
@Composable
fun FluidGlassSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(14.dp),
    testTag: String? = null
) {
    val colors = LocalMulberryColors.current

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .height(50.dp)
            .minimumInteractiveComponentSize()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .fluidGlassPressEffect(enabled = enabled, onClick = onClick)
            .glassEffect(variant = GlassVariant.SECONDARY, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = text,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.5.sp,
                color = colors.textSecondary
            )

            if (trailingIcon != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Frosted glass specular rim highlights and theme gradient border helpers.
 */
object FluidGlassDefaults {
    fun glassBorderBrush(isDark: Boolean, borderSubtle: Color): Brush =
        Brush.verticalGradient(
            colors = listOf(
                if (isDark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.65f),
                borderSubtle.copy(alpha = 0.40f)
            )
        )

    fun rimLightBrush(isDark: Boolean): Brush =
        Brush.verticalGradient(
            colors = listOf(
                if (isDark) Color.White.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.45f),
                Color.Transparent
            )
        )

    fun activePillBorderBrush(isDark: Boolean, primaryColor: Color): Brush =
        Brush.verticalGradient(
            colors = listOf(
                if (isDark) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.70f),
                primaryColor.copy(alpha = 0.40f)
            )
        )
}
