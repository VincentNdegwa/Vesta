package com.example.vesta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun PinSetupDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onPinConfirmed: (String) -> Unit
) {
    if (showDialog) {
        var pin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var isPinConfirmation by remember { mutableStateOf(false) }
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
                        text = if (isPinConfirmation) "Confirm PIN" else "Set PIN",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (isPinConfirmation) 
                            "Please enter your PIN again to confirm" 
                        else 
                            "Create a 4-digit PIN to secure your app",
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
                        val currentPin = if (isPinConfirmation) confirmPin else pin
                        for (i in 0 until 4) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(
                                        if (i < currentPin.length) MaterialTheme.colorScheme.primary
                                        else Color.Transparent,
                                        CircleShape
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (i < currentPin.length) 
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
                    
                    // PIN pad
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Row 1-3
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            PinButton(number = "1") { updatePin(it, isPinConfirmation, pin, confirmPin) { newPin, newConfirmPin ->
                                pin = newPin
                                confirmPin = newConfirmPin
                            }}
                            PinButton(number = "2") { updatePin(it, isPinConfirmation, pin, confirmPin) { newPin, newConfirmPin ->
                                pin = newPin
                                confirmPin = newConfirmPin
                            }}
                            PinButton(number = "3") { updatePin(it, isPinConfirmation, pin, confirmPin) { newPin, newConfirmPin ->
                                pin = newPin
                                confirmPin = newConfirmPin
                            }}
                        }
                        
                        // Row 4-6
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            PinButton(number = "4") { updatePin(it, isPinConfirmation, pin, confirmPin) { newPin, newConfirmPin ->
                                pin = newPin
                                confirmPin = newConfirmPin
                            }}
                            PinButton(number = "5") { updatePin(it, isPinConfirmation, pin, confirmPin) { newPin, newConfirmPin ->
                                pin = newPin
                                confirmPin = newConfirmPin
                            }}
                            PinButton(number = "6") { updatePin(it, isPinConfirmation, pin, confirmPin) { newPin, newConfirmPin ->
                                pin = newPin
                                confirmPin = newConfirmPin
                            }}
                        }
                        
                        // Row 7-9
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            PinButton(number = "7") { updatePin(it, isPinConfirmation, pin, confirmPin) { newPin, newConfirmPin ->
                                pin = newPin
                                confirmPin = newConfirmPin
                            }}
                            PinButton(number = "8") { updatePin(it, isPinConfirmation, pin, confirmPin) { newPin, newConfirmPin ->
                                pin = newPin
                                confirmPin = newConfirmPin
                            }}
                            PinButton(number = "9") { updatePin(it, isPinConfirmation, pin, confirmPin) { newPin, newConfirmPin ->
                                pin = newPin
                                confirmPin = newConfirmPin
                            }}
                        }
                        
                        // Row 0 and backspace
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            // Empty button
                            Box(modifier = Modifier.size(64.dp))
                            
                            // 0 button
                            PinButton(number = "0") { updatePin(it, isPinConfirmation, pin, confirmPin) { newPin, newConfirmPin ->
                                pin = newPin
                                confirmPin = newConfirmPin
                            }}
                            
                            // Backspace button
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (isPinConfirmation) {
                                            if (confirmPin.isNotEmpty()) {
                                                confirmPin = confirmPin.dropLast(1)
                                            }
                                        } else {
                                            if (pin.isNotEmpty()) {
                                                pin = pin.dropLast(1)
                                            }
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
        
        // Check if PIN is complete
        LaunchedEffect(pin, confirmPin, isPinConfirmation) {
            if (pin.length == 4 && !isPinConfirmation) {
                isPinConfirmation = true
            } else if (confirmPin.length == 4 && isPinConfirmation) {
                if (pin == confirmPin) {
                    // PIN confirmed
                    onPinConfirmed(pin)
                } else {
                    // PIN mismatch
                    errorMessage = "PINs don't match. Please try again."
                    confirmPin = ""
                }
            }
        }
    }
}

@Composable
fun PinButton(
    number: String,
    onClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .clickable { onClick(number) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun updatePin(
    digit: String,
    isPinConfirmation: Boolean,
    currentPin: String,
    currentConfirmPin: String,
    updatePins: (String, String) -> Unit
) {
    if (isPinConfirmation) {
        if (currentConfirmPin.length < 4) {
            updatePins(currentPin, currentConfirmPin + digit)
        }
    } else {
        if (currentPin.length < 4) {
            updatePins(currentPin + digit, currentConfirmPin)
        }
    }
}
