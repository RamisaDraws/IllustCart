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

class PendingOrdersFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var pendingRecyclerView: RecyclerView
    private lateinit var emptyPendingText: TextView
    private lateinit var pendingAdapter: PendingUserOrdersAdapter

    private val pendingOrders = mutableListOf<Order>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_pending_orders, container, false)

        auth = FirebaseAuth.getInstance()

        initializeViews(view)
        setupRecyclerView()
        loadPendingOrders()

        return view
    }

    private fun initializeViews(view: View) {
        pendingRecyclerView = view.findViewById(R.id.pending_orders_recyclerView)
        emptyPendingText = view.findViewById(R.id.empty_pending_orders_text)
    }

    private fun setupRecyclerView() {
        pendingAdapter = PendingUserOrdersAdapter(pendingOrders)
        pendingRecyclerView.adapter = pendingAdapter
        pendingRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun loadPendingOrders() {
        val userId = auth.currentUser?.uid ?: return
        val ordersRef = FirebaseDatabase.getInstance().getReference("orders")

        ordersRef.orderByChild("buyerId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    pendingOrders.clear()

                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(Order::class.java)
                        if (order?.status == "pending") {
                            pendingOrders.add(order)
                        }
                    }

                    // Sort by order date (newest first)
                    pendingOrders.sortByDescending { it.orderDate }

                    // Update UI
                    if (pendingOrders.isEmpty()) {
                        pendingRecyclerView.visibility = View.GONE
                        emptyPendingText.visibility = View.VISIBLE
                    } else {
                        pendingRecyclerView.visibility = View.VISIBLE
                        emptyPendingText.visibility = View.GONE
                    }

                    pendingAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("PendingOrdersFragment", "Failed to load orders: ${error.message}")
                }
            })
    }
}