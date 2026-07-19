package com.xito.claudetimer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class TimerService : Service() {

    companion object {
        const val CHANNEL_MAIN = "claude_timer_main"
        const val CHANNEL_ALERT = "claude_timer_alert"
        const val NOTIF_ID = 1001
        const val NOTIF_ALERT_ID = 1002

        const val ACTION_START = "com.xito.claudetimer.START"
        const val ACTION_RESET = "com.xito.claudetimer.RESET"
        const val ACTION_FINISHED = "com.xito.claudetimer.FINISHED"
        const val ACTION_SHOW = "com.xito.claudetimer.SHOW"
        const val ACTION_STOP = "com.xito.claudetimer.STOP"
        const val ACTION_HOUR_TICK = "com.xito.claudetimer.HOUR_TICK"

        const val EXTRA_DUR_MIN = "dur_min"

        const val PREFS = "claude_timer_prefs"
        const val KEY_END = "end_time"
        const val KEY_START = "start_time"
        const val KEY_DUR_MIN = "dur_min"

        const val CLAUDE_CORAL = 0xFFD97757.toInt()

        val ALIASES = listOf(".IconIdle", ".Icon1", ".Icon2", ".Icon3", ".Icon4", ".Icon5")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        when (intent?.action) {
            ACTION_START -> {
                val durMin = intent.getIntExtra(EXTRA_DUR_MIN, prefs.getInt(KEY_DUR_MIN, 300))
                    .coerceIn(1, 12 * 60)
                val now = System.currentTimeMillis()
                val end = now + durMin * 60_000L
                prefs.edit().putInt(KEY_DUR_MIN, durMin).putLong(KEY_START, now).putLong(KEY_END, end).apply()
                cancelAlarms()
                scheduleFinishAlarm(end)
                scheduleHourTicks(end)
                updateIcon(end)
                startForeground(NOTIF_ID, buildRunning(end))
            }
            ACTION_RESET -> {
                clear(prefs); updateIcon(0L); startForeground(NOTIF_ID, buildIdle())
            }
            ACTION_FINISHED -> {
                clear(prefs); updateIcon(0L); fireAlert(); startForeground(NOTIF_ID, buildIdle())
            }
            ACTION_STOP -> {
                clear(prefs); updateIcon(0L)
                stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY
            }
            ACTION_HOUR_TICK -> {
                val end = prefs.getLong(KEY_END, 0L)
                if (end > System.currentTimeMillis()) { updateIcon(end); startForeground(NOTIF_ID, buildRunning(end)) }
                else { updateIcon(0L); startForeground(NOTIF_ID, buildIdle()) }
            }
            else -> {
                val end = prefs.getLong(KEY_END, 0L)
                if (end > System.currentTimeMillis()) {
                    scheduleFinishAlarm(end); scheduleHourTicks(end)
                    updateIcon(end); startForeground(NOTIF_ID, buildRunning(end))
                } else {
                    prefs.edit().remove(KEY_END).apply(); updateIcon(0L); startForeground(NOTIF_ID, buildIdle())
                }
            }
        }
        return START_STICKY
    }

    private fun clear(prefs: android.content.SharedPreferences) {
        prefs.edit().remove(KEY_END).remove(KEY_START).apply()
        cancelAlarms()
    }

    // ---------- Icono dinámico ----------
    private fun updateIcon(end: Long) {
        val remaining = end - System.currentTimeMillis()
        val alias = if (remaining <= 0) ".IconIdle" else {
            val hours = ((remaining + 3_599_999L) / 3_600_000L).toInt().coerceIn(1, 5)
            ".Icon$hours"
        }
        val pm = packageManager
        for (a in ALIASES) {
            val comp = ComponentName(this, "$packageName$a")
            val want = if (a == alias) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            if (pm.getComponentEnabledSetting(comp) != want) {
                pm.setComponentEnabledSetting(comp, want, PackageManager.DONT_KILL_APP)
            }
        }
    }

    // ---------- Notificaciones ----------
    private fun pending(action: String): PendingIntent {
        val i = Intent(this, TimerService::class.java).apply { this.action = action }
        return PendingIntent.getService(this, action.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun openApp(): PendingIntent {
        val i = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(this, 0, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun baseBuilder(): NotificationCompat.Builder =
        NotificationCompat.Builder(this, CHANNEL_MAIN)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(CLAUDE_CORAL)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp())
            .setPriority(NotificationCompat.PRIORITY_LOW)

    private fun buildIdle(): Notification {
        val dur = getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_DUR_MIN, 300)
        val v = RemoteViews(packageName, R.layout.notification_idle)
        v.setTextViewText(R.id.subtitle, "Empezar cuenta atrás de ${durLabel(dur)}")
        v.setOnClickPendingIntent(R.id.btn_start, pending(ACTION_START))
        v.setOnClickPendingIntent(R.id.btn_stop, pending(ACTION_STOP))
        return baseBuilder().setCustomContentView(v).setCustomBigContentView(v).build()
    }

    private fun buildRunning(end: Long): Notification {
        val base = SystemClock.elapsedRealtime() + (end - System.currentTimeMillis())

        val small = RemoteViews(packageName, R.layout.notification_running)
        small.setChronometerCountDown(R.id.chrono, true)
        small.setChronometer(R.id.chrono, base, null, true)

        val big = RemoteViews(packageName, R.layout.notification_running_big)
        big.setChronometerCountDown(R.id.chrono, true)
        big.setChronometer(R.id.chrono, base, null, true)
        big.setOnClickPendingIntent(R.id.btn_reset, pending(ACTION_RESET))
        big.setOnClickPendingIntent(R.id.btn_stop, pending(ACTION_STOP))

        return baseBuilder().setCustomContentView(small).setCustomBigContentView(big).build()
    }

    private fun durLabel(min: Int): String {
        val h = min / 60; val m = min % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}min"
            h > 0 -> "${h}h"
            else -> "${m}min"
        }
    }

    private fun fireAlert() {
        val n = NotificationCompat.Builder(this, CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(CLAUDE_CORAL)
            .setContentTitle("¡Límite restablecido!")
            .setContentText("Ya puedes volver a usar Claude.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp())
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIF_ALERT_ID, n)
    }

    // ---------- Alarmas ----------
    private fun finishPending(): PendingIntent {
        val i = Intent(this, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(this, 99, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun hourPending(idx: Int): PendingIntent {
        val i = Intent(this, HourReceiver::class.java)
        return PendingIntent.getBroadcast(this, 200 + idx, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun scheduleFinishAlarm(end: Long) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        try { am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, finishPending()) }
        catch (e: SecurityException) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, finishPending()) }
    }

    private fun scheduleHourTicks(end: Long) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (h in 1..4) {
            val t = end - h * 3_600_000L
            if (t > System.currentTimeMillis()) {
                try { am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, hourPending(h)) }
                catch (e: SecurityException) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, hourPending(h)) }
            }
        }
    }

    private fun cancelAlarms() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(finishPending())
        for (h in 1..4) am.cancel(hourPending(h))
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val main = NotificationChannel(CHANNEL_MAIN, "Temporizador", NotificationManager.IMPORTANCE_LOW)
            main.setShowBadge(false)
            val alert = NotificationChannel(CHANNEL_ALERT, "Avisos", NotificationManager.IMPORTANCE_HIGH)
            mgr.createNotificationChannel(main)
            mgr.createNotificationChannel(alert)
        }
    }
}
