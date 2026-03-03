package com.example.illustcart

data class Order(
    val orderId: String? = null,
    val productId: String? = null,
    val productName: String? = null,
    val productPrice: String? = null,
    val productImage: String? = null,
    val productSize: String? = null,
    val category: String? = null,
    val artist: String? = null,
    val sellerId: String? = null,
    val sellerName: String? = null,
    val buyerId: String? = null,
    val buyerName: String? = null,
    val buyerEmail: String? = null,
    val status: String? = null, // "pending", "completed"
    val orderDate: Long? = null,
    val completionDate: Long? = null
)