package com.example.illustcart

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class DeliveredOrdersFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var deliveredRecyclerView: RecyclerView
    private lateinit var emptyDeliveredText: TextView
    private lateinit var deliveredAdapter: DeliveredOrdersAdapter

    private val deliveredOrders = mutableListOf<Order>()

    companion object {
        private const val STORAGE_PERMISSION_CODE = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_delivered_orders, container, false)

        auth = FirebaseAuth.getInstance()

        initializeViews(view)
        setupRecyclerView()
        loadDeliveredOrders()

        return view
    }

    private fun initializeViews(view: View) {
        deliveredRecyclerView = view.findViewById(R.id.delivered_orders_recyclerView)
        emptyDeliveredText = view.findViewById(R.id.empty_delivered_orders_text)
    }

    private fun setupRecyclerView() {
        deliveredAdapter = DeliveredOrdersAdapter(deliveredOrders)
        deliveredRecyclerView.adapter = deliveredAdapter
        deliveredRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        deliveredAdapter.onDownloadClickListener =
            object : DeliveredOrdersAdapter.OnDownloadClickListener {
                override fun onDownloadClick(order: Order) {
                    downloadOrderImage(order)
                }
            }

        // NEW: Rate button listener
        deliveredAdapter.onRateClickListener =
            object : DeliveredOrdersAdapter.OnRateClickListener {
                override fun onRateClick(order: Order) {
                    showRatingDialog(order)
                }
            }
    }

    private fun showRatingDialog(order: Order) {
        val dialog = RateProductDialog(requireActivity(), order)
        dialog.setOnRatingSubmittedListener {
            // Update adapter to reflect the new rating
            order.productId?.let { productId ->
                // Fetch the rating that was just submitted
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    FirebaseDatabase.getInstance()
                        .getReference("ratings")
                        .child(productId)
                        .child(currentUser.uid)
                        .get()
                        .addOnSuccessListener { snapshot ->
                            val rating = snapshot.getValue(Rating::class.java)
                            rating?.rating?.let { ratingValue ->
                                deliveredAdapter.updateRatingStatus(productId, ratingValue)
                            }
                        }
                }
            }
        }
        dialog.show()
    }

    private fun loadDeliveredOrders() {
        val userId = auth.currentUser?.uid ?: return
        val ordersRef = FirebaseDatabase.getInstance().getReference("orders")

        ordersRef.orderByChild("buyerId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    deliveredOrders.clear()

                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(Order::class.java)
                        if (order?.status == "completed" || order?.status == "delivered") {
                            deliveredOrders.add(order)
                        }
                    }

                    // Sort by completion date (newest first)
                    deliveredOrders.sortByDescending { it.completionDate ?: it.orderDate }

                    // Update UI
                    if (deliveredOrders.isEmpty()) {
                        deliveredRecyclerView.visibility = View.GONE
                        emptyDeliveredText.visibility = View.VISIBLE
                    } else {
                        deliveredRecyclerView.visibility = View.VISIBLE
                        emptyDeliveredText.visibility = View.GONE
                    }

                    deliveredAdapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("DeliveredOrdersFragment", "Failed to load orders: ${error.message}")
                }
            })
    }

    private fun downloadOrderImage(order: Order) {
        val imageUrl = order.productImage

        if (imageUrl.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "No image available", Toast.LENGTH_SHORT).show()
            return
        }

        // Check permission for Android 9 and below
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
                Toast.makeText(
                    requireContext(),
                    "Please grant storage permission and try again",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
        }

        Toast.makeText(requireContext(), "Downloading artwork...", Toast.LENGTH_SHORT).show()

        Glide.with(this)
            .asBitmap()
            .load(imageUrl)
            .into(object : CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?
                ) {
                    saveImageToStorage(resource, order.productName ?: "artwork")
                }

                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                }

                override fun onLoadFailed(errorDrawable: android.graphics.drawable.Drawable?) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to download image",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun saveImageToStorage(bitmap: Bitmap, productName: String) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${productName.replace(" ", "_")}_$timeStamp.jpg"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ - Use MediaStore (Scoped Storage)
            saveImageMediaStore(bitmap, fileName)
        } else {
            // Android 9 and below - Use traditional file system
            saveImageLegacy(bitmap, fileName)
        }
    }

    private fun saveImageMediaStore(bitmap: Bitmap, fileName: String) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/IllustCartImages")
            }

            val uri = requireContext().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            if (uri != null) {
                val outputStream: OutputStream? = requireContext().contentResolver.openOutputStream(uri)
                outputStream?.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                }

                Toast.makeText(
                    requireContext(),
                    "Artwork saved!\nLocation: Pictures/IllustCartImages/$fileName",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(requireContext(), "Failed to create file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Failed to save: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }

    private fun saveImageLegacy(bitmap: Bitmap, fileName: String) {
        try {
            val picturesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val illustCartDir = File(picturesDir, "IllustCartImages")

            if (!illustCartDir.exists()) {
                val created = illustCartDir.mkdirs()
                if (!created) {
                    Toast.makeText(
                        requireContext(),
                        "Failed to create directory",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            }

            val imageFile = File(illustCartDir, fileName)
            val outputStream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()

            // Add to gallery
            MediaStore.Images.Media.insertImage(
                requireContext().contentResolver,
                imageFile.absolutePath,
                fileName,
                "Artwork from IllustCart"
            )

            Toast.makeText(
                requireContext(),
                "Artwork saved!\nLocation: Pictures/IllustCartImages/$fileName",
                Toast.LENGTH_LONG
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Failed to save: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
            e.printStackTrace()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    requireContext(),
                    "Permission granted! Please try downloading again.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Permission denied. Cannot download images.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}