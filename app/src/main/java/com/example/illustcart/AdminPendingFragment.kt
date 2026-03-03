package com.example.illustcart

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminPendingFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var currentAdminId: String? = null

    private lateinit var pendingRecyclerView: RecyclerView
    private lateinit var emptyPendingText: TextView
    private lateinit var ordersAdapter: OrdersAdapter

    private val ordersList = mutableListOf<Order>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_pending, container, false)

        auth = FirebaseAuth.getInstance()
        currentAdminId = auth.currentUser?.uid

        initializeViews(view)
        setupRecyclerView()
        loadPendingOrders()

        return view
    }

    private fun initializeViews(view: View) {
        pendingRecyclerView = view.findViewById(R.id.admin_pending_recyclerView)
        emptyPendingText = view.findViewById(R.id.admin_empty_pending_text)
    }

    private fun setupRecyclerView() {
        ordersAdapter = OrdersAdapter(ordersList, isCompletedView = false)
        pendingRecyclerView.adapter = ordersAdapter
        pendingRecyclerView.layoutManager = LinearLayoutManager(context)

        // Complete order listener
        ordersAdapter.onCompleteClickListener = object : OrdersAdapter.OnCompleteClickListener {
            override fun onCompleteClick(order: Order) {
                completeOrder(order)
            }
        }
    }

    private fun loadPendingOrders() {
        val ordersRef = FirebaseDatabase.getInstance().getReference("orders")

        ordersRef.orderByChild("sellerId").equalTo(currentAdminId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    ordersList.clear()

                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(Order::class.java)
                        if (order?.status == "pending") {
                            ordersList.add(order)
                        }
                    }

                    // Update empty state
                    if (ordersList.isEmpty()) {
                        pendingRecyclerView.visibility = View.GONE
                        emptyPendingText.visibility = View.VISIBLE
                    } else {
                        pendingRecyclerView.visibility = View.VISIBLE
                        emptyPendingText.visibility = View.GONE
                    }

                    ordersAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AdminPendingFragment", "Failed to load orders: ${error.message}")
                }
            })
    }

    private fun completeOrder(order: Order) {
        val orderId = order.orderId ?: return

        val updates = hashMapOf<String, Any>(
            "status" to "completed",
            "completionDate" to System.currentTimeMillis()
        )

        FirebaseDatabase.getInstance().getReference("orders")
            .child(orderId)
            .updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(context, "Order completed!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to complete order", Toast.LENGTH_SHORT).show()
            }
    }
}