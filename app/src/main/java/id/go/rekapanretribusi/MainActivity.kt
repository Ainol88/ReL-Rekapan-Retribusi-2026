package id.go.rekapanretribusi

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
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
    private val CREATE_BACKUP = 4101
    private val RESTORE_BACKUP = 4102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.addJavascriptInterface(PrintBridge(this), "AndroidPrint")
        webView.addJavascriptInterface(BackupBridge(this), "AndroidBackup")
        webView.loadUrl("file:///android_asset/index.html")
        setContentView(webView)
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

    private class BackupBridge(private val activity: MainActivity) {
        @JavascriptInterface
        fun saveBackup(json: String) {
            activity.runOnUiThread {
                activity.pendingBackupJson = json
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, "cadangan_seluruh_tahun_retribusi.json")
                }
                activity.startActivityForResult(intent, activity.CREATE_BACKUP)
            }
        }

        @JavascriptInterface
        fun openRestorePicker() {
            activity.runOnUiThread {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/json", "text/plain"))
                }
                activity.startActivityForResult(intent, activity.RESTORE_BACKUP)
            }
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            if (requestCode == CREATE_BACKUP) {
                webView.evaluateJavascript("window.onBackupSaved && window.onBackupSaved(false)", null)
            }
            return
        }
        val uri: Uri = data.data!!
        try {
            when (requestCode) {
                CREATE_BACKUP -> {
                    val json = pendingBackupJson ?: throw IllegalStateException("Tidak ada data backup")
                    contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                        ?: throw IllegalStateException("Tidak dapat membuka file tujuan")
                    pendingBackupJson = null
                    webView.evaluateJavascript("window.onBackupSaved && window.onBackupSaved(true)", null)
                }
                RESTORE_BACKUP -> {
                    val json = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: throw IllegalStateException("File tidak dapat dibaca")
                    val quoted = org.json.JSONObject.quote(json)
                    webView.evaluateJavascript("window.onBackupFileLoaded($quoted)", null)
                }
            }
        } catch (e: Exception) {
            val message = org.json.JSONObject.quote(e.message ?: "Terjadi kesalahan file")
            webView.evaluateJavascript("alert('Gagal memproses file backup: '+$message)", null)
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
