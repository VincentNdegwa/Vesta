package com.example.vesta.data.services

import com.example.vesta.data.local.dao.SavingsContributionDao
import com.example.vesta.data.local.dao.SavingsGoalDao
import com.example.vesta.data.local.dao.SavingsRuleDao
import com.example.vesta.data.local.dao.TransactionDao
import com.example.vesta.data.local.entities.*
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartSavingsService @Inject constructor(
    private val savingsGoalDao: SavingsGoalDao,
    private val savingsRuleDao: SavingsRuleDao,
    private val savingsContributionDao: SavingsContributionDao,
    private val transactionDao: TransactionDao
) {
    suspend fun processSavingsRules(
        userId: String,
        currentIncome: Double,
        monthlyIncome: Double,
        monthlyExpenses: Double
    ) {
        // Process rules that are ready to execute
        val now = System.currentTimeMillis()
        
        // Process income-based rules
        processIncomeBasedRules(currentIncome)
        
        // Process scheduled rules
        processScheduledRules(now)
        
        // Update goal analytics
        updateGoalAnalytics(userId, monthlyIncome, monthlyExpenses)
    }

    private suspend fun processIncomeBasedRules(currentIncome: Double) {
        val incomeRules = savingsRuleDao.getReadyToExecuteRules(
            RuleFrequency.EVERY_INCOME.name,
            System.currentTimeMillis()
        )

        for (rule in incomeRules) {
            // Skip if income is below threshold
            if (rule.minimumIncomeThreshold != null && currentIncome < rule.minimumIncomeThreshold) {
                continue
            }

            val contributionAmount = when (rule.type) {
                RuleType.PERCENTAGE_OF_INCOME -> {
                    val amount = (currentIncome * (rule.percentage ?: 0.0)) / 100.0
                    rule.maximumContribution?.let { maxAmount ->
                        amount.coerceAtMost(maxAmount)
                    } ?: amount
                }
                RuleType.FIXED_AMOUNT -> rule.amount ?: 0.0
                else -> 0.0
            }

            if (contributionAmount > 0) {
                savingsGoalDao.updateGoalProgress(rule.goalId, contributionAmount)
                savingsGoalDao.updateContributionStreak(rule.goalId)
                savingsRuleDao.updateLastExecuted(rule.id, System.currentTimeMillis())
            }
        }
    }

    private suspend fun processScheduledRules(currentTime: Long) {
        val frequencies = listOf(
            RuleFrequency.DAILY,
            RuleFrequency.WEEKLY,
            RuleFrequency.MONTHLY
        )

        for (frequency in frequencies) {
            val rules = savingsRuleDao.getReadyToExecuteRules(frequency.name, currentTime)
            
            for (rule in rules) {
                if (rule.type == RuleType.FIXED_AMOUNT && rule.amount != null) {
                    savingsGoalDao.updateGoalProgress(rule.goalId, rule.amount)
                    savingsGoalDao.updateContributionStreak(rule.goalId)
                    
                    // Schedule next execution
                    val nextScheduled = calculateNextScheduledTime(currentTime, frequency)
                    savingsRuleDao.updateLastExecuted(rule.id, currentTime)
                    savingsRuleDao.updateNextScheduled(rule.id, nextScheduled)
                }
            }
        }
    }

    suspend fun updateMetricsAfterContribution(goalId: String, userId: String) {

        savingsGoalDao.getSavingsGoalById(goalId).collect { goal ->
            if (goal != null && goal.status != GoalStatus.COMPLETED) {
                val contributions = savingsContributionDao.getContributionsForGoal(goalId).first()
                val recentContributions = contributions.takeLast(3)
                val averageRecentContribution = if (recentContributions.isNotEmpty()) {
                    recentContributions.map { it.amount }.average()
                } else 0.0

                // Get actual income and expenses
                val monthlyIncome = transactionDao.getAverageMonthlyIncome(userId) ?: 0.0
                val monthlyExpenses = transactionDao.getAverageMonthlyExpenses(userId) ?: 0.0
                
                // Calculate time progress
                val timeProgress = (System.currentTimeMillis() - goal.startDate).toDouble() / 
                                 (goal.deadline - goal.startDate)
                val amountProgress = goal.currentAmount / goal.targetAmount
                val disposableIncome = monthlyIncome - monthlyExpenses

                val timeElapsed = System.currentTimeMillis() - goal.startDate
                val totalDuration = goal.deadline - goal.startDate
                val expectedProgress = (timeElapsed.toDouble() / totalDuration) * goal.targetAmount
                val actualProgress = goal.currentAmount
                val progressRate = if (expectedProgress > 0) {
                    (actualProgress / expectedProgress).coerceIn(0.0, 1.0)
                } else 0.0

                // Update goal metrics
                savingsGoalDao.updateSmartMetrics(
                    goalId = goalId,
                    score = calculateSustainabilityScore(
                        disposableIncome = disposableIncome,
                        goal = goal,
                        contributionTrend = averageRecentContribution,
                        timeProgress = timeProgress,
                        amountProgress = amountProgress
                    ),
                    suggestedAmount = calculateRequiredMonthlyAmount(goal),
                    riskLevel = calculateRiskLevel(timeProgress, amountProgress),
                    projectedDate = calculateProjectedCompletionDate(goal, averageRecentContribution),
                    progressRate = amountProgress / timeProgress.coerceAtLeast(0.01),
                    nextContribution = calculateNextSuggestedContribution(
                        goal = goal,
                        disposableIncome = disposableIncome,
                        progressRate = progressRate
                    ),
                    avgContribution = if (goal.totalContributions > 0) {
                        goal.currentAmount / ((System.currentTimeMillis() - goal.startDate) / 
                                           (30.0 * 24 * 60 * 60 * 1000))
                    } else 0.0
                )
            }
        }
    }

    private fun calculateNextScheduledTime(currentTime: Long, frequency: RuleFrequency): Long {
        val current = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(currentTime),
            ZoneId.systemDefault()
        )

        val next = when (frequency) {
            RuleFrequency.DAILY -> current.plusDays(1)
            RuleFrequency.WEEKLY -> current.plusWeeks(1)
            RuleFrequency.MONTHLY -> current.plusMonths(1)
            else -> current
        }

        return next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    private suspend fun updateGoalAnalytics(
        userId: String,
        monthlyIncome: Double,
        monthlyExpenses: Double
    ) {
        // Get actual income and expenses from transactions if not provided
        val actualMonthlyIncome = if (monthlyIncome <= 0) {
            transactionDao.getAverageMonthlyIncome(userId) ?: 0.0
        } else monthlyIncome
        
        val actualMonthlyExpenses = if (monthlyExpenses <= 0) {
            transactionDao.getAverageMonthlyExpenses(userId) ?: 0.0
        } else monthlyExpenses

        // Update all goals, but handle active and completed goals differently
        val goals = savingsGoalDao.getAllSavingsGoals(userId)
        goals.collect { allGoals ->
            for (goal in allGoals) {
                val isCompleted = goal.currentAmount >= goal.targetAmount
                
                if (isCompleted) {
                    savingsGoalDao.updateSmartMetrics(
                        goalId = goal.id,
                        score = 100,
                        suggestedAmount = 0.0,
                        riskLevel = 1,
                        projectedDate = System.currentTimeMillis(),
                        progressRate = 1.0,
                        nextContribution = 0.0,
                        avgContribution = goal.currentAmount / (
                            (System.currentTimeMillis() - goal.startDate) / 
                            (30.0 * 24 * 60 * 60 * 1000)
                        )
                    )
                    
                    if (goal.status != GoalStatus.COMPLETED) {
                        savingsGoalDao.updateSavingsGoal(goal.copy(status = GoalStatus.COMPLETED))
                    }
                    
                    continue
                }

                val timeElapsed = System.currentTimeMillis() - goal.startDate
                val totalDuration = goal.deadline - goal.startDate
                val expectedProgress = (timeElapsed.toDouble() / totalDuration) * goal.targetAmount
                val actualProgress = goal.currentAmount
                val progressRate = if (expectedProgress > 0) {
                    (actualProgress / expectedProgress).coerceIn(0.0, 1.0)
                } else 0.0

                // Calculate remaining amount and time
                val remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
                val remainingTime = (goal.deadline - System.currentTimeMillis())
                    .coerceAtLeast(1L) // Avoid division by zero

                // Calculate sustainability score (0-100)
                val disposableIncome = monthlyIncome - monthlyExpenses
                val requiredMonthlyAmount = if (remainingTime > 0) {
                    remainingAmount / (remainingTime / (30.0 * 24 * 60 * 60 * 1000))
                } else 0.0
                
                val sustainabilityScore = if (remainingAmount <= 0) {
                    100 // Goal amount reached
                } else if (requiredMonthlyAmount <= 0) {
                    0 // Invalid required amount
                } else {
                    ((disposableIncome / requiredMonthlyAmount) * 100)
                        .coerceIn(0.0, 100.0)
                        .toInt()
                }

                // Calculate risk level
                val riskLevel = calculateRiskLevel(
                    timeProgress = timeElapsed.toDouble() / totalDuration,
                    amountProgress = actualProgress / goal.targetAmount
                )

                // Calculate projected completion
                val avgMonthlyContribution = if (goal.totalContributions > 0) {
                    goal.currentAmount / (timeElapsed.coerceAtLeast(1) / (30.0 * 24 * 60 * 60 * 1000))
                } else null

                val projectedDate = if (remainingAmount <= 0) {
                    System.currentTimeMillis() // Already completed
                } else {
                    avgMonthlyContribution?.let {
                        if (it > 0) {
                            val monthsNeeded = remainingAmount / it
                            System.currentTimeMillis() + (monthsNeeded * 30 * 24 * 60 * 60 * 1000).toLong()
                        } else goal.deadline
                    } ?: goal.deadline
                }

                // Calculate next suggested contribution
                val nextSuggestedContribution = if (remainingAmount <= 0) {
                    0.0 // No more contributions needed
                } else {
                    calculateNextSuggestedContribution(
                        goal,
                        disposableIncome,
                        progressRate
                    )
                }

                // Update goal metrics
                savingsGoalDao.updateSmartMetrics(
                    goalId = goal.id,
                    score = sustainabilityScore,
                    suggestedAmount = requiredMonthlyAmount.coerceAtLeast(0.0),
                    riskLevel = riskLevel,
                    projectedDate = projectedDate,
                    progressRate = progressRate,
                    nextContribution = nextSuggestedContribution,
                    avgContribution = avgMonthlyContribution ?: 0.0
                )
            }
        }
    }

    private fun calculateSustainabilityScore(
        disposableIncome: Double,
        goal: SavingsGoalEntity,
        contributionTrend: Double,
        timeProgress: Double,
        amountProgress: Double
    ): Int {
        val remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
        val remainingTime = (goal.deadline - System.currentTimeMillis())
            .coerceAtLeast(1L) // Avoid division by zero

        val requiredMonthlyAmount = if (remainingTime > 0) {
            remainingAmount / (remainingTime / (30.0 * 24 * 60 * 60 * 1000))
        } else 0.0

        if (remainingAmount <= 0) return 100 // Goal amount reached
        if (requiredMonthlyAmount <= 0) return 0 // Invalid required amount

        // Calculate base sustainability score
        val baseScore = ((disposableIncome / requiredMonthlyAmount) * 60).coerceIn(0.0, 60.0)
        
        // Add contribution trend score (up to 20 points)
        val trendScore = ((contributionTrend / requiredMonthlyAmount) * 20).coerceIn(0.0, 20.0)
        
        // Add progress vs time score (up to 20 points)
        val progressScore = ((amountProgress / timeProgress.coerceAtLeast(0.01)) * 20).coerceIn(0.0, 20.0)

        return (baseScore + trendScore + progressScore).toInt()
    }

    private fun calculateRequiredMonthlyAmount(goal: SavingsGoalEntity): Double {
        val remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
        val remainingTime = (goal.deadline - System.currentTimeMillis())
            .coerceAtLeast(30L * 24 * 60 * 60 * 1000) // At least 1 month
        
        return remainingAmount / (remainingTime / (30.0 * 24 * 60 * 60 * 1000))
    }

    private fun calculateProjectedCompletionDate(
        goal: SavingsGoalEntity,
        averageContribution: Double
    ): Long {
        if (goal.currentAmount >= goal.targetAmount) {
            return System.currentTimeMillis() // Already completed
        }

        val remainingAmount = goal.targetAmount - goal.currentAmount
        if (averageContribution <= 0) {
            return goal.deadline // Use deadline if no contribution pattern
        }

        val monthsNeeded = remainingAmount / averageContribution
        return System.currentTimeMillis() + (monthsNeeded * 30 * 24 * 60 * 60 * 1000).toLong()
    }

    private fun calculateRiskLevel(
        timeProgress: Double,
        amountProgress: Double
    ): Int {
        // If we've achieved full progress, it's always low risk
        if (amountProgress >= 1.0) return 1

        val progressDeficit = timeProgress - amountProgress
        return when {
            progressDeficit > 0.2 || (timeProgress > 0.7 && amountProgress < 0.5) -> 3 // High risk
            progressDeficit > 0.1 || (timeProgress > 0.5 && amountProgress < 0.4) -> 2 // Medium risk
            else -> 1 // Low risk
        }
    }

    private fun calculateNextSuggestedContribution(
        goal: SavingsGoalEntity,
        disposableIncome: Double,
        progressRate: Double
    ): Double {
        // If goal is completed or over-achieved, no more contributions needed
        if (goal.currentAmount >= goal.targetAmount) return 0.0

        val remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
        val remainingTime = (goal.deadline - System.currentTimeMillis())
            .coerceAtLeast(30L * 24 * 60 * 60 * 1000) // At least 1 month to avoid huge suggestions

        val baseAmount = remainingAmount / (remainingTime / (30.0 * 24 * 60 * 60 * 1000))

        val suggestedAmount = when {
            progressRate < 0.8 -> baseAmount * 1.2 // Increase by 20% if behind
            progressRate < 0.9 -> baseAmount * 1.1 // Increase by 10% if slightly behind
            else -> baseAmount
        }

        // Ensure we don't suggest negative amounts or more than 50% of disposable income
        return suggestedAmount.coerceIn(0.0, disposableIncome * 0.5)
    }
}
