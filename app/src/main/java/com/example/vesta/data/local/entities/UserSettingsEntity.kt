package com.example.vesta.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val userId: String,
    val isPinEnabled: Boolean = false,
    val pinHash: String?, // Hashed PIN for security
    val isBiometricEnabled: Boolean = false,
    val lockTimeoutMinutes: Int = 5, // Auto-lock after 5 minutes
    val hideAmounts: Boolean = false, // Hide sensitive amounts in UI
    val requireAuthForExports: Boolean = true,
    val requireAuthForReports: Boolean = false,
    val darkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val billRemindersEnabled: Boolean = true,
    val budgetAlertsEnabled: Boolean = true,
    val backupEnabled: Boolean = true,
    val autoBackupFrequency: String = "WEEKLY", // DAILY, WEEKLY, MONTHLY
    val dataRetentionMonths: Int = 24, // Keep data for 2 years
    val createdAt: Instant,
    val updatedAt: Instant,
    val isSynced: Boolean = false
)
