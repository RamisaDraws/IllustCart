package com.example.illustcart

import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProductAdapter(val products: MutableList<Product>) :
    RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    interface OnDeleteClickListener {
        fun onDeleteClick(product: Product, position: Int)
    }

    interface OnEditClickListener {
        fun onEditClick(product: Product, position: Int)
    }

    interface OnCartIconClickListener {
        fun onCartIconClick(product: Product)
    }

    interface OnProductClickListener {
        fun onProductClick(product: Product)
    }

    var onDeleteClickListener: OnDeleteClickListener? = null
    var onEditClickListener: OnEditClickListener? = null
    var onCartIconClickListener: OnCartIconClickListener? = null
    var onProductClickListener: OnProductClickListener? = null
    var isDeleteButtonVisible: Boolean = true

    // Map to track countdown timers
    private val countdownTimers = mutableMapOf<Int, CountDownTimer>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productName: TextView = itemView.findViewById(R.id.productNameTextView)
        val productSize: TextView = itemView.findViewById(R.id.productSizeTextView)
        val productPrice: TextView = itemView.findViewById(R.id.productPriceTextView)
        val originalPrice: TextView = itemView.findViewById(R.id.originalPriceTextView)
        val productImage: ImageView = itemView.findViewById(R.id.productImageView)
        val cartIcon: ImageView = itemView.findViewById(R.id.buy_button)
        val deleteButton: Button = itemView.findViewById(R.id.deleteButton)
        val editButton: Button = itemView.findViewById(R.id.editButton)
        val artistText: TextView = itemView.findViewById(R.id.artistTextView)
        val categoryText: TextView = itemView.findViewById(R.id.categoryTextView)
        val printsAvailable: TextView = itemView.findViewById(R.id.printsAvailableTextView)
        val flashSaleBadgeContainer: LinearLayout = itemView.findViewById(R.id.flashSaleBadgeContainer)
        val flashDiscountBadge: TextView = itemView.findViewById(R.id.flashDiscountBadge)
        val flashSaleTimer: TextView = itemView.findViewById(R.id.flashSaleTimerTextView)
        val productRating: TextView = itemView.findViewById(R.id.productRatingTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = products[position]

        // Get actual adapter position to avoid position warnings
        val adapterPos = holder.adapterPosition
        if (adapterPos == RecyclerView.NO_POSITION) return

        // Cancel previous timer if exists
        countdownTimers[adapterPos]?.cancel()

        holder.productName.text = product.productName
        holder.productSize.text = "Size: ${product.productSize}"
        holder.artistText.text = "Artist: ${product.sellerName ?: "Unknown"}"
        holder.categoryText.text = "Category: ${product.category ?: "N/A"}"

        // Handle prints available
        val printsCount = product.printsAvailable ?: 0
        if (printsCount > 0) {
            holder.printsAvailable.text = "$printsCount prints available"
            holder.printsAvailable.setTextColor(holder.itemView.context.getColor(android.R.color.holo_green_dark))
        } else {
            holder.printsAvailable.text = "SOLD OUT"
            holder.printsAvailable.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))
        }

        // Load and display rating
        val productId = product.id
        if (productId != null) {
            FirebaseDatabase.getInstance()
                .getReference("ratings")
                .child(productId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var totalRating = 0.0
                        var count = 0

                        for (ratingSnapshot in snapshot.children) {
                            val rating = ratingSnapshot.getValue(Rating::class.java)
                            rating?.let {
                                totalRating += it.rating
                                count++
                            }
                        }

                        if (count > 0) {
                            val average = totalRating / count
                            holder.productRating.text = "Rated %.1f ⭐".format(average)
                            holder.productRating.visibility = View.VISIBLE
                        } else {
                            holder.productRating.text = "Not rated yet"
                            holder.productRating.visibility = View.VISIBLE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        holder.productRating.text = "Not rated yet"
                        holder.productRating.visibility = View.VISIBLE
                    }
                })
        } else {
            holder.productRating.text = "Not rated yet"
            holder.productRating.visibility = View.VISIBLE
        }

        // Handle flash sale
        val isFlashSale = product.isFlashSale ?: false
        val flashSaleEndTime = product.flashSaleEndTime ?: 0L
        val currentTime = System.currentTimeMillis()
        val isFlashSaleActive = isFlashSale && flashSaleEndTime > currentTime

        if (isFlashSaleActive) {
            // Show flash sale badge
            holder.flashSaleBadgeContainer.visibility = View.VISIBLE
            holder.flashDiscountBadge.text = "${product.discountRate}% OFF"

            // Show flash sale timer
            holder.flashSaleTimer.visibility = View.VISIBLE

            // Get prices - use originalPrice if available, otherwise use productPrice
            val originalPriceValue = product.originalPrice ?: product.productPrice ?: "$0"
            val discountRate = product.discountRate ?: 0

            // Show original price with strikethrough
            holder.originalPrice.visibility = View.VISIBLE
            holder.originalPrice.text = originalPriceValue
            holder.originalPrice.paintFlags = holder.originalPrice.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

            // Calculate and show discounted price
            val priceValue = originalPriceValue.replace("$", "").toDoubleOrNull() ?: 0.0
            val discountedPrice = priceValue * (1 - discountRate / 100.0)
            holder.productPrice.text = "$%.2f".format(discountedPrice)
            holder.productPrice.setTextColor(holder.itemView.context.getColor(android.R.color.holo_red_dark))

            // Start countdown timer
            val remainingMillis = flashSaleEndTime - currentTime
            val timer = object : CountDownTimer(remainingMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val seconds = (millisUntilFinished / 1000).toInt()
                    val hours = seconds / 3600
                    val minutes = (seconds % 3600) / 60
                    val secs = seconds % 60
                    holder.flashSaleTimer.text = "⏰ %02d:%02d:%02d".format(hours, minutes, secs)
                }

                override fun onFinish() {
                    // Flash sale ended - update Firebase to disable flash sale
                    if (product.id != null && product.sellerId != null) {
                        val productRef = com.google.firebase.database.FirebaseDatabase.getInstance()
                            .getReference("products")
                            .child(product.sellerId!!)
                            .child(product.id!!)

                        val updates = hashMapOf<String, Any>(
                            "isFlashSale" to false,
                            "productPrice" to (product.originalPrice ?: product.productPrice ?: "$0"),
                            "flashSaleEndTime" to 0L,
                            "discountRate" to 0,
                            "originalPrice" to ""
                        )

                        productRef.updateChildren(updates)
                            .addOnSuccessListener {
                                // Update local product object
                                product.isFlashSale = false
                                product.productPrice = product.originalPrice ?: product.productPrice
                                product.flashSaleEndTime = null
                                product.discountRate = null
                                product.originalPrice = null

                                // Refresh UI
                                val currentPos = holder.adapterPosition
                                if (currentPos != RecyclerView.NO_POSITION) {
                                    notifyItemChanged(currentPos)
                                }
                            }
                            .addOnFailureListener {
                                // Still refresh UI even if Firebase update fails
                                val currentPos = holder.adapterPosition
                                if (currentPos != RecyclerView.NO_POSITION) {
                                    notifyItemChanged(currentPos)
                                }
                            }
                    } else {
                        // Fallback: just refresh UI
                        val currentPos = holder.adapterPosition
                        if (currentPos != RecyclerView.NO_POSITION) {
                            notifyItemChanged(currentPos)
                        }
                    }
                }
            }
            timer.start()
            countdownTimers[adapterPos] = timer

        } else {
            // No flash sale or expired
            holder.flashSaleBadgeContainer.visibility = View.GONE
            holder.flashSaleTimer.visibility = View.GONE
            holder.originalPrice.visibility = View.GONE
            holder.productPrice.text = product.productPrice ?: "$0"
            holder.productPrice.setTextColor(holder.itemView.context.getColor(R.color.colorAccent))
        }

        Glide.with(holder.productImage.context)
            .load(product.imageUrl)
            .into(holder.productImage)

        // Make entire card clickable
        holder.itemView.setOnClickListener {
            onProductClickListener?.onProductClick(product)
        }

        if (isDeleteButtonVisible) {
            holder.deleteButton.visibility = View.VISIBLE
            holder.editButton.visibility = View.VISIBLE

            holder.deleteButton.setOnClickListener {
                val currentPos = holder.adapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onDeleteClickListener?.onDeleteClick(product, currentPos)
                }
            }

            holder.editButton.setOnClickListener {
                val currentPos = holder.adapterPosition
                if (currentPos != RecyclerView.NO_POSITION) {
                    onEditClickListener?.onEditClick(product, currentPos)
                }
            }
        } else {
            holder.deleteButton.visibility = View.GONE
            holder.editButton.visibility = View.GONE
        }

        holder.cartIcon.setOnClickListener {
            onCartIconClickListener?.onCartIconClick(product)
        }
    }

    override fun getItemCount(): Int {
        return products.size
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        // Cancel timer when view is recycled
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            countdownTimers[position]?.cancel()
            countdownTimers.remove(position)
        }
    }

    // Clean up timers when adapter is destroyed
    fun cleanup() {
        countdownTimers.values.forEach { it.cancel() }
        countdownTimers.clear()
    }
}