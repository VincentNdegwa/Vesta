package com.example.vesta.data.remote.models

data class FirebaseTransaction(
    val id: String = "",
    val userId: String = "",
    val amount: Double = 0.0,
    val type: String = "",
    val category: String = "",
    val subcategory: String? = null,
    val description: String? = null,
    val notes: String? = null,
    val date: Long = 0,
    val accountId: String? = null,
    val paymentMethod: String? = null,
    val location: String? = null,
    val receiptUrl: String? = null,
    val tags: List<String> = emptyList(),
    val recurringId: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val isDeleted: Boolean = false
)
