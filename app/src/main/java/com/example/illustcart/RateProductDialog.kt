package com.example.illustcart

import android.app.Activity
import android.app.Dialog
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class RateProductDialog(
    private val activity: Activity,
    private val order: Order
) : Dialog(activity) {

    private lateinit var productImage: ImageView
    private lateinit var productTitle: TextView
    private lateinit var productDescription: TextView
    private lateinit var commentEdit: EditText
    private lateinit var cancelButton: Button
    private lateinit var submitButton: Button

    private val stars = mutableListOf<ImageView>()
    private var selectedRating = 5

    private var onRatingSubmittedListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_rate_product)

        window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Set dialog width to 90% of screen width
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        initializeViews()
        loadProductData()
        setupStarRating()
        setupButtons()
    }

    private fun initializeViews() {
        productImage = findViewById(R.id.rate_product_image)
        productTitle = findViewById(R.id.rate_product_title)
        productDescription = findViewById(R.id.rate_product_description)
        commentEdit = findViewById(R.id.rate_comment_edit)
        cancelButton = findViewById(R.id.rate_cancel_btn)
        submitButton = findViewById(R.id.rate_submit_btn)

        // Initialize star views
        stars.add(findViewById(R.id.star1))
        stars.add(findViewById(R.id.star2))
        stars.add(findViewById(R.id.star3))
        stars.add(findViewById(R.id.star4))
        stars.add(findViewById(R.id.star5))
    }

    private fun loadProductData() {
        productTitle.text = order.productName ?: "Artwork"

        // For description, we'll need to fetch from product
        order.productId?.let { productId ->
            order.sellerId?.let { sellerId ->
                FirebaseDatabase.getInstance()
                    .getReference("products")
                    .child(sellerId)
                    .child(productId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val product = snapshot.getValue(Product::class.java)
                        productDescription.text = product?.description ?: "No description available"
                    }
            }
        }

        // Load product image
        Glide.with(context)
            .load(order.productImage)
            .placeholder(R.drawable.baseline_image_24)
            .into(productImage)
    }

    private fun setupStarRating() {
        // Default: all stars filled (rating = 5)
        updateStarDisplay(5)

        // Set click listeners for each star
        stars.forEachIndexed { index, star ->
            star.setOnClickListener {
                selectedRating = index + 1
                updateStarDisplay(selectedRating)
            }
        }
    }

    private fun updateStarDisplay(rating: Int) {
        stars.forEachIndexed { index, star ->
            if (index < rating) {
                star.setImageResource(R.drawable.ic_star_filled)
            } else {
                star.setImageResource(R.drawable.ic_star_hollow)
            }
        }
    }

    private fun setupButtons() {
        cancelButton.setOnClickListener {
            dismiss()
        }

        submitButton.setOnClickListener {
            submitRating()
        }
    }

    private fun submitRating() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Please login to rate", Toast.LENGTH_SHORT).show()
            return
        }

        val comment = commentEdit.text.toString().trim()
        val productId = order.productId
        val orderId = order.orderId

        if (productId == null) {
            Toast.makeText(context, "Invalid product", Toast.LENGTH_SHORT).show()
            return
        }

        submitButton.isEnabled = false
        Toast.makeText(context, "Submitting rating...", Toast.LENGTH_SHORT).show()

        // Get user profile image
        val storageRef = FirebaseStorage.getInstance().reference
        val profilePicRef = storageRef.child("images/${currentUser.uid}.jpg")

        profilePicRef.downloadUrl
            .addOnSuccessListener { uri ->
                saveRating(currentUser.uid, currentUser.displayName, uri.toString(), productId, orderId, comment)
            }
            .addOnFailureListener {
                // No profile image, use default
                saveRating(currentUser.uid, currentUser.displayName, null, productId, orderId, comment)
            }
    }

    private fun saveRating(
        userId: String,
        userName: String?,
        profileImage: String?,
        productId: String,
        orderId: String?,
        comment: String
    ) {
        val rating = Rating(
            userId = userId,
            userName = userName ?: "Anonymous",
            userProfileImage = profileImage,
            rating = selectedRating,
            comment = comment.ifEmpty { null },
            timestamp = System.currentTimeMillis(),
            orderId = orderId
        )

        // Save to Firebase: /ratings/{productId}/{userId}
        FirebaseDatabase.getInstance()
            .getReference("ratings")
            .child(productId)
            .child(userId)
            .setValue(rating)
            .addOnSuccessListener {
                Toast.makeText(context, "Rating submitted successfully!", Toast.LENGTH_SHORT).show()
                onRatingSubmittedListener?.invoke()
                dismiss()
            }
            .addOnFailureListener { exception ->
                submitButton.isEnabled = true
                Toast.makeText(
                    context,
                    "Failed to submit rating: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    fun setOnRatingSubmittedListener(listener: () -> Unit) {
        onRatingSubmittedListener = listener
    }
}