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
//import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
//import com.google.firebase.ktx.Firebase

class RegisterActivity : Activity() {

    private lateinit var auth: FirebaseAuth



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)



        // Initialize Firebase Auth
        auth = Firebase.auth
        val login = findViewById<Button>(R.id.lgbtn)



        login.setOnClickListener {

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }



        val register = findViewById<Button>(R.id.signupbtn)

        register.setOnClickListener {

            val email = findViewById<EditText>(R.id.email)
            val pass = findViewById<EditText>(R.id.password)
            val phone = findViewById<EditText>(R.id.phone)
            val name = findViewById<EditText>(R.id.name)

            val email1 = email.text.toString()
            val password = pass.text.toString()
            val phone1 = phone.text.toString()
            val name1 = name.text.toString()

            if (email.text.isEmpty() || pass.text.isEmpty() || phone.text.isEmpty())       {
                Toast.makeText(baseContext, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener


            }

            //user data save to firebase

            auth.createUserWithEmailAndPassword(email1, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(baseContext, "success", Toast.LENGTH_SHORT).show()

                        val user = auth.currentUser

                        if (user != null) {
                            // Update profile with name
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(name1)
                                .build()

                            user.updateProfile(profileUpdates)
                                .addOnCompleteListener { profileTask ->
                                    if (profileTask.isSuccessful) {
                                        // Store user data in Realtime Database
                                        val userRef = FirebaseDatabase.getInstance()
                                            .getReference("users")
                                            .child(user.uid)

                                        val userData = hashMapOf<String, Any>(
                                            "name" to name1,
                                            "email" to email1,
                                            "phone" to phone1,
                                            "address" to ""
                                        )

                                        userRef.setValue(userData)
                                            .addOnSuccessListener {
                                                // Navigate to MainActivity
                                                val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                                                startActivity(intent)
                                                finish()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(
                                                    this,
                                                    "Failed to save user data: ${e.message}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                    } else {
                                        Toast.makeText(this, "Failed to update name", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    } else {
                        Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "error occurred ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        }


    }
}