package com.xito.claudetimer

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

class UsageWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        for (id in ids) render(context, mgr, id)
        scheduleAutoRefresh(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleAutoRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        cancelAutoRefresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH -> {
                if (Store.isAuto(context) && Store.loggedIn(context)) UsageSync.syncNow(context)
                else updateAll(context)
            }
            ACTION_AUTO_TICK -> {
                updateAll(context)                 // repinta la cuenta atrás
                scheduleAutoRefresh(context)       // reprograma el siguiente tick
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.xito.claudetimer.WIDGET_REFRESH"
        const val ACTION_AUTO_TICK = "com.xito.claudetimer.WIDGET_TICK"
        private const val TICK = 60_000L   // repinta cada minuto

        private fun tickPending(ctx: Context): PendingIntent {
            val i = Intent(ctx, UsageWidget::class.java).apply { action = ACTION_AUTO_TICK }
            return PendingIntent.getBroadcast(ctx, 777, i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        fun scheduleAutoRefresh(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            if (mgr.getAppWidgetIds(ComponentName(ctx, UsageWidget::class.java)).isEmpty()) return
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.set(AlarmManager.RTC, System.currentTimeMillis() + TICK, tickPending(ctx))
        }

        fun cancelAutoRefresh(ctx: Context) {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            am.cancel(tickPending(ctx))
        }

        fun updateAll(ctx: Context) {
            val mgr = AppWidgetManager.getInstance(ctx)
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, UsageWidget::class.java))
            for (id in ids) render(ctx, mgr, id)
        }

        fun render(ctx: Context, mgr: AppWidgetManager, id: Int) {
            val v = RemoteViews(ctx.packageName, R.layout.widget_usage)
            val p = Store.prefs(ctx)
            val end = p.getLong(TimerService.KEY_END, 0L)
            val now = System.currentTimeMillis()
            val auto = Store.isAuto(ctx) && Store.loggedIn(ctx)

            // ---- Cuenta atrás ----
            if (end > now) {
                val rem = end - now
                val s = rem / 1000
                v.setTextViewText(R.id.wTime, String.format("%d:%02d", s / 3600, (s % 3600) / 60))
                v.setTextViewText(R.id.wLabel,
                    "se restablece a las " + java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(end)))
                val start = p.getLong(TimerService.KEY_START, 0L)
                val total = (end - start).coerceAtLeast(1L)
                v.setProgressBar(R.id.wBar, 1000, (1000 - rem * 1000 / total).toInt().coerceIn(0, 1000), false)
            } else {
                v.setTextViewText(R.id.wTime, "--:--")
                v.setTextViewText(R.id.wLabel, if (auto) "sin datos todavía" else "sin sesión activa")
                v.setProgressBar(R.id.wBar, 1000, 0, false)
            }

            // ---- Uso real ----
            val sp = p.getInt(Store.KEY_LAST_SESSION_PCT, -1)
            val wp = p.getInt(Store.KEY_LAST_WEEKLY_PCT, -1)
            if (auto && sp >= 0) {
                v.setViewVisibility(R.id.wUsageRow, android.view.View.VISIBLE)
                v.setTextViewText(R.id.wPct, "$sp%")
                v.setTextViewText(R.id.wWeekly, if (wp >= 0) "$wp%" else "—")
                val last = UsageLog.load(ctx).firstOrNull()
                v.setTextViewText(R.id.wLast, if (last != null) UsageLog.fmtDelta(last.deltaTenths) else "—")
                v.setTextViewText(R.id.wState,
                    if (UsageSync.isFast(ctx)) "en vivo · cada 30 s" else "automático · cada 2 min")
            } else {
                v.setViewVisibility(R.id.wUsageRow, android.view.View.GONE)
                v.setTextViewText(R.id.wState, "modo manual")
            }

            val t = p.getLong(Store.KEY_LAST_SYNC, 0L)
            v.setTextViewText(R.id.wSync, if (auto && t > 0)
                "actualizado " + UsageLog.ago(t) else "")

            // ---- Acciones ----
            v.setOnClickPendingIntent(R.id.wRoot, PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            v.setOnClickPendingIntent(R.id.wRefresh, PendingIntent.getBroadcast(
                ctx, 1, Intent(ctx, UsageWidget::class.java).apply { action = ACTION_REFRESH },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

            mgr.updateAppWidget(id, v)
        }
    }
}
