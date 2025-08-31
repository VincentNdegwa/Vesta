package com.example.vesta.data.local.dao

import androidx.room.*
import com.example.vesta.data.local.entities.SavingsRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsRuleDao {
    @Query("SELECT * FROM savings_rules WHERE goalId = :goalId")
    fun getRulesForGoal(goalId: String): Flow<List<SavingsRuleEntity>>
    
    @Query("SELECT * FROM savings_rules WHERE goalId = :goalId AND isEnabled = 1")
    fun getActiveRulesForGoal(goalId: String): Flow<List<SavingsRuleEntity>>
    
    @Query("SELECT * FROM savings_rules WHERE frequency = :frequency AND isEnabled = 1")
    fun getRulesByFrequency(frequency: String): Flow<List<SavingsRuleEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: SavingsRuleEntity)
    
    @Delete
    suspend fun deleteRule(rule: SavingsRuleEntity)
    
    @Query("UPDATE savings_rules SET isEnabled = :enabled WHERE id = :ruleId")
    suspend fun toggleRule(ruleId: String, enabled: Boolean)
    
    @Query("UPDATE savings_rules SET lastExecuted = :timestamp WHERE id = :ruleId")
    suspend fun updateLastExecuted(ruleId: String, timestamp: Long)
    
    @Query("UPDATE savings_rules SET nextScheduled = :timestamp WHERE id = :ruleId")
    suspend fun updateNextScheduled(ruleId: String, timestamp: Long)
    
    @Query("""
        SELECT * FROM savings_rules 
        WHERE frequency = :frequency 
        AND isEnabled = 1 
        AND (nextScheduled IS NULL OR nextScheduled <= :currentTime)
    """)
    suspend fun getReadyToExecuteRules(frequency: String, currentTime: Long): List<SavingsRuleEntity>
}
