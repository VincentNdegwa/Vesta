package com.example.vesta.data.remote.models

data class FirebaseUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String? = null,
    val photoUrl: String? = null,
    val createdAt: Long = 0,
    val updatedAt: Long = 0
)
