package com.example.illustcart

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var profileImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userEmailTextView: TextView
    private lateinit var userPhoneTextView: TextView
    private lateinit var userAddressTextView: TextView
    private lateinit var editProfileButton: Button

    private var editProfileDialog: EditProfileDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        auth = FirebaseAuth.getInstance()

        initializeViews()
        setupBottomNavigation()
        loadUserProfile()
        setupButtons()
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(this@ProfileActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        })
    }

    private fun initializeViews() {
        profileImageView = findViewById(R.id.profile_image_view)
        userNameTextView = findViewById(R.id.profile_name)
        userEmailTextView = findViewById(R.id.profile_email)
        userPhoneTextView = findViewById(R.id.profile_phone)
        userAddressTextView = findViewById(R.id.profile_address)
        editProfileButton = findViewById(R.id.edit_profile_btn)
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.navigation_you

        bottomNav.setOnItemSelectedListener { item ->
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
                R.id.navigation_orders -> {
                    startActivity(Intent(this, OrdersActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_you -> true
                else -> false
            }
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            userEmailTextView.text = currentUser.email ?: "Not set"

            val userId = currentUser.uid

            // Load profile picture
            val storageRef = FirebaseStorage.getInstance().reference
            val profilePicRef = storageRef.child("images/${userId}.jpg")

            profilePicRef.downloadUrl.addOnSuccessListener { uri ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.baseline_account_circle_24)
                    .into(profileImageView)
            }.addOnFailureListener {
                profileImageView.setImageResource(R.drawable.baseline_account_circle_24)
            }

            // Load user data from Realtime Database
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

            userRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userName = snapshot.child("name").getValue(String::class.java)
                    val userPhone = snapshot.child("phone").getValue(String::class.java)
                    val userAddress = snapshot.child("address").getValue(String::class.java)

                    userNameTextView.text = userName?.takeIf { it.isNotEmpty() } ?: "Not set"
                    userPhoneTextView.text = userPhone?.takeIf { it.isNotEmpty() } ?: "Not set"
                    userAddressTextView.text = userAddress?.takeIf { it.isNotEmpty() } ?: "Not set"
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("ProfileActivity", "Failed to load profile: ${error.message}")
                }
            })
        }
    }

    private fun setupButtons() {
        editProfileButton.setOnClickListener {
            showEditProfileDialog()
        }
    }

    private fun showEditProfileDialog() {
        editProfileDialog = EditProfileDialog(this)
        editProfileDialog?.setOnProfileUpdatedListener {
            // Refresh profile when updated
            loadUserProfile()
        }
        editProfileDialog?.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Pass the result to the dialog if it exists
        editProfileDialog?.handleImageResult(requestCode, resultCode, data)
    }
}