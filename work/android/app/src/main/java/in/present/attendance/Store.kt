package `in`.present.attendance

import android.content.Context
import android.util.AtomicFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

class Store(context: Context) {
    companion object { private val access = Any() }
    private val file = AtomicFile(File(context.filesDir, "attendance.json"))
    fun load(): AppData = synchronized(access) {
        if (file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()) decode(file.openRead().bufferedReader().use { it.readText() }) else AppData()
    }
    fun save(data: AppData) = synchronized(access) {
        val stream = file.startWrite()
        try { stream.write(encode(data).toByteArray(Charsets.UTF_8)); file.finishWrite(stream) }
        catch (e: Exception) { file.failWrite(stream); throw e }
    }
}
fun encode(d: AppData): String = JSONObject().apply {
    put("schema", 1); put("target", d.target)
    d.profile?.let { p -> put("profile", JSONObject().apply { put("name", p.name); put("college", p.college); put("department", p.department); put("programme", p.programme); put("year", p.year); put("semester", p.semester) }) }
    put("subjects", JSONArray().apply { d.subjects.forEach { s -> put(JSONObject().apply { put("id", s.id); put("name", s.name); put("attended", s.attended); put("held", s.held) }) } })
    put("periods", JSONArray().apply { d.periods.forEach { p -> put(JSONObject().apply { put("id", p.id); put("subject", p.subject); put("time", p.time); put("weekday", p.weekday); put("start", p.start); put("once", p.once) }) } })
    put("entries", JSONArray().apply { d.entries.forEach { e -> put(JSONObject().apply { put("key", e.key); put("subject", e.subject); put("date", e.date); put("status", e.status) }) } })
}.toString(2)
private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
fun decode(raw: String): AppData {
    require(raw.length <= 4_000_000) { "Backup is too large." }
    val o = JSONObject(raw)
    require(o.getInt("schema") == 1) { "Unrecognised backup format." }
    val p = o.optJSONObject("profile")?.let { Profile(it.getString("name"), it.getString("college"), it.getString("department"), it.getString("programme"), it.getString("year"), it.getString("semester")) }
    val d = AppData(p,
        o.getJSONArray("subjects").objects().map { Subject(it.getString("id"), it.getString("name"), it.getInt("attended"), it.getInt("held")) },
        o.getJSONArray("periods").objects().map { Period(it.getString("id"), it.getString("subject"), it.getString("time"), it.getInt("weekday"), it.getString("start"), it.getString("once")) },
        o.getJSONArray("entries").objects().map { Entry(it.getString("key"), it.getString("subject"), it.getString("date"), it.getString("status")) }, o.getInt("target"))
    require(d.target in 50..99 && d.subjects.size <= 100 && d.periods.size <= 2000 && d.entries.size <= 30000)
    require(d.subjects.map { it.id }.distinct().size == d.subjects.size)
    require(d.periods.map { it.id }.distinct().size == d.periods.size)
    require(d.entries.map { it.key }.distinct().size == d.entries.size)
    p?.let { require(it.name.isNotBlank() && it.name.length <= 40 && it.college.isNotBlank() && it.college.length <= 100 && it.department.isNotBlank() && it.department.length <= 80 && it.programme.isNotBlank() && it.programme.length <= 40 && (it.year.toIntOrNull() ?: 0) in 2000..2100 && (it.semester.toIntOrNull() ?: 0) in 1..12) }
    val ids = d.subjects.map { it.id }.toSet()
    d.subjects.forEach { require(it.name.isNotBlank() && it.name.length <= 80 && it.id.length <= 80 && it.attended in 0..10000 && it.held in it.attended..10000) }
    d.periods.forEach { require(it.subject in ids && it.id.length <= 80 && it.weekday in 1..7); LocalTime.parse(it.time); LocalDate.parse(it.start); if (it.once.isNotEmpty()) LocalDate.parse(it.once) }
    d.entries.forEach { require(it.subject in ids && it.status in listOf("present", "absent", "cancelled") && it.key.length <= 120); LocalDate.parse(it.date) }
    return d
}
