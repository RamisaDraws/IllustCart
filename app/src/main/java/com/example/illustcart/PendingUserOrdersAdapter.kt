package com.example.illustcart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class PendingUserOrdersAdapter(private val orders: MutableList<Order>) :
    RecyclerView.Adapter<PendingUserOrdersAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderIdText: TextView = itemView.findViewById(R.id.pending_order_id_text)
        val statusBadge: TextView = itemView.findViewById(R.id.pending_order_status_badge)
        val productImage: ImageView = itemView.findViewById(R.id.pending_order_product_image)
        val productName: TextView = itemView.findViewById(R.id.pending_order_product_name)
        val artist: TextView = itemView.findViewById(R.id.pending_order_artist)
        val size: TextView = itemView.findViewById(R.id.pending_order_size)
        val price: TextView = itemView.findViewById(R.id.pending_order_price)
        val orderDate: TextView = itemView.findViewById(R.id.pending_order_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pending_user_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]

        // Order ID (shortened)
        holder.orderIdText.text = "Order #${order.orderId?.take(8) ?: "Unknown"}"

        // Status badge
        holder.statusBadge.text = "Pending"

        // Product details
        holder.productName.text = order.productName ?: "Unknown Product"
        holder.artist.text = "by ${order.sellerName ?: "Unknown Artist"}"
        holder.size.text = "Size: ${order.productSize ?: "N/A"}"
        holder.price.text = order.productPrice ?: "$0"

        // Format date
        val date = Date(order.orderDate ?: 0)
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.orderDate.text = "Ordered on ${format.format(date)}"

        // Load product image
        Glide.with(holder.itemView.context)
            .load(order.productImage)
            .placeholder(R.drawable.baseline_image_24)
            .into(holder.productImage)
    }

    override fun getItemCount() = orders.size
}