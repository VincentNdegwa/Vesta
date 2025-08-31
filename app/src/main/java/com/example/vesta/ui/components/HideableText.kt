package com.example.vesta.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
fun HideableText(
    text: String,
    modifier: Modifier = Modifier,
    hideAmounts: Boolean = false,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
) {
    val isAmountVisible = remember { mutableStateOf(!hideAmounts) }

    Text(
        text = text,
        modifier = modifier
            .then(if (hideAmounts) Modifier.clickable { isAmountVisible.value = !isAmountVisible.value } else Modifier)
            .then(if (!isAmountVisible.value) Modifier.blur(10.dp) else Modifier),
        style = style,
        color = color
    )
}
