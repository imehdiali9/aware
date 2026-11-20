package `in`.present.attendance

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AttendanceTest {
    @Test fun totalsAreWeightedByPeriodsAndExcludeCancelled() {
        val d=AppData(subjects=listOf(Subject("a","Math",6,8),Subject("b","Physics",1,2)),entries=listOf(Entry("x","a","2026-11-02","cancelled"),Entry("y","b","2026-11-02","present")))
        assertEquals(Totals(8,11),totals(d));assertEquals(Totals(6,8),totals(d,"a"))
    }
    @Test fun markingReplacesAndTappingAgainUndoes() {
        val date=LocalDate.of(2026,11,2);val p=Period(id="p",subject="s",time="09:00")
        val present=mark(AppData(),p,date,"present")
        val absent=mark(present,p,date,"absent")
        assertEquals(Totals(0,1),totals(absent));assertEquals(1,absent.entries.size)
        assertTrue(mark(absent,p,date,"absent").entries.isEmpty())
    }
    @Test fun forecastsRespectThresholdBoundaries() {
        assertEquals(2 to true,forecast(Totals(7,10),75))
        assertEquals(2 to false,forecast(Totals(9,10),75))
        assertEquals(0 to false,forecast(Totals(3,4),75))
        assertEquals(0 to false,forecast(Totals(0,0),75))
        assertEquals(100 to true,forecast(Totals(98,100),99))
    }
    @Test fun timetableHonoursStartDateWeekdayAndOneOffDate() {
        val monday=LocalDate.of(2026,11,2)
        val d=AppData(periods=listOf(Period("1","a","10:00",1,"2026-11-02"),Period("2","a","09:00",1,"2026-11-09"),Period("3","a","08:00",2,once="2026-11-02")))
        assertEquals(listOf("3","1"),periodsOn(d,monday).map{it.id})
        assertTrue(periodsOn(d,monday.minusWeeks(1)).isEmpty())
    }
}
