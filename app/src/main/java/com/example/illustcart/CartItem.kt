package com.example.illustcart

data class CartItem(
    val cartItemId: String? = null,
    val userId: String? = null,
    val productId: String? = null,
    val productName: String? = null,
    val productPrice: String? = null,
    val productImage: String? = null,
    val productSize: String? = null,
    val category: String? = null,
    // REMOVE: val artist: String? = null,
    val sellerId: String? = null,
    val sellerName: String? = null,
    val addedDate: Long? = null
)