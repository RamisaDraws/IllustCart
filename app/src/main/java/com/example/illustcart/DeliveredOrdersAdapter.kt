package com.example.illustcart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class DeliveredOrdersAdapter(private val orders: MutableList<Order>) :
    RecyclerView.Adapter<DeliveredOrdersAdapter.ViewHolder>() {

    interface OnDownloadClickListener {
        fun onDownloadClick(order: Order)
    }

    interface OnRateClickListener {
        fun onRateClick(order: Order)
    }

    var onDownloadClickListener: OnDownloadClickListener? = null
    var onRateClickListener: OnRateClickListener? = null

    // Map to track which orders have been rated
    private val ratedOrders = mutableMapOf<String, Int>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderIdText: TextView = itemView.findViewById(R.id.order_id_text)
        val statusBadge: TextView = itemView.findViewById(R.id.order_status_badge)
        val productImage: ImageView = itemView.findViewById(R.id.delivered_order_product_image)
        val productName: TextView = itemView.findViewById(R.id.delivered_order_product_name)
        val artist: TextView = itemView.findViewById(R.id.delivered_order_artist)
        val size: TextView = itemView.findViewById(R.id.delivered_order_size)
        val price: TextView = itemView.findViewById(R.id.delivered_order_price)
        val orderDate: TextView = itemView.findViewById(R.id.delivered_order_date)
        val downloadBtn: Button = itemView.findViewById(R.id.download_btn)
        val rateBtn: Button = itemView.findViewById(R.id.rate_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_delivered_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]

        // Order ID (shortened)
        holder.orderIdText.text = "Order #${order.orderId?.take(8) ?: "Unknown"}"

        // Status badge
        holder.statusBadge.text = when(order.status) {
            "completed" -> "Delivered"
            "delivered" -> "Delivered"
            else -> "Completed"
        }

        // Product details
        holder.productName.text = order.productName ?: "Unknown Product"
        holder.artist.text = "by ${order.sellerName ?: "Unknown Artist"}"
        holder.size.text = "Size: ${order.productSize ?: "N/A"}"
        holder.price.text = order.productPrice ?: "$0"

        // Format date - use completion date if available, otherwise order date
        val dateToShow = order.completionDate ?: order.orderDate ?: 0
        val date = Date(dateToShow)
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.orderDate.text = format.format(date)

        // Load product image
        Glide.with(holder.itemView.context)
            .load(order.productImage)
            .placeholder(R.drawable.baseline_image_24)
            .into(holder.productImage)

        // Download button click
        holder.downloadBtn.setOnClickListener {
            onDownloadClickListener?.onDownloadClick(order)
        }

        // Check if user has already rated this product
        val currentUser = FirebaseAuth.getInstance().currentUser
        val productId = order.productId

        if (currentUser != null && productId != null) {
            // Check cache first
            if (ratedOrders.containsKey(productId)) {
                val rating = ratedOrders[productId]!!
                holder.rateBtn.text = "Rated $rating"
                holder.rateBtn.isEnabled = false
                holder.rateBtn.alpha = 0.6f
            } else {
                // Check Firebase
                FirebaseDatabase.getInstance()
                    .getReference("ratings")
                    .child(productId)
                    .child(currentUser.uid)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val rating = snapshot.getValue(Rating::class.java)
                                val ratingValue = rating?.rating ?: 5

                                // Cache the rating
                                ratedOrders[productId] = ratingValue

                                // Update button
                                holder.rateBtn.text = "Rated $ratingValue"
                                holder.rateBtn.isEnabled = false
                                holder.rateBtn.alpha = 0.6f
                            } else {
                                // Not rated yet
                                holder.rateBtn.text = "Rate"
                                holder.rateBtn.isEnabled = true
                                holder.rateBtn.alpha = 1.0f
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // On error, allow rating
                            holder.rateBtn.text = "Rate"
                            holder.rateBtn.isEnabled = true
                            holder.rateBtn.alpha = 1.0f
                        }
                    })
            }
        }

        // Rate button click
        holder.rateBtn.setOnClickListener {
            onRateClickListener?.onRateClick(order)
        }
    }

    override fun getItemCount() = orders.size

    // Call this after rating is submitted to update the UI
    fun updateRatingStatus(productId: String, rating: Int) {
        ratedOrders[productId] = rating
        notifyDataSetChanged()
    }
}