package com.example.illustcart

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.cardview.widget.CardView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class AdminHomeFragment : Fragment(), ProductAdapter.OnProductClickListener {

    companion object {
        private const val PICK_BANNER_IMAGE_REQUEST = 100
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private var currentAdminId: String? = null

    private lateinit var bannerImageView: ImageView
    private lateinit var bannerCard: CardView
    private lateinit var searchView: SearchView
    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var noProductsText: TextView
    private lateinit var productAdapter: ProductAdapter

    // Product detail views
    private lateinit var homeLayout: CoordinatorLayout
    private lateinit var productDetailLayout: RelativeLayout
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var reviewsAdapter: ReviewsAdapter
    private val reviewsList = mutableListOf<Rating>()

    private var allProducts: MutableList<Product> = mutableListOf()
    private var filteredProducts: MutableList<Product> = mutableListOf()
    private var bannerImageUri: Uri? = null
    private var currentProduct: Product? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_home, container, false)

        auth = FirebaseAuth.getInstance()
        currentAdminId = auth.currentUser?.uid
        storageReference = FirebaseStorage.getInstance().reference
        databaseReference = FirebaseDatabase.getInstance()
            .getReference("products")
            .child(currentAdminId!!)

        initializeViews(view)
        setupBanner()
        setupSearch()
        setupProductsList()
        setupProductDetailViews(view)
        loadProducts()

        return view
    }

    private fun initializeViews(view: View) {
        bannerImageView = view.findViewById(R.id.admin_banner_imageview)
        bannerCard = view.findViewById(R.id.admin_banner_card)
        searchView = view.findViewById(R.id.admin_searchView)
        productsRecyclerView = view.findViewById(R.id.admin_products_recyclerView)
        noProductsText = view.findViewById(R.id.admin_no_products_text)

        homeLayout = view.findViewById(R.id.admin_home_layout)
        productDetailLayout = view.findViewById(R.id.admin_product_detail_layout)
    }

    private fun setupProductDetailViews(view: View) {
        reviewsRecyclerView = view.findViewById(R.id.admin_reviews_recyclerView)
        reviewsAdapter = ReviewsAdapter(reviewsList)
        reviewsRecyclerView.adapter = reviewsAdapter
        reviewsRecyclerView.layoutManager = LinearLayoutManager(context)

        // Back button in detail view
        view.findViewById<ImageView>(R.id.admin_detail_back_btn)?.setOnClickListener {
            showHome()
        }
    }

    private fun setupBanner() {
        // Load existing banner if available
        loadAdminBanner()

        // Set click listener to upload new banner
        bannerCard.setOnClickListener {
            openBannerImageChooser()
        }
    }

    private fun loadAdminBanner() {
        val bannerRef = storageReference.child("admin_banners/${currentAdminId}/banner.jpg")

        bannerRef.downloadUrl
            .addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .into(bannerImageView)
            }
            .addOnFailureListener {
                // Keep placeholder image
                Log.d("AdminHomeFragment", "No banner image found, using placeholder")
            }
    }

    private fun openBannerImageChooser() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_BANNER_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_BANNER_IMAGE_REQUEST &&
            resultCode == Activity.RESULT_OK &&
            data != null &&
            data.data != null) {

            bannerImageUri = data.data
            uploadBannerImage()
        }
    }

    private fun uploadBannerImage() {
        if (bannerImageUri == null) return

        Toast.makeText(context, "Uploading banner...", Toast.LENGTH_SHORT).show()

        val bannerRef = storageReference.child("admin_banners/${currentAdminId}/banner.jpg")

        bannerRef.putFile(bannerImageUri!!)
            .addOnSuccessListener {
                bannerRef.downloadUrl.addOnSuccessListener { uri ->
                    Glide.with(this)
                        .load(uri)
                        .into(bannerImageView)

                    Toast.makeText(context, "Banner updated!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    context,
                    "Failed to upload banner: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterProducts(query ?: "")
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterProducts(newText ?: "")
                return true
            }
        })
    }

    private fun setupProductsList() {
        productAdapter = ProductAdapter(filteredProducts)
        productAdapter.isDeleteButtonVisible = true
        productsRecyclerView.adapter = productAdapter
        productsRecyclerView.layoutManager = LinearLayoutManager(context)

        // Product click listener - show detail
        productAdapter.onProductClickListener = this

        // Delete listener
        productAdapter.onDeleteClickListener = object : ProductAdapter.OnDeleteClickListener {
            override fun onDeleteClick(product: Product, position: Int) {
                deleteProduct(product)
            }
        }

        // Edit listener
        productAdapter.onEditClickListener = object : ProductAdapter.OnEditClickListener {
            override fun onEditClick(product: Product, position: Int) {
                Log.d("AdminHomeFragment", "Edit button clicked for product: ${product.productName}")
                navigateToEditProduct(product)
            }
        }
    }

    override fun onProductClick(product: Product) {
        showProductDetail(product)
    }

    private fun showProductDetail(product: Product) {
        currentProduct = product

        homeLayout.visibility = View.GONE
        productDetailLayout.visibility = View.VISIBLE

        val view = requireView()

        // Product Name
        view.findViewById<TextView>(R.id.admin_detail_product_name).text = product.productName

        // Artist Name
        view.findViewById<TextView>(R.id.admin_detail_artist).text = "Artist: ${product.sellerName ?: "Unknown"}"

        // Description
        view.findViewById<TextView>(R.id.admin_detail_description).text = product.description ?: "No description available"

        // Category
        view.findViewById<TextView>(R.id.admin_detail_category).text = "Category: ${product.category}"

        // Size
        view.findViewById<TextView>(R.id.admin_detail_size).text = "Size: ${product.productSize}"

        // Prints Available
        val printsAvailable = product.printsAvailable ?: 0
        val printsText = view.findViewById<TextView>(R.id.admin_detail_prints_available)
        if (printsAvailable > 0) {
            printsText.text = "$printsAvailable prints available"
            printsText.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        } else {
            printsText.text = "SOLD OUT"
            printsText.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        }

        // Price
        view.findViewById<TextView>(R.id.admin_detail_price).text = product.productPrice

        // Product Image
        Glide.with(this)
            .load(product.imageUrl)
            .into(view.findViewById<ImageView>(R.id.admin_detail_product_image))

        // Load ratings
        loadProductRatings(product)
    }

    private fun loadProductRatings(product: Product) {
        val productId = product.id ?: return

        val ratingsSection = requireView().findViewById<LinearLayout>(R.id.admin_ratings_section)
        val averageRatingText = requireView().findViewById<TextView>(R.id.admin_average_rating_text)
        val ratingsCountText = requireView().findViewById<TextView>(R.id.admin_ratings_count_text)
        val noReviewsText = requireView().findViewById<TextView>(R.id.admin_no_reviews_text)

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
                        ratingsCountText.text = "($count reviews)"

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
                    Log.e("AdminHomeFragment", "Failed to load ratings: ${error.message}")
                    ratingsSection.visibility = View.GONE
                }
            })
    }

    private fun showHome() {
        homeLayout.visibility = View.VISIBLE
        productDetailLayout.visibility = View.GONE
        currentProduct = null
    }

    private fun loadProducts() {
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                allProducts.clear()

                for (productSnapshot in dataSnapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    product?.let { allProducts.add(it) }
                }

                filterProducts(searchView.query.toString())
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("AdminHomeFragment", "Database error: ${databaseError.message}")
            }
        })
    }

    private fun filterProducts(query: String) {
        filteredProducts.clear()

        if (query.isEmpty()) {
            filteredProducts.addAll(allProducts)
        } else {
            val searchQuery = query.lowercase()
            filteredProducts.addAll(
                allProducts.filter { product ->
                    product.productName?.lowercase()?.contains(searchQuery) == true ||
                            product.sellerName?.lowercase()?.contains(searchQuery) == true ||
                            product.category?.lowercase()?.contains(searchQuery) == true
                }
            )
        }

        productAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (filteredProducts.isEmpty()) {
            productsRecyclerView.visibility = View.GONE
            noProductsText.visibility = View.VISIBLE
        } else {
            productsRecyclerView.visibility = View.VISIBLE
            noProductsText.visibility = View.GONE
        }
    }

    private fun deleteProduct(product: Product) {
        val productId = product.id
        if (productId == null || currentAdminId == null) {
            Toast.makeText(context, "Error: Invalid product or admin ID", Toast.LENGTH_SHORT).show()
            return
        }

        databaseReference.child(productId).removeValue()
            .addOnSuccessListener {
                val imageRef = storageReference.child("product_images/$currentAdminId/${productId}.jpg")
                imageRef.delete()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Product deleted successfully", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { exception ->
                        Log.e("AdminHomeFragment", "Failed to delete image: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    context,
                    "Failed to delete product: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun navigateToEditProduct(product: Product) {
        Log.d("AdminHomeFragment", "Navigating to edit product fragment")

        try {
            val fragment = AdminEditProductFragment.newInstance(product)

            parentFragmentManager.beginTransaction()
                .replace(R.id.admin_fragment_container, fragment)
                .addToBackStack("editProduct")
                .commit()

            Log.d("AdminHomeFragment", "Edit fragment transaction committed successfully")
        } catch (e: Exception) {
            Log.e("AdminHomeFragment", "Error navigating to edit fragment: ${e.message}")
        }
    }
}