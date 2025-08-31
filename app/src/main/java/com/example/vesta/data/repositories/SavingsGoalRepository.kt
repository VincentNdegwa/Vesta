package com.example.vesta.data.repositories

import com.example.vesta.data.local.dao.SavingsGoalDao
import com.example.vesta.data.local.dao.SavingsContributionDao
import com.example.vesta.data.local.dao.TransactionDao
import com.example.vesta.data.local.entities.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SavingsGoalRepository @Inject constructor(
    private val savingsGoalDao: SavingsGoalDao,
    private val savingsContributionDao: SavingsContributionDao,
    private val transactionDao: TransactionDao
) {
    // Goal Management
    fun getSavingsGoals(userId: String) = savingsGoalDao.getAllSavingsGoals(userId)
    
    fun getActiveSavingsGoals(userId: String) = 
        savingsGoalDao.getSavingsGoalsByStatus(userId, GoalStatus.ACTIVE)

    fun getSavingsGoalById(goalId: String) = savingsGoalDao.getSavingsGoalById(goalId)

    suspend fun createSavingsGoal(goal: SavingsGoalEntity) {
        savingsGoalDao.insertSavingsGoal(goal)
    }

    suspend fun updateSavingsGoal(goal: SavingsGoalEntity) {
        savingsGoalDao.updateSavingsGoal(goal)
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        savingsGoalDao.deleteSavingsGoal(goal)
    }

    // Contribution Management
    fun getContributionsForGoal(goalId: String) = 
        savingsContributionDao.getContributionsForGoal(goalId)

    suspend fun addContribution(
        goalId: String,
        userId: String,
        amount: Double,
        type: String,
        transactionId: String? = null
    ) {
        val contribution = SavingsContributionEntity(
            goalId = goalId,
            userId = userId,
            amount = amount,
            type = type,
            transactionId = transactionId
        )
        withContext(Dispatchers.IO) {
            savingsContributionDao.insertContribution(contribution)
            savingsGoalDao.updateGoalProgress(goalId, amount)
            checkAndUpdateMilestones(goalId)
        }
    }

    // Auto-contribution Processing
    suspend fun processAutoContributions(userId: String, transactionId: String, amount: Double) {
        val goals = savingsGoalDao.getAutoContributeGoals(userId)
        goals.forEach { goal ->
            val contributionAmount = when {
                goal.autoContributeAmount != null -> goal.autoContributeAmount
                goal.autoContributePercentage != null -> amount * (goal.autoContributePercentage / 100)
                else -> return@forEach
            }
            
            if (contributionAmount > 0) {
                addContribution(
                    goalId = goal.id,
                    userId = userId,
                    amount = contributionAmount,
                    type = "AUTO",
                    transactionId = transactionId
                )
            }
        }
    }

    // Progress Tracking
    suspend fun getGoalProgress(goalId: String): Flow<GoalProgress> = flow {
        val goal = getSavingsGoalById(goalId).first()
        if (goal != null) {
            val totalContributions = savingsContributionDao.getTotalContributionsForGoal(goalId) ?: 0.0
            val timeProgress = calculateTimeProgress(goal.startDate, goal.deadline)
            val amountProgress = (totalContributions / goal.targetAmount).coerceIn(0.0, 1.0)
            
            emit(GoalProgress(
                timeProgress = timeProgress,
                amountProgress = amountProgress,
                isOnTrack = amountProgress >= timeProgress,
                currentAmount = totalContributions,
                targetAmount = goal.targetAmount,
                remainingAmount = goal.targetAmount - totalContributions,
                daysRemaining = calculateDaysRemaining(goal.deadline)
            ))
        }
    }

    // Milestone Tracking
    private suspend fun checkAndUpdateMilestones(goalId: String) {
        val goal = getSavingsGoalById(goalId).first() ?: return
        val progress = (goal.currentAmount / goal.targetAmount)
        
        val newMilestones = mutableListOf<String>()
        if (progress >= 0.25 && !goal.achievedMilestones.contains("25%")) newMilestones.add("25%")
        if (progress >= 0.50 && !goal.achievedMilestones.contains("50%")) newMilestones.add("50%")
        if (progress >= 0.75 && !goal.achievedMilestones.contains("75%")) newMilestones.add("75%")
        if (progress >= 1.00 && !goal.achievedMilestones.contains("100%")) newMilestones.add("100%")
        
        if (newMilestones.isNotEmpty()) {
            val updatedMilestones = goal.achievedMilestones + newMilestones
            savingsGoalDao.updateAchievedMilestones(goalId, updatedMilestones)
            
            if (progress >= 1.0) {
                savingsGoalDao.updateSavingsGoal(goal.copy(status = GoalStatus.COMPLETED))
            }
        }
    }

    // Utility Functions
    private fun calculateTimeProgress(startDate: Long, deadline: Long): Double {
        val now = System.currentTimeMillis()
        return ((now - startDate).toDouble() / (deadline - startDate)).coerceIn(0.0, 1.0)
    }

    private fun calculateDaysRemaining(deadline: Long): Int {
        val now = System.currentTimeMillis()
        return ((deadline - now) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
    }
}

data class GoalProgress(
    val timeProgress: Double,
    val amountProgress: Double,
    val isOnTrack: Boolean,
    val currentAmount: Double,
    val targetAmount: Double,
    val remainingAmount: Double,
    val daysRemaining: Int
)
