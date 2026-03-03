package com.example.illustcart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class AnalyticsArtworkAdapter(private val analyticsList: MutableList<ProductAnalytics>) :
    RecyclerView.Adapter<AnalyticsArtworkAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImage: ImageView = itemView.findViewById(R.id.analytics_product_image)
        val productName: TextView = itemView.findViewById(R.id.analytics_product_name)
        val productDescription: TextView = itemView.findViewById(R.id.analytics_product_description)
        val productPrice: TextView = itemView.findViewById(R.id.analytics_product_price)
        val printsSold: TextView = itemView.findViewById(R.id.analytics_prints_sold)
        val totalEarned: TextView = itemView.findViewById(R.id.analytics_total_earned)
        val rating: TextView = itemView.findViewById(R.id.analytics_rating)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_analytics_artwork, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val analytics = analyticsList[position]

        holder.productName.text = analytics.productName ?: "Unknown Product"
        holder.productDescription.text = analytics.description ?: "No description"
        holder.productPrice.text = analytics.productPrice ?: "$0"
        holder.printsSold.text = analytics.printsSold.toString()
        holder.totalEarned.text = "$%.2f".format(analytics.totalEarned)

        // Display rating
        if (analytics.ratingCount > 0) {
            holder.rating.text = "%.1f".format(analytics.averageRating)
        } else {
            holder.rating.text = "No Ratings Yet"
        }

        Glide.with(holder.itemView.context)
            .load(analytics.imageUrl)
            .placeholder(R.drawable.baseline_image_24)
            .into(holder.productImage)
    }

    override fun getItemCount() = analyticsList.size
}