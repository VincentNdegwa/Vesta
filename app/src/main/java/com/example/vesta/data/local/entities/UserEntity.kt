package com.example.vesta.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.Instant

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val uid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?,
    val createdAt: Instant,
    val updatedAt: Instant,
    val lastSyncedAt: Instant?,
    val isDeleted: Boolean = false
)
