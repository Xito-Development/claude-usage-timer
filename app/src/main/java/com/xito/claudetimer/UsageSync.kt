package com.xito.claudetimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

object UsageSync {

    const val ACTION_TICK = "com.xito.claudetimer.SYNC_TICK"
    private const val INTERVAL = 5 * 60 * 1000L        // ritmo normal
    private const val INTERVAL_FAST = 30 * 1000L       // seguimiento en vivo

    fun syncNow(ctx: Context, onDone: (() -> Unit)? = null) {
        val key = Store.sessionKey(ctx)
        val org = Store.orgId(ctx)
        if (key.isEmpty()) { onDone?.invoke(); return }
        Thread {
            val orgId = if (org.isNotEmpty()) org else ClaudeApi.fetchOrgId(key)?.also { Store.saveOrg(ctx, it) }
            if (orgId != null) {
                val u = ClaudeApi.fetchUsage(key, orgId)
                if (u != null) {
                    val prev = Store.prefs(ctx).getInt(Store.KEY_LAST_SESSION_PCT, -1)
                    Store.saveUsage(ctx, u.sessionPct, u.weeklyPct, u.resetsAt)

                    // Registro de consumo: si el uso ha subido, es un mensaje reciente
                    if (prev >= 0 && u.sessionPct > prev) {
                        UsageLog.record(ctx, (u.sessionPct - prev) * 10, u.sessionPct)
                    }
                    // Si el uso ha bajado mucho, el límite se ha restablecido
                    if (prev >= 0 && u.sessionPct in 0 until prev - 20) {
                        Alerts.rearm(ctx)
                        UsageLog.clear(ctx)
                    }
                    Alerts.check(ctx, u.sessionPct, u.weeklyPct)
                    refreshUi(ctx)
                }
            }
            onDone?.invoke()
        }.start()
    }

    private fun refreshUi(ctx: Context) {
        // Refresca notificación
        val i = Intent(ctx, TimerService::class.java).apply { action = TimerService.ACTION_SHOW }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
            else ctx.startService(i)
        } catch (e: Exception) { }
        // Refresca widgets
        UsageWidget.updateAll(ctx)
    }

    private fun pending(ctx: Context): PendingIntent {
        val i = Intent(ctx, SyncReceiver::class.java).apply { action = ACTION_TICK }
        return PendingIntent.getBroadcast(ctx, 555, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    const val KEY_FAST = "fast_tracking"

    fun isFast(ctx: Context) = Store.prefs(ctx).getBoolean(KEY_FAST, false)

    fun setFast(ctx: Context, fast: Boolean) {
        Store.prefs(ctx).edit().putBoolean(KEY_FAST, fast).apply()
        schedule(ctx)
    }

    fun schedule(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(ctx))
        val step = if (isFast(ctx)) INTERVAL_FAST else INTERVAL
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + step,
            step,
            pending(ctx)
        )
    }

    fun cancel(ctx: Context) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(ctx))
    }
}

class SyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Store.isAuto(context) && Store.loggedIn(context)) UsageSync.syncNow(context)
    }
}
