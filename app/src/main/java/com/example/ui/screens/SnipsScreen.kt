package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FluentIcons
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import com.example.viewmodel.MulberryViewModel
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * SnipsScreen: Fresh clipping workspace with cleared sample mock items.
 * Displays a vector animation with a medical bookmark motif, highlighter sweep,
 * and precision clipping reticles.
 */
@Composable
fun SnipsScreen(
    modifier: Modifier = Modifier,
    viewModel: MulberryViewModel? = null
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundGradient)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // 1. TOP HEADER ("Snips")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Snips",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = colors.textPrimary,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.testTag("snips_title")
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.primary.copy(alpha = 0.12f),
                modifier = Modifier.testTag("snips_status_badge")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = FluentIcons.Bookmark24Regular,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Workspace Staging",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = colors.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. ANIMATED GRAPHIC WITH MEDICAL BOOKMARK MOTIF
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            MedicalBookmarkAnimationGraphic(colors = colors)
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 3. TITLE & EXPLANATORY TEXT
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "High-Yield Snips",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Sample mock data cleared. The study clipping workspace will be rebuilt from scratch—allowing you to snip histology slides, anatomical diagrams, and clinical pearls directly from reader pages.",
                fontFamily = InterFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.5.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = FluentIcons.Bookmark24Regular,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Ready for Ingestion • Workflow in Staging",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = colors.textPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

/**
 * Custom Vector Canvas Animation:
 * - Floating textbook page with subtle elevated shadow
 * - Ruled study lines with an animated sweeping highlighter beam
 * - Dangling medical ribbon bookmark with smooth sinusoidal vertical bobbing
 * - Precision clipping guide corner reticles pulsing with physics springs
 */
