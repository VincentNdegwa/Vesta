package com.example.vesta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun PinInputDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onPinEntered: (String) -> Unit,
    onPinValidated: (Boolean) -> Unit,
    showFingerprint: Boolean = false,
    onUseFingerprintClick: () -> Unit = {},
    title: String = "Enter PIN",
    subtitle: String = "Please enter your PIN to continue"
) {
    if (showDialog) {
        var pin by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        
        Dialog(
            onDismissRequest = onDismiss
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // PIN dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        for (i in 0 until 4) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(
                                        if (i < pin.length) MaterialTheme.colorScheme.primary
                                        else Color.Transparent,
                                        CircleShape
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (i < pin.length) 
                                            MaterialTheme.colorScheme.primary 
                                        else 
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                    
                    // Error message if any
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = errorMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Fingerprint option
                    if (showFingerprint) {
                        TextButton(
                            onClick = onUseFingerprintClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Use Fingerprint",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Use Fingerprint",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    // PIN pad
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Row 1-3
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            PinButton(number = "1") { 
                                if (pin.length < 4) pin += it
                                checkPinComplete(pin, onPinEntered, onPinValidated) 
                            }
                            PinButton(number = "2") { 
                                if (pin.length < 4) pin += it
                                checkPinComplete(pin, onPinEntered, onPinValidated) 
                            }
                            PinButton(number = "3") { 
                                if (pin.length < 4) pin += it
                                checkPinComplete(pin, onPinEntered, onPinValidated)  
                            }
                        }
                        
                        // Row 4-6
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            PinButton(number = "4") { 
                                if (pin.length < 4) pin += it
                                checkPinComplete(pin, onPinEntered, onPinValidated)  
                            }
                            PinButton(number = "5") { 
                                if (pin.length < 4) pin += it
                                checkPinComplete(pin, onPinEntered, onPinValidated)  
                            }
                            PinButton(number = "6") { 
                                if (pin.length < 4) pin += it
                                checkPinComplete(pin, onPinEntered, onPinValidated)  
                            }
                        }
                        
                        // Row 7-9
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            PinButton(number = "7") { 
                                if (pin.length < 4) pin += it
                                checkPinComplete(pin, onPinEntered, onPinValidated)  
                            }
                            PinButton(number = "8") { 
                                if (pin.length < 4) pin += it
                                checkPinComplete(pin, onPinEntered, onPinValidated)  
                            }
                            PinButton(number = "9") { 
                                if (pin.length < 4) pin += it
                                checkPinComplete(pin, onPinEntered, onPinValidated)  
                            }
                        }
                        
                        // Row 0 and backspace
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Empty button
                            Box(modifier = Modifier.size(64.dp))
                            
                            // 0 button
                            PinButton(number = "0") { 
                                if (pin.length < 4) pin += it
                                checkPinComplete(pin, onPinEntered, onPinValidated)  
                            }
                            
                            // Backspace button
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (pin.isNotEmpty()) {
                                            pin = pin.dropLast(1)
                                        }
                                        errorMessage = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backspace,
                                    contentDescription = "Backspace",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun checkPinComplete(pin: String, onPinEntered: (String) -> Unit, onPinValidated: (Boolean) -> Unit) {
    if (pin.length == 4) {
        onPinEntered(pin)
    }
}
