package com.example.illustcart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ReviewsAdapter(private val reviews: MutableList<Rating>) :
    RecyclerView.Adapter<ReviewsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userImage: ImageView = itemView.findViewById(R.id.review_user_image)
        val userName: TextView = itemView.findViewById(R.id.review_user_name)
        val comment: TextView = itemView.findViewById(R.id.review_comment)

        val star1: ImageView = itemView.findViewById(R.id.review_star1)
        val star2: ImageView = itemView.findViewById(R.id.review_star2)
        val star3: ImageView = itemView.findViewById(R.id.review_star3)
        val star4: ImageView = itemView.findViewById(R.id.review_star4)
        val star5: ImageView = itemView.findViewById(R.id.review_star5)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val review = reviews[position]

        // User name
        holder.userName.text = review.userName ?: "Anonymous"

        // User profile image
        if (!review.userProfileImage.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(review.userProfileImage)
                .placeholder(R.drawable.baseline_account_circle_24)
                .into(holder.userImage)
        } else {
            holder.userImage.setImageResource(R.drawable.baseline_account_circle_24)
        }

        // Star rating
        val stars = listOf(holder.star1, holder.star2, holder.star3, holder.star4, holder.star5)
        stars.forEachIndexed { index, star ->
            if (index < review.rating) {
                star.setImageResource(R.drawable.ic_star_filled)
            } else {
                star.setImageResource(R.drawable.ic_star_hollow)
            }
        }

        // Comment
        if (!review.comment.isNullOrEmpty()) {
            holder.comment.visibility = View.VISIBLE
            holder.comment.text = review.comment
        } else {
            holder.comment.visibility = View.GONE
        }
    }

    override fun getItemCount() = reviews.size
}