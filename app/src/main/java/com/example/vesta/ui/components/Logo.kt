package com.example.vesta.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.vesta.R
import com.example.vesta.ui.theme.VestaTheme

@Composable
fun Logo(
    modifier: Modifier = Modifier,
    size: Int = 72
) {
    val context = LocalContext.current
    val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
    val bitmap = drawable?.toBitmap()?.asImageBitmap()
    
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Finvesta Logo",
            modifier = modifier.size(size.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LogoPreview() {
    VestaTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Logo(size = 48)
            Logo(size = 72)
            Logo(size = 96)
        }
    }
}
