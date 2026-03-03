package com.example.illustcart

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class AdminRegisterActivity : Activity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_register)

        // Initialize Firebase Auth
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        val loginButton = findViewById<Button>(R.id.admin_login_redirect_btn)
        loginButton.setOnClickListener {
            val intent = Intent(this, AdminLoginActivity::class.java)
            startActivity(intent)
            finish()
        }

        val registerButton = findViewById<Button>(R.id.admin_signup_btn)
        registerButton.setOnClickListener {
            val email = findViewById<EditText>(R.id.admin_email)
            val pass = findViewById<EditText>(R.id.admin_password)
            val phone = findViewById<EditText>(R.id.admin_phone)
            val name = findViewById<EditText>(R.id.admin_name)
            val secretKey = findViewById<EditText>(R.id.admin_secret_key)

            val email1 = email.text.toString().trim()
            val password = pass.text.toString().trim()
            val phone1 = phone.text.toString().trim()
            val name1 = name.text.toString().trim()
            val secretKeyInput = secretKey.text.toString().trim()

            if (email1.isEmpty() || password.isEmpty() ||
                phone1.isEmpty() || name1.isEmpty() || secretKeyInput.isEmpty()) {
                Toast.makeText(baseContext, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verify secret key
            if (secretKeyInput != "ILLUSTCART_ADMIN_2024") {
                Toast.makeText(
                    baseContext,
                    "Invalid admin secret key. Contact system administrator.",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            // Disable button to prevent multiple clicks
            registerButton.isEnabled = false

            // Show loading message
            Toast.makeText(baseContext, "Creating admin account...", Toast.LENGTH_SHORT).show()

            auth.createUserWithEmailAndPassword(email1, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser

                        if (user != null) {
                            // Update profile with name
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(name1)
                                .build()

                            user.updateProfile(profileUpdates)
                                .addOnCompleteListener { profileTask ->
                                    // Store admin data in Firestore (fire and forget)
                                    val adminData = hashMapOf(
                                        "name" to name1,
                                        "email" to email1,
                                        "phone" to phone1,
                                        "isAdmin" to true,
                                        "createdAt" to System.currentTimeMillis()
                                    )

                                    db.collection("admins").document(user.uid).set(adminData)


                                    // FIX: ALSO store artist data in Realtime Database "users" node
                                    // This allows the ProfileActivity to read artist info correctly

                                    val userRef = FirebaseDatabase.getInstance()
                                        .getReference("users")
                                        .child(user.uid)

                                    val userData = hashMapOf<String, Any>(
                                        "name" to name1,
                                        "email" to email1,
                                        "phone" to phone1,
                                        "address" to "",
                                        "isAdmin" to true
                                    )

                                    userRef.setValue(userData)
                                        .addOnSuccessListener {
                                            // Navigate to Admin Panel
                                            Toast.makeText(
                                                baseContext,
                                                "Admin account created successfully!",
                                                Toast.LENGTH_SHORT
                                            ).show()

                                            val intent = Intent(this@AdminRegisterActivity, Admin_Panel::class.java)
                                            startActivity(intent)
                                            finish()
                                        }
                                        .addOnFailureListener { e ->
                                            registerButton.isEnabled = true
                                            Toast.makeText(
                                                this,
                                                "Failed to save user data: ${e.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                }
                        }
                    } else {
                        registerButton.isEnabled = true

                        // Get specific error message
                        val errorMessage = when {
                            task.exception?.message?.contains("already in use") == true ->
                                "This email is already registered. Try logging in instead."
                            task.exception?.message?.contains("badly formatted") == true ->
                                "Please enter a valid email address."
                            task.exception?.message?.contains("Password should be at least 6") == true ->
                                "Password must be at least 6 characters long."
                            else -> "Registration failed: ${task.exception?.message}"
                        }

                        Toast.makeText(
                            baseContext,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener {
                    registerButton.isEnabled = true
                    Toast.makeText(
                        this,
                        "Error occurred: ${it.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}