package com.example.vesta.ui.savings.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vesta.data.local.entities.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SmartGoalCard(
    goal: SavingsGoalEntity,
    onContribute: () -> Unit,
    onManageRules: () -> Unit,
    onShowDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val currency = NumberFormat.getCurrencyInstance()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Header with risk indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = goal.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (goal.riskLevel) {
                                1 -> Color.Green
                                2 -> Color(0xFFFFA500) // Orange
                                else -> Color.Red
                            }
                        )
                )
            }

            // Progress section
            LinearProgressIndicator(
                progress = (goal.currentAmount / goal.targetAmount).toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            // Amount progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currency.format(goal.currentAmount),
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = currency.format(goal.targetAmount),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Smart metrics
                SmartMetricsSection(goal)
                
                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    OutlinedButton(onClick = onContribute) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
//                        Text("Contribute")
                    }
                    
                    OutlinedButton(onClick = onManageRules) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
//                        Text("Rules")
                    }
                    
                    OutlinedButton(onClick = onShowDetails) {
                        Icon(Icons.Default.Analytics, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
//                        Text("Details")
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartMetricsSection(goal: SavingsGoalEntity) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        // Sustainability score
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sustainability",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${goal.sustainabilityScore}%",
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    goal.sustainabilityScore ?: 0 >= 80 -> Color.Green
                    goal.sustainabilityScore ?: 0 >= 50 -> Color(0xFFFFA500)
                    else -> Color.Red
                }
            )
        }

        // Next contribution suggestion
        goal.nextSuggestedContribution?.let { suggested ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Suggested Next",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = NumberFormat.getCurrencyInstance().format(suggested),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Achievement badges
        if (goal.achievements.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                goal.achievements.take(3).forEach { achievement ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = achievement,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                
                if (goal.achievements.size > 3) {
                    Text(
                        text = "+${goal.achievements.size - 3}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun SavingsRuleItem(
    rule: SavingsRuleEntity,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (rule.type) {
                        RuleType.PERCENTAGE_OF_INCOME -> "${rule.percentage}% of income"
                        RuleType.FIXED_AMOUNT -> NumberFormat.getCurrencyInstance().format(rule.amount)
                        RuleType.ROUND_UP -> "Round up savings"
                        RuleType.SMART_SAVE -> "Smart save"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggle
                )
                
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit rule")
                }
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete rule")
                }
            }
        }
    }
}
