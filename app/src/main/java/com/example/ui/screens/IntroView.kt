package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AtelierLogo
import com.example.ui.theme.AccentMagenta
import kotlinx.coroutines.delay

/**
 * iOS Splash / Opening screen matching Atelier Control design:
 * - Deep dark violet background with concentric wave rings
 * - Prominent Atelier Control geometric ribbon logo
 * - "Atelier Control" title (White + Magenta)
 * - "Sistema de Gestão" subtitle
 * - 3 animated rhythmic dots (Cyan, Magenta, Purple)
 * - "Vendas • Produtos • Relatórios" footer
 */
@Composable
fun IntroView(
    onEnterApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Fade in state on launch
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        contentVisible = true
        // Auto-dismiss after 2.4 seconds to enter app
        delay(2400)
        onEnterApp()
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "splash_fade_in"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (contentVisible) 1f else 0.94f,
        animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing),
        label = "splash_scale_in"
    )

    // Animated dots rhythm
    val infiniteTransition = rememberInfiniteTransition(label = "splash_dots")
    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 180, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0D061A), // Deep Midnight Violet
            Color(0xFF140827), // Rich Indigo Purple
            Color(0xFF080312)  // Near Black Base
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Tap anywhere to skip instantly
                onEnterApp()
            }
            .testTag("ios_splash_screen")
    ) {
        // Concentric Translucent Wave Rings in Background (matching iOS aesthetic)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = Offset(size.width * 0.5f, size.height * 0.40f)
            val ringRadii = listOf(
                size.minDimension * 0.36f,
                size.minDimension * 0.54f,
                size.minDimension * 0.74f,
                size.minDimension * 0.96f,
                size.minDimension * 1.22f,
                size.minDimension * 1.50f
            )

            ringRadii.forEachIndexed { index, radius ->
                val alpha = when (index) {
                    0 -> 0.12f
                    1 -> 0.10f
                    2 -> 0.08f
                    3 -> 0.06f
                    4 -> 0.04f
                    else -> 0.02f
                }
                drawCircle(
                    color = if (index % 2 == 0) Color(0xFF7C3AED).copy(alpha = alpha) else Color(0xFFC026D3).copy(alpha = alpha),
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            // Radial ambient glow behind logo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF9333EA).copy(alpha = 0.22f),
                        Color(0xFF581C87).copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = centerOffset,
                    radius = size.minDimension * 0.48f
                ),
                center = centerOffset,
                radius = size.minDimension * 0.48f
            )
        }

        // Center Content: Logo, Title, Subtitle
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
                .alpha(alphaAnim)
                .scale(scaleAnim),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Atelier Monogram Logo
            AtelierLogo(
                logoWidth = 106.dp,
                logoHeight = 90.dp,
                textScale = 1.35f,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Large Title: "Atelier Control"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Atelier ",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Control",
                    color = AccentMagenta,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle: "Sistema de Gestão"
            Text(
                text = "Sistema de Gestão",
                color = Color(0xFF94A3B8),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.2.sp
            )
        }

        // Bottom Content: 3 Animated Dots & Subtitle
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp)
                .alpha(alphaAnim),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 3 Progress / Pagination Dots (Cyan, Magenta, Purple)
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Dot 1: Cyan / Light Blue
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .scale(dot1Scale)
                        .background(Color(0xFF38BDF8), CircleShape)
                )

                // Dot 2: Bright Magenta
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .scale(dot2Scale)
                        .background(Color(0xFFE024B5), CircleShape)
                )

                // Dot 3: Purple / Violet
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .scale(dot3Scale)
                        .background(Color(0xFFA855F7), CircleShape)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Text
            Text(
                text = "Vendas • Produtos • Relatórios",
                color = Color(0xFF71717A),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.8.sp
            )
        }
    }
}
