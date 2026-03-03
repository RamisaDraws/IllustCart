package com.example.illustcart

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class AdminAnalyticsActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var fabAdd: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_analytics)

        initializeViews()
        setupViewPager()
        setupBottomNavigation()
        setupFAB()
        setupBackPressHandler()
    }

    private fun initializeViews() {
        tabLayout = findViewById(R.id.admin_analytics_tab_layout)
        viewPager = findViewById(R.id.admin_analytics_view_pager)
        bottomNavigation = findViewById(R.id.admin_analytics_bottom_navigation)
        fabAdd = findViewById(R.id.admin_analytics_fab_add)
    }

    private fun setupViewPager() {
        val adapter = AdminAnalyticsPagerAdapter(this)
        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Top Artworks"
                1 -> "Top Buyers"
                else -> null
            }
        }.attach()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.admin_nav_analytics

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.admin_nav_home -> {
                    startActivity(Intent(this, Admin_Panel::class.java))
                    finish()
                    true
                }
                R.id.admin_nav_orders -> {
                    startActivity(Intent(this, AdminOrdersActivity::class.java))
                    finish()
                    true
                }
                R.id.admin_nav_add -> false
                R.id.admin_nav_analytics -> true
                R.id.admin_nav_user_mode -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupFAB() {
        fabAdd.setOnClickListener {
            Toast.makeText(this, "Add product from Home screen", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                startActivity(Intent(this@AdminAnalyticsActivity, Admin_Panel::class.java))
                finish()
            }
        })
    }
}