package com.xito.claudetimer

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente mínimo del endpoint interno de claude.ai.
 * NO es una API oficial: puede cambiar sin previo aviso.
 */
object ClaudeApi {

    private const val UA =
        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125 Mobile Safari/537.36"

    data class Usage(
        val sessionPct: Int,        // 0..100 usado de la sesión de 5 h
        val weeklyPct: Int,         // 0..100 usado semanal (-1 si no hay dato)
        val resetsAt: Long          // epoch millis del reinicio (0 si no hay dato)
    )

    private fun get(url: String, sessionKey: String): String? {
        return try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.requestMethod = "GET"
            c.connectTimeout = 15000
            c.readTimeout = 15000
            c.setRequestProperty("Cookie", "sessionKey=$sessionKey")
            c.setRequestProperty("User-Agent", UA)
            c.setRequestProperty("Accept", "application/json")
            c.setRequestProperty("Referer", "https://claude.ai/")
            if (c.responseCode !in 200..299) { c.disconnect(); return null }
            val body = c.inputStream.bufferedReader().use { it.readText() }
            c.disconnect()
            body
        } catch (e: Exception) { null }
    }

    fun fetchOrgId(sessionKey: String): String? {
        val body = get("https://claude.ai/api/organizations", sessionKey) ?: return null
        return try {
            val arr = JSONArray(body)
            var best: String? = null
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id = o.optString("uuid", o.optString("id", ""))
                if (id.isNotEmpty() && best == null) best = id
            }
            best
        } catch (e: Exception) { null }
    }

    fun fetchUsage(sessionKey: String, orgId: String): Usage? {
        val body = get("https://claude.ai/api/organizations/$orgId/usage", sessionKey) ?: return null
        return try {
            val root = JSONObject(body)
            val session = findNode(root, listOf("five_hour", "fiveHour", "session"))
            val weekly = findNode(root, listOf("seven_day", "sevenDay", "weekly", "week"))
            Usage(
                sessionPct = pctOf(session ?: root),
                weeklyPct = weekly?.let { pctOf(it) } ?: -1,
                resetsAt = resetOf(session ?: root)
            )
        } catch (e: Exception) { null }
    }

    /** Busca en profundidad el primer objeto cuya clave contenga alguno de los nombres. */
    private fun findNode(root: JSONObject, names: List<String>): JSONObject? {
        val keys = root.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = root.opt(k)
            if (v is JSONObject) {
                if (names.any { k.contains(it, true) }) return v
                val deep = findNode(v, names)
                if (deep != null) return deep
            }
        }
        return null
    }

    /** Extrae un porcentaje usado (0..100) de las claves habituales. */
    private fun pctOf(o: JSONObject): Int {
        for (k in listOf("utilization", "used_percent", "percent_used", "percentage")) {
            if (o.has(k)) {
                val d = o.optDouble(k, -1.0)
                if (d >= 0) return (if (d <= 1.0) d * 100 else d).toInt().coerceIn(0, 100)
            }
        }
        val used = firstNum(o, listOf("used", "utilized", "consumed"))
        val limit = firstNum(o, listOf("limit", "total", "cap", "allowance"))
        if (used != null && limit != null && limit > 0) {
            return ((used / limit) * 100).toInt().coerceIn(0, 100)
        }
        val remaining = firstNum(o, listOf("remaining", "left"))
        if (remaining != null && limit != null && limit > 0) {
            return (100 - (remaining / limit) * 100).toInt().coerceIn(0, 100)
        }
        return -1
    }

    private fun firstNum(o: JSONObject, names: List<String>): Double? {
        val keys = o.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            if (names.any { k.contains(it, true) }) {
                val d = o.optDouble(k, Double.NaN)
                if (!d.isNaN()) return d
            }
        }
        return null
    }

    /** Extrae el instante de reinicio (ISO-8601 o epoch). */
    private fun resetOf(o: JSONObject): Long {
        val keys = o.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            if (k.contains("reset", true) || k.contains("expires", true)) {
                val v = o.opt(k)
                if (v is Number) {
                    val n = v.toLong()
                    return if (n < 9_999_999_999L) n * 1000 else n
                }
                if (v is String) parseIso(v)?.let { return it }
            }
            val v = o.opt(k)
            if (v is JSONObject) { val deep = resetOf(v); if (deep > 0) return deep }
        }
        return 0L
    }

    private fun parseIso(s: String): Long? {
        val fmts = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        )
        for (f in fmts) {
            try {
                val sdf = java.text.SimpleDateFormat(f, java.util.Locale.US)
                if (f.endsWith("'Z'")) sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                return sdf.parse(s)?.time
            } catch (e: Exception) { }
        }
        return null
    }
}
