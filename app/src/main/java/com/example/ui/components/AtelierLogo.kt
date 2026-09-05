package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AtelierLogo(
    modifier: Modifier = Modifier,
    logoWidth: androidx.compose.ui.unit.Dp = 64.dp,
    logoHeight: androidx.compose.ui.unit.Dp = 54.dp,
    showText: Boolean = true,
    textScale: Float = 1f
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Geometric Monogram "A" with Gold & Purple Ribbons (matching iOS splash)
        Canvas(
            modifier = Modifier
                .size(width = logoWidth, height = logoHeight)
        ) {
            val w = size.width
            val h = size.height

            val goldBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFDE68A), // Light Gold
                    Color(0xFFF59E0B), // Vibrant Gold
                    Color(0xFFD97706), // Warm Amber
                    Color(0xFFB45309)  // Deep Bronze Gold
                ),
                start = Offset(0f, 0f),
                end = Offset(w * 0.7f, h)
            )

            val purpleFoldBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFE879F9), // Light Pink/Magenta
                    Color(0xFFC084FC), // Lavender
                    Color(0xFF9333EA), // Royal Violet
                    Color(0xFF581C87)  // Deep Purple
                ),
                start = Offset(w * 0.4f, 0f),
                end = Offset(w, h)
            )

            // Outer Frame "A" Left Leg (Gold)
            val pathLeft = Path().apply {
                moveTo(w * 0.46f, h * 0.08f)
                lineTo(w * 0.16f, h * 0.92f)
                lineTo(w * 0.28f, h * 0.92f)
                lineTo(w * 0.50f, h * 0.28f)
                close()
            }
            drawPath(path = pathLeft, brush = goldBrush)

            // Outer Frame "A" Right Leg (Purple/Magenta fold ribbon)
            val pathRight = Path().apply {
                moveTo(w * 0.54f, h * 0.08f)
                lineTo(w * 0.84f, h * 0.92f)
                lineTo(w * 0.72f, h * 0.92f)
                lineTo(w * 0.50f, h * 0.28f)
                close()
            }
            drawPath(path = pathRight, brush = purpleFoldBrush)

            // Crossbar
            val crossBar = Path().apply {
                moveTo(w * 0.28f, h * 0.60f)
                lineTo(w * 0.72f, h * 0.60f)
                lineTo(w * 0.69f, h * 0.69f)
                lineTo(w * 0.31f, h * 0.69f)
                close()
            }
            drawPath(path = crossBar, brush = goldBrush)

            // Top apex connector
            drawCircle(
                brush = goldBrush,
                radius = w * 0.045f,
                center = Offset(w * 0.5f, h * 0.12f)
            )
        }

        if (showText) {
            Spacer(modifier = Modifier.height(2.dp * textScale))
            Text(
                text = "ATELIER",
                color = Color(0xFFD4AF37),
                fontSize = (11 * textScale).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (3 * textScale).sp,
                fontFamily = FontFamily.Serif
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp * textScale)
            ) {
                Box(
                    modifier = Modifier
                        .width(14.dp * textScale)
                        .height(1.dp)
                        .background(Color(0xFFB45309))
                )
                Text(
                    text = "CONTROL",
                    color = Color(0xFFA1A1AA),
                    fontSize = (8 * textScale).sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (2 * textScale).sp
                )
                Box(
                    modifier = Modifier
                        .width(14.dp * textScale)
                        .height(1.dp)
                        .background(Color(0xFFB45309))
                )
            }
        }
    }
}
