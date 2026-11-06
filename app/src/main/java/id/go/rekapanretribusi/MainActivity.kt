package id.go.rekapanretribusi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var pendingBackupJson: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.addJavascriptInterface(PrintBridge(this), "AndroidPrint")
        webView.addJavascriptInterface(FileBridge(this), "AndroidFiles")
        webView.loadUrl("file:///android_asset/index.html")
        setContentView(webView)
    }


    // Backup dan restore manual melalui pemilih dokumen Android.
    private class FileBridge(private val activity: MainActivity) {
        @JavascriptInterface
        fun saveJSON(json: String) {
            if (json.isBlank() || json.length > 20_000_000) {
                activity.runOnUiThread {
                    activity.webView.evaluateJavascript("window.onJSONSaved && window.onJSONSaved(false)", null)
                }
                return
            }
            activity.runOnUiThread {
                activity.pendingBackupJson = json
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "cadangan_seluruh_tahun_retribusi.json")
                }
                activity.startActivityForResult(intent, REQUEST_SAVE_JSON)
            }
        }

        @JavascriptInterface
        fun openJSON() {
            activity.runOnUiThread {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                }
                activity.startActivityForResult(intent, REQUEST_OPEN_JSON)
            }
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) {
            if (requestCode == REQUEST_SAVE_JSON) {
                pendingBackupJson = null
                webView.evaluateJavascript("window.onJSONSaved && window.onJSONSaved(false)", null)
            }
            return
        }

        val uri: Uri = data.data!!
        when (requestCode) {
            REQUEST_SAVE_JSON -> {
                val json = pendingBackupJson
                pendingBackupJson = null
                val success = try {
                    if (json == null) false else {
                        contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) } != null
                    }
                } catch (_: Exception) { false }
                webView.evaluateJavascript("window.onJSONSaved && window.onJSONSaved($success)", null)
            }
            REQUEST_OPEN_JSON -> {
                try {
                    val json = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    if (json.isNullOrBlank() || json.length > 20_000_000) {
                        webView.evaluateJavascript("window.onJSONRestoreResult && window.onJSONRestoreResult('')", null)
                    } else {
                        webView.evaluateJavascript("window.onJSONRestoreResult && window.onJSONRestoreResult(${JSONObject.quote(json)})", null)
                    }
                } catch (_: Exception) {
                    webView.evaluateJavascript("window.onJSONRestoreResult && window.onJSONRestoreResult('')", null)
                }
            }
        }
    }

    companion object {
        private const val REQUEST_SAVE_JSON = 7101
        private const val REQUEST_OPEN_JSON = 7102
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
