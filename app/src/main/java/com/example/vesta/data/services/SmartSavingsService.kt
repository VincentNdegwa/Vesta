package com.example.vesta.data.services

import com.example.vesta.data.local.dao.SavingsGoalDao
import com.example.vesta.data.local.dao.SavingsRuleDao
import com.example.vesta.data.local.entities.*
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartSavingsService @Inject constructor(
    private val savingsGoalDao: SavingsGoalDao,
    private val savingsRuleDao: SavingsRuleDao
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

    private fun calculateNextScheduledTime(currentTime: Long, frequency: RuleFrequency): Long {
        val current = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(currentTime),
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
        val goals = savingsGoalDao.getSavingsGoalsByStatus(userId, GoalStatus.ACTIVE)
        goals.collect { activeGoals ->
            for (goal in activeGoals) {
                // Calculate progress metrics
                val timeElapsed = System.currentTimeMillis() - goal.startDate
                val totalDuration = goal.deadline - goal.startDate
                val expectedProgress = (timeElapsed.toDouble() / totalDuration) * goal.targetAmount
                val actualProgress = goal.currentAmount
                val progressRate = actualProgress / expectedProgress

                // Calculate sustainability score (0-100)
                val disposableIncome = monthlyIncome - monthlyExpenses
                val requiredMonthlyAmount = (goal.targetAmount - goal.currentAmount) / 
                    ((goal.deadline - System.currentTimeMillis()) / (30L * 24 * 60 * 60 * 1000))
                val sustainabilityScore = ((disposableIncome / requiredMonthlyAmount) * 100)
                    .coerceIn(0.0, 100.0)
                    .toInt()

                // Calculate risk level based on progress and deadline proximity
                val riskLevel = calculateRiskLevel(
                    progressRate,
                    timeElapsed.toDouble() / totalDuration,
                    sustainabilityScore
                )

                // Calculate projected completion date
                val avgMonthlyContribution = if (goal.totalContributions > 0) {
                    goal.currentAmount / (timeElapsed / (30.0 * 24 * 60 * 60 * 1000))
                } else null

                val projectedDate = avgMonthlyContribution?.let {
                    val remainingAmount = goal.targetAmount - goal.currentAmount
                    val monthsNeeded = remainingAmount / it
                    System.currentTimeMillis() + (monthsNeeded * 30 * 24 * 60 * 60 * 1000).toLong()
                }

                // Calculate next suggested contribution
                val nextSuggestedContribution = calculateNextSuggestedContribution(
                    goal,
                    disposableIncome,
                    progressRate
                )

                // Update goal metrics
                savingsGoalDao.updateSmartMetrics(
                    goalId = goal.id,
                    score = sustainabilityScore,
                    suggestedAmount = requiredMonthlyAmount,
                    riskLevel = riskLevel,
                    projectedDate = projectedDate ?: goal.deadline,
                    progressRate = progressRate,
                    nextContribution = nextSuggestedContribution,
                    avgContribution = avgMonthlyContribution ?: 0.0
                )
            }
        }
    }

    private fun calculateRiskLevel(
        progressRate: Double,
        timeProgress: Double,
        sustainabilityScore: Int
    ): Int {
        return when {
            progressRate < 0.8 && timeProgress > 0.7 -> 3 // High risk
            progressRate < 0.9 && timeProgress > 0.5 -> 2 // Medium risk
            sustainabilityScore < 50 -> 2 // Medium risk
            else -> 1 // Low risk
        }
    }

    private fun calculateNextSuggestedContribution(
        goal: SavingsGoalEntity,
        disposableIncome: Double,
        progressRate: Double
    ): Double {
        val baseAmount = (goal.targetAmount - goal.currentAmount) /
            ((goal.deadline - System.currentTimeMillis()) / (30L * 24 * 60 * 60 * 1000))

        return when {
            progressRate < 0.8 -> baseAmount * 1.2 // Increase by 20% if behind
            progressRate < 0.9 -> baseAmount * 1.1 // Increase by 10% if slightly behind
            else -> baseAmount
        }.coerceAtMost(disposableIncome * 0.5) // Cap at 50% of disposable income
    }
}
