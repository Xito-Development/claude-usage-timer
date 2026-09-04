package com.xito.claudetimer

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
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            if (Store.isAuto(context) && Store.loggedIn(context)) UsageSync.syncNow(context)
            else updateAll(context)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.xito.claudetimer.WIDGET_REFRESH"

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

            if (end > now) {
                val rem = end - now
                val s = rem / 1000
                v.setTextViewText(R.id.wTime, String.format("%d:%02d", s / 3600, (s % 3600) / 60))
                v.setTextViewText(R.id.wLabel, if (auto) "hasta el reinicio" else "restante")
                val start = p.getLong(TimerService.KEY_START, 0L)
                val total = (end - start).coerceAtLeast(1L)
                v.setProgressBar(R.id.wBar, 1000, (1000 - rem * 1000 / total).toInt().coerceIn(0, 1000), false)
            } else {
                v.setTextViewText(R.id.wTime, "--:--")
                v.setTextViewText(R.id.wLabel, if (auto) "sin datos aún" else "sin sesión")
                v.setProgressBar(R.id.wBar, 1000, 0, false)
            }

            val sp = p.getInt(Store.KEY_LAST_SESSION_PCT, -1)
            v.setTextViewText(R.id.wPct, if (auto && sp >= 0) "$sp% usado" else "modo manual")

            val open = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.wRoot, open)

            val refresh = PendingIntent.getBroadcast(
                ctx, 1, Intent(ctx, UsageWidget::class.java).apply { action = ACTION_REFRESH },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.wRefresh, refresh)

            mgr.updateAppWidget(id, v)
        }
    }
}
