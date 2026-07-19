package com.xito.claudetimer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var pickHours: NumberPicker
    private lateinit var pickMinutes: NumberPicker
    private lateinit var countdown: TextView
    private lateinit var stateLabel: TextView
    private lateinit var progress: ProgressBar

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ensureNotificationPermission()

        pickHours = findViewById(R.id.pickHours)
        pickMinutes = findViewById(R.id.pickMinutes)
        countdown = findViewById(R.id.countdown)
        stateLabel = findViewById(R.id.stateLabel)
        progress = findViewById(R.id.progress)

        pickHours.minValue = 0; pickHours.maxValue = 12
        pickMinutes.minValue = 0; pickMinutes.maxValue = 59
        pickMinutes.setFormatter { String.format("%02d", it) }

        val prefs = getSharedPreferences(TimerService.PREFS, Context.MODE_PRIVATE)
        val dur = prefs.getInt(TimerService.KEY_DUR_MIN, 300)
        pickHours.value = dur / 60
        pickMinutes.value = dur % 60

        findViewById<TextView>(R.id.chip5).setOnClickListener { setDur(5, 0) }
        findViewById<TextView>(R.id.chip4).setOnClickListener { setDur(4, 0) }
        findViewById<TextView>(R.id.chip3).setOnClickListener { setDur(3, 0) }
        findViewById<TextView>(R.id.chip2).setOnClickListener { setDur(2, 0) }
        findViewById<TextView>(R.id.chip1).setOnClickListener { setDur(1, 0) }

        findViewById<TextView>(R.id.btnStart).setOnClickListener { start() }
        findViewById<TextView>(R.id.btnReset).setOnClickListener { send(TimerService.ACTION_RESET) }
        findViewById<TextView>(R.id.btnStop).setOnClickListener { send(TimerService.ACTION_STOP) }
        findViewById<TextView>(R.id.btnShow).setOnClickListener { send(TimerService.ACTION_SHOW) }
    }

    private fun setDur(h: Int, m: Int) {
        pickHours.value = h
        pickMinutes.value = m
    }

    private fun start() {
        val mins = pickHours.value * 60 + pickMinutes.value
        if (mins <= 0) return
        getSharedPreferences(TimerService.PREFS, Context.MODE_PRIVATE)
            .edit().putInt(TimerService.KEY_DUR_MIN, mins).apply()
        val i = Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_DUR_MIN, mins)
        }
        launch(i)
    }

    private fun send(action: String) {
        launch(Intent(this, TimerService::class.java).apply { this.action = action })
    }

    private fun launch(i: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
    }

    private fun refresh() {
        val prefs = getSharedPreferences(TimerService.PREFS, Context.MODE_PRIVATE)
        val end = prefs.getLong(TimerService.KEY_END, 0L)
        val startT = prefs.getLong(TimerService.KEY_START, 0L)
        val now = System.currentTimeMillis()
        if (end > now && startT > 0) {
            val remaining = end - now
            val total = (end - startT).coerceAtLeast(1L)
            stateLabel.text = "Sesión activa"
            countdown.text = fmt(remaining)
            progress.progress = (1000 - (remaining * 1000 / total)).toInt().coerceIn(0, 1000)
        } else {
            stateLabel.text = "Sin sesión activa"
            countdown.text = "--:--:--"
            progress.progress = 0
        }
    }

    private fun fmt(ms: Long): String {
        val s = ms / 1000
        return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }

    override fun onResume() {
        super.onResume()
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }
}
