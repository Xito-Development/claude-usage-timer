package com.xito.claudetimer

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat

/**
 * Recordatorios por umbral de uso: avisa cuando el % consumido cruza
 * los valores elegidos, por separado para la sesión de 5 h y el semanal.
 */
object Alerts {

    const val KEY_SESSION_THRESHOLDS = "th_session"   // "50,80,90"
    const val KEY_WEEKLY_THRESHOLDS = "th_weekly"
    const val KEY_FIRED_PREFIX = "fired_"             // fired_session_80 = true

    val OPTIONS = listOf(50, 75, 80, 90, 95, 100)
    private const val DEFAULT_SESSION = "80,95"
    private const val DEFAULT_WEEKLY = "80,95"

    fun thresholds(c: Context, weekly: Boolean): Set<Int> {
        val key = if (weekly) KEY_WEEKLY_THRESHOLDS else KEY_SESSION_THRESHOLDS
        val def = if (weekly) DEFAULT_WEEKLY else DEFAULT_SESSION
        val raw = Store.prefs(c).getString(key, def) ?: def
        return raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun setThresholds(c: Context, weekly: Boolean, values: Set<Int>) {
        val key = if (weekly) KEY_WEEKLY_THRESHOLDS else KEY_SESSION_THRESHOLDS
        Store.prefs(c).edit().putString(key, values.sorted().joinToString(",")).apply()
    }

    fun toggle(c: Context, weekly: Boolean, value: Int) {
        val cur = thresholds(c, weekly).toMutableSet()
        if (!cur.remove(value)) cur.add(value)
        setThresholds(c, weekly, cur)
    }

    /** Comprueba los umbrales tras una sincronización y lanza los avisos pendientes. */
    fun check(c: Context, sessionPct: Int, weeklyPct: Int) {
        checkOne(c, false, sessionPct, "sesión de 5 h")
        checkOne(c, true, weeklyPct, "límite semanal")
    }

    private fun checkOne(c: Context, weekly: Boolean, pct: Int, label: String) {
        if (pct < 0) return
        val p = Store.prefs(c)
        val tag = if (weekly) "weekly" else "session"
        for (t in thresholds(c, weekly).sorted()) {
            val firedKey = "$KEY_FIRED_PREFIX${tag}_$t"
            val already = p.getBoolean(firedKey, false)
            if (pct >= t && !already) {
                notify(c, t, pct, label, weekly)
                p.edit().putBoolean(firedKey, true).apply()
            } else if (pct < t && already) {
                // el uso se reinició: rearmamos el aviso
                p.edit().putBoolean(firedKey, false).apply()
            }
        }
    }

    /** Rearma todos los avisos (al reiniciarse el límite). */
    fun rearm(c: Context) {
        val e = Store.prefs(c).edit()
        for (tag in listOf("session", "weekly"))
            for (t in OPTIONS) e.putBoolean("$KEY_FIRED_PREFIX${tag}_$t", false)
        e.apply()
    }

    private fun notify(c: Context, threshold: Int, pct: Int, label: String, weekly: Boolean) {
        val open = PendingIntent.getActivity(
            c, 0, android.content.Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = if (threshold >= 100) "Has agotado tu $label"
        else "Has usado el $threshold% de tu $label"
        val text = if (threshold >= 100) "Toca para ver cuándo se restablece."
        else "Vas por el $pct%. Toca para ver el detalle."

        val n = NotificationCompat.Builder(c, TimerService.CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(TimerService.CLAUDE_CORAL)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .build()
        val id = (if (weekly) 3000 else 2000) + threshold
        (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(id, n)
    }
}
