package com.example.vesta.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier
) {
    // Create a gradient background for a modern look
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surface
    
    // Animate text
    var dotCount by remember { mutableStateOf(0) }
    var visible by remember { mutableStateOf(true) }
    
    // Create animated typing effect for dots
    LaunchedEffect(Unit) {
        while (true) {
            delay(500)
            dotCount = (dotCount + 1) % 4
        }
    }
    
    // Create pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    // Background blur animation
    val blurRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "blur"
    )
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.8f),
                        primaryColor.copy(alpha = 0.9f),
                        secondaryColor.copy(alpha = 0.6f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative blurred circles in the background
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius.dp)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            // Draw decorative circles
            drawCircle(
                color = tertiaryColor.copy(alpha = 0.15f),
                radius = canvasWidth * 0.3f,
                center = Offset(canvasWidth * 0.8f, canvasHeight * 0.2f)
            )
            
            drawCircle(
                color = secondaryColor.copy(alpha = 0.1f),
                radius = canvasWidth * 0.4f,
                center = Offset(canvasWidth * 0.1f, canvasHeight * 0.8f)
            )
        }
        
        // Modern loading container
        Surface(
            modifier = Modifier
                .width(280.dp)
                .scale(scale)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(28.dp),
                    spotColor = primaryColor.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(28.dp),
            color = surfaceColor.copy(alpha = 0.9f),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Modern circular progress
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(64.dp)
                        .padding(4.dp),
                    color = primaryColor,
                    trackColor = primaryColor.copy(alpha = 0.15f),
                    strokeWidth = 5.dp,
                    strokeCap = StrokeCap.Round
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Modern pulsing dots animation
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "Loading" + ".".repeat(dotCount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Preparing your dashboard",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun WaveLoadingAnimation(
    modifier: Modifier = Modifier,
    waveColor: Color = MaterialTheme.colorScheme.primary
) {
    val waves = 5
    
    val infiniteTransition = rememberInfiniteTransition(label = "wave_animation")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(waves) { index ->
            val delay = index * 100
            
            val waveHeight by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1000,
                        easing = FastOutSlowInEasing
                    ),
                    initialStartOffset = StartOffset(delay),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wave_$index"
            )
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((20 * waveHeight).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        waveColor.copy(
                            alpha = 0.4f + (0.6f * waveHeight)
                        )
                    )
            )
        }
    }
}

// Keep the original dots animation as an option
@Composable
fun DotsLoadingAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_animation")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier
    ) {
        // Three dots with different phase animations
        repeat(3) { index ->
            val delay = index * 150
            
            // Scale animation
            val scaleAnimation by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 800
                        0.6f at 0 with LinearEasing
                        1f at 400 with LinearEasing
                        0.6f at 800 with LinearEasing
                    },
                    initialStartOffset = StartOffset(delay)
                ),
                label = "loading_scale_$index"
            )
            
            // Alpha animation
            val alphaAnimation by infiniteTransition.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 800
                        0.4f at 0 with LinearEasing
                        1f at 400 with LinearEasing
                        0.4f at 800 with LinearEasing
                    },
                    initialStartOffset = StartOffset(delay)
                ),
                label = "loading_alpha_$index"
            )

            // Animated dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .scale(scaleAnimation)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(
                            alpha = alphaAnimation
                        )
                    )
                    .shadow(
                        elevation = 2.dp,
                        shape = CircleShape,
                        spotColor = MaterialTheme.colorScheme.primary,
                        ambientColor = MaterialTheme.colorScheme.primary
                    )
            )
        }
    }
}
