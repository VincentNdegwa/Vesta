package com.example.vesta.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.vesta.R

@Composable
fun LoadingScreen(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        // Branded card container for loading indicator
        Card(
            modifier = Modifier
                .width(240.dp)
                .padding(16.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 6.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App logo or icon could go here
                /*
                Image(
                    painter = painterResource(id = R.drawable.app_logo),
                    contentDescription = "Vesta Logo",
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                */
                
                // The animated dots loading indicator
                DotsLoadingAnimation()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Loading",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

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
