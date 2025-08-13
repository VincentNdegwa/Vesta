package com.example.vesta.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AutoLockTimeoutDropdown(
    currentTimeout: String,
    timeoutOptions: List<String>,
    onTimeoutSelected: (String) -> Unit,
    menuContent: @Composable (onClick: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Box {
        // Menu display
        menuContent { expanded = true }
        
        // Dropdown
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.5f)
        ) {
            timeoutOptions.forEach { timeout ->
                DropdownMenuItem(
                    text = { Text(timeout) },
                    onClick = {
                        onTimeoutSelected(timeout)
                        expanded = false
                    }
                )
            }
        }
    }
}
