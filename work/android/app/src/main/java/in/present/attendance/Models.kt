package `in`.present.attendance

import kotlin.math.ceil
import kotlin.math.floor
import java.time.LocalDate
import java.util.UUID

data class Profile(val name: String = "", val college: String = "", val department: String = "", val programme: String = "B.Tech", val year: String = "", val semester: String = "1")
data class Subject(val id: String = UUID.randomUUID().toString(), val name: String, val attended: Int = 0, val held: Int = 0)
data class Period(val id: String = UUID.randomUUID().toString(), val subject: String, val time: String, val weekday: Int = 1, val start: String = LocalDate.now().toString(), val once: String = "")
data class Entry(val key: String, val subject: String, val date: String, val status: String)
data class AppData(val profile: Profile? = null, val subjects: List<Subject> = emptyList(), val periods: List<Period> = emptyList(), val entries: List<Entry> = emptyList(), val target: Int = 75)
data class Totals(val attended: Int, val held: Int) { val percent: Double? get() = if (held == 0) null else attended * 100.0 / held }
fun totals(data: AppData, id: String? = null): Totals {
    val s = data.subjects.filter { id == null || it.id == id }
    val e = data.entries.filter { id == null || it.subject == id }
    return Totals(s.sumOf { it.attended } + e.count { it.status == "present" }, s.sumOf { it.held } + e.count { it.status in listOf("present", "absent") })
}
fun forecast(t: Totals, target: Int): Pair<Int, Boolean> {
    require(target in 1..99)
    val ratio = target / 100.0
    val below = t.held > 0 && t.attended.toDouble() / t.held < ratio
    return if (below) ceil((ratio * t.held - t.attended) / (1 - ratio) - 1e-9).toInt().coerceAtLeast(0) to true
    else floor(t.attended / ratio - t.held + 1e-9).toInt().coerceAtLeast(0) to false
}
fun periodsOn(data: AppData, date: LocalDate) = data.periods.filter { if (it.once.isNotEmpty()) it.once == date.toString() else it.weekday == date.dayOfWeek.value && it.start <= date.toString() }.sortedBy { it.time }
fun mark(data: AppData, p: Period, date: LocalDate, status: String): AppData {
    require(status in listOf("present", "absent", "cancelled"))
    val key = "$date/${p.id}"
    val previous = data.entries.find { it.key == key }
    val rest = data.entries.filterNot { it.key == key }
    return data.copy(entries = if (previous?.status == status) rest else rest + Entry(key, p.subject, date.toString(), status))
}
