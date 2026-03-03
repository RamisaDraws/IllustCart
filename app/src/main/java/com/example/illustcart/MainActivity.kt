package com.example.illustcart

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.messaging.FirebaseMessaging
import com.squareup.picasso.Picasso
import java.io.File
import android.Manifest
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity(), ProductAdapter.OnProductClickListener, ProductAdapter.OnCartIconClickListener {
    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var imageView: ImageView
    private lateinit var banner_imageview: ImageView
    private lateinit var storageRef: StorageReference
    private lateinit var profilePicRef: StorageReference

    private lateinit var databaseReference: DatabaseReference
    private lateinit var productRecyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var noArtworksText: TextView
    private lateinit var artworksSectionTitle: TextView

    private var allProducts: MutableList<Product> = mutableListOf()
    private var filteredProducts: MutableList<Product> = mutableListOf()
    private var selectedCategories: MutableSet<String> = mutableSetOf()
    private var searchQuery: String = ""

    private val categoryViews = mutableMapOf<String, LinearLayout>()
    private val categoryIndicators = mutableMapOf<String, View>()

    private var currentProduct: Product? = null
    private var flashSaleCountdownTimer: CountDownTimer? = null

    // Rating/Reviews related
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var reviewsAdapter: ReviewsAdapter
    private val reviewsList = mutableListOf<Rating>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkStoragePermission()

        changeStatusBarColor("#91837E")

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = auth.currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val textView = findViewById<TextView>(R.id.name)
        textView.text = user.displayName ?: "User"

        val logout = findViewById<ImageView>(R.id.logout)
        logout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        imageView = findViewById(R.id.profile_picture_image_view)
        banner_imageview = findViewById(R.id.banner_imageview)
        storageRef = FirebaseStorage.getInstance().reference
        profilePicRef = storageRef.child("images/${user.uid}.jpg")

        profilePicRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this).load(uri).into(imageView)
        }.addOnFailureListener { exception ->
            Log.d("ProfilePic", "Profile picture not available", exception)
        }

        val shimmerViewContainer: ShimmerFrameLayout = findViewById(R.id.shimmer_view_container)
        shimmerViewContainer.startShimmer()

        val latestImageRef = storageRef.child("common_folder/latest_banner.jpg")
        latestImageRef.downloadUrl
            .addOnSuccessListener { imageUrl ->
                Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.color.white)
                    .error(R.drawable.ic_launcher_foreground)
                    .into(banner_imageview)
                shimmerViewContainer.stopShimmer()
                shimmerViewContainer.setShimmer(null)
            }
            .addOnFailureListener { exception ->
                Log.e("MainActivity", "Error fetching banner image", exception)
                shimmerViewContainer.stopShimmer()
                shimmerViewContainer.setShimmer(null)
            }

        val userRef = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val phone = snapshot.child("phone").getValue(String::class.java) ?: "Not provided"
                val userInfoTextView = findViewById<TextView>(R.id.name21)
                userInfoTextView.text = "Welcome ${user.displayName}\n\nYour Phone Number Is: $phone"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to retrieve user data", Toast.LENGTH_SHORT).show()
            }
        })

        databaseReference = FirebaseDatabase.getInstance().getReference("products")
        productRecyclerView = findViewById(R.id.productRecyclerView)
        noArtworksText = findViewById(R.id.noArtworksText)
        artworksSectionTitle = findViewById(R.id.artworksSectionTitle)

        productAdapter = ProductAdapter(filteredProducts)
        productAdapter.onProductClickListener = this
        productAdapter.onCartIconClickListener = this
        productAdapter.isDeleteButtonVisible = false
        productRecyclerView.adapter = productAdapter
        productRecyclerView.layoutManager = GridLayoutManager(this, 2)

        // Clean up expired flash sales on app startup
        cleanupExpiredFlashSales()

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                allProducts.clear()
                for (sellerSnapshot in dataSnapshot.children) {
                    for (productSnapshot in sellerSnapshot.children) {
                        val product = productSnapshot.getValue(Product::class.java)
                        product?.let { allProducts.add(it) }
                    }
                }
                applyFilters()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("MainActivity", "Database error: ${databaseError.message}")
            }
        })

        setupCategoryViews()
        setupCategoryClickListeners()
        setupSearchView()
        setupBottomNavigation()

        requestNotificationPermission()
        subscribeToFlashSalesTopic()

        setupBackPressHandler()
    }

    /**
     * Clean up any expired flash sales in Firebase when app starts
     * This handles cases where the app was closed and timer didn't run
     */
    private fun cleanupExpiredFlashSales() {
        val currentTime = System.currentTimeMillis()

        databaseReference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                var cleanedCount = 0

                for (sellerSnapshot in dataSnapshot.children) {
                    val sellerId = sellerSnapshot.key ?: continue

                    for (productSnapshot in sellerSnapshot.children) {
                        val product = productSnapshot.getValue(Product::class.java)
                        val productId = productSnapshot.key

                        if (product != null && productId != null) {
                            val isFlashSale = product.isFlashSale ?: false
                            val flashSaleEndTime = product.flashSaleEndTime ?: 0L

                            // Check if flash sale has expired
                            if (isFlashSale && flashSaleEndTime > 0 && flashSaleEndTime < currentTime) {
                                // Flash sale expired - update Firebase
                                val productRef = FirebaseDatabase.getInstance()
                                    .getReference("products")
                                    .child(sellerId)
                                    .child(productId)

                                val updates = hashMapOf<String, Any>(
                                    "isFlashSale" to false,
                                    "productPrice" to (product.originalPrice ?: product.productPrice ?: "$0"),
                                    "flashSaleEndTime" to 0L,
                                    "discountRate" to 0,
                                    "originalPrice" to ""
                                )

                                productRef.updateChildren(updates)
                                    .addOnSuccessListener {
                                        cleanedCount++
                                        Log.d("MainActivity", "âœ… Cleaned up expired flash sale for product: ${product.productName}")
                                    }
                                    .addOnFailureListener { error ->
                                        Log.e("MainActivity", "âŒ Failed to cleanup flash sale for ${product.productName}: ${error.message}")
                                    }
                            }
                        }
                    }
                }

                if (cleanedCount > 0) {
                    Log.d("MainActivity", "ðŸ§¹ Cleaned up $cleanedCount expired flash sale(s)")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Failed to cleanup expired flash sales: ${error.message}")
            }
        })
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val productDetailLayout = findViewById<RelativeLayout>(R.id.product_detail_layout)

                if (productDetailLayout.visibility == View.VISIBLE) {
                    // If viewing product detail, go back to home
                    showHome()
                } else {
                    // If on main home, show exit dialog
                    showExitConfirmationDialog()
                }
            }
        })
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.navigation_home

        bottomNav.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_home -> true
                R.id.navigation_cart -> {
                    startActivity(Intent(this, CartActivity::class.java))
                    true
                }
                R.id.navigation_orders -> {
                    startActivity(Intent(this, OrdersActivity::class.java))
                    true
                }
                R.id.navigation_you -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    override fun onProductClick(product: Product) {
        showProductDetail(product)
    }

    override fun onCartIconClick(product: Product) {
        addToCart(product)
    }

    private fun showProductDetail(product: Product) {
        currentProduct = product

        // Cancel any existing timer
        flashSaleCountdownTimer?.cancel()

        findViewById<ScrollView>(R.id.home_layout).visibility = View.GONE
        findViewById<RelativeLayout>(R.id.search_layout).visibility = View.GONE
        findViewById<RelativeLayout>(R.id.product_detail_layout).visibility = View.VISIBLE

        // Product Name
        findViewById<TextView>(R.id.detail_product_name).text = product.productName

        // Artist Name (sellerName is the artist)
        findViewById<TextView>(R.id.detail_artist).text = "Artist: ${product.sellerName ?: "Unknown"}"

        // Description
        findViewById<TextView>(R.id.detail_description).text = product.description ?: "No description available"

        // Category
        findViewById<TextView>(R.id.detail_category).text = "Category: ${product.category}"

        // Size
        findViewById<TextView>(R.id.detail_size).text = "Size: ${product.productSize}"

        // Prints Available
        val printsAvailable = product.printsAvailable ?: 0
        val printsText = findViewById<TextView>(R.id.detail_prints_available)
        if (printsAvailable > 0) {
            printsText.text = "$printsAvailable prints available"
            printsText.setTextColor(getColor(android.R.color.holo_green_dark))
        } else {
            printsText.text = "SOLD OUT"
            printsText.setTextColor(getColor(android.R.color.holo_red_dark))
        }

        // Flash Sale Handling
        val flashSaleBadge = findViewById<TextView>(R.id.detail_flash_sale_badge)
        val flashSaleTimer = findViewById<TextView>(R.id.detail_flash_sale_timer)
        val priceTextView = findViewById<TextView>(R.id.detail_price)
        val originalPriceTextView = findViewById<TextView>(R.id.detail_original_price)

        val isFlashSaleActive = PriceHelper.isFlashSaleActive(product)

        if (isFlashSaleActive) {
            // Show flash sale UI
            flashSaleBadge.visibility = View.VISIBLE
            flashSaleTimer.visibility = View.VISIBLE
            originalPriceTextView.visibility = View.VISIBLE

            // Set badge text
            flashSaleBadge.text = "FLASH SALE!\n${product.discountRate}% OFF"

            // Show original price with strikethrough
            originalPriceTextView.text = product.originalPrice
            originalPriceTextView.paintFlags = originalPriceTextView.paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG

            // Show discounted price
            priceTextView.text = PriceHelper.getDisplayPrice(product)
            priceTextView.setTextColor(getColor(android.R.color.holo_red_dark))

            // Start countdown timer
            val flashSaleEndTime = product.flashSaleEndTime ?: 0L
            val remainingMillis = flashSaleEndTime - System.currentTimeMillis()

            flashSaleCountdownTimer = object : CountDownTimer(remainingMillis, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val seconds = (millisUntilFinished / 1000).toInt()
                    val hours = seconds / 3600
                    val minutes = (seconds % 3600) / 60
                    val secs = seconds % 60
                    flashSaleTimer.text = "ENDS IN %02d:%02d:%02d".format(hours, minutes, secs)
                }

                override fun onFinish() {
                    // Flash sale ended - update Firebase
                    if (product.id != null && product.sellerId != null) {
                        val productRef = FirebaseDatabase.getInstance()
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
                                // Update UI to show regular price
                                flashSaleBadge.visibility = View.GONE
                                flashSaleTimer.visibility = View.GONE
                                originalPriceTextView.visibility = View.GONE
                                priceTextView.text = product.originalPrice ?: product.productPrice
                                priceTextView.setTextColor(getColor(android.R.color.black))

                                Toast.makeText(this@MainActivity, "Flash sale ended!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }.start()
        } else {
            // No flash sale - hide flash sale UI
            flashSaleBadge.visibility = View.GONE
            flashSaleTimer.visibility = View.GONE
            originalPriceTextView.visibility = View.GONE

            // Show regular price
            priceTextView.text = PriceHelper.getDisplayPrice(product)
            priceTextView.setTextColor(getColor(android.R.color.black))
        }

        // Product Image
        Glide.with(this)
            .load(product.imageUrl)
            .into(findViewById<ImageView>(R.id.detail_product_image))

        // Add to Cart Button
        findViewById<Button>(R.id.add_to_cart_btn).setOnClickListener {
            addToCart(product)
        }

        // Buy Now Button
        findViewById<Button>(R.id.buy_now_btn).setOnClickListener {
            buyNow(product)
        }

        // Load ratings
        loadProductRatings(product)
    }

    private fun loadProductRatings(product: Product) {
        val productId = product.id ?: return

        val ratingsSection = findViewById<LinearLayout>(R.id.ratings_section)
        reviewsRecyclerView = findViewById(R.id.reviews_recyclerView)
        val averageRatingText = findViewById<TextView>(R.id.average_rating_text)
        val ratingsCountText = findViewById<TextView>(R.id.ratings_count_text)
        val noReviewsText = findViewById<TextView>(R.id.no_reviews_text)

        // Setup RecyclerView
        reviewsAdapter = ReviewsAdapter(reviewsList)
        reviewsRecyclerView.adapter = reviewsAdapter
        reviewsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Load ratings from Firebase
        FirebaseDatabase.getInstance()
            .getReference("ratings")
            .child(productId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    reviewsList.clear()

                    var totalRating = 0.0
                    var count = 0

                    for (ratingSnapshot in snapshot.children) {
                        val rating = ratingSnapshot.getValue(Rating::class.java)
                        rating?.let {
                            reviewsList.add(it)
                            totalRating += it.rating
                            count++
                        }
                    }

                    if (count > 0) {
                        // Calculate average
                        val average = totalRating / count

                        // Show ratings section
                        ratingsSection.visibility = View.VISIBLE
                        averageRatingText.text = "%.1f".format(average)
                        ratingsCountText.text = "($count)"

                        // Sort reviews by timestamp (newest first)
                        reviewsList.sortByDescending { it.timestamp }

                        // Show/hide empty state
                        if (reviewsList.isEmpty()) {
                            reviewsRecyclerView.visibility = View.GONE
                            noReviewsText.visibility = View.VISIBLE
                        } else {
                            reviewsRecyclerView.visibility = View.VISIBLE
                            noReviewsText.visibility = View.GONE
                        }

                        reviewsAdapter.notifyDataSetChanged()
                    } else {
                        // No ratings yet
                        ratingsSection.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("MainActivity", "Failed to load ratings: ${error.message}")
                    ratingsSection.visibility = View.GONE
                }
            })
    }

    private fun showHome() {
        findViewById<ScrollView>(R.id.home_layout).visibility = View.VISIBLE
        findViewById<RelativeLayout>(R.id.search_layout).visibility = View.VISIBLE
        findViewById<RelativeLayout>(R.id.product_detail_layout).visibility = View.GONE
        currentProduct = null
    }

    private fun addToCart(product: Product) {
        val userId = auth.currentUser?.uid ?: return

        // Check if user is trying to add their own product
        if (product.sellerId == userId) {
            Toast.makeText(this, "You cannot buy your own artwork", Toast.LENGTH_SHORT).show()
            return
        }

        val cartRef = FirebaseDatabase.getInstance()
            .getReference("carts")
            .child(userId)

        val cartItemId = cartRef.push().key ?: return

        // Use PriceHelper to get the correct price (with discount if flash sale is active)
        val finalPrice = PriceHelper.getDisplayPrice(product)

        val cartItem = CartItem(
            cartItemId = cartItemId,
            userId = userId,
            productId = product.id,
            productName = product.productName,
            productPrice = finalPrice,  // Use discounted price if flash sale is active
            productImage = product.imageUrl,
            productSize = product.productSize,
            category = product.category,
            sellerId = product.sellerId,
            sellerName = product.sellerName,
            addedDate = System.currentTimeMillis()
        )

        cartRef.child(cartItemId).setValue(cartItem)
            .addOnSuccessListener {
                Toast.makeText(this, "Added to cart!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to add to cart", Toast.LENGTH_SHORT).show()
            }
    }

    private fun buyNow(product: Product) {
        val user = auth.currentUser ?: return

        // Check if user is trying to buy their own product
        if (product.sellerId == user.uid) {
            Toast.makeText(this, "You cannot buy your own artwork", Toast.LENGTH_SHORT).show()
            return
        }

        // First, fetch the current product to check availability and get current prints
        val productRef = FirebaseDatabase.getInstance()
            .getReference("products")
            .child(product.sellerId!!)
            .child(product.id!!)

        productRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentProduct = snapshot.getValue(Product::class.java)

                if (currentProduct == null) {
                    Toast.makeText(this@MainActivity, "Product no longer available", Toast.LENGTH_SHORT).show()
                    return
                }

                // Check if product is sold out
                val printsAvailable = currentProduct.printsAvailable ?: 0
                if (printsAvailable <= 0) {
                    Toast.makeText(this@MainActivity, "This artwork is sold out", Toast.LENGTH_SHORT).show()
                    return
                }

                val ordersRef = FirebaseDatabase.getInstance().getReference("orders")
                val orderId = ordersRef.push().key ?: return

                // Use PriceHelper to get the correct price (with discount if flash sale)
                val finalPrice = PriceHelper.getDisplayPrice(currentProduct)

                val order = Order(
                    orderId = orderId,
                    productId = currentProduct.id,
                    productName = currentProduct.productName,
                    productPrice = finalPrice,
                    productImage = currentProduct.imageUrl,
                    productSize = currentProduct.productSize,
                    category = currentProduct.category,
                    artist = null,  // No longer using artist field
                    sellerId = currentProduct.sellerId,
                    sellerName = currentProduct.sellerName,
                    buyerId = user.uid,
                    buyerName = user.displayName ?: "Unknown",
                    buyerEmail = user.email ?: "",
                    status = "pending",
                    orderDate = System.currentTimeMillis()
                )

                ordersRef.child(orderId).setValue(order)
                    .addOnSuccessListener {
                        // Decrease prints available in Firebase
                        productRef.child("printsAvailable")
                            .setValue(printsAvailable - 1)
                            .addOnSuccessListener {
                                Toast.makeText(this@MainActivity, "Order placed successfully!", Toast.LENGTH_SHORT).show()
                                showHome()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@MainActivity, "Order placed but inventory update failed", Toast.LENGTH_SHORT).show()
                                showHome()
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@MainActivity, "Failed to place order", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to check product availability", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Do you want to quit the app?")
            .setPositiveButton("Yes") { dialog, which ->
                finish()
            }
            .setNegativeButton("No") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setupCategoryViews() {
        val categories = listOf(
            "FanArt" to R.id.catFanArt,
            "Original" to R.id.catOriginal,
            "Character" to R.id.catCharacter,
            "Chibi" to R.id.catChibi,
            "Background" to R.id.catBackground,
            "IllustA" to R.id.catIllusta,
            "Comic" to R.id.catComic
        )

        for ((categoryName, viewId) in categories) {
            val categoryLayout = findViewById<LinearLayout>(viewId)
            categoryViews[categoryName] = categoryLayout

            val indicator = View(this)
            indicator.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                8
            )
            indicator.setBackgroundColor(getColor(android.R.color.holo_blue_dark))
            indicator.visibility = View.GONE

            categoryLayout.addView(indicator)
            categoryIndicators[categoryName] = indicator
        }
    }

    private fun setupCategoryClickListeners() {
        categoryViews.forEach { (categoryName, layout) ->
            layout.setOnClickListener {
                toggleCategorySelection(categoryName)
            }
        }
    }

    private fun toggleCategorySelection(category: String) {
        if (selectedCategories.contains(category)) {
            selectedCategories.remove(category)
            categoryIndicators[category]?.visibility = View.GONE
        } else {
            selectedCategories.add(category)
            categoryIndicators[category]?.visibility = View.VISIBLE
        }

        applyFilters()

        val message = when {
            selectedCategories.isEmpty() -> "Showing all artworks"
            selectedCategories.size == 1 -> "Showing $category artworks"
            else -> "Showing ${selectedCategories.size} categories"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupSearchView() {
        val searchView = findViewById<SearchView>(R.id.searchView)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchQuery = query?.trim() ?: ""
                applyFilters()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText?.trim() ?: ""
                applyFilters()
                return true
            }
        })
    }

    private fun applyFilters() {
        filteredProducts.clear()

        var productsToSearch = if (selectedCategories.isEmpty()) {
            allProducts
        } else {
            allProducts.filter { product ->
                selectedCategories.contains(product.category)
            }
        }

        if (searchQuery.isNotEmpty()) {
            productsToSearch = productsToSearch.filter { product ->
                product.productName?.contains(searchQuery, ignoreCase = true) == true ||
                        product.sellerName?.contains(searchQuery, ignoreCase = true) == true ||
                        product.productSize?.contains(searchQuery, ignoreCase = true) == true
            }
        }

        filteredProducts.addAll(productsToSearch)
        productAdapter.notifyDataSetChanged()
        updateProductDisplay()
    }

    private fun updateProductDisplay() {
        if (filteredProducts.isEmpty()) {
            productRecyclerView.visibility = View.GONE
            artworksSectionTitle.visibility = View.GONE
            noArtworksText.visibility = View.VISIBLE
        } else {
            productRecyclerView.visibility = View.VISIBLE
            artworksSectionTitle.visibility = View.VISIBLE
            noArtworksText.visibility = View.GONE
        }
    }

    private fun checkStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
                Log.d("MainActivity", "ðŸ“± Requesting notification permission")
            } else {
                Log.d("MainActivity", "âœ… Notification permission already granted")
            }
        } else {
            Log.d("MainActivity", "âœ… Notification permission not required (Android < 13)")
        }
    }

    private fun subscribeToFlashSalesTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("flash_sales")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("MainActivity", "Subscribed to flash_sales topic")
                } else {
                    Log.e("MainActivity", "Failed to subscribe to flash_sales topic: ${task.exception?.message}")
                }
            }
    }

    private fun changeStatusBarColor(color: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.apply {
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                statusBarColor = android.graphics.Color.parseColor(color)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        flashSaleCountdownTimer?.cancel()
    }
}