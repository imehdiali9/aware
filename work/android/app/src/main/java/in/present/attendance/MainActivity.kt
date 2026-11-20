package `in`.present.attendance

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Ink.toArgb()), navigationBarStyle = SystemBarStyle.light(Paper.toArgb(), Paper.toArgb()))
        setContent { PresentTheme { PresentApp() } }
    }
}

@Composable fun PresentApp() {
    val context = LocalContext.current
    val store = remember { Store(context) }
    var data by remember { mutableStateOf(AppData()) }
    var loaded by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var retry by remember { mutableIntStateOf(0) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var profileEditor by rememberSaveable { mutableStateOf(false) }
    var restoring by remember { mutableStateOf<AppData?>(null) }
    val darkHeader = loaded && !loadError && data.profile != null && tab == 0
    SideEffect {
        (context as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = if (darkHeader) SystemBarStyle.dark(Ink.toArgb()) else SystemBarStyle.light(Paper.toArgb(), Paper.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Paper.toArgb(), Paper.toArgb())
        )
    }
    val scope = rememberCoroutineScope()
    val mutex = remember { Mutex() }
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(retry) {
        try { data = withContext(Dispatchers.IO) { store.load() }; loaded = true; loadError = false }
        catch (_: Exception) { loadError = true }
    }
    val update: ((AppData) -> AppData) -> Unit = { transform ->
        scope.launch {
            mutex.withLock {
                try {
                    val next = transform(data)
                    withContext(Dispatchers.IO) { store.save(next) }
                    data = next
                } catch (_: Exception) { scope.launch { snack.showSnackbar("Could not save. Your previous records are safe. Please try again.") } }
            }
        }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            try {
                val snapshot = data
                withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openOutputStream(uri, "wt") ?: error("Unable to open file")
                    stream.bufferedWriter().use { it.write(encode(snapshot)) }
                }
                snack.showSnackbar("Backup exported.")
            } catch (_: Exception) { snack.showSnackbar("Could not export this backup. Try a different location.") }
        }
    }
    val restore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri != null)scope.launch {
            try {
                val imported = withContext(Dispatchers.IO) {
                    val stream = context.contentResolver.openInputStream(uri) ?: error("Unable to open file")
                    val bytes = stream.use { input ->
                        val buffer = ByteArray(8192)
                        val output = java.io.ByteArrayOutputStream()
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(output.size() + count <= 4_000_000)
                            output.write(buffer, 0, count)
                        }
                        output.toByteArray()
                    }
                    decode(bytes.toString(Charsets.UTF_8)).also { require(it.profile != null) }
                }
                restoring = imported
            } catch (_: Exception) { snack.showSnackbar("This file isn’t a valid Present Android backup.") }
        }
    }
    BackHandler(loaded && data.profile != null && tab != 0 && !profileEditor) { tab = 0 }
    Box(Modifier.fillMaxSize().background(if(darkHeader) Ink else Paper)) {
        when {
            loadError -> Column(Modifier.fillMaxSize().background(Paper).safeDrawingPadding().padding(28.dp), verticalArrangement=Arrangement.Center) {
                Title("Your records\nneed attention.")
                Note("We couldn’t read the saved file. It has not been changed. Retry, or restore a backup you exported earlier.")
                Primary("Try again",Modifier.fillMaxWidth()){retry++}
                TextButton({restore.launch(arrayOf("application/json","text/plain","application/octet-stream"))}) { Text("Restore a backup") }
            }
            !loaded -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator(color=Lime) }
            data.profile == null -> Onboarding(data,update)
            else -> Scaffold(
                modifier=Modifier.statusBarsPadding(),containerColor=Paper,
                contentWindowInsets=WindowInsets(0,0,0,0),
                bottomBar={BottomNav(tab){tab=it}}
            ) { padding -> Box(Modifier.padding(padding)) {
                when(tab) {
                    0 -> DayScreen(data,update,{tab=2},{tab=1})
                    1 -> SubjectsScreen(data,update)
                    2 -> WeekScreen(data,update,{tab=1})
                    3 -> YouScreen(data,update,{profileEditor=true},{export.launch("Present-${LocalDate.now()}.json")},{restore.launch(arrayOf("application/json","text/plain","application/octet-stream"))})
                }
            } }
        }
        SnackbarHost(snack,Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal=16.dp,vertical=90.dp))
    }
    if(profileEditor && data.profile != null) Dialog({profileEditor=false},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        PresentTheme { ProfileForm(data.profile!!,false,{profileEditor=false}){p->update{it.copy(profile=p)};profileEditor=false} }
    }
    restoring?.let { backup -> AlertDialog(
        onDismissRequest={restoring=null},title={Text("Restore this backup?")},
        text={Text("This replaces the profile and attendance on this device with ${backup.profile!!.name}’s backup (${backup.subjects.size} subjects, ${backup.entries.size} records). Export your current records first if you want to keep them.")},
        confirmButton={TextButton({
            scope.launch {
                mutex.withLock {
                    try { withContext(Dispatchers.IO){store.save(backup)};data=backup;loaded=true;loadError=false;tab=0;restoring=null }
                    catch (_: Exception) { restoring=null;scope.launch{snack.showSnackbar("Could not restore. Existing records were kept.")} }
                }
            }
        }){Text("Replace and restore")}},dismissButton={TextButton({restoring=null}){Text("Cancel")}}
    ) }
}
