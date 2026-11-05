package id.go.rekapanretribusi

import android.app.Activity
import android.content.Context
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


    /**
     * Cadangan otomatis lokal bergilir di direktori privat aplikasi.
     * Backup manual tetap diperlukan untuk perlindungan dari uninstall/kerusakan HP.
     */
    private class BackupBridge(private val context: Context) {
        @JavascriptInterface
        @Synchronized
        fun saveBackup(json: String): Boolean {
            return try {
                if (json.isBlank() || json.length > 20_000_000) return false
                val data = JSONObject(json)
                val keys = data.keys()
                var hasYear = false
                while (keys.hasNext()) {
                    val year = keys.next()
                    if (!year.matches(Regex("\\d{4}")) || data.optJSONArray(year) == null) return false
                    hasYear = true
                }
                if (!hasYear) return false

                val digest = MessageDigest.getInstance("SHA-256")
                    .digest(json.toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it) }
                val stamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
                val dir = File(context.filesDir, "automatic_backups")
                if (!dir.exists() && !dir.mkdirs()) return false

                val envelope = JSONObject().apply {
                    put("format", "ReL-Retribusi-AutoBackup")
                    put("version", 1)
                    put("createdAt", System.currentTimeMillis())
                    put("sha256", digest)
                    put("data", data)
                }.toString()

                val target = File(dir, "backup_$stamp.json")
                val temp = File(dir, "backup_$stamp.tmp")
                temp.writeText(envelope, Charsets.UTF_8)
                if (!temp.renameTo(target)) {
                    temp.delete()
                    return false
                }

                dir.listFiles { file -> file.isFile && file.name.endsWith(".json") }
                    ?.sortedByDescending { it.lastModified() }
                    ?.drop(10)
                    ?.forEach { it.delete() }
                true
            } catch (_: Exception) {
                false
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
