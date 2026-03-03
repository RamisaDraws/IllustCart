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

class AdminSalesFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private var currentAdminId: String? = null

    private lateinit var totalSalesText: TextView
    private lateinit var salesRecyclerView: RecyclerView
    private lateinit var emptySalesText: TextView
    private lateinit var salesAdapter: OrdersAdapter

    private val salesList = mutableListOf<Order>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_sales, container, false)

        auth = FirebaseAuth.getInstance()
        currentAdminId = auth.currentUser?.uid

        initializeViews(view)
        setupRecyclerView()
        loadSales()

        return view
    }

    private fun initializeViews(view: View) {
        totalSalesText = view.findViewById(R.id.admin_total_sales_text)
        salesRecyclerView = view.findViewById(R.id.admin_sales_recyclerView)
        emptySalesText = view.findViewById(R.id.admin_empty_sales_text)
    }

    private fun setupRecyclerView() {
        salesAdapter = OrdersAdapter(salesList, isCompletedView = true)
        salesRecyclerView.adapter = salesAdapter
        salesRecyclerView.layoutManager = LinearLayoutManager(context)

        // No complete button needed for completed sales
        salesAdapter.onCompleteClickListener = null
    }

    private fun loadSales() {
        val ordersRef = FirebaseDatabase.getInstance().getReference("orders")

        ordersRef.orderByChild("sellerId").equalTo(currentAdminId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    salesList.clear()
                    var completedCount = 0

                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(Order::class.java)
                        if (order?.status == "completed") {
                            salesList.add(order)
                            completedCount++
                        }
                    }

                    // Update total sales count with "Sales Overview:" prefix
                    totalSalesText.text = "Total Sales: $completedCount"

                    // Update empty state
                    if (salesList.isEmpty()) {
                        salesRecyclerView.visibility = View.GONE
                        emptySalesText.visibility = View.VISIBLE
                    } else {
                        salesRecyclerView.visibility = View.VISIBLE
                        emptySalesText.visibility = View.GONE
                    }

                    salesAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AdminSalesFragment", "Failed to load sales: ${error.message}")
                }
            })
    }
}