package com.example.illustcart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class OrdersAdapter(
    private val orders: MutableList<Order>,
    private val isCompletedView: Boolean = false
) : RecyclerView.Adapter<OrdersAdapter.ViewHolder>() {

    interface OnCompleteClickListener {
        fun onCompleteClick(order: Order)
    }

    var onCompleteClickListener: OnCompleteClickListener? = null

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.order_product_image)
        val productName: TextView = itemView.findViewById(R.id.order_product_name)
        val buyerName: TextView = itemView.findViewById(R.id.order_buyer_name)
        val orderDate: TextView = itemView.findViewById(R.id.order_date)
        val price: TextView = itemView.findViewById(R.id.order_price)
        val completeButton: Button? = itemView.findViewById(R.id.complete_order_btn)
        val completedText: TextView? = itemView.findViewById(R.id.completed_text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = orders[position]

        holder.productName.text = order.productName
        holder.buyerName.text = "Buyer: ${order.buyerName}"
        holder.price.text = order.productPrice

        val date = Date(order.orderDate ?: 0)
        val format = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.orderDate.text = format.format(date)

        Glide.with(holder.itemView.context)
            .load(order.productImage)
            .into(holder.productImage)

        // Handle complete button vs completed text based on view type
        if (isCompletedView) {
            // This is the sales/completed view - hide button, show "Completed" text
            holder.completeButton?.visibility = View.GONE
            holder.completedText?.visibility = View.VISIBLE
            holder.completedText?.text = "Completed"
        } else {
            // This is the pending view - show the complete button
            holder.completeButton?.visibility = View.VISIBLE
            holder.completedText?.visibility = View.GONE
            holder.completeButton?.setOnClickListener {
                onCompleteClickListener?.onCompleteClick(order)
            }
        }
    }

    override fun getItemCount() = orders.size
}