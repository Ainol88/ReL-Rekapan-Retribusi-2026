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
import android.util.Base64
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var pendingBackupContent: String? = null
    private val REQUEST_SAVE_BACKUP = 4101
    private val REQUEST_OPEN_BACKUP = 4102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        webView.addJavascriptInterface(PrintBridge(this), "AndroidPrint")
        webView.addJavascriptInterface(FileBridge(), "AndroidFiles")
        webView.loadUrl("file:///android_asset/index.html")
        setContentView(webView)
    }

    private inner class FileBridge {
        @JavascriptInterface
        fun saveBackup(content: String, filename: String) {
            runOnUiThread {
                pendingBackupContent = content
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(Intent.EXTRA_TITLE, filename)
                }
                startActivityForResult(intent, REQUEST_SAVE_BACKUP)
            }
        }

        @JavascriptInterface
        fun chooseBackup() {
            runOnUiThread {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivityForResult(intent, REQUEST_OPEN_BACKUP)
            }
        }
    }

    @Deprecated("Deprecated in Android")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data?.data == null) {
            pendingBackupContent = null
            return
        }
        val uri: Uri = data.data!!
        when (requestCode) {
            REQUEST_SAVE_BACKUP -> {
                val content = pendingBackupContent
                pendingBackupContent = null
                try {
                    if (content == null) throw IllegalStateException("Backup kosong")
                    contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
                        ?: throw IllegalStateException("Tidak dapat membuka lokasi penyimpanan")
                    Toast.makeText(this, "Backup berhasil disimpan", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal menyimpan backup: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            REQUEST_OPEN_BACKUP -> {
                try {
                    val content = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                        ?: throw IllegalStateException("File tidak dapat dibaca")
                    val encoded = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    webView.evaluateJavascript(
                        "window.onNativeBackupLoaded && window.onNativeBackupLoaded(new TextDecoder().decode(Uint8Array.from(atob('$encoded'), c => c.charCodeAt(0))), 'backup.json');",
                        null
                    )
                } catch (e: Exception) {
                    Toast.makeText(this, "Gagal membaca backup: ${e.message}", Toast.LENGTH_LONG).show()
                }
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
