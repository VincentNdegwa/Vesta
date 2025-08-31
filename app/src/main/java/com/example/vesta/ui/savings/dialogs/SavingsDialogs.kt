package com.example.vesta.ui.savings.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.vesta.data.local.entities.*
import com.example.vesta.data.repositories.GoalProgress
import com.example.vesta.ui.savings.components.SavingsRuleItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ManageRulesDialog(
    goalId: String,
    rules: List<SavingsRuleEntity>,
    onCreateRule: (SavingsRuleEntity) -> Unit,
    onToggleRule: (String, Boolean) -> Unit,
    onEditRule: (SavingsRuleEntity) -> Unit,
    onDeleteRule: (SavingsRuleEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddRuleDialog by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<SavingsRuleEntity?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Savings Rules",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(rules) { rule ->
                        SavingsRuleItem(
                            rule = rule,
                            onToggle = { enabled -> onToggleRule(rule.id, enabled) },
                            onEdit = { ruleToEdit = rule },
                            onDelete = { onDeleteRule(rule) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showAddRuleDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Rule")
                }
            }
        }
    }

    if (showAddRuleDialog) {
        AddEditRuleDialog(
            goalId = goalId,
            existingRule = null,
            onSave = { 
                onCreateRule(it)
                showAddRuleDialog = false
            },
            onDismiss = { showAddRuleDialog = false }
        )
    }

    ruleToEdit?.let { rule ->
        AddEditRuleDialog(
            goalId = goalId,
            existingRule = rule,
            onSave = { 
                onEditRule(it)
                ruleToEdit = null
            },
            onDismiss = { ruleToEdit = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRuleDialog(
    goalId: String,
    existingRule: SavingsRuleEntity?,
    onSave: (SavingsRuleEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(existingRule?.type ?: RuleType.PERCENTAGE_OF_INCOME) }
    var frequency by remember { mutableStateOf(existingRule?.frequency ?: RuleFrequency.EVERY_INCOME) }
    var amount by remember { mutableStateOf(existingRule?.amount?.toString() ?: "") }
    var percentage by remember { mutableStateOf(existingRule?.percentage?.toString() ?: "") }
    var description by remember { mutableStateOf(existingRule?.description ?: "") }
    var minimumIncome by remember { mutableStateOf(existingRule?.minimumIncomeThreshold?.toString() ?: "") }
    var maximumContribution by remember { mutableStateOf(existingRule?.maximumContribution?.toString() ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = if (existingRule == null) "Add Rule" else "Edit Rule",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Rule type selector
                ExposedDropdownMenuBox(
                    expanded = false,
                    onExpandedChange = { }
                ) {
                    OutlinedTextField(
                        value = type.name,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Rule Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = false) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                when (type) {
                    RuleType.PERCENTAGE_OF_INCOME -> {
                        OutlinedTextField(
                            value = percentage,
                            onValueChange = { percentage = it },
                            label = { Text("Percentage") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    RuleType.FIXED_AMOUNT -> {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            label = { Text("Amount") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Optional thresholds
                OutlinedTextField(
                    value = minimumIncome,
                    onValueChange = { minimumIncome = it },
                    label = { Text("Minimum Income (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = maximumContribution,
                    onValueChange = { maximumContribution = it },
                    label = { Text("Maximum Contribution (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                            val rule = SavingsRuleEntity(
                                id = existingRule?.id ?: UUID.randomUUID().toString(),
                                goalId = goalId,
                                type = type,
                                frequency = frequency,
                                amount = amount.toDoubleOrNull(),
                                percentage = percentage.toDoubleOrNull(),
                                minimumIncomeThreshold = minimumIncome.toDoubleOrNull(),
                                maximumContribution = maximumContribution.toDoubleOrNull(),
                                description = description
                            )
                            onSave(rule)
                        },
                        enabled = when (type) {
                            RuleType.PERCENTAGE_OF_INCOME -> percentage.isNotEmpty()
                            RuleType.FIXED_AMOUNT -> amount.isNotEmpty()
                            else -> true
                        } && description.isNotEmpty()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun GoalDetailsDialog(
    goal: SavingsGoalEntity,
    progress: GoalProgress?,
    contributions: List<SavingsContributionEntity>,
    onDismiss: () -> Unit
) {
    val currency = NumberFormat.getCurrencyInstance()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Progress metrics
                progress?.let { p ->
                    Column {
//                        MetricRow("Progress", "${(p.progressPercentage * 100).toInt()}%")
                        MetricRow("Current Amount", currency.format(p.currentAmount))
                        MetricRow("Target Amount", currency.format(p.targetAmount))
                        MetricRow("Remaining", currency.format(p.remainingAmount))
                        MetricRow("On Track", if (p.isOnTrack) "Yes" else "No")
//                        p.projectedCompletionDate?.let {
//                            MetricRow("Projected Completion", dateFormat.format(Date(it)))
//                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Recent contributions
                Text(
                    text = "Recent Contributions",
                    style = MaterialTheme.typography.titleMedium
                )
                
                LazyColumn(
                    modifier = Modifier.height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(contributions.take(10)) { contribution ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = dateFormat.format(Date(contribution.createdAt)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = currency.format(contribution.amount),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
