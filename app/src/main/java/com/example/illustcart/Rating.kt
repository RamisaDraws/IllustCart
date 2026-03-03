package com.example.illustcart

data class Rating(
    val userId: String? = null,
    val userName: String? = null,
    val userProfileImage: String? = null,
    val rating: Int = 5,
    val comment: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val orderId: String? = null
)