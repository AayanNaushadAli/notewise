package com.notewise.clone.core.markdown

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import org.json.JSONObject

@SuppressLint("SetJavaScriptEnabled")
class MilkdownEditorView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val webView: WebView = WebView(context)
    var onContentChanged: ((markdown: String) -> Unit)? = null
    var onEditorReady: (() -> Unit)? = null
    var isReady: Boolean = false
        private set

    private var pendingMarkdown: String? = null

    init {
        addView(
            webView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )
        )

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
        }

        webView.addJavascriptInterface(EditorBridge(), "notewiseEditorBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isReady = true
                pendingMarkdown?.let { md ->
                    setMarkdown(md)
                    pendingMarkdown = null
                }
                onEditorReady?.invoke()
            }
        }

        webView.loadUrl("file:///android_asset/milkdown-editor/index.html")
    }

    fun setMarkdown(markdown: String) {
        if (!isReady) {
            pendingMarkdown = markdown
            return
        }
        val escaped = JSONObject.quote(markdown)
        webView.evaluateJavascript("window.notewiseEditor && window.notewiseEditor.setMarkdown($escaped);", null)
    }

    fun setEditable(editable: Boolean) {
        if (isReady) {
            webView.evaluateJavascript("window.notewiseEditor && window.notewiseEditor.setEditable($editable);", null)
        }
    }

    inner class EditorBridge {
        @JavascriptInterface
        fun postMessage(jsonString: String) {
            try {
                val json = JSONObject(jsonString)
                val type = json.optString("type")
                when (type) {
                    "contentChange" -> {
                        val md = json.optString("markdown", "")
                        post { onContentChanged?.invoke(md) }
                    }
                    "contentReady" -> {
                        post { onEditorReady?.invoke() }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

