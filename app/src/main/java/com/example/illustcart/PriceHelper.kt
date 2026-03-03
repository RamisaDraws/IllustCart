package com.example.illustcart

object PriceHelper {

    /**
     * Calculate discounted price based on original price and discount rate
     */
    fun calculateDiscountedPrice(originalPrice: String, discountRate: Int): String {
        val priceValue = originalPrice.replace("$", "").toDoubleOrNull() ?: 0.0
        val discountedPrice = priceValue * (1 - discountRate / 100.0)
        return "$%.2f".format(discountedPrice)
    }

    /**
     * Get display price for a product (considers flash sale)
     */
    fun getDisplayPrice(product: Product): String {
        val isFlashSale = product.isFlashSale ?: false
        val flashSaleEndTime = product.flashSaleEndTime ?: 0L
        val currentTime = System.currentTimeMillis()
        val isFlashSaleActive = isFlashSale && flashSaleEndTime > currentTime

        val discountRate = product.discountRate
        val originalPrice = product.originalPrice

        return if (isFlashSaleActive && originalPrice != null && discountRate != null) {
            // Flash sale is active - calculate and return discounted price
            calculateDiscountedPrice(originalPrice, discountRate)
        } else {
            // No flash sale or expired - return regular product price
            product.productPrice ?: "$0"
        }
    }

    /**
     * Check if flash sale is currently active
     */
    fun isFlashSaleActive(product: Product): Boolean {
        val isFlashSale = product.isFlashSale ?: false
        val flashSaleEndTime = product.flashSaleEndTime ?: 0L
        val currentTime = System.currentTimeMillis()
        return isFlashSale && flashSaleEndTime > currentTime
    }

    /**
     * Format remaining time as HH:MM:SS
     */
    fun formatRemainingTime(endTime: Long): String {
        val remainingMillis = endTime - System.currentTimeMillis()
        if (remainingMillis <= 0) return "00:00:00"

        val seconds = (remainingMillis / 1000).toInt()
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return "%02d:%02d:%02d".format(hours, minutes, secs)
    }
}