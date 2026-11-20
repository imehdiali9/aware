package `in`.present.attendance

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.time.LocalDate

// Fictional preview data only. Never written into the installed app's store.
private fun previewData(): AppData {
    val today=LocalDate.now()
    return AppData(
        profile=Profile("Alex","School of Engineering, CUSAT","Computer Science","B.Tech","2026","1"),
        subjects=listOf(Subject("math","Engineering Mathematics",24,28),Subject("physics","Applied Physics",16,20),Subject("programming","Computer Programming",20,22)),
        periods=listOf(Period("p1","math","09:00",today.dayOfWeek.value,today.toString()),Period("p2","physics","10:00",today.dayOfWeek.value,today.toString()),Period("p3","programming","11:00",today.dayOfWeek.value,today.toString())),target=75)
}
@Preview(name="01 Introduction",showBackground=true,widthDp=400,heightDp=850)
@Composable private fun IntroductionPreview(){PresentTheme{ProfileForm(Profile(),true,onSave={})}}
@Preview(name="02 Day",showBackground=true,widthDp=400,heightDp=850)
@Composable private fun DayPreview(){PresentTheme{DayScreen(previewData(),{}, {}, {})}}
@Preview(name="03 Subjects",showBackground=true,widthDp=400,heightDp=850)
@Composable private fun SubjectsPreview(){PresentTheme{SubjectsScreen(previewData(),{})}}
@Preview(name="04 Week",showBackground=true,widthDp=400,heightDp=850)
@Composable private fun WeekPreview(){PresentTheme{WeekScreen(previewData(),{}, {})}}
@Preview(name="05 Profile",showBackground=true,widthDp=400,heightDp=850)
@Composable private fun ProfilePreview(){PresentTheme{YouScreen(previewData(),{}, {}, {}, {})}}
