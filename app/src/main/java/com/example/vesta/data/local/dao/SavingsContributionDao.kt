package com.example.vesta.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.example.vesta.data.local.entities.SavingsContributionEntity

@Dao
interface SavingsContributionDao {
    @Query("SELECT * FROM savings_contributions WHERE goalId = :goalId ORDER BY date DESC")
    fun getContributionsForGoal(goalId: String): Flow<List<SavingsContributionEntity>>

    @Query("SELECT * FROM savings_contributions WHERE userId = :userId ORDER BY date DESC")
    fun getAllUserContributions(userId: String): Flow<List<SavingsContributionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: SavingsContributionEntity)

    @Update
    suspend fun updateContribution(contribution: SavingsContributionEntity)

    @Delete
    suspend fun deleteContribution(contribution: SavingsContributionEntity)

    @Query("SELECT SUM(amount) FROM savings_contributions WHERE goalId = :goalId")
    suspend fun getTotalContributionsForGoal(goalId: String): Double?

    @Query("SELECT * FROM savings_contributions WHERE transactionId = :transactionId")
    suspend fun getContributionByTransactionId(transactionId: String): SavingsContributionEntity?
}
