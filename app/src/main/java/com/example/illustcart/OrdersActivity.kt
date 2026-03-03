package com.example.illustcart

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class OrdersActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        initializeViews()
        setupViewPager()
        setupBottomNavigation()
        setupBackPressHandler()
    }

    private fun initializeViews() {
        tabLayout = findViewById(R.id.orders_tab_layout)
        viewPager = findViewById(R.id.orders_view_pager)
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupViewPager() {
        val adapter = OrdersPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Pending"
                1 -> "Delivered"
                else -> null
            }
        }.attach()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.navigation_orders

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_cart -> {
                    startActivity(Intent(this, CartActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_orders -> true
                R.id.navigation_you -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@OrdersActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        })
    }
}