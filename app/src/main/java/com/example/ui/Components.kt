package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// Reusable custom cybernetic styling tokens
val DarkBg = Color(0xFF050608) // Matches HTML pure dark bg
val PanelBg = Color(0xFF0C0F17) // Slightly dark base behind glass
val BorderColor = Color(0x1FFFFFFF) // White 12% translucent border
val LedOffColor = Color(0xFF141824)
val LedGreen = Color(0xFF10B981) // Emerald Green from HTML
val LedYellow = Color(0xFFEAB308) // Vibrant Amber Yellow
val LedRed = Color(0xFFEF4444) // Intense Warm Crimson Red

/**
 * A highly responsive, custom-drawn Solid State Logic rotary knob.
 */
@Composable
fun StudioKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedRange<Float> = 0f..1f,
    label: String = "",
    accentColor: Color = Color.Cyan
) {
    var dragAccum by remember { mutableStateOf(0f) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { dragAccum = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            // Vertical drag mimics standard audio knob behavior
                            dragAccum -= dragAmount.y
                            val sensitivity = 0.005f
                            val newValue = (value + dragAccum * sensitivity).coerceIn(range.start, range.endInclusive)
                            onValueChange(newValue)
                        }
                    )
                }
                .testTag("knob_${label.lowercase()}")
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f * 0.82f
                
                // Draw bottom shadow glow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f),
                    radius = radius + 3.dp.toPx(),
                    center = center
                )
                
                // Outer ring track
                drawArc(
                    color = LedOffColor,
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(center.x - radius, center.y - radius)
                )

                // Computed active arc
                val normalizedVal = (value - range.start) / (range.endInclusive - range.start)
                val activeSweep = normalizedVal * 270f
                
                drawArc(
                    color = accentColor,
                    startAngle = 135f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(center.x - radius, center.y - radius)
                )

                // Draw central knob metal cap
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2C3549), Color(0xFF141822)),
                        center = center,
                        radius = radius * 0.8f
                    ),
                    radius = radius * 0.8f,
                    center = center
                )

                // Radial steel pointer line representing the current potentiometer position
                val pointerAngle = 135f + activeSweep
                val radians = Math.toRadians(pointerAngle.toDouble())
                val pointerLength = radius * 0.72f
                val endX = center.x + pointerLength * cos(radians).toFloat()
                val endY = center.y + pointerLength * sin(radians).toFloat()
                
                drawLine(
                    color = accentColor,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 3.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
        
        if (label.isNotEmpty()) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.Gray,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

/**
 * Visual LED Level VU Meter that dynamically responds to real-time decibels from AudioEngine.
 */
@Composable
fun VuMeter(
    level: Float, // 0.0 to 1.0 peak
    modifier: Modifier = Modifier,
    label: String = "L"
) {
    val totalLeds = 14
    
    // Animate meter fallback smoothly
    val animatedLevel by animateFloatAsState(
        targetValue = level,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(36.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.LightGray,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF0F121B))
                .padding(vertical = 4.dp, horizontal = 6.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // LED bars drawn from top (highest Peak) to bottom (lowest background DB)
            for (i in totalLeds - 1 downTo 0) {
                val ledThreshold = i.toFloat() / totalLeds
                val isActive = animatedLevel >= ledThreshold
                
                val ledColor = when {
                    i >= 12 -> if (isActive) LedRed else LedRed.copy(alpha = 0.15f)
                    i >= 9 -> if (isActive) LedYellow else LedYellow.copy(alpha = 0.15f)
                    else -> if (isActive) LedGreen else LedGreen.copy(alpha = 0.15f)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(ledColor)
                )
            }
        }
    }
}

/**
 * Responsive Realtime Frequency Canvas Spectrum Analyzer
 */
@Composable
fun RealtimeSpectrumVisualizer(
    buffer: FloatArray,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.Cyan
) {
    val infiniteTransition = rememberInfiniteTransition()
    val glowAnimation by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 11.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF05070A))
    ) {
        val width = size.width
        val height = size.height
        val barCount = buffer.size.coerceAtLeast(32)
        val spacing = 3f
        val rawWidth = width / barCount
        val barWidth = rawWidth - spacing

        // Back grid design (Studio spectrum look)
        val numGridLines = 7
        for (g in 1 until numGridLines) {
            val gridX = (width / numGridLines) * g
            drawLine(
                color = Color(0xFF141824).copy(alpha = 0.65f),
                start = Offset(gridX, 0f),
                end = Offset(gridX, height),
                strokeWidth = 1f
            )
            
            val gridY = (height / numGridLines) * g
            drawLine(
                color = Color(0xFF141824).copy(alpha = 0.65f),
                start = Offset(0f, gridY),
                end = Offset(width, gridY),
                strokeWidth = 1f
            )
        }

        // Draw RGB-reactive equalizer bars with glow
        for (idx in 0 until barCount) {
            val amplitude = if (idx < buffer.size) buffer[idx] else 0f
            // Enforce smooth minimum visual bar amplitude for live flicker feel
            val barAmp = ((amplitude * 0.95f) + 0.05f).coerceIn(0.04f, 1.0f)
            val barHeight = barAmp * height
            
            val xOffset = idx * rawWidth + spacing / 2
            val yOffset = height - barHeight
            
            // Neon gradient shading
            val barBrush = Brush.verticalGradient(
                colors = listOf(
                    accentColor,
                    accentColor.copy(alpha = 0.65f),
                    accentColor.copy(alpha = 0.15f)
                ),
                startY = yOffset,
                endY = height
            )

            // Draw primary bar
            drawRect(
                brush = barBrush,
                topLeft = Offset(xOffset, yOffset),
                size = Size(barWidth, barHeight),
                alpha = 0.92f
            )
        }
    }
}

/**
 * Neat, glowing glassmorphic frame card
 */
@Composable
fun GlassStudioCard(
    modifier: Modifier = Modifier,
    accentColor: Color = Color.Cyan,
    borderColor: Color = Color.White.copy(alpha = 0.12f),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            // Layer 1: Dark translucent backplate
            .background(Color(0xFF07090E).copy(alpha = 0.55f))
            // Layer 2: Frosted glass specularity highlight
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.02f)
                    )
                )
            )
            .drawBehind {
                // Fiber-optic glowing neon side accent line representing signal activity flow
                drawLine(
                    color = accentColor.copy(alpha = 0.45f),
                    start = Offset(1.5.dp.toPx(), 6.dp.toPx()),
                    end = Offset(1.5.dp.toPx(), size.height - 6.dp.toPx()),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            .padding(16.dp)
    ) {
        content()
    }
}
