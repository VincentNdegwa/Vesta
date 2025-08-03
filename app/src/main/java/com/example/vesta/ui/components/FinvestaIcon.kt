package com.example.vesta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vesta.ui.theme.VestaTheme

@Composable
fun FinvestaIcon(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White.copy(alpha = 0.2f),
    textColor: Color = Color.White,
    size: Int = 80
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(
                backgroundColor,
                RoundedCornerShape((size * 0.25f).dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "F",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (size * 0.5f).sp
            ),
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FinvestaIconPreview() {
    VestaTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FinvestaIcon()
            FinvestaIcon(
                backgroundColor = MaterialTheme.colorScheme.primary,
                textColor = MaterialTheme.colorScheme.onPrimary,
                size = 60
            )
        }
    }
}
