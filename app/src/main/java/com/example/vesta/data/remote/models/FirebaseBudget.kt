package com.example.vesta.data.remote.models

data class FirebaseBudget(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "",
    val targetAmount: Double = 0.0,
    val spentAmount: Double = 0.0,
    val period: String = "",
    val startDate: Long = 0,
    val endDate: Long = 0,
    val alertThreshold: Double = 0.8,
    val isActive: Boolean = true,
    val color: String? = null,
    val icon: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false
)
