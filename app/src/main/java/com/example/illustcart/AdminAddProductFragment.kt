package com.example.illustcart

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class AdminAddProductFragment : Fragment() {

    companion object {
        private const val PICK_PRODUCT_IMAGE_REQUEST = 200
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var databaseReference: DatabaseReference
    private lateinit var storageReference: StorageReference
    private var currentAdminId: String? = null
    private var currentAdminName: String? = null

    private lateinit var productImageView: ImageView
    private lateinit var productNameEdit: EditText
    private lateinit var productDescriptionEdit: EditText
    private lateinit var productSizeEdit: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var printsAvailableEdit: EditText
    private lateinit var productPriceEdit: EditText
    private lateinit var flashSaleCheckbox: CheckBox
    private lateinit var flashSaleContainer: LinearLayout
    private lateinit var flashHoursEdit: EditText
    private lateinit var flashMinutesEdit: EditText
    private lateinit var flashSecondsEdit: EditText
    private lateinit var discountRateEdit: EditText
    private lateinit var submitButton: Button

    private var productImageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_add_product, container, false)

        auth = FirebaseAuth.getInstance()
        currentAdminId = auth.currentUser?.uid
        currentAdminName = auth.currentUser?.displayName ?: "Unknown Artist"
        storageReference = FirebaseStorage.getInstance().reference
        databaseReference = FirebaseDatabase.getInstance()
            .getReference("products")
            .child(currentAdminId!!)

        initializeViews(view)
        setupCategorySpinner()
        setupImagePicker()
        setupFlashSaleToggle()
        setupTimeValidation()
        setupSubmitButton()

        return view
    }

    private fun initializeViews(view: View) {
        productImageView = view.findViewById(R.id.add_product_imageView)
        productNameEdit = view.findViewById(R.id.add_product_name)
        productDescriptionEdit = view.findViewById(R.id.add_product_description)
        productSizeEdit = view.findViewById(R.id.add_product_size)
        categorySpinner = view.findViewById(R.id.add_product_category_spinner)
        printsAvailableEdit = view.findViewById(R.id.add_product_prints)
        productPriceEdit = view.findViewById(R.id.add_product_price)
        flashSaleCheckbox = view.findViewById(R.id.add_product_flash_sale_checkbox)
        flashSaleContainer = view.findViewById(R.id.add_product_flash_sale_container)
        flashHoursEdit = view.findViewById(R.id.add_product_flash_hours)
        flashMinutesEdit = view.findViewById(R.id.add_product_flash_minutes)
        flashSecondsEdit = view.findViewById(R.id.add_product_flash_seconds)
        discountRateEdit = view.findViewById(R.id.add_product_discount_rate)
        submitButton = view.findViewById(R.id.add_product_submit_btn)
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf(
            "Select Category",
            "FanArt",
            "Original",
            "Character",
            "Chibi",
            "Background",
            "IllustA",
            "Comic"
        )

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter
    }

    private fun setupImagePicker() {
        productImageView.setOnClickListener {
            openProductImageChooser()
        }
    }

    private fun openProductImageChooser() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_PRODUCT_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_PRODUCT_IMAGE_REQUEST &&
            resultCode == Activity.RESULT_OK &&
            data != null &&
            data.data != null) {

            productImageUri = data.data
            productImageView.setImageURI(productImageUri)
        }
    }

    private fun setupFlashSaleToggle() {
        flashSaleCheckbox.setOnCheckedChangeListener { _, isChecked ->
            flashSaleContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun setupTimeValidation() {
        // Validate minutes (0-59)
        flashMinutesEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val value = s.toString().toIntOrNull()
                if (value != null && value > 59) {
                    flashMinutesEdit.error = "Minutes must be 0-59"
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Validate seconds (0-59)
        flashSecondsEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val value = s.toString().toIntOrNull()
                if (value != null && value > 59) {
                    flashSecondsEdit.error = "Seconds must be 0-59"
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Validate discount rate (0-100)
        discountRateEdit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val value = s.toString().toIntOrNull()
                if (value != null && value > 100) {
                    discountRateEdit.error = "Discount must be 0-100%"
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupSubmitButton() {
        submitButton.setOnClickListener {
            uploadProductData()
        }
    }

    private fun uploadProductData() {
        val productName = productNameEdit.text.toString().trim()
        val productDescription = productDescriptionEdit.text.toString().trim()
        val productSize = productSizeEdit.text.toString().trim()
        val productPrice = productPriceEdit.text.toString().trim()
        val printsAvailable = printsAvailableEdit.text.toString().trim()
        val selectedCategory = categorySpinner.selectedItem.toString()

        // Validation
        if (productName.isEmpty() || productDescription.isEmpty() || productSize.isEmpty() ||
            productPrice.isEmpty() || printsAvailable.isEmpty()) {
            Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedCategory == "Select Category") {
            Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }

        if (productImageUri == null) {
            Toast.makeText(context, "Please select a product image", Toast.LENGTH_SHORT).show()
            return
        }

        val printsCount = printsAvailable.toIntOrNull()
        if (printsCount == null || printsCount < 1) {
            Toast.makeText(context, "Prints available must be at least 1", Toast.LENGTH_SHORT).show()
            return
        }

        // Flash sale validation
        val isFlashSale = flashSaleCheckbox.isChecked
        var flashSaleEndTime: Long? = null
        var discountRate: Int? = null
        var originalPrice: String? = null

        if (isFlashSale) {
            val hours = flashHoursEdit.text.toString().toIntOrNull() ?: 0
            val minutes = flashMinutesEdit.text.toString().toIntOrNull() ?: 0
            val seconds = flashSecondsEdit.text.toString().toIntOrNull() ?: 0
            val discount = discountRateEdit.text.toString().toIntOrNull()

            if (minutes > 59) {
                Toast.makeText(context, "Minutes must be 0-59", Toast.LENGTH_SHORT).show()
                return
            }

            if (seconds > 59) {
                Toast.makeText(context, "Seconds must be 0-59", Toast.LENGTH_SHORT).show()
                return
            }

            if (hours == 0 && minutes == 0 && seconds == 0) {
                Toast.makeText(context, "Flash sale duration must be greater than 0", Toast.LENGTH_SHORT).show()
                return
            }

            if (discount == null || discount < 1 || discount > 100) {
                Toast.makeText(context, "Discount rate must be between 1-100%", Toast.LENGTH_SHORT).show()
                return
            }

            // Calculate flash sale end time
            val durationMillis = (hours * 3600L + minutes * 60L + seconds) * 1000L
            flashSaleEndTime = System.currentTimeMillis() + durationMillis
            discountRate = discount
            originalPrice = productPrice
        }

        // Disable button to prevent multiple clicks
        submitButton.isEnabled = false
        Toast.makeText(context, "Uploading product...", Toast.LENGTH_SHORT).show()

        val productId = databaseReference.push().key

        if (productId != null) {
            val productImageRef = storageReference.child("product_images/$currentAdminId/$productId.jpg")

            productImageRef.putFile(productImageUri!!)
                .addOnSuccessListener {
                    productImageRef.downloadUrl.addOnSuccessListener { uri ->
                        val product = Product(
                            id = productId,
                            productName = productName,
                            description = productDescription,
                            productSize = productSize,
                            productPrice = productPrice,
                            imageUrl = uri.toString(),
                            sellerId = currentAdminId,
                            sellerName = currentAdminName,
                            category = selectedCategory,
                            printsAvailable = printsCount,
                            isFlashSale = isFlashSale,
                            flashSaleEndTime = flashSaleEndTime,
                            discountRate = discountRate,
                            originalPrice = originalPrice
                        )

                        databaseReference.child(productId).setValue(product)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    context,
                                    "Product added successfully!",
                                    Toast.LENGTH_SHORT
                                ).show()

                                // Send flash sale notification if applicable
                                if (isFlashSale) {
                                    val hours = flashHoursEdit.text.toString().toIntOrNull() ?: 0
                                    val minutes = flashMinutesEdit.text.toString().toIntOrNull() ?: 0
                                    val discount = discountRate ?: 0

                                    FCMHelperV1.sendFlashSaleNotification(
                                        context = requireContext(),
                                        productName = productName,
                                        discount = discount,
                                        durationHours = hours,
                                        durationMinutes = minutes,
                                        originalPrice = productPrice,
                                        salePrice = PriceHelper.calculateDiscountedPrice(productPrice, discount),
                                        printsLeft = printsCount,
                                        imageUrl = uri.toString()
                                    )
                                }

                                // Navigate back to home
                                parentFragmentManager.popBackStack()
                            }
                            .addOnFailureListener { exception ->
                                submitButton.isEnabled = true
                                Toast.makeText(
                                    context,
                                    "Failed to add product: ${exception.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    submitButton.isEnabled = true
                    Toast.makeText(
                        context,
                        "Image upload failed: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}