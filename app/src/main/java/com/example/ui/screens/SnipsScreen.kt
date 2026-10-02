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
import androidx.compose.ui.geometry.Size
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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SnipsScreen(
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
                text = "Snips",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                letterSpacing = (-0.5).sp
            )
        }

        // Center Animated Cooking Content
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
                // Playful Chef Hat with Culinary Shimmer Animation
                ChefCookingAnimation(
                    primaryColor = colors.primary,
                    surfaceTintColor = colors.surfaceTint,
                    isDark = colors.isDark,
                    modifier = Modifier.size(240.dp)
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Improvised Headline
                Text(
                    text = "Something's Cooking. Don't Enter the Kitchen!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Improvised Supporting Line (No accent pills, no badges, no long subtitles)
                Text(
                    text = "Master chefs at work. Simmering the finest visual scrapbook to perfection.",
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
 * Animated Canvas: Playful floating & tilting chef hat with rising culinary steam and sparkle shimmer
 */
@Composable
private fun ChefCookingAnimation(
    primaryColor: Color,
    surfaceTintColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CookingAnim")

    // Gentle vertical bobbing
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BobOffset"
    )

    // Playful subtle tilt angle (degrees)
    val tiltAngle by infiniteTransition.animateFloat(
        initialValue = -4.5f,
        targetValue = 4.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TiltAngle"
    )

    // Rising culinary steam phase
    val steamPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SteamPhase"
    )

    // Culinary shimmer sparkle phase
    val sparklePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SparklePhase"
    )

    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f + 25f + bobOffset

        // 1. Warm Kitchen Glow beneath hat
        val glowRadius = 75.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = if (isDark) 0.35f else 0.22f),
                    primaryColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(centerX, centerY - 20f),
                radius = glowRadius
            ),
            radius = glowRadius,
            center = Offset(centerX, centerY - 20f)
        )

        // 2. Rising Culinary Steam Waves (Curving upward)
        val steamStreams = listOf(
            Triple(-40f, 0.0f, 28f),
            Triple(0f, 0.38f, 32f),
            Triple(42f, 0.72f, 26f)
        )

        steamStreams.forEach { (xStartOffset, phaseOffset, streamHeight) ->
            val progress = (steamPhase + phaseOffset) % 1f
            val startY = centerY - 65.dp.toPx() - (progress * 70.dp.toPx())
            val alpha = (sin(progress * Math.PI.toFloat())).coerceIn(0f, 1f) * 0.75f
            val sway = sin((progress + phaseOffset) * 6.28f * 1.5f) * 14f

            val steamPath = Path().apply {
                val sx = centerX + xStartOffset + sway
                moveTo(sx, startY)
                cubicTo(
                    sx - 10f, startY - streamHeight * 0.35f,
                    sx + 10f, startY - streamHeight * 0.7f,
                    sx, startY - streamHeight
                )
            }

            drawPath(
                path = steamPath,
                color = primaryColor.copy(alpha = alpha),
                style = Stroke(
                    width = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Steam bubble dot
            drawCircle(
                color = primaryColor.copy(alpha = alpha * 0.8f),
                radius = 3.dp.toPx(),
                center = Offset(centerX + xStartOffset + sway, startY - streamHeight)
            )
        }

        // 3. Playful Chef Hat (Drawn rotated with tiltAngle)
        val radTilt = Math.toRadians(tiltAngle.toDouble())
        val cosT = cos(radTilt).toFloat()
        val sinT = sin(radTilt).toFloat()

        fun transformPoint(x: Float, y: Float): Offset {
            val dx = x - centerX
            val dy = y - centerY
            val rx = dx * cosT - dy * sinT + centerX
            val ry = dx * sinT + dy * cosT + centerY
            return Offset(rx, ry)
        }

        // Hat Dimensions
        val bandWidth = 72.dp.toPx()
        val bandHeight = 22.dp.toPx()
        val bandTop = centerY + 10f
        val bandBottom = bandTop + bandHeight

        // Puffy Top of the Chef Hat (3 Clouds / Pleats)
        val puffPath = Path().apply {
            val pStart = transformPoint(centerX - bandWidth * 0.44f, bandTop)
            moveTo(pStart.x, pStart.y)

            // Left puff
            val c1 = transformPoint(centerX - bandWidth * 0.8f, bandTop - 35.dp.toPx())
            val c2 = transformPoint(centerX - bandWidth * 0.45f, bandTop - 70.dp.toPx())
            val p1 = transformPoint(centerX - bandWidth * 0.15f, bandTop - 62.dp.toPx())
            cubicTo(c1.x, c1.y, c2.x, c2.y, p1.x, p1.y)

            // Center tall puff
            val c3 = transformPoint(centerX - 10f, bandTop - 86.dp.toPx())
            val c4 = transformPoint(centerX + 15f, bandTop - 86.dp.toPx())
            val p2 = transformPoint(centerX + bandWidth * 0.18f, bandTop - 62.dp.toPx())
            cubicTo(c3.x, c3.y, c4.x, c4.y, p2.x, p2.y)

            // Right puff
            val c5 = transformPoint(centerX + bandWidth * 0.45f, bandTop - 70.dp.toPx())
            val c6 = transformPoint(centerX + bandWidth * 0.8f, bandTop - 35.dp.toPx())
            val p3 = transformPoint(centerX + bandWidth * 0.44f, bandTop)
            cubicTo(c5.x, c5.y, c6.x, c6.y, p3.x, p3.y)

            close()
        }

        // Fill puff with subtle tint
        drawPath(
            path = puffPath,
            color = surfaceTintColor.copy(alpha = if (isDark) 0.4f else 0.6f)
        )
        // Stroke puff
        drawPath(
            path = puffPath,
            color = primaryColor,
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Chef Hat Pleat Crease Lines inside the puff
        val pleat1Start = transformPoint(centerX - bandWidth * 0.18f, bandTop - 6f)
        val pleat1End = transformPoint(centerX - bandWidth * 0.22f, bandTop - 45.dp.toPx())
        drawLine(
            color = primaryColor.copy(alpha = 0.6f),
            start = pleat1Start,
            end = pleat1End,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        val pleat2Start = transformPoint(centerX + bandWidth * 0.18f, bandTop - 6f)
        val pleat2End = transformPoint(centerX + bandWidth * 0.22f, bandTop - 45.dp.toPx())
        drawLine(
            color = primaryColor.copy(alpha = 0.6f),
            start = pleat2Start,
            end = pleat2End,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )

        // Chef Hat Headband (Base)
        val bandPath = Path().apply {
            val tl = transformPoint(centerX - bandWidth * 0.48f, bandTop)
            val tr = transformPoint(centerX + bandWidth * 0.48f, bandTop)
            val br = transformPoint(centerX + bandWidth * 0.44f, bandBottom)
            val bl = transformPoint(centerX - bandWidth * 0.44f, bandBottom)
            moveTo(tl.x, tl.y)
            lineTo(tr.x, tr.y)
            lineTo(br.x, br.y)
            lineTo(bl.x, bl.y)
            close()
        }
        drawPath(
            path = bandPath,
            color = primaryColor
        )

        // Decorative band stitch line
        val stitchStart = transformPoint(centerX - bandWidth * 0.38f, bandTop + bandHeight / 2f)
        val stitchEnd = transformPoint(centerX + bandWidth * 0.38f, bandTop + bandHeight / 2f)
        drawLine(
            color = Color.White.copy(alpha = 0.85f),
            start = stitchStart,
            end = stitchEnd,
            strokeWidth = 1.75.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 4. Culinary Shimmer Sparkles around the Chef Hat
        val sparkleCoordinates = listOf(
            Pair(-68.dp.toPx(), -20.dp.toPx()),
            Pair(68.dp.toPx(), -30.dp.toPx()),
            Pair(50.dp.toPx(), 25.dp.toPx()),
            Pair(-55.dp.toPx(), 30.dp.toPx())
        )

        sparkleCoordinates.forEachIndexed { index, (ox, oy) ->
            val angleOffset = index * 90f
            val currentSparkleAngle = sparklePhase + angleOffset
            val sparkleScale = (0.5f + 0.5f * sin(Math.toRadians(currentSparkleAngle.toDouble()).toFloat())).coerceIn(0.2f, 1f)
            val spX = centerX + ox
            val spY = centerY + oy

            val sparkleArm = 7.dp.toPx() * sparkleScale
            // 4-point Diamond Sparkle
            val starPath = Path().apply {
                moveTo(spX, spY - sparkleArm)
                lineTo(spX + sparkleArm * 0.35f, spY)
                lineTo(spX, spY + sparkleArm)
                lineTo(spX - sparkleArm * 0.35f, spY)
                close()
            }
            drawPath(
                path = starPath,
                color = primaryColor.copy(alpha = (0.3f + 0.6f * sparkleScale).coerceIn(0f, 1f))
            )
            val starPathH = Path().apply {
                moveTo(spX - sparkleArm, spY)
                lineTo(spX, spY + sparkleArm * 0.35f)
                lineTo(spX + sparkleArm, spY)
                lineTo(spX, spY - sparkleArm * 0.35f)
                close()
            }
            drawPath(
                path = starPathH,
                color = primaryColor.copy(alpha = (0.3f + 0.6f * sparkleScale).coerceIn(0f, 1f))
            )
        }
    }
}
