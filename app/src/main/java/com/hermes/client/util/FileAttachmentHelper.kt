package com.hermes.client.util

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class FileAttachmentHelper(
    private val activity: Activity,
    private val onFileSelected: (Uri, String) -> Unit,
    private val onImageCaptured: (Uri) -> Unit
) {
    private var currentImageUri: Uri? = null

    // Request codes
    companion object {
        const val REQUEST_PICK_IMAGE = 1001
        const val REQUEST_CAPTURE_IMAGE = 1002
        const val REQUEST_PICK_FILE = 1003
        const val PERMISSION_REQUEST_STORAGE = 1004
    }

    fun pickImage() {
        if (!hasStoragePermission()) {
            requestStoragePermission()
            return
        }

        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        activity.startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    fun captureImage() {
        if (!hasStoragePermission()) {
            requestStoragePermission()
            return
        }

        currentImageUri = createImageUri()
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, currentImageUri)
        activity.startActivityForResult(intent, REQUEST_CAPTURE_IMAGE)
    }

    fun pickFile() {
        if (!hasStoragePermission()) {
            requestStoragePermission()
            return
        }

        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        activity.startActivityForResult(
            Intent.createChooser(intent, "Select File"),
            REQUEST_PICK_FILE
        )
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (resultCode != Activity.RESULT_OK) return false

        return when (requestCode) {
            REQUEST_PICK_IMAGE -> {
                data?.data?.let { uri ->
                    onFileSelected(uri, "image")
                }
                true
            }
            REQUEST_CAPTURE_IMAGE -> {
                currentImageUri?.let { uri ->
                    onFileSelected(uri, "image")
                }
                true
            }
            REQUEST_PICK_FILE -> {
                data?.data?.let { uri ->
                    onFileSelected(uri, "file")
                }
                true
            }
            else -> false
        }
    }

    private fun createImageUri(): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "hermes_${System.currentTimeMillis()}")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Hermes")
            }
        }
        return activity.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw IllegalStateException("Failed to create image URI")
    }

    private fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(
            activity,
            permissions,
            PERMISSION_REQUEST_STORAGE
        )
    }
}
