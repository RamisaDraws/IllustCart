package com.example.illustcart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CartAdapter(private val cartItems: MutableList<CartItem>) :
    RecyclerView.Adapter<CartAdapter.ViewHolder>() {

    interface OnDeleteClickListener {
        fun onDeleteClick(cartItem: CartItem)
    }

    interface OnBuyClickListener {
        fun onBuyClick(cartItem: CartItem)
    }

    var onDeleteClickListener: OnDeleteClickListener? = null
    var onBuyClickListener: OnBuyClickListener? = null

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.cart_product_image)
        val productName: TextView = itemView.findViewById(R.id.cart_product_name)
        val sellerName: TextView = itemView.findViewById(R.id.cart_seller_name)
        val productSize: TextView = itemView.findViewById(R.id.cart_product_size)
        val productPrice: TextView = itemView.findViewById(R.id.cart_product_price)
        val deleteButton: Button = itemView.findViewById(R.id.delete_from_cart_btn)
        val buyButton: Button = itemView.findViewById(R.id.buy_from_cart_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val cartItem = cartItems[position]

        holder.productName.text = cartItem.productName
        holder.sellerName.text = "Artist: ${cartItem.sellerName ?: "Unknown"}"
        holder.productSize.text = "Size: ${cartItem.productSize}"
        holder.productPrice.text = cartItem.productPrice

        Glide.with(holder.itemView.context)
            .load(cartItem.productImage)
            .into(holder.productImage)

        holder.deleteButton.setOnClickListener {
            onDeleteClickListener?.onDeleteClick(cartItem)
        }

        holder.buyButton.setOnClickListener {
            onBuyClickListener?.onBuyClick(cartItem)
        }
    }

    override fun getItemCount() = cartItems.size
}