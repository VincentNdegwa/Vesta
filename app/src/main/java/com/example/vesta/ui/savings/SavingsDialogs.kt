package com.example.vesta.ui.savings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.*

@Composable
fun AddSavingsGoalDialog(
    onDismiss: () -> Unit,
    onCreateGoal: (name: String, amount: Double, deadline: Long, autoContribute: Boolean, autoAmount: Double?, autoPercentage: Double?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var deadlineMonths by remember { mutableStateOf("12") }
    var autoContribute by remember { mutableStateOf(false) }
    var autoAmountText by remember { mutableStateOf("") }
    var autoPercentageText by remember { mutableStateOf("") }
    var usePercentage by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Create New Savings Goal",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Goal Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Target Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = deadlineMonths,
                    onValueChange = { deadlineMonths = it },
                    label = { Text("Duration (months)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Auto-contributions")
                    Switch(
                        checked = autoContribute,
                        onCheckedChange = { autoContribute = it }
                    )
                }

                if (autoContribute) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Use percentage of income")
                        Switch(
                            checked = usePercentage,
                            onCheckedChange = { usePercentage = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (usePercentage) {
                        OutlinedTextField(
                            value = autoPercentageText,
                            onValueChange = { autoPercentageText = it },
                            label = { Text("Percentage of Income") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        OutlinedTextField(
                            value = autoAmountText,
                            onValueChange = { autoAmountText = it },
                            label = { Text("Monthly Contribution") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: return@Button
                            val months = deadlineMonths.toIntOrNull() ?: return@Button
                            val deadline = Calendar.getInstance().apply {
                                add(Calendar.MONTH, months)
                            }.timeInMillis

                            val autoAmount = if (!usePercentage) autoAmountText.toDoubleOrNull() else null
                            val autoPercentage = if (usePercentage) autoPercentageText.toDoubleOrNull() else null

                            onCreateGoal(
                                name,
                                amount,
                                deadline,
                                autoContribute,
                                autoAmount,
                                autoPercentage
                            )
                        },
                        enabled = name.isNotBlank() && 
                                 amountText.isNotBlank() && 
                                 deadlineMonths.isNotBlank() &&
                                 (!autoContribute || 
                                    (usePercentage && autoPercentageText.isNotBlank()) ||
                                    (!usePercentage && autoAmountText.isNotBlank())
                                 )
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun ContributeDialog(
    goalName: String,
    onDismiss: () -> Unit,
    onContribute: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Contribute to $goalName",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val amount = amountText.toDoubleOrNull() ?: return@Button
                            onContribute(amount)
                        },
                        enabled = amountText.isNotBlank()
                    ) {
                        Text("Contribute")
                    }
                }
            }
        }
    }
}
