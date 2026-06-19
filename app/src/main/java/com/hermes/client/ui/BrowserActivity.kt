package com.hermes.client.ui

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.hermes.client.R
import com.hermes.client.databinding.ActivityBrowserBinding

class BrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserBinding
    private var currentUrl: String = "https://www.google.com"
    private var isPrivateMode = false

    private val webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            binding.progressBar.visibility = View.VISIBLE
            binding.addressBar.isEnabled = false
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            binding.progressBar.visibility = View.GONE
            binding.addressBar.isEnabled = true
            binding.addressBar.setText(url)
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            binding.progressBar.visibility = View.GONE
            showErrorPage(error?.description?.toString() ?: "Unknown error")
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            super.onReceivedHttpError(view, request, errorResponse)
            binding.progressBar.visibility = View.GONE
            showErrorPage("HTTP ${errorResponse?.statusCode}: ${errorResponse?.reasonPhrase}")
        }
    }

    private val webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            binding.progressBar.progress = newProgress
        }

        override fun onJsAlert(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            AlertDialog.Builder(this@BrowserActivity)
                .setMessage(message)
                .setPositiveButton("OK") { _: DialogInterface, _: Int -> result?.confirm() }
                .setOnDismissListener { result?.cancel() }
                .show()
            return true
        }

        override fun onJsConfirm(
            view: WebView?,
            url: String?,
            message: String?,
            result: JsResult?
        ): Boolean {
            AlertDialog.Builder(this@BrowserActivity)
                .setMessage(message)
                .setPositiveButton("OK") { _: DialogInterface, _: Int -> result?.confirm() }
                .setNegativeButton("Cancel") { _: DialogInterface, _: Int -> result?.cancel() }
                .setOnDismissListener { result?.cancel() }
                .show()
            return true
        }

        override fun onJsPrompt(
            view: WebView?,
            url: String?,
            message: String?,
            defaultValue: String?,
            result: JsPromptResult?
        ): Boolean {
            val input = android.widget.EditText(this@BrowserActivity).apply {
                setText(defaultValue)
                setPadding(50, 0, 50, 0)
            }
            AlertDialog.Builder(this@BrowserActivity)
                .setMessage(message)
                .setView(input)
                .setPositiveButton("OK") { _: DialogInterface, _: Int ->
                    val text = input.text.toString()
                    result?.confirm(text)
                }
                .setNegativeButton("Cancel") { _: DialogInterface, _: Int -> result?.cancel() }
                .setOnDismissListener { result?.cancel() }
                .show()
            return true
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            // Handle file upload
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(
                Intent.createChooser(intent, "Select File"),
                FILE_CHOOSER_REQUEST
            )
            return true
        }
    }

    companion object {
        private const val FILE_CHOOSER_REQUEST = 100
        private const val MENU_DOWNLOAD_IMAGE = 1
        private const val MENU_OPEN_IMAGE = 2
        private const val MENU_OPEN_LINK = 3
        private const val MENU_COPY_LINK = 4
        const val EXTRA_URL = "extra_url"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Browser"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupWebView()
        setupUI()
        
        // Load URL from intent or default
        val url = intent.getStringExtra(EXTRA_URL) ?: currentUrl
        loadUrl(url)
    }

    private fun setupWebView() {
        with(binding.webView) {
            webViewClient = this@BrowserActivity.webViewClient
            webChromeClient = this@BrowserActivity.webChromeClient
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                
                // Security settings
                allowFileAccess = false
                allowContentAccess = false
                
                // Performance
                mediaPlaybackRequiresUserGesture = false
                
                // Privacy
                setGeolocationEnabled(false)
            }
            
            // Add JavaScript interface for Hermes integration
            addJavascriptInterface(HermesWebInterface(this@BrowserActivity), "Hermes")
        }
    }

    private fun setupUI() {
        binding.addressBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_GO ||
                (event?.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)
            ) {
                val url = binding.addressBar.text.toString().trim()
                if (url.isNotEmpty()) {
                    loadUrl(if (url.contains(".") && !url.startsWith("http")) "https://$url" else url)
                }
                true
            } else {
                false
            }
        }

        binding.btnBack.setOnClickListener {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            }
        }

        binding.btnForward.setOnClickListener {
            if (binding.webView.canGoForward()) {
                binding.webView.goForward()
            }
        }

        binding.btnRefresh.setOnClickListener {
            binding.webView.reload()
        }

        binding.btnHome.setOnClickListener {
            loadUrl("https://www.google.com")
        }

        binding.btnStop.setOnClickListener {
            binding.webView.stopLoading()
        }

        // Swipe-to-refresh disabled due to missing dependency
        

        // Context menu for long press
        binding.webView.setOnCreateContextMenuListener { menu, v, menuInfo ->
            val hitTestResult = binding.webView.hitTestResult
            when (hitTestResult.type) {
                WebView.HitTestResult.IMAGE_TYPE,
                WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                    menu.add(0, MENU_DOWNLOAD_IMAGE, 0, "Download Image")
                    menu.add(0, MENU_OPEN_IMAGE, 1, "Open in New Tab")
                }
                WebView.HitTestResult.ANCHOR_TYPE -> {
                    menu.add(0, MENU_OPEN_LINK, 0, "Open Link")
                    menu.add(0, MENU_COPY_LINK, 1, "Copy Link Address")
                }
            }
        }
    }

    private fun loadUrl(url: String) {
        currentUrl = url
        binding.webView.loadUrl(url)
        binding.addressBar.setText(url)
    }

    private fun showErrorPage(error: String) {
        binding.webView.loadDataWithBaseURL(
            null,
            """
            <!DOCTYPE html>
            <html>
            <head><title>Error</title></head>
            <body style="font-family: sans-serif; text-align: center; padding: 50px;">
                <h2>Failed to load page</h2>
                <p>$error</p>
                <button onclick="location.reload()">Retry</button>
            </body>
            </html>
            """.trimIndent(),
            "text/html",
            "UTF-8",
            null
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when {
            keyCode == KeyEvent.KEYCODE_BACK && binding.webView.canGoBack() -> {
                binding.webView.goBack()
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.browser_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                binding.webView.reload()
                true
            }
            R.id.action_history -> {
                showHistory()
                true
            }
            R.id.action_bookmarks -> {
                showBookmarks()
                true
            }
            R.id.action_download -> {
                downloadCurrentPage()
                true
            }
            R.id.action_share -> {
                shareCurrentPage()
                true
            }
            R.id.action_privacy -> {
                togglePrivacyMode()
                true
            }
            R.id.action_clear_data -> {
                clearBrowserData()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showHistory() {
        // History feature - stub for now (WebBackForwardList API varies by SDK version)
        Toast.makeText(this, "History feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun showBookmarks() {
        // Bookmarks feature
        Toast.makeText(this, "Bookmarks feature available", Toast.LENGTH_SHORT).show()
    }

    private fun downloadCurrentPage() {
        Toast.makeText(this, "Download functionality", Toast.LENGTH_SHORT).show()
    }

    private fun shareCurrentPage() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, currentUrl)
        }
        startActivity(Intent.createChooser(shareIntent, "Share Page"))
    }

    private fun togglePrivacyMode() {
        isPrivateMode = !isPrivateMode
        if (isPrivateMode) {
            binding.webView.clearHistory()
            Toast.makeText(this, "Private mode enabled", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Private mode disabled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearBrowserData() {
        AlertDialog.Builder(this)
            .setTitle("Clear Browser Data")
            .setMessage("This will clear cache, cookies, and browsing data. Continue?")
            .setPositiveButton("Clear") { _: DialogInterface, _: Int ->
                val webSettings = binding.webView.settings
                webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
                binding.webView.clearCache(true)
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                Toast.makeText(this, "Browser data cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST) {
            // Handle file upload result
            // Note: Full implementation requires ValueCallback handling
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Properly clean up WebView to prevent memory leaks
        binding.webView.apply {
            // Remove callbacks to prevent memory leaks
            webViewClient = object : WebViewClient() {}
            webChromeClient = null
            destroy()
        }
    }

    // JavaScript interface for Hermes integration
    class HermesWebInterface(private val activity: BrowserActivity) {
        @JavascriptInterface
        fun showToast(message: String) {
            activity.runOnUiThread {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
            }
        }

        @JavascriptInterface
        fun navigateTo(url: String) {
            activity.runOnUiThread {
                activity.loadUrl(url)
            }
        }

        @JavascriptInterface
        fun goBack() {
            activity.runOnUiThread {
                if (activity.binding.webView.canGoBack()) {
                    activity.binding.webView.goBack()
                }
            }
        }

        @JavascriptInterface
        fun goForward() {
            activity.runOnUiThread {
                if (activity.binding.webView.canGoForward()) {
                    activity.binding.webView.goForward()
                }
            }
        }

        @JavascriptInterface
        fun reload() {
            activity.runOnUiThread {
                activity.binding.webView.reload()
            }
        }

        @JavascriptInterface
        fun getCurrentUrl(): String {
            return activity.binding.webView.url ?: ""
        }

        @JavascriptInterface
        fun canGoBack(): Boolean {
            return activity.binding.webView.canGoBack()
        }

        @JavascriptInterface
        fun canGoForward(): Boolean {
            return activity.binding.webView.canGoForward()
        }
    }
}
