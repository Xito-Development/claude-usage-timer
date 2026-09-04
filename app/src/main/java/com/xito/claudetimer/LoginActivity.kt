package com.xito.claudetimer

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import android.widget.Toast

class LoginActivity : Activity() {

    private lateinit var web: WebView
    private lateinit var status: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var done = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        web = findViewById(R.id.web)
        status = findViewById(R.id.status)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125 Mobile Safari/537.36"

        web.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                checkCookie()
            }
        }
        web.loadUrl("https://claude.ai/login")
        pollCookie()
    }

    private fun pollCookie() {
        handler.postDelayed({
            if (!done) { checkCookie(); pollCookie() }
        }, 1500)
    }

    private fun checkCookie() {
        if (done) return
        val cookies = CookieManager.getInstance().getCookie("https://claude.ai") ?: return
        val key = cookies.split(";")
            .map { it.trim() }
            .firstOrNull { it.startsWith("sessionKey=") }
            ?.removePrefix("sessionKey=")
            ?: return
        if (key.length < 20) return
        done = true
        status.text = "Sesión detectada · comprobando cuenta…"
        Store.saveSession(this, key)
        Thread {
            val org = ClaudeApi.fetchOrgId(key)
            handler.post {
                if (org != null) {
                    Store.saveOrg(this, org)
                    Store.setAuto(this, true)
                    Toast.makeText(this, "Cuenta conectada", Toast.LENGTH_SHORT).show()
                    UsageSync.syncNow(this)
                    setResult(RESULT_OK)
                    finish()
                } else {
                    status.text = "No se pudo leer la cuenta. Reintentando…"
                    done = false
                }
            }
        }.start()
    }
}
