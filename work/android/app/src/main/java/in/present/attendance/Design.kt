package `in`.present.attendance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF171C19)
val Paper = Color(0xFFF5F6F0)
val Lime = Color(0xFFD5F35B)
val Quiet = Color(0xFF69736B)
val Line = Color(0xFFDEE2D8)
val DarkQuiet = Color(0xFFA4AFA5)
val Red = Color(0xFFA64232)
val Mono = FontFamily.Monospace

@Composable fun PresentTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Ink, onPrimary = Paper, secondary = Lime, onSecondary = Ink, surface = Paper, onSurface = Ink, background = Paper, onBackground = Ink, surfaceVariant = Color(0xFFE9EDE3), onSurfaceVariant = Quiet, outline = Color(0xFF929B90), error = Red),
        typography = Typography(bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp), bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp), titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold), labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold)), content = content)
}
@Composable fun Micro(text: String, color: Color = Quiet, modifier: Modifier = Modifier) { Text(text.uppercase(), modifier, color, fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.Medium) }
@Composable fun Title(text: String, color: Color = Ink, size: Int = 34) { Text(text, color = color, fontSize = size.sp, letterSpacing = (-1.3).sp, lineHeight = (size + 3).sp, fontWeight = FontWeight.Bold) }
@Composable fun Primary(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, dark: Boolean = false, action: () -> Unit) {
    Button(action, modifier.heightIn(min = 54.dp), enabled = enabled, shape = RoundedCornerShape(15.dp), colors = ButtonDefaults.buttonColors(containerColor = if (dark) Lime else Ink, contentColor = if (dark) Ink else Paper)) {
        Text(text, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold); Text("↗", fontSize = 22.sp)
    }
}
@Composable fun PageHeading(kicker: String, title: String, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 25.dp, bottom = 25.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) { Micro(kicker); Title(title) }; trailing?.invoke()
    }
}
@Composable fun AddButton(label: String, action: () -> Unit) {
    FilledTonalButton(action, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.filledTonalButtonColors(containerColor = Lime, contentColor = Ink), contentPadding = PaddingValues(horizontal = 15.dp), modifier = Modifier.heightIn(min = 48.dp)) { Text("+  $label", fontWeight = FontWeight.Bold) }
}
@Composable fun EmptyState(number: String, title: String, body: String, actionLabel: String, action: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(number, color = Color(0xFFBAC2B6), fontFamily = Mono, fontSize = 52.sp, letterSpacing = (-4).sp)
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-.6).sp)
        Text(body, color = Quiet, fontSize = 14.sp, lineHeight = 21.sp)
        TextButton(action, contentPadding = PaddingValues(0.dp)) { Text("$actionLabel  ↗", color = Ink, fontWeight = FontWeight.Bold) }
    }
}
@Composable fun NavGlyph(index: Int, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(22.dp)) {
        val w = size.width; val h = size.height; val stroke = 1.7.dp.toPx()
        fun line(x1: Float,y1: Float,x2: Float,y2: Float) = drawLine(color, Offset(x1*w,y1*h),Offset(x2*w,y2*h),stroke, StrokeCap.Round)
        when(index) {
            0 -> { drawRect(color, Offset(w*.15f,h*.22f), androidx.compose.ui.geometry.Size(w*.7f,h*.65f), style=Stroke(stroke));line(.15f,.43f,.85f,.43f);line(.33f,.12f,.33f,.3f);line(.67f,.12f,.67f,.3f);line(.33f,.64f,.47f,.76f);line(.47f,.76f,.7f,.55f) }
            1 -> { line(.2f,.2f,.2f,.82f);line(.2f,.82f,.85f,.82f);line(.4f,.63f,.4f,.77f);line(.6f,.43f,.6f,.77f);line(.8f,.17f,.8f,.77f) }
            2 -> { for(y in listOf(.22f,.5f,.78f)){line(.32f,y,.85f,y);drawCircle(color,2.dp.toPx(),Offset(.14f*w,y*h))} }
            3 -> { drawCircle(color,w*.18f,Offset(w*.5f,h*.3f), style=Stroke(stroke));drawPath(Path().apply{moveTo(.15f*w,.86f*h);cubicTo(.15f*w,.49f*h,.85f*w,.49f*h,.85f*w,.86f*h)},color,style=Stroke(stroke,cap=StrokeCap.Round)) }
        }
    }
}
@Composable fun BottomNav(selected: Int, change: (Int) -> Unit) {
    Column(Modifier.background(Paper)) {
        HorizontalDivider(color = Line)
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
            listOf("Day", "Subjects", "Week", "You").forEachIndexed { i, label ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable { change(i) }.padding(vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(Modifier.clip(RoundedCornerShape(9.dp)).background(if(i==selected)Lime else Color.Transparent).padding(horizontal = 17.dp, vertical = 4.dp)) { NavGlyph(i, if(i==selected)Ink else Quiet) }
                    Text(label, fontSize = 11.sp, color = if(i==selected)Ink else Quiet, fontWeight = if(i==selected)FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}
@Composable fun SubjectTag(index: Int) { Box(Modifier.size(40.dp).clip(RoundedCornerShape(11.dp)).background(listOf(Lime,Color(0xFFDBE7F0),Color(0xFFE8DDD2),Color(0xFFE2DDF0))[index%4]),contentAlignment=Alignment.Center){Text(String.format(java.util.Locale.ROOT,"%02d",index+1),fontFamily=Mono,fontSize=12.sp,fontWeight=FontWeight.Bold)} }
@Composable fun Note(text: String) { Text(text, color = Quiet, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 12.dp)) }

