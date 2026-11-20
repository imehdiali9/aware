package `in`.present.attendance

import android.app.DatePickerDialog
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun percent(t: Totals) = t.percent?.let{String.format(Locale.ROOT,"%.1f",it)}?:"—"
fun forecastText(t: Totals, target: Int): String {
    if(t.held==0)return "Mark your first class to see your forecast."
    val (n,below)=forecast(t,target)
    return if(below)"Attend the next $n ${if(n==1)"period" else "periods"} to reach $target%." else "You can miss $n ${if(n==1)"period" else "periods"} and stay at $target%."
}
@Composable fun AttendanceTrack(value: Float, target: Int, modifier: Modifier = Modifier, dark: Boolean = false) {
    val progress by animateFloatAsState(value.coerceIn(0f,100f),label="attendance progress")
    Canvas(modifier.fillMaxWidth().height(26.dp).semantics{contentDescription="Attendance ${value.toInt()} percent. Target $target percent."}) {
        val gap=3.dp.toPx();val count=40;val width=(size.width-gap*(count-1))/count
        repeat(count){i->drawRoundRect(if((i+.5f)/count*100<=progress)if(dark)Lime else Ink else if(dark)Color(0xFF39423A)else Line,Offset(i*(width+gap),5.dp.toPx()),Size(width,14.dp.toPx()),cornerRadius=androidx.compose.ui.geometry.CornerRadius(1.dp.toPx()))}
        val x=size.width*target/100;drawLine(if(dark)Paper else Quiet,Offset(x,0f),Offset(x,size.height),1.dp.toPx())
    }
}
@Composable fun DayScreen(data: AppData, update: ((AppData)->AppData)->Unit, goWeek: ()->Unit, goSubjects: ()->Unit) {
    var selected by rememberSaveable { mutableStateOf(LocalDate.now().toString()) };val date=LocalDate.parse(selected);val today=LocalDate.now()
    var add by remember { mutableStateOf(false) };val context=LocalContext.current;val total=totals(data)
    val regular=periodsOn(data,date)
    val historical=data.entries.filter{it.date==selected&&regular.none{p->it.key=="$selected/${p.id}"}}.map{Period(id=it.key.substringAfter('/'),subject=it.subject,time="—",once=selected)}
    val periods=regular+historical
    val done=periods.count{p->data.entries.any{it.key=="$selected/${p.id}"}}
    LazyColumn(Modifier.fillMaxSize().background(Paper),contentPadding=PaddingValues(bottom=24.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Ink).padding(horizontal=24.dp).padding(top=22.dp,bottom=27.dp)) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text("present",color=Paper,fontSize=24.sp,fontWeight=FontWeight.Black,letterSpacing=(-1).sp);Text(" /",color=Lime,fontSize=27.sp,fontWeight=FontWeight.Black);Spacer(Modifier.weight(1f));Micro("SEM ${data.profile!!.semester}",DarkQuiet)}
                Spacer(Modifier.height(29.dp));Micro("${data.profile!!.name}’S ATTENDANCE",DarkQuiet)
                Row(verticalAlignment=Alignment.Bottom){Text(percent(total),color=Paper,fontSize=78.sp,fontWeight=FontWeight.Medium,letterSpacing=(-5).sp,lineHeight=90.sp);Text("%",color=Lime,fontFamily=Mono,fontSize=25.sp,modifier=Modifier.padding(start=6.dp,bottom=17.dp))}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("${total.attended} attended / ${total.held} held",color=DarkQuiet,fontSize=12.sp,fontFamily=Mono);Text("TARGET ${data.target}%",color=Lime,fontSize=10.sp,fontFamily=Mono)}
                Spacer(Modifier.height(17.dp));AttendanceTrack((total.percent?:0.0).toFloat(),data.target,dark=true)
                Spacer(Modifier.height(12.dp));Text(forecastText(total,data.target),color=Paper,fontSize=13.sp,lineHeight=19.sp)
            }
        }
        item {
            Column(Modifier.padding(horizontal=24.dp)) {
                Row(Modifier.fillMaxWidth().padding(top=24.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically){Text(if(date==today)"Today" else date.format(DateTimeFormatter.ofPattern("EEE, d MMM")),fontSize=24.sp,fontWeight=FontWeight.Bold,letterSpacing=(-.7).sp,modifier=Modifier.weight(1f));TextButton({DatePickerDialog(context,{_,y,m,d->selected=LocalDate.of(y,m+1,d).toString()},date.year,date.monthValue-1,date.dayOfMonth).apply{datePicker.maxDate=today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()}.show()}){Text("Choose date",fontSize=12.sp)}}
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    val monday=date.minusDays((date.dayOfWeek.value-1).toLong())
                    repeat(7){i->val day=monday.plusDays(i.toLong());val chosen=day==date
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if(chosen)Ink else Color.Transparent).clickable(enabled=day<=today){selected=day.toString()}.padding(vertical=12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)){
                            Text(day.dayOfWeek.name.take(1),color=if(chosen)DarkQuiet else Quiet.copy(alpha=if(day>today).4f else 1f),fontSize=10.sp,fontFamily=Mono)
                            Text(day.dayOfMonth.toString(),color=if(chosen)Lime else Ink.copy(alpha=if(day>today).3f else 1f),fontSize=15.sp,fontWeight=FontWeight.SemiBold)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top=25.dp,bottom=10.dp),horizontalArrangement=Arrangement.SpaceBetween){Micro("CLASS REGISTER");Micro("$done / ${periods.size} MARKED")}
            }
        }
        if(periods.isEmpty())item{Box(Modifier.padding(horizontal=24.dp)){EmptyState("—","Nothing on the timetable.","Add your weekly periods or record an extra class for this day.","Set up my week",goWeek)}}
        items(periods,key={it.id}){p->
            val e=data.entries.find{it.key=="$selected/${p.id}"};val effective=p.copy(subject=e?.subject?:p.subject)
            ClassRow(data,effective,e?.status){status->update{mark(it,effective,date,status)}}
        }
        item {
            Column(Modifier.padding(horizontal=24.dp)) {
                Spacer(Modifier.height(12.dp));TextButton({if(data.subjects.isEmpty())goSubjects()else add=true},contentPadding=PaddingValues(0.dp)){Text("+  Add an extra class",fontWeight=FontWeight.Bold)}
                Note("Tap a selected status to undo. Unmarked and cancelled periods are excluded from the percentage.")
            }
        }
    }
    if(add)PeriodEditor(data,null,false,date.dayOfWeek.value,date,{add=false},{p->update{it.copy(periods=it.periods+p)};add=false})
}
@Composable fun ClassRow(data: AppData, period: Period, status: String?, mark: (String)->Unit) {
    val s=data.subjects.find{it.id==period.subject};val i=data.subjects.indexOfFirst{it.id==period.subject}.coerceAtLeast(0)
    Column(Modifier.padding(horizontal=24.dp)) {
        HorizontalDivider(color=Line)
        Row(Modifier.padding(top=21.dp,bottom=16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            SubjectTag(i)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(s?.name?:"Subject",fontWeight=FontWeight.SemiBold,fontSize=16.sp);Text("${period.time}  /  1 period",color=Quiet,fontFamily=Mono,fontSize=11.sp)}
            if(status!=null)Text(if(status=="present")"✓" else if(status=="absent")"−" else "·",color=if(status=="absent")Red else Quiet,fontSize=21.sp)
        }
        Row(Modifier.fillMaxWidth().padding(bottom=20.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)) {
            listOf("present" to "Present","absent" to "Absent","cancelled" to "Cancelled").forEach{(key,label)->
                val chosen=status==key;val bg by animateColorAsState(if(chosen)when(key){"present"->Lime;"absent"->Color(0xFFF3DCD4);else->Line}else Color(0xFFEAEEE5),label="attendance state")
                FilterChip(chosen,{mark(key)},label={Text(label,fontSize=12.sp,fontWeight=if(chosen)FontWeight.Bold else FontWeight.Normal)},modifier=Modifier.weight(1f).heightIn(min=48.dp),shape=RoundedCornerShape(12.dp),border=null,colors=FilterChipDefaults.filterChipColors(containerColor=bg,selectedContainerColor=bg,labelColor=Quiet,selectedLabelColor=if(key=="absent")Red else Ink))
            }
        }
    }
}
@Composable fun SubjectsScreen(data: AppData, update: ((AppData)->AppData)->Unit) {
    var editing by remember { mutableStateOf<Subject?>(null) };var editor by remember{mutableStateOf(false)}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=24.dp,vertical=8.dp)) {
        item{PageHeading("SEMESTER ${data.profile!!.semester}","Subjects."){AddButton("Add"){editing=null;editor=true}}}
        if(data.subjects.isEmpty())item{EmptyState("01","Start with a subject.","Your subject totals and forecasts will live here.","Add a subject"){editor=true}}
        items(data.subjects,key={it.id}){s->val t=totals(data,s.id);val i=data.subjects.indexOf(s)
            Column(Modifier.fillMaxWidth().padding(bottom=24.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFFE9EDE3)).clickable{editing=s;editor=true}.padding(20.dp)){
                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){SubjectTag(i);Text(s.name,Modifier.weight(1f),fontSize=17.sp,fontWeight=FontWeight.Bold);Text("↗",fontSize=23.sp,color=Quiet)}
                Row(Modifier.fillMaxWidth().padding(top=23.dp,bottom=8.dp),verticalAlignment=Alignment.Bottom){Text(percent(t),fontSize=43.sp,letterSpacing=(-2).sp,fontWeight=FontWeight.Medium);Text("%",fontSize=20.sp,color=Quiet,modifier=Modifier.padding(start=4.dp,bottom=5.dp));Spacer(Modifier.weight(1f));Text("${t.attended} / ${t.held}",fontFamily=Mono,fontSize=12.sp,color=Quiet,modifier=Modifier.padding(bottom=8.dp))}
                AttendanceTrack((t.percent?:0.0).toFloat(),data.target)
                Text(forecastText(t,data.target),modifier=Modifier.padding(top=12.dp),fontSize=12.sp,lineHeight=18.sp,color=if((t.percent?:100.0)<data.target)Red else Quiet)
            }
        }
        item{Note("Tap a subject to rename it or add starting counts. Overall attendance is weighted by periods, not by subject percentages.")}
    }
    if(editor)SubjectEditor(editing,data.subjects,{editor=false}){s->update{d->d.copy(subjects=if(editing==null)d.subjects+s else d.subjects.map{if(it.id==s.id)s else it})};editor=false}
}
@Composable fun WeekScreen(data: AppData, update: ((AppData)->AppData)->Unit, goSubjects: ()->Unit) {
    var day by rememberSaveable { mutableIntStateOf(LocalDate.now().dayOfWeek.value) };var editor by remember{mutableStateOf(false)};var editing by remember{mutableStateOf<Period?>(null)}
    val periods=data.periods.filter{it.once.isEmpty()&&it.weekday==day}.sortedBy{it.time}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=24.dp,vertical=8.dp)) {
        item{PageHeading("YOUR REPEATING SCHEDULE","The week."){AddButton("Period"){if(data.subjects.isEmpty())goSubjects()else{editing=null;editor=true}}}}
        item{
            Row(Modifier.fillMaxWidth().padding(bottom=24.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)){(1..7).forEach{d->val chosen=day==d
                Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if(chosen)Ink else Color(0xFFE9EDE3)).clickable{day=d}.padding(vertical=17.dp),contentAlignment=Alignment.Center){Text(java.time.DayOfWeek.of(d).name.take(2),color=if(chosen)Lime else Quiet,fontFamily=Mono,fontSize=11.sp,fontWeight=FontWeight.Bold)}
            }}}
        item{Row(Modifier.fillMaxWidth().padding(bottom=18.dp),horizontalArrangement=Arrangement.SpaceBetween){Micro(java.time.DayOfWeek.of(day).name);Micro("${periods.size} PERIODS")}}
        if(periods.isEmpty())item{EmptyState("—","An open ${java.time.DayOfWeek.of(day).name.lowercase().replaceFirstChar{it.uppercase()}}.","Add the periods you attend each week. They’ll appear automatically in your daily register.","Add a period"){if(data.subjects.isEmpty())goSubjects()else{editing=null;editor=true}}}
        items(periods,key={it.id}){p->
            HorizontalDivider(color=Line)
            Row(Modifier.fillMaxWidth().clickable{editing=p;editor=true}.padding(vertical=23.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)){
                Text(p.time,fontFamily=Mono,fontSize=15.sp)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(data.subjects.find{it.id==p.subject}?.name?:"Subject",fontWeight=FontWeight.SemiBold,fontSize=16.sp);Text("1 period · from ${p.start}",fontSize=11.sp,color=Quiet)}
                Text("↗",color=Quiet,fontSize=22.sp)
            }
        }
        item{Note("One entry = one attendance period. Tap a period to edit its subject, time or start date.")}
    }
    if(editor)PeriodEditor(data,editing,true,day,LocalDate.now(),{editor=false},{p->update{d->d.copy(periods=if(editing==null)d.periods+p else d.periods.map{if(it.id==p.id)p else it})};editor=false},{p->update{d->d.copy(periods=d.periods.filterNot{it.id==p.id})}})
}
@Composable fun YouScreen(data: AppData, update: ((AppData)->AppData)->Unit, edit: ()->Unit, export: ()->Unit, restore: ()->Unit) {
    val p=data.profile!!;var targetEditor by remember{mutableStateOf(false)};var target by rememberSaveable(data.target){mutableStateOf(data.target.toString())}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=24.dp,vertical=8.dp)) {
        item{PageHeading("PERSONAL, BY DESIGN","Your space.")}
        item{
            Row(Modifier.fillMaxWidth().padding(bottom=28.dp),horizontalArrangement=Arrangement.spacedBy(16.dp),verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Lime),contentAlignment=Alignment.Center){Text(p.name.take(1).uppercase(),fontSize=30.sp,fontWeight=FontWeight.Bold)};Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(p.name,fontSize=25.sp,fontWeight=FontWeight.Bold);Text("${p.programme} / Semester ${p.semester}",color=Quiet,fontSize=12.sp,fontFamily=Mono)}}
            Text(p.department,fontWeight=FontWeight.Medium);Text(p.college,color=Quiet,fontSize=13.sp,modifier=Modifier.padding(top=6.dp,bottom=16.dp))
        }
        item{SettingsRow("Profile details","${p.year} admission",edit)}
        item{SettingsRow("Attendance target","${data.target}%",{targetEditor=true})}
        item{Spacer(Modifier.height(26.dp));Micro("YOUR DATA");Spacer(Modifier.height(10.dp))}
        item{SettingsRow("Export backup","Save a copy",export)}
        item{SettingsRow("Restore backup","From a Present file",restore)}
        item{Note("Records are saved on this device. Export a backup before uninstalling or switching phones. No account, cloud sync or internet connection is required.")}
        item{Spacer(Modifier.height(22.dp));HorizontalDivider(color=Line);Spacer(Modifier.height(22.dp));Micro("PRESENT / 0.2.0");Note("An independent attendance companion for CUSAT students. Your target is for planning; confirm your batch’s requirement with your department. This app does not certify exam eligibility or approve condonation.")}
    }
    if(targetEditor)AlertDialog(onDismissRequest={targetEditor=false},title={Text("Your attendance target")},text={Column{Input("Target (%)",target,{target=it},numeric=true,max=2);Note("Set a target between 50% and 99%. It should be at least your department’s required minimum.")}},confirmButton={TextButton({update{it.copy(target=target.toInt())};targetEditor=false},enabled=(target.toIntOrNull()?:0)in 50..99){Text("Save target")}},dismissButton={TextButton({targetEditor=false}){Text("Cancel")}})
}
@Composable fun SettingsRow(title: String, value: String, action: ()->Unit) {
    HorizontalDivider(color=Line)
    Row(Modifier.fillMaxWidth().clickable(onClick=action).padding(vertical=22.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(title,fontWeight=FontWeight.SemiBold,fontSize=15.sp);Text(value,color=Quiet,fontSize=12.sp)};Text("↗",fontSize=23.sp,color=Quiet)}
}
