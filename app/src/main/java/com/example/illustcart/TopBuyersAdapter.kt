package com.example.illustcart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class TopBuyersAdapter(private val buyersList: MutableList<BuyerAnalytics>) :
    RecyclerView.Adapter<TopBuyersAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val rankBadge: TextView = itemView.findViewById(R.id.buyer_rank)
        val profileImage: ImageView = itemView.findViewById(R.id.buyer_profile_image)
        val buyerName: TextView = itemView.findViewById(R.id.buyer_name)
        val buyerEmail: TextView = itemView.findViewById(R.id.buyer_email)
        val totalSpent: TextView = itemView.findViewById(R.id.buyer_total_spent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_buyer, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val buyer = buyersList[position]

        holder.rankBadge.text = (position + 1).toString()
        holder.buyerName.text = buyer.buyerName ?: "Unknown Buyer"
        holder.buyerEmail.text = buyer.buyerEmail ?: "No email"
        holder.totalSpent.text = "$%.2f".format(buyer.totalSpent)

        // Load profile image
        if (buyer.profileImageUrl != null) {
            Glide.with(holder.itemView.context)
                .load(buyer.profileImageUrl)
                .placeholder(R.drawable.baseline_account_circle_24)
                .into(holder.profileImage)
        } else {
            holder.profileImage.setImageResource(R.drawable.baseline_account_circle_24)
        }
    }

    override fun getItemCount() = buyersList.size
}