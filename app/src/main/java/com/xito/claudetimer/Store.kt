package com.xito.claudetimer

import android.content.Context

object Store {
    const val PREFS = "claude_timer_prefs"

    const val KEY_SESSION = "session_key"
    const val KEY_ORG = "org_id"
    const val KEY_AUTO = "auto_mode"
    const val KEY_LAST_SESSION_PCT = "last_session_pct"
    const val KEY_LAST_WEEKLY_PCT = "last_weekly_pct"
    const val KEY_LAST_SYNC = "last_sync"

    fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun sessionKey(c: Context): String = prefs(c).getString(KEY_SESSION, "") ?: ""
    fun orgId(c: Context): String = prefs(c).getString(KEY_ORG, "") ?: ""
    fun isAuto(c: Context): Boolean = prefs(c).getBoolean(KEY_AUTO, false)
    fun loggedIn(c: Context): Boolean = sessionKey(c).isNotEmpty()

    fun saveSession(c: Context, key: String) {
        prefs(c).edit().putString(KEY_SESSION, key).apply()
    }

    fun saveOrg(c: Context, org: String) {
        prefs(c).edit().putString(KEY_ORG, org).apply()
    }

    fun setAuto(c: Context, auto: Boolean) {
        prefs(c).edit().putBoolean(KEY_AUTO, auto).apply()
    }

    fun logout(c: Context) {
        prefs(c).edit()
            .remove(KEY_SESSION).remove(KEY_ORG)
            .remove(KEY_LAST_SESSION_PCT).remove(KEY_LAST_WEEKLY_PCT).remove(KEY_LAST_SYNC)
            .putBoolean(KEY_AUTO, false)
            .apply()
    }

    fun saveUsage(c: Context, sessionPct: Int, weeklyPct: Int, resetsAt: Long) {
        val e = prefs(c).edit()
            .putInt(KEY_LAST_SESSION_PCT, sessionPct)
            .putInt(KEY_LAST_WEEKLY_PCT, weeklyPct)
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
        if (resetsAt > System.currentTimeMillis()) {
            e.putLong(TimerService.KEY_END, resetsAt)
            if (prefs(c).getLong(TimerService.KEY_START, 0L) <= 0L ||
                prefs(c).getLong(TimerService.KEY_END, 0L) != resetsAt) {
                e.putLong(TimerService.KEY_START, System.currentTimeMillis())
            }
        }
        e.apply()
    }
}
