package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R

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
