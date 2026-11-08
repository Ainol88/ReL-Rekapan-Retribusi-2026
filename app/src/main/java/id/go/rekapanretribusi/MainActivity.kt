package id.go.rekapanretribusi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.widget.Toast
import java.io.IOException

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingBackupJson: String? = null

    companion object {
        private const val REQUEST_OPEN_BACKUP = 4101
        private const val REQUEST_SAVE_BACKUP = 4102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView?,
                callback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/json", "text/plain"))
                }
                return try {
                    startActivityForResult(intent, REQUEST_OPEN_BACKUP)
                    true
                } catch (e: Exception) {
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = null
                    Toast.makeText(this@MainActivity, "Tidak dapat membuka pemilih file.", Toast.LENGTH_LONG).show()
                    false
                }
            }
        }
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.addJavascriptInterface(PrintBridge(this), "AndroidPrint")
        webView.addJavascriptInterface(BackupBridge(this), "AndroidBackup")
        webView.loadUrl("file:///android_asset/index.html")
        setContentView(webView)
    }

    fun beginSaveBackup(json: String) {
        pendingBackupJson = json
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "cadangan_seluruh_tahun_retribusi.json")
        }
        try {
            startActivityForResult(intent, REQUEST_SAVE_BACKUP)
        } catch (e: Exception) {
            pendingBackupJson = null
            Toast.makeText(this, "Tidak dapat membuka lokasi penyimpanan.", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_OPEN_BACKUP -> {
                val callback = filePathCallback
                filePathCallback = null
                if (resultCode == Activity.RESULT_OK && data?.data != null) {
                    callback?.onReceiveValue(arrayOf(data.data!!))
                } else {
                    callback?.onReceiveValue(null)
                }
            }
            REQUEST_SAVE_BACKUP -> {
                val json = pendingBackupJson
                pendingBackupJson = null
                if (resultCode == Activity.RESULT_OK && data?.data != null && json != null) {
                    try {
                        contentResolver.openOutputStream(data.data!!)?.use { output ->
                            output.write(json.toByteArray(Charsets.UTF_8))
                        } ?: throw IOException("Tidak dapat membuka file tujuan.")
                        Toast.makeText(this, "Backup berhasil disimpan.", Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(this, "Backup gagal disimpan: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private class BackupBridge(private val activity: Activity) {
        @JavascriptInterface
        fun saveBackup(json: String) {
            activity.runOnUiThread {
                (activity as MainActivity).beginSaveBackup(json)
            }
        }
    }

    private class PrintBridge(private val activity: Activity) {
        @JavascriptInterface
        fun print() {
            activity.runOnUiThread {
                val manager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val adapter = (activity as MainActivity).webView.createPrintDocumentAdapter("Rekapan_Retribusi_2026")
                manager.print("Rekapan Retribusi 2026", adapter, PrintAttributes.Builder().build())
            }
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
