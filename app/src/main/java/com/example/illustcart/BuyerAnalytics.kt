package com.example.illustcart

data class BuyerAnalytics(
    val buyerId: String? = null,
    val buyerName: String? = null,
    val buyerEmail: String? = null,
    var profileImageUrl: String? = null,
    var totalSpent: Double = 0.0
)