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
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class AdminEditProductFragment : Fragment() {

    companion object {
        private const val PICK_EDIT_PRODUCT_IMAGE_REQUEST = 300

        fun newInstance(product: Product): AdminEditProductFragment {
            val fragment = AdminEditProductFragment()
            val args = Bundle()
            args.putString("productId", product.id)
            args.putString("productName", product.productName)
            args.putString("description", product.description)
            args.putString("productSize", product.productSize)
            args.putString("productPrice", product.productPrice)
            args.putString("imageUrl", product.imageUrl)
            args.putString("category", product.category)
            args.putInt("printsAvailable", product.printsAvailable ?: 1)
            args.putBoolean("isFlashSale", product.isFlashSale ?: false)
            args.putLong("flashSaleEndTime", product.flashSaleEndTime ?: 0L)
            args.putInt("discountRate", product.discountRate ?: 0)
            args.putString("originalPrice", product.originalPrice)
            fragment.arguments = args
            return fragment
        }
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
    private lateinit var cancelButton: Button
    private lateinit var updateButton: Button

    private var productImageUri: Uri? = null
    private var productId: String? = null
    private var originalImageUrl: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_edit_product, container, false)

        auth = FirebaseAuth.getInstance()
        currentAdminId = auth.currentUser?.uid
        currentAdminName = auth.currentUser?.displayName ?: "Unknown Artist"
        storageReference = FirebaseStorage.getInstance().reference
        databaseReference = FirebaseDatabase.getInstance()
            .getReference("products")
            .child(currentAdminId!!)

        initializeViews(view)
        setupCategorySpinner()
        loadProductData()
        setupImagePicker()
        setupFlashSaleToggle()
        setupTimeValidation()
        setupButtons()

        return view
    }

    private fun initializeViews(view: View) {
        productImageView = view.findViewById(R.id.edit_product_imageView)
        productNameEdit = view.findViewById(R.id.edit_product_name)
        productDescriptionEdit = view.findViewById(R.id.edit_product_description)
        productSizeEdit = view.findViewById(R.id.edit_product_size)
        categorySpinner = view.findViewById(R.id.edit_product_category_spinner)
        printsAvailableEdit = view.findViewById(R.id.edit_product_prints)
        productPriceEdit = view.findViewById(R.id.edit_product_price)
        flashSaleCheckbox = view.findViewById(R.id.edit_product_flash_sale_checkbox)
        flashSaleContainer = view.findViewById(R.id.edit_product_flash_sale_container)
        flashHoursEdit = view.findViewById(R.id.edit_product_flash_hours)
        flashMinutesEdit = view.findViewById(R.id.edit_product_flash_minutes)
        flashSecondsEdit = view.findViewById(R.id.edit_product_flash_seconds)
        discountRateEdit = view.findViewById(R.id.edit_product_discount_rate)
        cancelButton = view.findViewById(R.id.edit_product_cancel_btn)
        updateButton = view.findViewById(R.id.edit_product_update_btn)
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

    private fun loadProductData() {
        arguments?.let { args ->
            productId = args.getString("productId")
            originalImageUrl = args.getString("imageUrl")

            productNameEdit.setText(args.getString("productName"))
            productDescriptionEdit.setText(args.getString("description"))
            productSizeEdit.setText(args.getString("productSize"))
            printsAvailableEdit.setText(args.getInt("printsAvailable", 1).toString())

            // Handle price
            val isFlashSale = args.getBoolean("isFlashSale", false)
            if (isFlashSale) {
                // If it's a flash sale, show the original price for editing
                productPriceEdit.setText(args.getString("originalPrice"))
            } else {
                productPriceEdit.setText(args.getString("productPrice"))
            }

            // Load image
            Glide.with(this)
                .load(originalImageUrl)
                .placeholder(R.drawable.baseline_image_24)
                .into(productImageView)

            // Set category spinner
            val category = args.getString("category")
            val categories = arrayOf(
                "Select Category", "FanArt", "Original", "Character",
                "Chibi", "Background", "IllustA", "Comic"
            )
            val categoryIndex = categories.indexOf(category)
            if (categoryIndex >= 0) {
                categorySpinner.setSelection(categoryIndex)
            }

            // Load flash sale data
            flashSaleCheckbox.isChecked = isFlashSale
            if (isFlashSale) {
                flashSaleContainer.visibility = View.VISIBLE

                val endTime = args.getLong("flashSaleEndTime", 0L)
                val currentTime = System.currentTimeMillis()
                val remainingMillis = endTime - currentTime

                if (remainingMillis > 0) {
                    // Calculate remaining time
                    val remainingSeconds = (remainingMillis / 1000).toInt()
                    val hours = remainingSeconds / 3600
                    val minutes = (remainingSeconds % 3600) / 60
                    val seconds = remainingSeconds % 60

                    flashHoursEdit.setText(hours.toString())
                    flashMinutesEdit.setText(minutes.toString())
                    flashSecondsEdit.setText(seconds.toString())
                } else {
                    // Flash sale expired
                    flashHoursEdit.setText("0")
                    flashMinutesEdit.setText("0")
                    flashSecondsEdit.setText("0")
                }

                discountRateEdit.setText(args.getInt("discountRate", 0).toString())
            }
        }
    }

    private fun setupImagePicker() {
        productImageView.setOnClickListener {
            openProductImageChooser()
        }
    }

    private fun openProductImageChooser() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_EDIT_PRODUCT_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_EDIT_PRODUCT_IMAGE_REQUEST &&
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

    private fun setupButtons() {
        cancelButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        updateButton.setOnClickListener {
            updateProduct()
        }
    }

    private fun updateProduct() {
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

        if (productId == null) {
            Toast.makeText(context, "Error: Product ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        val printsCount = printsAvailable.toIntOrNull()
        if (printsCount == null || printsCount < 0) {
            Toast.makeText(context, "Prints available must be 0 or greater", Toast.LENGTH_SHORT).show()
            return
        }

        // Flash sale validation
        val isFlashSale = flashSaleCheckbox.isChecked
        var flashSaleEndTime: Long? = null
        var discountRate: Int? = null
        var originalPrice: String? = null
        var finalPrice = productPrice // This will be the discounted price if flash sale

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

            // âœ… NEW: Calculate discounted price
            val priceValue = productPrice.replace("$", "").toDoubleOrNull() ?: 0.0
            val discountedPrice = priceValue * (1 - discount / 100.0)
            finalPrice = "$%.2f".format(discountedPrice)
        }

        // Disable button to prevent multiple clicks
        updateButton.isEnabled = false
        Toast.makeText(context, "Updating product...", Toast.LENGTH_SHORT).show()

        // If new image selected, upload it first
        if (productImageUri != null) {
            uploadNewImageAndUpdate(productName, productDescription, productSize, selectedCategory,
                printsCount, finalPrice, isFlashSale, flashSaleEndTime, discountRate, originalPrice)
        } else {
            // Update with existing image
            updateProductData(productName, productDescription, productSize, selectedCategory,
                printsCount, finalPrice, originalImageUrl ?: "", isFlashSale, flashSaleEndTime,
                discountRate, originalPrice)
        }
    }

    private fun uploadNewImageAndUpdate(
        productName: String,
        productDescription: String,
        productSize: String,
        category: String,
        printsAvailable: Int,
        productPrice: String, // âœ… CHANGED: This is now finalPrice (discounted if flash sale)
        isFlashSale: Boolean,
        flashSaleEndTime: Long?,
        discountRate: Int?,
        originalPrice: String?
    ) {
        val productImageRef = storageReference.child("product_images/$currentAdminId/$productId.jpg")

        productImageRef.putFile(productImageUri!!)
            .addOnSuccessListener {
                productImageRef.downloadUrl.addOnSuccessListener { uri ->
                    updateProductData(productName, productDescription, productSize, category,
                        printsAvailable, productPrice, uri.toString(), isFlashSale, flashSaleEndTime,
                        discountRate, originalPrice)
                }
            }
            .addOnFailureListener { exception ->
                updateButton.isEnabled = true
                Toast.makeText(
                    context,
                    "Image upload failed: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateProductData(
        productName: String,
        productDescription: String,
        productSize: String,
        category: String,
        printsAvailable: Int,
        productPrice: String, // âœ… CHANGED: This is now finalPrice (discounted if flash sale)
        imageUrl: String,
        isFlashSale: Boolean,
        flashSaleEndTime: Long?,
        discountRate: Int?,
        originalPrice: String?
    ) {
        val updatedProduct = Product(
            id = productId,
            productName = productName,
            description = productDescription,
            productSize = productSize,
            productPrice = productPrice, // âœ… CHANGED: Use discounted price if flash sale
            imageUrl = imageUrl,
            sellerId = currentAdminId,
            sellerName = currentAdminName,
            category = category,
            printsAvailable = printsAvailable,
            isFlashSale = isFlashSale,
            flashSaleEndTime = flashSaleEndTime,
            discountRate = discountRate,
            originalPrice = originalPrice
        )

        databaseReference.child(productId!!).setValue(updatedProduct)
            .addOnSuccessListener {
                // ✅ NEW: Send flash sale notification if flash sale is active
                if (isFlashSale && context != null) {
                    val hours = flashSaleEndTime?.let {
                        val remainingMillis = it - System.currentTimeMillis()
                        (remainingMillis / (1000 * 60 * 60)).toInt()
                    } ?: 0
                    val minutes = flashSaleEndTime?.let {
                        val remainingMillis = it - System.currentTimeMillis()
                        ((remainingMillis / (1000 * 60)) % 60).toInt()
                    } ?: 0

                    val salePrice = PriceHelper.calculateDiscountedPrice(originalPrice ?: productPrice, discountRate ?: 0)

                    FCMHelperV1.sendFlashSaleNotification(
                        context = requireContext(),
                        productName = productName,
                        discount = discountRate ?: 0,
                        durationHours = hours,
                        durationMinutes = minutes,
                        originalPrice = originalPrice ?: productPrice,
                        salePrice = salePrice,
                        printsLeft = printsAvailable,
                        imageUrl = imageUrl
                    )
                }

                Toast.makeText(context, "Product updated successfully!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { exception ->
                updateButton.isEnabled = true
                Toast.makeText(
                    context,
                    "Failed to update product: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}