@Composable
private fun MedicalBookmarkAnimationGraphic(
    colors: com.example.ui.theme.MulberryColors
) {
    val coroutineScope = rememberCoroutineScope()
    val bounceScale = remember { Animatable(1f) }
    val infiniteTransition = rememberInfiniteTransition(label = "BookmarkMotif")

    // Vertical bobbing motion for the bookmark ribbon
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "RibbonBobbing"
    )

    // Highlighter sweep animation across textbook lines
    val highlightSweep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "HighlightSweep"
    )

    // Corner reticle pulse
    val reticlePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ReticlePulse"
    )

    Box(
        modifier = Modifier
            .size(280.dp)
            .scale(bounceScale.value)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                coroutineScope.launch {
                    bounceScale.animateTo(
                        0.94f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessHigh
                        )
                    )
                    bounceScale.animateTo(
                        1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            // 1. Soft ambient glow behind card
            drawCircle(
                color = colors.primary.copy(alpha = 0.08f),
                radius = canvasW * 0.44f,
                center = Offset(canvasW * 0.5f, canvasH * 0.52f)
            )

            // 2. Stylized Textbook Page Card
            val pageLeft = canvasW * 0.16f
            val pageTop = canvasH * 0.18f
            val pageWidth = canvasW * 0.68f
            val pageHeight = canvasH * 0.64f
            val cornerRadius = 18.dp.toPx()

            // Card background & border
            drawRoundRect(
                color = colors.surfaceCard,
                topLeft = Offset(pageLeft, pageTop),
                size = Size(pageWidth, pageHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )
            drawRoundRect(
                color = colors.borderSubtle,
                topLeft = Offset(pageLeft, pageTop),
                size = Size(pageWidth, pageHeight),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = 1.5.dp.toPx())
            )

            // 3. Ruled Textbook Study Lines
            val lineMarginX = pageLeft + 22.dp.toPx()
            val lineWidth = pageWidth - 44.dp.toPx()
            val startY = pageTop + 38.dp.toPx()
            val lineSpacing = 16.dp.toPx()

            for (i in 0 until 5) {
                val currentY = startY + (i * lineSpacing)
                val effectiveLineWidth = if (i == 4) lineWidth * 0.55f else if (i == 2) lineWidth * 0.85f else lineWidth

                drawLine(
                    color = colors.borderSubtle.copy(alpha = 0.7f),
                    start = Offset(lineMarginX, currentY),
                    end = Offset(lineMarginX + effectiveLineWidth, currentY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Highlighting effect on line 1 & 2
                if (i == 1) {
                    val sweepProgress = highlightSweep.coerceIn(0f, 1f)
                    val highWidth = effectiveLineWidth * sweepProgress
                    if (highWidth > 0f) {
                        drawLine(
                            color = colors.primary.copy(alpha = 0.38f),
                            start = Offset(lineMarginX, currentY),
                            end = Offset(lineMarginX + highWidth, currentY),
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            // 4. Precision Clipping Guides / Marquee Reticles
            val cropMargin = 12.dp.toPx()
            val cropLeft = pageLeft + cropMargin
            val cropTop = pageTop + cropMargin + (lineSpacing * 0.4f)
            val cropRight = pageLeft + pageWidth - cropMargin
            val cropBottom = pageTop + pageHeight - cropMargin
            val reticleArm = 14.dp.toPx() * reticlePulse
            val reticleColor = colors.primary

            // Top-Left Reticle
            drawLine(
                color = reticleColor,
                start = Offset(cropLeft, cropTop),
                end = Offset(cropLeft + reticleArm, cropTop),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = reticleColor,
                start = Offset(cropLeft, cropTop),
                end = Offset(cropLeft, cropTop + reticleArm),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Top-Right Reticle
            drawLine(
                color = reticleColor,
                start = Offset(cropRight, cropTop),
                end = Offset(cropRight - reticleArm, cropTop),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = reticleColor,
                start = Offset(cropRight, cropTop),
                end = Offset(cropRight, cropTop + reticleArm),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Bottom-Left Reticle
            drawLine(
                color = reticleColor,
                start = Offset(cropLeft, cropBottom),
                end = Offset(cropLeft + reticleArm, cropBottom),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = reticleColor,
                start = Offset(cropLeft, cropBottom),
                end = Offset(cropLeft, cropBottom - reticleArm),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Bottom-Right Reticle
            drawLine(
                color = reticleColor,
                start = Offset(cropRight, cropBottom),
                end = Offset(cropRight - reticleArm, cropBottom),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = reticleColor,
                start = Offset(cropRight, cropBottom),
                end = Offset(cropRight, cropBottom - reticleArm),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // 5. Medical Ribbon Bookmark with Sinusoidal Vertical Bobbing
            val ribbonWidth = 26.dp.toPx()
            val ribbonLeft = pageLeft + pageWidth - 42.dp.toPx()
            val ribbonTop = pageTop - 12.dp.toPx() + bobOffset
            val ribbonLength = 76.dp.toPx()
            val notchDepth = 12.dp.toPx()

            // Bookmark Ribbon Path with V-notch cut
            val ribbonPath = Path().apply {
                moveTo(ribbonLeft, ribbonTop)
                lineTo(ribbonLeft + ribbonWidth, ribbonTop)
                lineTo(ribbonLeft + ribbonWidth, ribbonTop + ribbonLength)
                lineTo(ribbonLeft + (ribbonWidth / 2f), ribbonTop + ribbonLength - notchDepth)
                lineTo(ribbonLeft, ribbonTop + ribbonLength)
                close()
            }

            // Draw Ribbon Drop Shadow
            drawPath(
                path = ribbonPath,
                color = Color.Black.copy(alpha = 0.12f)
            )

            // Draw Ribbon Body
            drawPath(
                path = ribbonPath,
                color = colors.primary
            )

            // Draw Medical Cross / Accent on Ribbon
            val crossCenterX = ribbonLeft + (ribbonWidth / 2f)
            val crossCenterY = ribbonTop + (ribbonLength * 0.42f)
            val crossArm = 4.5.dp.toPx()

            drawLine(
                color = colors.onPrimary,
                start = Offset(crossCenterX - crossArm, crossCenterY),
                end = Offset(crossCenterX + crossArm, crossCenterY),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = colors.onPrimary,
                start = Offset(crossCenterX, crossCenterY - crossArm),
                end = Offset(crossCenterX, crossCenterY + crossArm),
                strokeWidth = 2.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Floating Crop Icon Badge on bottom-left of card
        Surface(
            shape = CircleShape,
            color = colors.primary,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, bottom = 28.dp)
                .size(38.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = FluentIcons.Scissors24Regular,
                    contentDescription = "Snip",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
