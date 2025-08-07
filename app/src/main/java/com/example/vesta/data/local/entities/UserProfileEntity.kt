package com.example.vesta.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val userId: String,
    val firstName: String?,
    val lastName: String?,
    val phoneNumber: String?,
    val dateOfBirth: Instant?,
    val address: String?,
    val city: String?,
    val country: String?,
    val currency: String = "USD",
    val language: String = "en",
    val timezone: String?,
    val profilePicture: String?,
    val occupation: String?,
    val monthlyIncome: Double?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastSyncedAt: Instant?,
    val isDeleted: Boolean = false,
    val needsSync: Boolean = false
)
