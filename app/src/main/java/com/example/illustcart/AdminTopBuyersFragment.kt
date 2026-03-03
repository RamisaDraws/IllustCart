package com.example.illustcart

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class AdminTopBuyersFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var currentAdminId: String? = null

    private lateinit var topBuyersRecyclerView: RecyclerView
    private lateinit var emptyTopBuyersText: TextView
    private lateinit var buyersAdapter: TopBuyersAdapter

    private val buyerAnalyticsList = mutableListOf<BuyerAnalytics>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_top_buyers, container, false)

        auth = FirebaseAuth.getInstance()
        currentAdminId = auth.currentUser?.uid

        initializeViews(view)
        setupRecyclerView()
        loadBuyersData()

        return view
    }

    private fun initializeViews(view: View) {
        topBuyersRecyclerView = view.findViewById(R.id.top_buyers_recyclerView)
        emptyTopBuyersText = view.findViewById(R.id.empty_top_buyers_text)
    }

    private fun setupRecyclerView() {
        buyersAdapter = TopBuyersAdapter(buyerAnalyticsList)
        topBuyersRecyclerView.adapter = buyersAdapter
        topBuyersRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadBuyersData() {
        if (currentAdminId == null) return

        val ordersRef = FirebaseDatabase.getInstance().getReference("orders")

        ordersRef.orderByChild("sellerId").equalTo(currentAdminId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Map to track spending per buyer
                    val spendingMap = mutableMapOf<String, Triple<String, String, Double>>()

                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(Order::class.java)
                        if (order?.status == "completed" && order.buyerId != null) {
                            val buyerId = order.buyerId!!
                            val buyerName = order.buyerName ?: "Unknown"
                            val buyerEmail = order.buyerEmail ?: ""
                            val price = order.productPrice?.replace("$", "")?.toDoubleOrNull() ?: 0.0

                            val current = spendingMap[buyerId]
                            if (current != null) {
                                spendingMap[buyerId] = Triple(buyerName, buyerEmail, current.third + price)
                            } else {
                                spendingMap[buyerId] = Triple(buyerName, buyerEmail, price)
                            }
                        }
                    }

                    // Build buyers list
                    buyerAnalyticsList.clear()

                    for ((buyerId, data) in spendingMap) {
                        val analytics = BuyerAnalytics(
                            buyerId = buyerId,
                            buyerName = data.first,
                            buyerEmail = data.second,
                            totalSpent = data.third
                        )
                        buyerAnalyticsList.add(analytics)
                    }

                    // Sort by total spent and take top 5
                    buyerAnalyticsList.sortByDescending { it.totalSpent }
                    if (buyerAnalyticsList.size > 5) {
                        val top5 = buyerAnalyticsList.take(5)
                        buyerAnalyticsList.clear()
                        buyerAnalyticsList.addAll(top5)
                    }

                    // Load profile images
                    loadBuyerProfileImages()

                    updateEmptyState()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AdminTopBuyers", "Failed to load orders: ${error.message}")
                }
            })
    }

    private fun loadBuyerProfileImages() {
        val storageRef = FirebaseStorage.getInstance().reference

        for (buyer in buyerAnalyticsList) {
            if (buyer.buyerId != null) {
                val profilePicRef = storageRef.child("images/${buyer.buyerId}.jpg")

                profilePicRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        buyer.profileImageUrl = uri.toString()
                        buyersAdapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener {
                        // Profile picture not available, adapter will use placeholder
                    }
            }
        }

        buyersAdapter.notifyDataSetChanged()
    }

    private fun updateEmptyState() {
        if (buyerAnalyticsList.isEmpty()) {
            topBuyersRecyclerView.visibility = View.GONE
            emptyTopBuyersText.visibility = View.VISIBLE
        } else {
            topBuyersRecyclerView.visibility = View.VISIBLE
            emptyTopBuyersText.visibility = View.GONE
        }
    }
}