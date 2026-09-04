package com.xito.claudetimer

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Historial de consumo: cada vez que el % de uso sube entre dos consultas,
 * se registra un "evento de consumo" (equivalente aproximado a un mensaje).
 */
object UsageLog {

    private const val KEY_LOG = "usage_log"
    private const val KEY_LAST_EVENT = "last_event_at"
    private const val MAX_ENTRIES = 60

    data class Entry(val at: Long, val deltaTenths: Int, val pctAfter: Int)

    /** Registra una subida de uso. delta viene en décimas de punto (8 = 0,8%). */
    fun record(c: Context, deltaTenths: Int, pctAfter: Int) {
        if (deltaTenths <= 0) return
        val list = load(c).toMutableList()
        list.add(0, Entry(System.currentTimeMillis(), deltaTenths, pctAfter))
        while (list.size > MAX_ENTRIES) list.removeAt(list.size - 1)
        save(c, list)
        Store.prefs(c).edit().putLong(KEY_LAST_EVENT, System.currentTimeMillis()).apply()
    }

    fun lastEventAt(c: Context): Long = Store.prefs(c).getLong(KEY_LAST_EVENT, 0L)

    fun load(c: Context): List<Entry> {
        val raw = Store.prefs(c).getString(KEY_LOG, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Entry(o.getLong("t"), o.getInt("d"), o.getInt("p"))
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun save(c: Context, list: List<Entry>) {
        val arr = JSONArray()
        for (e in list) {
            arr.put(JSONObject().put("t", e.at).put("d", e.deltaTenths).put("p", e.pctAfter))
        }
        Store.prefs(c).edit().putString(KEY_LOG, arr.toString()).apply()
    }

    fun clear(c: Context) {
        Store.prefs(c).edit().remove(KEY_LOG).remove(KEY_LAST_EVENT).apply()
    }

    /** Consumo total registrado en la sesión actual, en décimas de punto. */
    fun sessionTotalTenths(c: Context): Int {
        val start = Store.prefs(c).getLong(TimerService.KEY_START, 0L)
        if (start <= 0) return 0
        return load(c).filter { it.at >= start }.sumOf { it.deltaTenths }
    }

    fun countSince(c: Context, since: Long): Int = load(c).count { it.at >= since }

    fun fmtDelta(tenths: Int): String {
        val whole = tenths / 10
        val dec = tenths % 10
        return "+$whole,$dec%"
    }

    fun ago(at: Long): String {
        val s = (System.currentTimeMillis() - at) / 1000
        return when {
            s < 60 -> "hace ${s}s"
            s < 3600 -> "hace ${s / 60} min"
            else -> "hace ${s / 3600} h"
        }
    }
}
