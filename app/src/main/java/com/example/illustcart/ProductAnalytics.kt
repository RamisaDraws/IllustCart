package com.example.illustcart

data class ProductAnalytics(
    val productId: String? = null,
    val productName: String? = null,
    val description: String? = null,
    val productSize: String? = null,
    val productPrice: String? = null,
    val imageUrl: String? = null,
    val category: String? = null,
    var printsSold: Int = 0,
    var totalEarned: Double = 0.0,
    var averageRating: Double = 0.0,
    var ratingCount: Int = 0
)