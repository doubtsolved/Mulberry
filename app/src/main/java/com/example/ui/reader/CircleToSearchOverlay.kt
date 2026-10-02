package com.example.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InterFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CircleToSearchOverlay(
    centerOffset: Offset?,
    visible: Boolean,
    onDismiss: () -> Unit
) {
    if (!visible || centerOffset == null) return

    val scaleAnim = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(0f) }
    val rotationAnim = remember { Animatable(0f) }

    LaunchedEffect(visible, centerOffset) {
        scaleAnim.snapTo(0f)
        alphaAnim.snapTo(0f)
        rotationAnim.snapTo(0f)

        launch {
            alphaAnim.animateTo(1f, animationSpec = tween(150))
        }
        launch {
            scaleAnim.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            rotationAnim.animateTo(
                360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2400, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }

        // Keep visible for animation feedback before completing
        delay(900)
        alphaAnim.animateTo(0f, animationSpec = tween(200))
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.28f * alphaAnim.value))
    ) {
        // Glowing Google Circle to Search lens ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = 80.dp.toPx() * scaleAnim.value
            val glowRadius = radius + 14.dp.toPx()

            // Ethereal circular gradient
            val googleGradient = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF4285F4), // Google Blue
                    Color(0xFFEA4335), // Google Red
                    Color(0xFFFBBC05), // Google Yellow
                    Color(0xFF34A853), // Google Green
                    Color(0xFF4285F4)  // Close loop
                ),
                center = centerOffset
            )

            // Outer soft glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF4285F4).copy(alpha = 0.22f * alphaAnim.value),
                        Color(0xFF34A853).copy(alpha = 0.08f * alphaAnim.value),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = centerOffset
            )

            // Inner circle tint
            drawCircle(
                color = Color.White.copy(alpha = 0.06f * alphaAnim.value),
                radius = radius,
                center = centerOffset
            )

            // Main illuminated search ring
            drawCircle(
                brush = googleGradient,
                radius = radius,
                center = centerOffset,
                style = Stroke(width = 3.5.dp.toPx() * alphaAnim.value)
            )
        }

        // Floating "Circle to Search" indicator badge
        AnimatedVisibility(
            visible = scaleAnim.value > 0.4f,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 18.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E1E24).copy(alpha = 0.92f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Circle to Search",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
