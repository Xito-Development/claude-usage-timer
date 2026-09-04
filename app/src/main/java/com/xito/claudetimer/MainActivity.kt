package com.xito.claudetimer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var pickHours: NumberPicker
    private lateinit var pickMinutes: NumberPicker
    private lateinit var timePicker: TimePicker
    private lateinit var countdown: TextView
    private lateinit var stateLabel: TextView
    private lateinit var resetAt: TextView
    private lateinit var headerSub: TextView
    private lateinit var progress: ProgressBar
    private lateinit var rowChips: LinearLayout
    private lateinit var rowPickers: LinearLayout
    private lateinit var rowClock: LinearLayout
    private lateinit var cardManual: LinearLayout
    private lateinit var panelAuto: LinearLayout
    private lateinit var modeHint: TextView
    private lateinit var tabManual: TextView
    private lateinit var tabAuto: TextView
    private lateinit var modeDuration: TextView
    private lateinit var modeClock: TextView
    private lateinit var accountState: TextView
    private lateinit var barSession: ProgressBar
    private lateinit var barWeekly: ProgressBar
    private lateinit var lblSession: TextView
    private lateinit var lblWeekly: TextView
    private lateinit var lastSync: TextView
    private lateinit var btnLogin: TextView
    private lateinit var btnStart: TextView
    private lateinit var cardAlerts: LinearLayout
    private lateinit var cardActivity: LinearLayout
    private lateinit var historyBox: LinearLayout
    private lateinit var activitySub: TextView
    private lateinit var liveDot: TextView
    private lateinit var lastDelta: TextView
    private lateinit var lastDeltaWhen: TextView
    private lateinit var statMsgs: TextView
    private lateinit var statTotal: TextView
    private lateinit var btnFast: TextView

    private var clockMode = false

    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() { refresh(); handler.postDelayed(this, 1000) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ensureNotificationPermission()
        bind()

        val prefs = Store.prefs(this)
        pickHours.minValue = 0; pickHours.maxValue = 12
        pickMinutes.minValue = 0; pickMinutes.maxValue = 59
        pickMinutes.setFormatter { String.format("%02d", it) }
        timePicker.setIs24HourView(true)

        val dur = prefs.getInt(TimerService.KEY_DUR_MIN, 300)
        pickHours.value = dur / 60
        pickMinutes.value = dur % 60

        findViewById<TextView>(R.id.chip5).setOnClickListener { setDur(5, 0) }
        findViewById<TextView>(R.id.chip4).setOnClickListener { setDur(4, 0) }
        findViewById<TextView>(R.id.chip3).setOnClickListener { setDur(3, 0) }
        findViewById<TextView>(R.id.chip2).setOnClickListener { setDur(2, 0) }
        findViewById<TextView>(R.id.chip1).setOnClickListener { setDur(1, 0) }

        btnStart.setOnClickListener { start() }
        findViewById<TextView>(R.id.btnReset).setOnClickListener { send(TimerService.ACTION_RESET) }
        findViewById<TextView>(R.id.btnStop).setOnClickListener { send(TimerService.ACTION_STOP) }
        findViewById<TextView>(R.id.btnShow).setOnClickListener { send(TimerService.ACTION_SHOW) }

        modeDuration.setOnClickListener { setClockMode(false) }
        modeClock.setOnClickListener { setClockMode(true) }
        tabManual.setOnClickListener { setAuto(false) }
        tabAuto.setOnClickListener { setAuto(true) }

        btnLogin.setOnClickListener {
            if (Store.loggedIn(this)) {
                Store.logout(this); UsageSync.cancel(this)
                Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
                renderAuto()
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
        findViewById<TextView>(R.id.btnSync).setOnClickListener {
            if (!Store.loggedIn(this)) {
                Toast.makeText(this, "Inicia sesión primero", Toast.LENGTH_SHORT).show()
            } else {
                lastSync.text = "Actualizando…"
                UsageSync.syncNow(this) { handler.post { renderAuto() } }
            }
        }

        btnFast.setOnClickListener {
            UsageSync.setFast(this, !UsageSync.isFast(this))
            renderActivity()
        }
        setupThresholdChips()
        setClockMode(prefs.getBoolean(TimerService.KEY_CLOCK_MODE, false))
        setAuto(Store.isAuto(this))
    }

    private fun bind() {
        pickHours = findViewById(R.id.pickHours)
        pickMinutes = findViewById(R.id.pickMinutes)
        timePicker = findViewById(R.id.timePicker)
        countdown = findViewById(R.id.countdown)
        stateLabel = findViewById(R.id.stateLabel)
        resetAt = findViewById(R.id.resetAt)
        headerSub = findViewById(R.id.headerSub)
        progress = findViewById(R.id.progress)
        rowChips = findViewById(R.id.rowChips)
        rowPickers = findViewById(R.id.rowPickers)
        rowClock = findViewById(R.id.rowClock)
        cardManual = findViewById(R.id.cardManual)
        panelAuto = findViewById(R.id.panelAuto)
        modeHint = findViewById(R.id.modeHint)
        tabManual = findViewById(R.id.tabManual)
        tabAuto = findViewById(R.id.tabAuto)
        modeDuration = findViewById(R.id.modeDuration)
        modeClock = findViewById(R.id.modeClock)
        accountState = findViewById(R.id.accountState)
        barSession = findViewById(R.id.barSession)
        barWeekly = findViewById(R.id.barWeekly)
        lblSession = findViewById(R.id.lblSession)
        lblWeekly = findViewById(R.id.lblWeekly)
        lastSync = findViewById(R.id.lastSync)
        btnLogin = findViewById(R.id.btnLogin)
        btnStart = findViewById(R.id.btnStart)
        cardAlerts = findViewById(R.id.cardAlerts)
        cardActivity = findViewById(R.id.cardActivity)
        historyBox = findViewById(R.id.historyBox)
        activitySub = findViewById(R.id.activitySub)
        liveDot = findViewById(R.id.liveDot)
        lastDelta = findViewById(R.id.lastDelta)
        lastDeltaWhen = findViewById(R.id.lastDeltaWhen)
        statMsgs = findViewById(R.id.statMsgs)
        statTotal = findViewById(R.id.statTotal)
        btnFast = findViewById(R.id.btnFast)
    }

    // ---------- Modo manual / automático ----------
    private fun setAuto(auto: Boolean) {
        Store.setAuto(this, auto)
        tabManual.setBackgroundResource(if (auto) R.drawable.bg_chip else R.drawable.bg_chip_on)
        tabManual.setTextColor(color(if (auto) R.color.text_secondary else R.color.on_coral))
        tabAuto.setBackgroundResource(if (auto) R.drawable.bg_chip_on else R.drawable.bg_chip)
        tabAuto.setTextColor(color(if (auto) R.color.on_coral else R.color.text_secondary))

        panelAuto.visibility = if (auto) View.VISIBLE else View.GONE
        cardManual.visibility = if (auto) View.GONE else View.VISIBLE
        btnStart.visibility = if (auto) View.GONE else View.VISIBLE
        cardAlerts.visibility = if (auto) View.VISIBLE else View.GONE
        cardActivity.visibility = if (auto) View.VISIBLE else View.GONE
        headerSub.text = if (auto) "Modo automático" else "Modo manual"

        if (auto && Store.loggedIn(this)) { UsageSync.schedule(this); UsageSync.syncNow(this) }
        else UsageSync.cancel(this)
        renderAuto()
    }

    // ---------- Recordatorios por umbral ----------
    private fun chipId(prefix: String, v: Int): Int =
        resources.getIdentifier("$prefix$v", "id", packageName)

    private fun setupThresholdChips() {
        for (v in Alerts.OPTIONS) {
            findViewById<TextView>(chipId("ts", v))?.setOnClickListener {
                Alerts.toggle(this, false, v); renderThresholds()
            }
            findViewById<TextView>(chipId("tw", v))?.setOnClickListener {
                Alerts.toggle(this, true, v); renderThresholds()
            }
        }
        renderThresholds()
    }

    private fun renderThresholds() {
        val sess = Alerts.thresholds(this, false)
        val week = Alerts.thresholds(this, true)
        for (v in Alerts.OPTIONS) {
            paintChip(findViewById(chipId("ts", v)), sess.contains(v))
            paintChip(findViewById(chipId("tw", v)), week.contains(v))
        }
    }

    private fun paintChip(t: TextView?, on: Boolean) {
        if (t == null) return
        t.setBackgroundResource(if (on) R.drawable.bg_chip_on else R.drawable.bg_chip)
        t.setTextColor(color(if (on) R.color.on_coral else R.color.text_secondary))
    }

    private fun renderAuto() {
        val p = Store.prefs(this)
        val logged = Store.loggedIn(this)
        accountState.text = if (logged) "Cuenta conectada" else "Sin cuenta conectada"
        btnLogin.text = if (logged) "Cerrar sesión" else "Iniciar sesión"

        val sp = p.getInt(Store.KEY_LAST_SESSION_PCT, -1)
        val wp = p.getInt(Store.KEY_LAST_WEEKLY_PCT, -1)
        barSession.progress = if (sp >= 0) sp else 0
        barWeekly.progress = if (wp >= 0) wp else 0
        lblSession.text = if (sp >= 0) "$sp% usado" else "—"
        lblWeekly.text = if (wp >= 0) "$wp% usado" else "—"

        val t = p.getLong(Store.KEY_LAST_SYNC, 0L)
        lastSync.text = if (t > 0)
            "actualizado a las " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(t))
        else ""
    }

    private fun renderActivity() {
        val fast = UsageSync.isFast(this)
        btnFast.text = if (fast) "Desactivar seguimiento en vivo" else "Activar seguimiento en vivo (30 s)"

        val log = UsageLog.load(this)
        val last = log.firstOrNull()
        val recent = last != null && System.currentTimeMillis() - last.at < 120_000

        liveDot.text = when {
            recent -> "Consumo reciente"
            fast -> "Vigilando"
            else -> "En reposo"
        }

        if (last != null) {
            lastDelta.text = UsageLog.fmtDelta(last.deltaTenths)
            lastDeltaWhen.text = "detectado " + UsageLog.ago(last.at) + " · quedaste en el " + last.pctAfter + "%"
            activitySub.text = "Se registra al detectar subidas de tu uso"
        } else {
            lastDelta.text = "—"
            lastDeltaWhen.text = "sin consumo detectado aún"
            activitySub.text = if (fast) "Vigilando cada 30 s" else "Comprobando cada 5 min"
        }

        val startT = Store.prefs(this).getLong(TimerService.KEY_START, 0L)
        statMsgs.text = (if (startT > 0) UsageLog.countSince(this, startT) else 0).toString()
        val tot = UsageLog.sessionTotalTenths(this)
        statTotal.text = (tot / 10).toString() + "," + (tot % 10) + "%"

        historyBox.removeAllViews()
        for (e in log.take(6)) {
            val row = TextView(this)
            row.text = UsageLog.fmtDelta(e.deltaTenths) + "   ·   " + UsageLog.ago(e.at) +
                    "   ·   " + e.pctAfter + "% usado"
            row.textSize = 12f
            row.setTextColor(color(R.color.text_secondary))
            row.setPadding(0, 8, 0, 8)
            historyBox.addView(row)
        }
        if (log.isEmpty()) {
            val row = TextView(this)
            row.text = "Todavía no hay registros."
            row.textSize = 12f
            row.setTextColor(color(R.color.text_tertiary))
            row.setPadding(0, 8, 0, 8)
            historyBox.addView(row)
        }
    }

    private fun color(id: Int) = resources.getColor(id, theme)

    // ---------- Duración / hora exacta ----------
    private fun setClockMode(clock: Boolean) {
        clockMode = clock
        Store.prefs(this).edit().putBoolean(TimerService.KEY_CLOCK_MODE, clock).apply()
        modeDuration.setBackgroundResource(if (clock) R.drawable.bg_chip else R.drawable.bg_chip_on)
        modeDuration.setTextColor(color(if (clock) R.color.text_secondary else R.color.on_coral))
        modeClock.setBackgroundResource(if (clock) R.drawable.bg_chip_on else R.drawable.bg_chip)
        modeClock.setTextColor(color(if (clock) R.color.on_coral else R.color.text_secondary))
        rowChips.visibility = if (clock) View.GONE else View.VISIBLE
        rowPickers.visibility = if (clock) View.GONE else View.VISIBLE
        rowClock.visibility = if (clock) View.VISIBLE else View.GONE
        modeHint.text = if (clock)
            "Elige la hora exacta a la que se restablece tu límite"
        else "Elige cuánto quieres que dure la cuenta atrás"
    }

    private fun setDur(h: Int, m: Int) { pickHours.value = h; pickMinutes.value = m }

    private fun start() {
        val mins = if (clockMode) minutesUntilClock() else pickHours.value * 60 + pickMinutes.value
        if (mins <= 0) return
        Store.prefs(this).edit().putInt(TimerService.KEY_DUR_MIN, mins).apply()
        launch(Intent(this, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_DUR_MIN, mins)
        })
    }

    private fun minutesUntilClock(): Int {
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance()
        target.set(java.util.Calendar.HOUR_OF_DAY, timePicker.hour)
        target.set(java.util.Calendar.MINUTE, timePicker.minute)
        target.set(java.util.Calendar.SECOND, 0)
        target.set(java.util.Calendar.MILLISECOND, 0)
        if (!target.after(now)) target.add(java.util.Calendar.DAY_OF_YEAR, 1)
        return ((target.timeInMillis - now.timeInMillis) / 60000L).toInt().coerceAtLeast(1)
    }

    private fun send(action: String) {
        launch(Intent(this, TimerService::class.java).apply { this.action = action })
    }

    private fun launch(i: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
    }

    // ---------- Cuenta atrás en vivo ----------
    private fun refresh() {
        val p = Store.prefs(this)
        val end = p.getLong(TimerService.KEY_END, 0L)
        val startT = p.getLong(TimerService.KEY_START, 0L)
        val now = System.currentTimeMillis()
        if (end > now && startT > 0) {
            val remaining = end - now
            val total = (end - startT).coerceAtLeast(1L)
            stateLabel.text = "Sesión activa"
            countdown.text = fmt(remaining)
            resetAt.text = "se restablece a las " +
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(end))
            progress.progress = (1000 - remaining * 1000 / total).toInt().coerceIn(0, 1000)
        } else {
            stateLabel.text = "Sin sesión activa"
            countdown.text = "--:--:--"
            resetAt.text = if (Store.isAuto(this)) "esperando datos de tu cuenta" else "pulsa comenzar para iniciar"
            progress.progress = 0
        }
        UsageWidget.updateAll(this)
        tickCount++
        if (tickCount % 5 == 0) renderActivity()
    }

    private var tickCount = 0

    private fun fmt(ms: Long): String {
        val s = ms / 1000
        return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60)
    }

    override fun onResume() {
        super.onResume()
        handler.post(ticker)
        renderAuto()
        renderActivity()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
    }
}
