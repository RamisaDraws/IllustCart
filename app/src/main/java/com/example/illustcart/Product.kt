package com.example.illustcart

import com.google.firebase.database.PropertyName

data class Product(
    var id: String? = null,
    var productName: String? = null,
    var description: String? = null,
    var productSize: String? = null,
    var productPrice: String? = null,
    var imageUrl: String? = null,
    var sellerId: String? = null,
    var sellerName: String? = null,
    var category: String? = null,
    var printsAvailable: Int? = null,
    @get:PropertyName("isFlashSale") @set:PropertyName("isFlashSale")
    var isFlashSale: Boolean? = false,
    var flashSaleEndTime: Long? = null,
    var discountRate: Int? = null,
    var originalPrice: String? = null
)