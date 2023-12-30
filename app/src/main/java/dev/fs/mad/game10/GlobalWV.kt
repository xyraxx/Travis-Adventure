package dev.fs.mad.game10

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import org.json.JSONException
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
class GlobalWV : WebView {

    private inner class JSInterface {
        @JavascriptInterface
        fun postMessage(name: String, data: String) {
            val eventValue: MutableMap<String, Any> = HashMap()

            if ("openWindow" == name) {
                try {
                    val extLink = JSONObject(data)
                    val newWindow = Intent(Intent.ACTION_VIEW)
                    newWindow.data = Uri.parse(extLink.getString("url"))
                    newWindow.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(newWindow)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            } else {
                eventValue[name] = data
            }
        }
    }

    constructor(context: Context) : super(context) {
        initWebViewSettings()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initWebViewSettings()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        initWebViewSettings()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebViewSettings() {
        val webSettings: WebSettings = settings
        webSettings.javaScriptEnabled = true
        webSettings.builtInZoomControls = true
        webSettings.displayZoomControls = false
        webSettings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        webSettings.domStorageEnabled = true
        webSettings.loadsImagesAutomatically = true
        webSettings.setSupportMultipleWindows(true)
        webSettings.javaScriptCanOpenWindowsAutomatically = true

        webViewClient = CustomWebClient()
        addJavascriptInterface(JSInterface(), "jsBridge")
    }

    private inner class CustomWebClient : WebViewClient() {

        override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
            // Fix Copyright Logo
            Handler(Looper.getMainLooper()).postDelayed({
                view.evaluateJavascript(
                    "" +
                            "(function() { " +
                            "   if(document.getElementById('pngPreloaderWrapper'))" +
                            "   {" +
                            "       document.getElementById('pngPreloaderWrapper').removeChild(document.getElementById('pngLogoWrapper'));" +
                            "   }" +
                            "})();"
                    , null)
            }, 600)

            // Fix Lobby Button
            Handler(Looper.getMainLooper()).postDelayed({
                view.evaluateJavascript(
                    "" +
                            "(function() { " +
                            "   if(document.getElementById('lobbyButtonWrapper'))" +
                            "   {" +
                            "       document.getElementById('lobbyButtonWrapper').style = 'display:none';" +
                            "   }" +
                            "})"
                    , null)
            }, 5400)

            // Define the JavaScript code to remove the element
            val removeElementCode = "(function() { " +
                    "   var elementToRemove = document.querySelector('.suggest-download-h5_top');" +
                    "   if (elementToRemove) {" +
                    "       elementToRemove.parentNode.removeChild(elementToRemove);" +
                    "   }" +
                    "})();"

            view.evaluateJavascript(removeElementCode, null)
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            Handler(Looper.getMainLooper()).postDelayed({
                view.evaluateJavascript(
                    "" +
                            "(function() { document.getElementById('suggest-download-h5_top').innerHTML = ''; document.getElementById('headerWrap').style = 'position:fixed; top:0px;';})();"
                    , null)
            }, 1800)
        }

        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            Handler(Looper.getMainLooper()).postDelayed({
                view.evaluateJavascript(
                    "" +
                            "(function() { " +
                            "   if(document.getElementById('suggest-download-h5_top'))" +
                            "   {" +
                            "   document.getElementById('suggest-download-h5_top').innerHTML = '';" +
                            "   document.getElementById('headerWrap').style = 'position:fixed; top:0px;';" +
                            "   }" +
                            "});" +
                            "" +
                            "", null)
            }, 600)
        }
    }
    

}



