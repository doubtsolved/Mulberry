package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalMulberryColors
import com.example.viewmodel.MulberryViewModel
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AgendaScreen(
    viewModel: MulberryViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Minimal Top Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Study",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                letterSpacing = (-0.5).sp
            )
        }

        // Center Animated Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated Floating Thought Bubbles + Pulsing Lightbulb Graphic
                AjitThinkingAnimation(
                    primaryColor = colors.primary,
                    surfaceTintColor = colors.surfaceTint,
                    isDark = colors.isDark,
                    modifier = Modifier.size(240.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Improvised Headline
                Text(
                    text = "Hold Tight, Ajit is Thinking...",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Improvised Supporting Copy (No accent pills or badges)
                Text(
                    text = "Synapses are firing and notes are brewing. A smarter way to master your coursework is dropping soon.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp
                )
            }
        }
    }
}

/**
 * Animated Canvas: Pulsing lightbulb idea with radial aura and floating thought bubbles
 */
@Composable
private fun AjitThinkingAnimation(
    primaryColor: Color,
    surfaceTintColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ThinkingAnim")

    // Gentle breathing scale for the bulb
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    // Glowing aura intensity
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "GlowAlpha"
    )

    // Bubble floating phases
    val bubblePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "BubblePhase"
    )

    // Sparkle rotation
    val sparkleAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SparkleRotation"
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f + 16f

        // 1. Radial Glow Aura
        val glowRadius = 85.dp.toPx() * pulseScale
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = glowAlpha * (if (isDark) 0.45f else 0.28f)),
                    primaryColor.copy(alpha = glowAlpha * 0.15f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY - 15f),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(centerX, centerY - 15f)
        )

        // 2. Ascending Thought Bubbles
        val bubbleOffsets = listOf(
            Triple(-50f, 0.0f, 10f),
            Triple(55f, 0.35f, 14f),
            Triple(-25f, 0.65f, 8f),
            Triple(35f, 0.85f, 11f),
            Triple(-65f, 0.45f, 12f)
        )

        bubbleOffsets.forEach { (xOffset, phaseOffset, radius) ->
            val progress = (bubblePhase + phaseOffset) % 1f
            // Y travels from below bulb upwards into the atmosphere
            val bubbleY = (centerY + 40f) - progress * (size.height * 0.85f)
            // Gentle sinusoidal horizontal sway
            val waveX = centerX + xOffset + sin(progress * 6.28f * 2f) * 12f
            // Fade in as it emerges, fade out near top
            val alpha = (sin(progress * Math.PI.toFloat())).coerceIn(0f, 1f) * 0.65f

            drawCircle(
                color = primaryColor.copy(alpha = alpha),
                radius = radius * (0.8f + progress * 0.4f),
                center = Offset(waveX, bubbleY)
            )

            // Inner bubble highlight
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.7f),
                radius = radius * 0.3f,
                center = Offset(waveX - radius * 0.3f, bubbleY - radius * 0.3f)
            )
        }

        // 3. Central Lightbulb (Glass Globe & Base)
        val bulbRadius = 38.dp.toPx() * pulseScale
        val bulbCenter = Offset(centerX, centerY - 15f)

        // Bulb Glass Body
        drawCircle(
            color = surfaceTintColor.copy(alpha = if (isDark) 0.35f else 0.5f),
            radius = bulbRadius,
            center = bulbCenter
        )
        drawCircle(
            color = primaryColor,
            radius = bulbRadius,
            center = bulbCenter,
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Filament (Tungsten coil inside bulb)
        val filamentPath = Path().apply {
            moveTo(bulbCenter.x - 14f, bulbCenter.y + 12f)
            lineTo(bulbCenter.x - 8f, bulbCenter.y - 12f)
            lineTo(bulbCenter.x, bulbCenter.y - 6f)
            lineTo(bulbCenter.x + 8f, bulbCenter.y - 12f)
            lineTo(bulbCenter.x + 14f, bulbCenter.y + 12f)
        }
        drawPath(
            path = filamentPath,
            color = primaryColor,
            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Filament Heart Core Spark
        drawCircle(
            color = primaryColor.copy(alpha = 0.85f),
            radius = 3.5.dp.toPx() * pulseScale,
            center = Offset(bulbCenter.x, bulbCenter.y - 6f)
        )

        // Glass reflection arc
        val arcPath = Path().apply {
            moveTo(bulbCenter.x - bulbRadius * 0.65f, bulbCenter.y - bulbRadius * 0.45f)
            quadraticTo(
                bulbCenter.x - bulbRadius * 0.5f,
                bulbCenter.y - bulbRadius * 0.75f,
                bulbCenter.x - bulbRadius * 0.15f,
                bulbCenter.y - bulbRadius * 0.8f
            )
        }
        drawPath(
            path = arcPath,
            color = Color.White.copy(alpha = if (isDark) 0.6f else 0.8f),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // Screw Base at bottom of bulb
        val baseTopY = bulbCenter.y + bulbRadius * 0.82f
        val baseWidth = 24.dp.toPx()
        val baseHalf = baseWidth / 2f

        // Threads
        for (i in 0..2) {
            val threadY = baseTopY + i * 6.5.dp.toPx()
            drawLine(
                color = primaryColor,
                start = Offset(centerX - baseHalf + (i * 1.5f), threadY),
                end = Offset(centerX + baseHalf - (i * 1.5f), threadY),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Contact point at very bottom
        drawCircle(
            color = primaryColor,
            radius = 4.dp.toPx(),
            center = Offset(centerX, baseTopY + 20.dp.toPx())
        )

        // 4. Subtle Ambient Sparkles around the bulb
        val sparkleDist = 65.dp.toPx()
        val angles = listOf(45f, 135f, 225f, 315f)
        angles.forEachIndexed { index, angleDeg ->
            val rad = Math.toRadians((angleDeg + sparkleAngle * (if (index % 2 == 0) 1 else -1)).toDouble())
            val sx = bulbCenter.x + (cos(rad) * sparkleDist).toFloat()
            val sy = bulbCenter.y + (sin(rad) * sparkleDist).toFloat()
            val sAlpha = (0.3f + 0.4f * sin((sparkleAngle + index * 90f) * 0.05f)).coerceIn(0.1f, 0.7f)

            // Draw 4-point star sparkle
            drawLine(
                color = primaryColor.copy(alpha = sAlpha),
                start = Offset(sx - 6.dp.toPx(), sy),
                end = Offset(sx + 6.dp.toPx(), sy),
                strokeWidth = 1.75.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = primaryColor.copy(alpha = sAlpha),
                start = Offset(sx, sy - 6.dp.toPx()),
                end = Offset(sx, sy + 6.dp.toPx()),
                strokeWidth = 1.75.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
