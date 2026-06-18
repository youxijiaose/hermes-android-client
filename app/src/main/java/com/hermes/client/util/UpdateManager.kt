package com.hermes.client.util

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import com.hermes.client.api.HermesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

class UpdateManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("hermes_prefs", Context.MODE_PRIVATE)
    private var api: HermesApi? = null

    fun setupApi(baseUrl: String, apiKey: String) {
        api = HermesApi(baseUrl, apiKey)
    }

    /**
     * Check for available updates from the Hermes API Server
     */
    suspend fun checkForUpdates(): UpdateInfo? {
        return try {
            val currentVersion = getCurrentVersionInfo()
            val updateInfo = api?.getUpdateInfo()
            
            updateInfo?.onSuccess { info ->
                if (info.versionCode > currentVersion.versionCode) {
                    return@checkForUpdates info
                }
                return@checkForUpdates null
            }?.onFailure {
                Log.e(TAG, "Failed to check for updates: ${it.message}")
                return@checkForUpdates null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for updates", e)
            return@checkForUpdates null
        }
    }

    /**
     * Download and install update APK
     */
    suspend fun downloadAndUpdate(updateInfo: UpdateInfo): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val apkUrl = updateInfo.apkUrl
                val apkFile = downloadApk(apkUrl)
                
                if (apkFile != null) {
                    installApk(apkFile, updateInfo.versionName)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download/update", e)
                false
            }
        }
    }

    /**
     * Get current app version info
     */
    private fun getCurrentVersionInfo(): VersionInfo {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    PackageManager.GET_SIGNING_CERTIFICATES
                } else {
                    @Suppress("DEPRECATION")
                    PackageManager.GET_SIGNATURES
                }
            )
            VersionInfo(
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
                versionName = packageInfo.versionName ?: "unknown"
            )
        } catch (e: PackageManager.NameNotFoundException) {
            VersionInfo(0, "unknown")
        }
    }

    /**
     * Download APK from URL
     */
    private suspend fun downloadApk(url: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 30000
                connection.readTimeout = 60000
                connection.requestMethod = "GET"
                
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream: InputStream = connection.inputStream
                    val apkFile = File(context.cacheDir, "hermes_update_${System.currentTimeMillis()}.apk")
                    
                    FileOutputStream(apkFile).use { output ->
                        inputStream.copyTo(output)
                    }
                    
                    Log.d(TAG, "APK downloaded to: ${apkFile.absolutePath}")
                    apkFile
                } else {
                    Log.e(TAG, "Failed to download APK: HTTP ${connection.responseCode}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading APK", e)
                null
            }
        }
    }

    /**
     * Install APK using PackageInstaller API (Android 5.0+)
     */
    private suspend fun installApk(apkFile: File, versionName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // For Android 7.0+ we need to use FileProvider
                // This is a simplified implementation - full implementation requires FileProvider setup
                
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Use FileProvider for Android 7.0+
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                } else {
                    Uri.fromFile(apkFile)
                }
                
                intent.setDataAndType(uri, "application/vnd.android.package-archive")
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to install APK", e)
                false
            }
        }
    }

    /**
     * Get update preferences
     */
    fun getUpdatePreferences(): UpdatePreferences {
        return UpdatePreferences(
            autoCheck = prefs.getBoolean(KEY_AUTO_CHECK, true),
            autoDownload = prefs.getBoolean(KEY_AUTO_DOWNLOAD, false),
            autoInstall = prefs.getBoolean(KEY_AUTO_INSTALL, false),
            notifyOnUpdate = prefs.getBoolean(KEY_NOTIFY, true),
            checkInterval = prefs.getInt(KEY_CHECK_INTERVAL, 24 * 60 * 60 * 1000) // Default: 24 hours
        )
    }

    /**
     * Save update preferences
     */
    fun saveUpdatePreferences(preferences: UpdatePreferences) {
        prefs.edit().apply {
            putBoolean(KEY_AUTO_CHECK, preferences.autoCheck)
            putBoolean(KEY_AUTO_DOWNLOAD, preferences.autoDownload)
            putBoolean(KEY_AUTO_INSTALL, preferences.autoInstall)
            putBoolean(KEY_NOTIFY, preferences.notifyOnUpdate)
            putInt(KEY_CHECK_INTERVAL, preferences.checkInterval)
            apply()
        }
    }

    companion object {
        private const val TAG = "UpdateManager"
        private const val KEY_AUTO_CHECK = "auto_check"
        private const val KEY_AUTO_DOWNLOAD = "auto_download"
        private const val KEY_AUTO_INSTALL = "auto_install"
        private const val KEY_NOTIFY = "notify_on_update"
        private const val KEY_CHECK_INTERVAL = "check_interval"
    }
}

/**
 * Update information from server
 */
data class UpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String? = null,
    val mandatory: Boolean = false,
    val publishedAt: Long = System.currentTimeMillis()
)

/**
 * Current app version
 */
data class VersionInfo(
    val versionCode: Long,
    val versionName: String
)

/**
 * User preferences for updates
 */
data class UpdatePreferences(
    val autoCheck: Boolean = true,
    val autoDownload: Boolean = false,
    val autoInstall: Boolean = false,
    val notifyOnUpdate: Boolean = true,
    val checkInterval: Int = 24 * 60 * 60 * 1000 // 24 hours in milliseconds
)
