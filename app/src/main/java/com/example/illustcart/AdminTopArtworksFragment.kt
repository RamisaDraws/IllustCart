package com.example.illustcart

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminTopArtworksFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var currentAdminId: String? = null

    private lateinit var sortRadioGroup: RadioGroup
    private lateinit var topArtworksRecyclerView: RecyclerView
    private lateinit var emptyTopArtworksText: TextView
    private lateinit var analyticsAdapter: AnalyticsArtworkAdapter

    private val productAnalyticsList = mutableListOf<ProductAnalytics>()
    private val allProductAnalyticsList = mutableListOf<ProductAnalytics>()
    private val productMap = mutableMapOf<String, Product>()
    private var currentSortMode = "prints_sold"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_top_artworks, container, false)

        auth = FirebaseAuth.getInstance()
        currentAdminId = auth.currentUser?.uid

        initializeViews(view)
        setupRecyclerView()
        setupSortOptions()
        loadAnalyticsData()

        return view
    }

    private fun initializeViews(view: View) {
        sortRadioGroup = view.findViewById(R.id.sort_radio_group)
        topArtworksRecyclerView = view.findViewById(R.id.top_artworks_recyclerView)
        emptyTopArtworksText = view.findViewById(R.id.empty_top_artworks_text)
    }

    private fun setupRecyclerView() {
        analyticsAdapter = AnalyticsArtworkAdapter(productAnalyticsList)
        topArtworksRecyclerView.adapter = analyticsAdapter
        topArtworksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupSortOptions() {
        sortRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radio_prints_sold -> {
                    currentSortMode = "prints_sold"
                    sortByPrintsSold()
                }
                R.id.radio_total_earned -> {
                    currentSortMode = "total_earned"
                    sortByTotalEarned()
                }
                R.id.radio_top_rated -> {
                    currentSortMode = "top_rated"
                    sortByTopRated()
                }
            }
        }
    }

    private fun loadAnalyticsData() {
        if (currentAdminId == null) return

        // First, load all products by this admin
        val productsRef = FirebaseDatabase.getInstance()
            .getReference("products")
            .child(currentAdminId!!)

        productsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                productMap.clear()

                for (productSnapshot in snapshot.children) {
                    val product = productSnapshot.getValue(Product::class.java)
                    product?.id?.let { productId ->
                        productMap[productId] = product
                    }
                }

                // Now load orders to calculate sales
                loadSalesData()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminTopArtworks", "Failed to load products: ${error.message}")
            }
        })
    }

    private fun loadSalesData() {
        val ordersRef = FirebaseDatabase.getInstance().getReference("orders")

        ordersRef.orderByChild("sellerId").equalTo(currentAdminId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val salesMap = mutableMapOf<String, Pair<Int, Double>>()

                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(Order::class.java)
                        if (order?.status == "completed") {
                            val productId = order.productId ?: continue
                            val price = order.productPrice?.replace("$", "")?.toDoubleOrNull() ?: 0.0

                            val current = salesMap[productId] ?: Pair(0, 0.0)
                            salesMap[productId] = Pair(current.first + 1, current.second + price)
                        }
                    }

                    // Build analytics list with sales data
                    allProductAnalyticsList.clear()

                    for ((productId, salesData) in salesMap) {
                        val product = productMap[productId]
                        if (product != null) {
                            val analytics = ProductAnalytics(
                                productId = product.id,
                                productName = product.productName,
                                description = product.description,
                                productSize = product.productSize,
                                productPrice = product.productPrice,
                                imageUrl = product.imageUrl,
                                category = product.category,
                                printsSold = salesData.first,
                                totalEarned = salesData.second
                            )
                            allProductAnalyticsList.add(analytics)
                        }
                    }

                    // Load ratings for all products
                    loadRatingsData()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AdminTopArtworks", "Failed to load orders: ${error.message}")
                }
            })
    }

    private fun loadRatingsData() {
        val ratingsRef = FirebaseDatabase.getInstance().getReference("ratings")

        ratingsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Calculate average ratings for each product
                for (analytics in allProductAnalyticsList) {
                    val productId = analytics.productId ?: continue

                    val productRatingsSnapshot = snapshot.child(productId)
                    if (productRatingsSnapshot.exists()) {
                        var totalRating = 0.0
                        var count = 0

                        for (ratingSnapshot in productRatingsSnapshot.children) {
                            val rating = ratingSnapshot.getValue(Rating::class.java)
                            rating?.let {
                                totalRating += it.rating
                                count++
                            }
                        }

                        if (count > 0) {
                            analytics.averageRating = totalRating / count
                            analytics.ratingCount = count
                        }
                    }
                }

                // Apply current sort
                when (currentSortMode) {
                    "prints_sold" -> sortByPrintsSold()
                    "total_earned" -> sortByTotalEarned()
                    "top_rated" -> sortByTopRated()
                }

                updateEmptyState()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AdminTopArtworks", "Failed to load ratings: ${error.message}")
                // Still show data even if ratings fail to load
                sortByPrintsSold()
                updateEmptyState()
            }
        })
    }

    private fun sortByPrintsSold() {
        productAnalyticsList.clear()
        productAnalyticsList.addAll(allProductAnalyticsList)
        productAnalyticsList.sortByDescending { it.printsSold }
        analyticsAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun sortByTotalEarned() {
        productAnalyticsList.clear()
        productAnalyticsList.addAll(allProductAnalyticsList)
        productAnalyticsList.sortByDescending { it.totalEarned }
        analyticsAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun sortByTopRated() {
        productAnalyticsList.clear()

        // Filter to only show products with ratings
        val ratedProducts = allProductAnalyticsList.filter { it.ratingCount > 0 }
        productAnalyticsList.addAll(ratedProducts)

        // Sort by average rating (highest first)
        productAnalyticsList.sortByDescending { it.averageRating }

        analyticsAdapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun updateEmptyState() {
        if (productAnalyticsList.isEmpty()) {
            topArtworksRecyclerView.visibility = View.GONE
            emptyTopArtworksText.visibility = View.VISIBLE

            // Show appropriate message based on sort mode
            emptyTopArtworksText.text = when (currentSortMode) {
                "top_rated" -> "No rated artworks yet"
                else -> "No sales data available yet"
            }
        } else {
            topArtworksRecyclerView.visibility = View.VISIBLE
            emptyTopArtworksText.visibility = View.GONE
        }
    }
}