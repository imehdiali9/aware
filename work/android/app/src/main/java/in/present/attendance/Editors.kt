package `in`.present.attendance

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val ProfileSaver = listSaver<Profile, String>(save = { listOf(it.name,it.college,it.department,it.programme,it.year,it.semester) }, restore = { Profile(it[0],it[1],it[2],it[3],it[4],it[5]) })
@Composable fun Input(label: String, value: String, change: (String)->Unit, modifier: Modifier = Modifier, numeric: Boolean = false, placeholder: String = "", max: Int = 80) {
    OutlinedTextField(value, { if(it.length <= max) change(it) }, modifier.fillMaxWidth(), label = { Text(label) }, placeholder = { Text(placeholder) }, singleLine = true, shape = RoundedCornerShape(12.dp), keyboardOptions = KeyboardOptions(capitalization = if(numeric)KeyboardCapitalization.None else KeyboardCapitalization.Words, keyboardType = if(numeric)KeyboardType.Number else KeyboardType.Text))
}
@Composable fun Choice(label: String, value: String, options: List<String>, change: (String)->Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton({expanded=true},Modifier.fillMaxWidth().heightIn(min=59.dp),shape=RoundedCornerShape(12.dp),contentPadding=PaddingValues(horizontal=13.dp)) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){Text(label,color=Quiet,fontSize=10.sp);Text(value,fontSize=14.sp,color=Ink)};Text("⌄",color=Quiet)
        }
        DropdownMenu(expanded,{expanded=false}) { options.forEach { item -> DropdownMenuItem(text={Text(item)},onClick={change(item);expanded=false}) } }
    }
}
@Composable fun ProfileForm(initial: Profile, setup: Boolean, onBack: (() -> Unit)? = null, onSave: (Profile)->Unit) {
    var name by rememberSaveable { mutableStateOf(initial.name) };var college by rememberSaveable { mutableStateOf(initial.college) }
    var department by rememberSaveable { mutableStateOf(initial.department) };var programme by rememberSaveable { mutableStateOf(initial.programme) }
    var year by rememberSaveable { mutableStateOf(initial.year) };var semester by rememberSaveable { mutableStateOf(initial.semester) }
    val valid = name.isNotBlank()&&college.isNotBlank()&&department.isNotBlank()&&(year.toIntOrNull()?:0) in 2000..LocalDate.now().year+1
    Column(Modifier.fillMaxSize().background(Paper).statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
            Text("present",fontSize=22.sp,fontWeight=FontWeight.Black,letterSpacing=(-1).sp,modifier=Modifier.weight(1f))
            if(onBack!=null)TextButton(onBack){Text("Close")}else Micro("01 / 02")
        }
        Spacer(Modifier.height(15.dp));Micro(if(setup)"YOUR SPACE, FIRST." else "YOUR DETAILS")
        Title(if(setup)"Let’s start\nwith you." else "Your profile.",size=42)
        Text(if(setup)"A few details. Then you’re in." else "Keep your semester details up to date.",color=Quiet,fontSize=15.sp)
        Spacer(Modifier.height(8.dp))
        Input("First name",name,{name=it},max=40,placeholder="What should we call you?")
        Input("College / campus",college,{college=it},max=100,placeholder="School of Engineering, CUSAT")
        Input("Department",department,{department=it},placeholder="Computer Science")
        Choice("Programme",programme,listOf("B.Tech","M.Tech","B.Sc","M.Sc","Integrated M.Sc","MBA","MCA","LLB","Other"),{programme=it})
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Input("Admission year",year,{year=it},Modifier.weight(1f),true,"2026",4)
            Choice("Semester",semester,(1..12).map{it.toString()},{semester=it},Modifier.weight(1f))
        }
        if(year.length==4&&!valid&&(year.toIntOrNull()?:0) !in 2000..LocalDate.now().year+1)Text("Enter a valid admission year.",color=Red,fontSize=12.sp)
        Spacer(Modifier.height(6.dp))
        Primary(if(setup)"Continue" else "Save profile",Modifier.fillMaxWidth(),valid){onSave(Profile(name.trim(),college.trim(),department.trim(),programme,year,semester))}
        Note(if(setup)"NO SIGN-UP. Your records stay on this device." else "Profile changes do not reset your attendance.")
        Spacer(Modifier.navigationBarsPadding())
    }
}
@Composable fun Onboarding(data: AppData, update: ((AppData)->AppData)->Unit) {
    var step by rememberSaveable { mutableIntStateOf(0) };var draft by rememberSaveable(stateSaver=ProfileSaver) { mutableStateOf(Profile()) }
    var add by remember { mutableStateOf(false) };var target by rememberSaveable { mutableIntStateOf(75) }
    BackHandler(step==1){step=0}
    if(step==0)ProfileForm(draft,true,onSave={draft=it;step=1}) else {
        Column(Modifier.fillMaxSize().background(Paper).statusBarsPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton({step=0},contentPadding=PaddingValues(0.dp)){Text("← Back")};Spacer(Modifier.weight(1f));Micro("02 / 02")}
            Spacer(Modifier.height(10.dp));Micro("BUILD YOUR SEMESTER");Title("What are you\nstudying?",size=42)
            Text("Start with your subjects. Add your weekly periods once you’re in.",color=Quiet,lineHeight=22.sp)
            data.subjects.forEachIndexed { i,s ->
                Row(Modifier.fillMaxWidth().padding(vertical=6.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){SubjectTag(i);Text(s.name,Modifier.weight(1f),fontWeight=FontWeight.Medium);IconButton({update{d->d.copy(subjects=d.subjects.filterNot{it.id==s.id})}}){Text("×",fontSize=24.sp)}}
                HorizontalDivider(color=Line)
            }
            AddButton("Add subject"){add=true}
            Spacer(Modifier.height(10.dp));Micro("ATTENDANCE TARGET")
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){listOf(75,80,85,90).forEach{n->FilterChip(target==n,{target=n},label={Text("$n%",fontFamily=Mono)},modifier=Modifier.weight(1f),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=Lime,selectedLabelColor=Ink))}}
            Text("Choose a planning target. Confirm the required minimum for your programme and batch with your department.",color=Quiet,fontSize=12.sp,lineHeight=18.sp)
            Primary("Open my day",Modifier.fillMaxWidth(),data.subjects.isNotEmpty()){update{it.copy(profile=draft,target=target)}}
            Note("Joining mid-semester? Add previous attended and held counts under Subjects. Unmarked classes won’t count as absences.")
            Spacer(Modifier.navigationBarsPadding())
        }
    }
    if(add)SubjectEditor(null,data.subjects,{add=false}) { subject -> update{it.copy(subjects=it.subjects+subject)};add=false }
}
@Composable fun SubjectEditor(subject: Subject?, all: List<Subject>, close: ()->Unit, save: (Subject)->Unit) {
    var name by rememberSaveable { mutableStateOf(subject?.name ?: "") }
    var attended by rememberSaveable { mutableStateOf((subject?.attended?:0).toString()) };var held by rememberSaveable { mutableStateOf((subject?.held?:0).toString()) }
    val a=attended.toIntOrNull();val h=held.toIntOrNull()
    val duplicate=all.any{it.id!=subject?.id&&it.name.equals(name.trim(),true)}
    val valid=name.isNotBlank()&&!duplicate&&a!=null&&h!=null&&a in 0..10000&&h in a..10000
    AlertDialog(onDismissRequest=close,containerColor=Paper,title={Text(if(subject==null)"Add a subject" else "Edit subject",fontWeight=FontWeight.Bold)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Input("Subject name",name,{name=it})
        Micro("STARTING COUNTS")
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){Input("Attended",attended,{attended=it},Modifier.weight(1f),true,max=5);Input("Held",held,{held=it},Modifier.weight(1f),true,max=5)}
        Text("Only classes before you started logging here. New records are added separately.",fontSize=12.sp,color=Quiet,lineHeight=18.sp)
        if(duplicate)Text("This subject already exists.",color=Red,fontSize=12.sp)
        if(a!=null&&h!=null&&a>h)Text("Attended cannot exceed held.",color=Red,fontSize=12.sp)
    }},confirmButton={TextButton({save(Subject(subject?.id?:java.util.UUID.randomUUID().toString(),name.trim(),a!!,h!!))},enabled=valid){Text("Save subject",fontWeight=FontWeight.Bold)}},dismissButton={TextButton(close){Text("Cancel")}})
}
@Composable fun PeriodEditor(data: AppData, existing: Period?, weekly: Boolean, day: Int, date: LocalDate, close: ()->Unit, save: (Period)->Unit, remove: ((Period)->Unit)? = null) {
    val context=LocalContext.current
    var subject by rememberSaveable { mutableStateOf(existing?.subject?:data.subjects.first().id) }
    var time by rememberSaveable { mutableStateOf(existing?.time?:"09:00") };var weekday by rememberSaveable { mutableIntStateOf(existing?.weekday?:day) }
    var start by rememberSaveable { mutableStateOf(existing?.start?:date.toString()) };var confirmDelete by remember{mutableStateOf(false)}
    val names=data.subjects.map{it.name};val subjectName=data.subjects.find{it.id==subject}?.name?:""
    val conflict=data.periods.any{it.id!=existing?.id&&it.time==time&&if(weekly)it.once.isEmpty()&&it.weekday==weekday else (it.once==date.toString()||(it.once.isEmpty()&&it.weekday==date.dayOfWeek.value&&it.start<=date.toString()))}
    AlertDialog(onDismissRequest=close,containerColor=Paper,title={Text(if(existing!=null)"Edit period" else if(weekly)"Weekly period" else "Extra class",fontWeight=FontWeight.Bold)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(14.dp)){
        Choice("Subject",subjectName,names,{n->subject=data.subjects.first{it.name==n}.id})
        if(weekly)Choice("Day",java.time.DayOfWeek.of(weekday).name.lowercase().replaceFirstChar{it.uppercase()},(1..7).map{java.time.DayOfWeek.of(it).name.lowercase().replaceFirstChar{it.uppercase()}},{weekday=java.time.DayOfWeek.valueOf(it.uppercase()).value})
        OutlinedButton({val t=time.split(":");TimePickerDialog(context,{_,h,m->time=String.format(Locale.ROOT,"%02d:%02d",h,m)},t[0].toInt(),t[1].toInt(),true).show()},Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(12.dp)){Text("Start time",Modifier.weight(1f));Text(time,fontFamily=Mono)}
        if(weekly)OutlinedButton({val d=LocalDate.parse(start);DatePickerDialog(context,{_,y,m,dt->start=LocalDate.of(y,m+1,dt).toString()},d.year,d.monthValue-1,d.dayOfMonth).show()},Modifier.fillMaxWidth().heightIn(min=52.dp),shape=RoundedCornerShape(12.dp)){Text("Applies from",Modifier.weight(1f));Text(start,fontFamily=Mono,fontSize=12.sp)}
        Text(if(weekly)"Add one entry per attendance period. For a lab with two periods, add each separately." else "For ${date.format(DateTimeFormatter.ofPattern("d MMM yyyy"))}. Your weekly schedule won’t change.",color=Quiet,fontSize=12.sp,lineHeight=18.sp)
        if(conflict)Text("There is already a period at this time.",color=Red,fontSize=12.sp)
        if(existing!=null&&remove!=null)TextButton({confirmDelete=true}){Text("Remove from schedule",color=Red)}
    }},confirmButton={TextButton({save(Period(existing?.id?:java.util.UUID.randomUUID().toString(),subject,time,weekday,start,if(weekly)"" else date.toString()))},enabled=!conflict){Text("Save period",fontWeight=FontWeight.Bold)}},dismissButton={TextButton(close){Text("Cancel")}})
    if(confirmDelete)AlertDialog(onDismissRequest={confirmDelete=false},title={Text("Remove this period?")},text={Text("Attendance already recorded for it will be kept. The period will no longer appear in your schedule.")},confirmButton={TextButton({remove?.invoke(existing!!);confirmDelete=false;close()}){Text("Remove",color=Red)}},dismissButton={TextButton({confirmDelete=false}){Text("Keep period")}})
}
