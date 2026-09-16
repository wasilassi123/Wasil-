package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.live.ZoyaState
import kotlinx.coroutines.launch

@Composable
fun ZoyaOrb(state: ZoyaState) {
    val radiusScale = remember { Animatable(1f) }
    val glowAlpha = remember { Animatable(0.5f) }

    val ring1Angle = remember { Animatable(0f) }
    val ring2Angle = remember { Animatable(120f) }
    val ring3Angle = remember { Animatable(240f) }
    val ring4Angle = remember { Animatable(45f) }

    LaunchedEffect(state) {
        when (state) {
            ZoyaState.IDLE -> {
                radiusScale.animateTo(1f, animationSpec = tween(1000))
                glowAlpha.animateTo(
                    targetValue = 0.4f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            ZoyaState.LISTENING -> {
                radiusScale.animateTo(1.1f, animationSpec = tween(500))
                glowAlpha.animateTo(
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            ZoyaState.THINKING -> {
                radiusScale.animateTo(1.05f, animationSpec = tween(400))
                glowAlpha.animateTo(
                    targetValue = 0.6f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
            ZoyaState.SPEAKING -> {
                radiusScale.animateTo(1.2f, animationSpec = tween(200))
                glowAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(300, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        launch {
            ring1Angle.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(6000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
        launch {
            ring2Angle.animateTo(
                targetValue = 360f + 120f,
                animationSpec = infiniteRepeatable(
                    animation = tween(7000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
        launch {
            ring3Angle.animateTo(
                targetValue = 360f + 240f,
                animationSpec = infiniteRepeatable(
                    animation = tween(5500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
        launch {
            ring4Angle.animateTo(
                targetValue = -360f + 45f,
                animationSpec = infiniteRepeatable(
                    animation = tween(8000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    Box(
        modifier = Modifier.size(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseRadius = size.minDimension / 4f
            val currentRadius = baseRadius * radiusScale.value

            val coreInnerColor = when (state) {
                ZoyaState.IDLE -> Color(0xFF80D8FF)
                ZoyaState.LISTENING -> Color(0xFFB388FF)
                ZoyaState.THINKING -> Color(0xFFFFD180)
                ZoyaState.SPEAKING -> Color(0xFF69F0AE)
            }

            val coreOuterColor = when (state) {
                ZoyaState.IDLE -> Color(0xFF00B0FF)
                ZoyaState.LISTENING -> Color(0xFF651FFF)
                ZoyaState.THINKING -> Color(0xFFFF9100)
                ZoyaState.SPEAKING -> Color(0xFF00E676)
            }

            // 1. Ambient Background Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(coreOuterColor.copy(alpha = glowAlpha.value * 0.5f), Color.Transparent),
                    center = center,
                    radius = currentRadius * 2.5f
                ),
                radius = currentRadius * 2.5f
            )

            // 2. The Glass Sphere (Core)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        coreInnerColor.copy(alpha = 0.8f),
                        coreOuterColor.copy(alpha = 0.9f),
                        Color.Black.copy(alpha = 0.5f)
                    ),
                    center = androidx.compose.ui.geometry.Offset(center.x - currentRadius * 0.3f, center.y - currentRadius * 0.3f),
                    radius = currentRadius * 1.2f
                ),
                radius = currentRadius
            )

            // Inner Core Highlight for 3D effect
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                center = androidx.compose.ui.geometry.Offset(center.x - currentRadius * 0.4f, center.y - currentRadius * 0.4f),
                radius = currentRadius * 0.3f
            )

            // 3. Neon Orbital Rings
            val ringRadiusX = currentRadius * 1.8f
            val ringRadiusY = currentRadius * 0.6f

            fun drawNeonRing(angle: Float, startColor: Color, endColor: Color, strokeWidth: Float) {
                rotate(angle, center) {
                    drawOval(
                        brush = Brush.sweepGradient(
                            colors = listOf(startColor, endColor, startColor, Color.Transparent, startColor),
                            center = center
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(center.x - ringRadiusX, center.y - ringRadiusY),
                        size = androidx.compose.ui.geometry.Size(ringRadiusX * 2, ringRadiusY * 2),
                        style = Stroke(width = strokeWidth)
                    )
                    drawOval(
                        color = startColor.copy(alpha = 0.3f),
                        topLeft = androidx.compose.ui.geometry.Offset(center.x - ringRadiusX, center.y - ringRadiusY),
                        size = androidx.compose.ui.geometry.Size(ringRadiusX * 2, ringRadiusY * 2),
                        style = Stroke(width = strokeWidth * 3)
                    )
                }
            }

            val speedMultiplier = if (state == ZoyaState.THINKING || state == ZoyaState.SPEAKING) 2f else 1f

            // Red/Pink Ring
            drawNeonRing(ring1Angle.value * speedMultiplier, Color(0xFFFF1744), Color(0xFFD50000), 4f)

            // Green/Yellow Ring
            drawNeonRing(ring2Angle.value * speedMultiplier, Color(0xFF00E676), Color(0xFF76FF03), 4f)

            // Blue/Cyan Ring
            drawNeonRing(ring3Angle.value * speedMultiplier, Color(0xFF00E5FF), Color(0xFF2979FF), 4f)

            // Outer subtle glass ring
            drawNeonRing(ring4Angle.value * speedMultiplier, Color.White.copy(alpha = 0.5f), Color.White.copy(alpha = 0.1f), 2f)

            // 4. Outer Glass Dome Reflection
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.3f)),
                    center = center,
                    radius = currentRadius * 2.2f
                ),
                radius = currentRadius * 2.2f,
                style = Stroke(width = 2f)
            )
        }
    }
}
