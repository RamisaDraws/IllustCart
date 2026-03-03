package com.example.illustcart

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth

class Admin_Panel : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var currentAdminId: String? = null
    private var currentAdminName: String? = null

    private lateinit var toolbar: Toolbar
    private lateinit var toolbarTitle: TextView
    private lateinit var logoutIcon: ImageView
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var fabAdd: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            val intent = Intent(this, AdminLoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        currentAdminId = currentUser.uid
        currentAdminName = currentUser.displayName ?: "Unknown Artist"

        initializeViews()
        setupToolbar()
        setupBottomNavigation()
        setupFAB()
        setupBackPressHandler()

        // Load default fragment (Home)
        if (savedInstanceState == null) {
            loadFragment(AdminHomeFragment())
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.admin_toolbar)
        toolbarTitle = findViewById(R.id.admin_toolbar_title)
        logoutIcon = findViewById(R.id.admin_logout_icon)
        bottomNavigation = findViewById(R.id.admin_bottom_navigation)
        fabAdd = findViewById(R.id.admin_fab_add)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        toolbarTitle.text = "IllustCart Artist: $currentAdminName"

        logoutIcon.setOnClickListener {
            auth.signOut()
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, AdminLoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.admin_nav_home

        bottomNavigation.setOnItemSelectedListener { item ->
            android.util.Log.d("AdminPanel", "Bottom nav item selected: ${item.itemId}")

            when (item.itemId) {
                R.id.admin_nav_home -> {
                    android.util.Log.d("AdminPanel", "Loading Home fragment")
                    loadFragment(AdminHomeFragment())
                    true
                }
                R.id.admin_nav_orders -> {
                    android.util.Log.d("AdminPanel", "Orders selected - navigating to AdminOrdersActivity")
                    val intent = Intent(this, AdminOrdersActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.admin_nav_add -> {
                    // This is handled by FAB, do nothing
                    android.util.Log.d("AdminPanel", "Center item clicked - ignoring (handled by FAB)")
                    false
                }
                R.id.admin_nav_analytics -> {
                    android.util.Log.d("AdminPanel", "Analytics selected - navigating to AdminAnalyticsActivity")
                    val intent = Intent(this, AdminAnalyticsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.admin_nav_user_mode -> {
                    android.util.Log.d("AdminPanel", "User Mode selected - navigating to MainActivity")
                    // Navigate to user MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> {
                    android.util.Log.w("AdminPanel", "Unknown item selected: ${item.itemId}")
                    false
                }
            }
        }
    }

    private fun setupFAB() {
        fabAdd.setOnClickListener {
            android.util.Log.d("AdminPanel", "FAB clicked - opening Add Product fragment")

            try {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.admin_fragment_container, AdminAddProductFragment())
                    .addToBackStack("addProduct")
                    .commit()

                android.util.Log.d("AdminPanel", "Fragment transaction committed successfully")

                // FIX: Temporarily disable checkable to prevent auto-selection
                bottomNavigation.menu.setGroupCheckable(0, false, true)

                // Deselect all bottom nav items
                bottomNavigation.menu.findItem(R.id.admin_nav_home)?.isChecked = false
                bottomNavigation.menu.findItem(R.id.admin_nav_orders)?.isChecked = false
                bottomNavigation.menu.findItem(R.id.admin_nav_analytics)?.isChecked = false
                bottomNavigation.menu.findItem(R.id.admin_nav_user_mode)?.isChecked = false

                // Re-enable checkable
                bottomNavigation.menu.setGroupCheckable(0, true, true)

            } catch (e: Exception) {
                android.util.Log.e("AdminPanel", "Error loading fragment: ${e.message}")
                Toast.makeText(this, "Error loading Add Product screen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.admin_fragment_container, fragment)
            .commit()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Check if there are fragments in back stack
                if (supportFragmentManager.backStackEntryCount > 0) {
                    supportFragmentManager.popBackStack()

                    // Re-select Home in bottom nav after popping back stack
                    bottomNavigation.selectedItemId = R.id.admin_nav_home
                } else {
                    // If on main fragment, show exit confirmation
                    androidx.appcompat.app.AlertDialog.Builder(this@Admin_Panel)
                        .setTitle("Exit Admin Panel")
                        .setMessage("Do you want to exit?")
                        .setPositiveButton("Yes") { _, _ ->
                            finish()
                        }
                        .setNegativeButton("No") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
        })
    }
}