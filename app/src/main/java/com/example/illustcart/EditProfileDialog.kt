package com.example.illustcart

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class EditProfileDialog(private val activity: Activity) : Dialog(activity) {

    companion object {
        const val PICK_IMAGE_REQUEST = 300
        const val CROP_IMAGE_REQUEST = 301
    }

    private lateinit var profileImageView: ImageView
    private lateinit var nameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var addressEditText: EditText
    private lateinit var cancelButton: Button
    private lateinit var updateButton: Button

    private val auth = FirebaseAuth.getInstance()
    private val storageRef = FirebaseStorage.getInstance().reference
    private var selectedImageUri: Uri? = null
    private var onProfileUpdatedListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_edit_profile)

        window?.setBackgroundDrawableResource(android.R.color.transparent)

        initializeViews()
        loadCurrentProfile()
        setupButtons()
    }

    private fun initializeViews() {
        profileImageView = findViewById(R.id.edit_profile_image)
        nameEditText = findViewById(R.id.edit_profile_name)
        phoneEditText = findViewById(R.id.edit_profile_phone)
        addressEditText = findViewById(R.id.edit_profile_address)
        cancelButton = findViewById(R.id.cancel_edit_profile_btn)
        updateButton = findViewById(R.id.update_profile_btn)

        // Image click listener
        profileImageView.setOnClickListener {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        activity.startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    fun handleImageResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri = data.data
            if (imageUri != null) {
                val destination = Uri.fromFile(File(activity.externalCacheDir, "cropped_profile.jpg"))
                com.soundcloud.android.crop.Crop.of(imageUri, destination).asSquare().start(activity)
            }
        } else if (requestCode == com.soundcloud.android.crop.Crop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            val file = File(activity.externalCacheDir, "cropped_profile.jpg")
            if (file.exists()) {
                selectedImageUri = Uri.fromFile(file)
                Glide.with(activity)
                    .load(selectedImageUri)
                    .into(profileImageView)
            }
        }
    }

    private fun loadCurrentProfile() {
        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Load profile picture
        val profilePicRef = storageRef.child("images/${userId}.jpg")
        profilePicRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(context)
                .load(uri)
                .placeholder(R.drawable.baseline_account_circle_24)
                .into(profileImageView)
        }.addOnFailureListener {
            profileImageView.setImageResource(R.drawable.baseline_account_circle_24)
        }

        // Load user data
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userName = snapshot.child("name").getValue(String::class.java)
                val userPhone = snapshot.child("phone").getValue(String::class.java)
                val userAddress = snapshot.child("address").getValue(String::class.java)

                nameEditText.setText(userName ?: "")
                phoneEditText.setText(userPhone ?: "")
                addressEditText.setText(userAddress ?: "")
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupButtons() {
        cancelButton.setOnClickListener {
            dismiss()
        }

        updateButton.setOnClickListener {
            updateProfile()
        }
    }

    private fun updateProfile() {
        val newName = nameEditText.text.toString().trim()
        val newPhone = phoneEditText.text.toString().trim()
        val newAddress = addressEditText.text.toString().trim()

        if (newName.isEmpty()) {
            Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPhone.isEmpty()) {
            Toast.makeText(context, "Phone cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        updateButton.isEnabled = false
        Toast.makeText(context, "Updating profile...", Toast.LENGTH_SHORT).show()

        val currentUser = auth.currentUser ?: return
        val userId = currentUser.uid

        // Update Firebase Auth display name
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        currentUser.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Update Realtime Database
                val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                val updates = hashMapOf<String, Any>(
                    "name" to newName,
                    "phone" to newPhone,
                    "address" to newAddress
                )

                userRef.updateChildren(updates).addOnSuccessListener {
                    // Upload profile image if selected
                    if (selectedImageUri != null) {
                        uploadProfileImage(userId)
                    } else {
                        Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        onProfileUpdatedListener?.invoke()
                        dismiss()
                    }
                }.addOnFailureListener { exception ->
                    updateButton.isEnabled = true
                    Toast.makeText(
                        context,
                        "Failed to update profile: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                updateButton.isEnabled = true
                Toast.makeText(context, "Failed to update name", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadProfileImage(userId: String) {
        val profilePicRef = storageRef.child("images/${userId}.jpg")

        profilePicRef.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                onProfileUpdatedListener?.invoke()
                dismiss()
            }
            .addOnFailureListener { exception ->
                updateButton.isEnabled = true
                Toast.makeText(
                    context,
                    "Profile updated but image upload failed: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    fun setOnProfileUpdatedListener(listener: () -> Unit) {
        onProfileUpdatedListener = listener
    }
}