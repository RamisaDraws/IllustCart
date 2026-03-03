package com.example.illustcart

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class AdminLoginActivity : Activity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_login)

        // Initialize Firebase Auth
        auth = Firebase.auth
        db = FirebaseFirestore.getInstance()

        // Check if admin is already signed in
        val currentUser = auth.currentUser
        if (currentUser != null) {
            checkIfUserIsAdmin(currentUser.uid)
        }

        val registerButton = findViewById<Button>(R.id.admin_register_btn)
        registerButton.setOnClickListener {
            val intent = Intent(this, AdminRegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        val loginButton = findViewById<Button>(R.id.adminLoginbtn)
        loginButton.setOnClickListener {
            val email = findViewById<EditText>(R.id.admin_email)
            val pass = findViewById<EditText>(R.id.admin_password)

            val email1 = email.text.toString()
            val password = pass.text.toString()

            if (email.text.isEmpty() || pass.text.isEmpty()) {
                Toast.makeText(baseContext, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email1, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Check if user is actually an admin
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            checkIfUserIsAdmin(userId)
                        }
                    } else {
                        Toast.makeText(
                            baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        baseContext, "Authentication failed. ${it.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        val backButton = findViewById<Button>(R.id.back_to_user_login)
        backButton.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkIfUserIsAdmin(userId: String) {
        db.collection("admins").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists() && document.getBoolean("isAdmin") == true) {
                    // User is an admin, proceed to admin panel
                    Toast.makeText(baseContext, "Welcome Admin!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, Admin_Panel::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // User is not an admin
                    auth.signOut()
                    Toast.makeText(
                        baseContext,
                        "Access denied. This account is not registered as admin.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .addOnFailureListener { exception ->
                auth.signOut()
                Toast.makeText(
                    baseContext,
                    "Error verifying admin status: ${exception.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}