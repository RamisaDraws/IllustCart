package com.example.illustcart

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class CartActivity : AppCompatActivity() {

    private lateinit var cartRecyclerView: RecyclerView
    private lateinit var cartAdapter: CartAdapter
    private val cartItems = mutableListOf<CartItem>()
    private lateinit var auth: FirebaseAuth
    private lateinit var emptyCartLayout: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        auth = FirebaseAuth.getInstance()

        cartRecyclerView = findViewById(R.id.cartRecyclerView)
        emptyCartLayout = findViewById(R.id.emptyCartLayout)
        cartAdapter = CartAdapter(cartItems)
        cartRecyclerView.adapter = cartAdapter
        cartRecyclerView.layoutManager = LinearLayoutManager(this)

        cartAdapter.onDeleteClickListener = object : CartAdapter.OnDeleteClickListener {
            override fun onDeleteClick(cartItem: CartItem) {
                removeFromCart(cartItem)
            }
        }

        cartAdapter.onBuyClickListener = object : CartAdapter.OnBuyClickListener {
            override fun onBuyClick(cartItem: CartItem) {
                buyFromCart(cartItem)
            }
        }

        setupBottomNavigation()
        loadCartItems()

        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Go back to MainActivity
                val intent = Intent(this@CartActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        })
    }


    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.navigation_cart

        bottomNav.setOnNavigationItemSelectedListener { item ->
            when(item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_cart -> true
                R.id.navigation_orders -> {
                    startActivity(Intent(this, OrdersActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_you -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun updateEmptyCartState() {
        if (cartItems.isEmpty()) {
            emptyCartLayout.visibility = View.VISIBLE
            cartRecyclerView.visibility = View.GONE
        } else {
            emptyCartLayout.visibility = View.GONE
            cartRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun loadCartItems() {
        val userId = auth.currentUser?.uid ?: return
        val cartRef = FirebaseDatabase.getInstance().getReference("carts").child(userId)

        cartRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cartItems.clear()

                if (!snapshot.exists()) {
                    cartAdapter.notifyDataSetChanged()
                    updateEmptyCartState()
                    return
                }

                // Count how many items we need to process
                var itemsToProcess = snapshot.childrenCount.toInt()

                for (cartSnapshot in snapshot.children) {
                    val cartItem = cartSnapshot.getValue(CartItem::class.java)

                    if (cartItem != null) {
                        // Fetch current product data to check if flash sale is still active
                        val productRef = FirebaseDatabase.getInstance()
                            .getReference("products")
                            .child(cartItem.sellerId ?: "")
                            .child(cartItem.productId ?: "")

                        productRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(productSnapshot: DataSnapshot) {
                                val currentProduct = productSnapshot.getValue(Product::class.java)

                                if (currentProduct != null) {
                                    // Get the current price based on flash sale status
                                    val currentPrice = PriceHelper.getDisplayPrice(currentProduct)

                                    // Update cart item with current price if it has changed
                                    if (cartItem.productPrice != currentPrice) {
                                        // Update the price in Firebase
                                        val cartItemRef = FirebaseDatabase.getInstance()
                                            .getReference("carts")
                                            .child(userId)
                                            .child(cartItem.cartItemId ?: "")

                                        cartItemRef.child("productPrice").setValue(currentPrice)

                                        // Update local cart item
                                        val updatedCartItem = cartItem.copy(productPrice = currentPrice)
                                        cartItems.add(updatedCartItem)
                                    } else {
                                        cartItems.add(cartItem)
                                    }
                                } else {
                                    // Product no longer exists, still add to show but user will see error on purchase
                                    cartItems.add(cartItem)
                                }

                                itemsToProcess--
                                if (itemsToProcess == 0) {
                                    cartAdapter.notifyDataSetChanged()
                                    updateEmptyCartState()
                                }
                            }

                            override fun onCancelled(error: DatabaseError) {
                                // If we can't fetch product, still add the cart item
                                cartItems.add(cartItem)
                                itemsToProcess--
                                if (itemsToProcess == 0) {
                                    cartAdapter.notifyDataSetChanged()
                                    updateEmptyCartState()
                                }
                            }
                        })
                    } else {
                        itemsToProcess--
                        if (itemsToProcess == 0) {
                            cartAdapter.notifyDataSetChanged()
                            updateEmptyCartState()
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CartActivity", "Failed to load cart: ${error.message}")
            }
        })
    }

    private fun removeFromCart(cartItem: CartItem) {
        val userId = auth.currentUser?.uid ?: return
        val cartItemId = cartItem.cartItemId ?: return

        FirebaseDatabase.getInstance().getReference("carts")
            .child(userId)
            .child(cartItemId)
            .removeValue()
            .addOnSuccessListener {
                Toast.makeText(this, "Removed from cart", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to remove", Toast.LENGTH_SHORT).show()
            }
    }

    private fun buyFromCart(cartItem: CartItem) {
        val user = auth.currentUser ?: return

        // First, fetch the current product to check availability and get current prints
        val productRef = FirebaseDatabase.getInstance()
            .getReference("products")
            .child(cartItem.sellerId!!)
            .child(cartItem.productId!!)

        productRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val product = snapshot.getValue(Product::class.java)

                if (product == null) {
                    Toast.makeText(this@CartActivity, "Product no longer available", Toast.LENGTH_SHORT).show()
                    return
                }

                // Check if product is sold out
                val printsAvailable = product.printsAvailable ?: 0
                if (printsAvailable <= 0) {
                    Toast.makeText(this@CartActivity, "This artwork is sold out", Toast.LENGTH_SHORT).show()
                    return
                }

                // Use current price from PriceHelper (in case flash sale status changed)
                val currentPrice = PriceHelper.getDisplayPrice(product)

                // Proceed with order
                val ordersRef = FirebaseDatabase.getInstance().getReference("orders")
                val orderId = ordersRef.push().key ?: return

                val order = Order(
                    orderId = orderId,
                    productId = cartItem.productId,
                    productName = cartItem.productName,
                    productPrice = currentPrice,  // Use current price, not saved cart price
                    productImage = cartItem.productImage,
                    productSize = cartItem.productSize,
                    category = cartItem.category,
                    artist = null,  // No longer using artist field
                    sellerId = cartItem.sellerId,
                    sellerName = cartItem.sellerName,
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
                                removeFromCart(cartItem)
                                Toast.makeText(this@CartActivity, "Order placed!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@CartActivity, "Order placed but inventory update failed", Toast.LENGTH_SHORT).show()
                                removeFromCart(cartItem)
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this@CartActivity, "Failed to place order", Toast.LENGTH_SHORT).show()
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@CartActivity, "Failed to check product availability", Toast.LENGTH_SHORT).show()
            }
        })
    }
}