package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch

/**
 * Universal interactive modifier providing:
 * - Spring-loaded bouncy scale-down to 0.94f on touch down
 * - Dual-stage tactile haptic feedback (light tick on down, confirmation click on select)
 * - Elastic spring snap back to 1.0f with medium bouncy overshoot on release
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
                        // Confirm haptic & perform click
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    }
                },
                onLongPress = if (onLongClick != null) {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                } else null
            )
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
