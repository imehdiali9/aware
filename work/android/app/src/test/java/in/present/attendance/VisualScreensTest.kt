package `in`.present.attendance

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import java.io.File
import java.time.LocalDate

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [35], qualifiers = "w400dp-h850dp-xhdpi")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class VisualScreensTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val seed = object : ExternalResource() {
        override fun before() {
            val today=LocalDate.now()
            Store(InstrumentationRegistry.getInstrumentation().targetContext).save(AppData(
                profile=Profile("Alex","School of Engineering, CUSAT","Computer Science","B.Tech","2026","1"),
                subjects=listOf(Subject("math","Engineering Mathematics",24,28),Subject("physics","Applied Physics",16,20),Subject("code","Computer Programming",20,22)),
                periods=listOf(Period("p1","math","09:00",today.dayOfWeek.value,today.toString()),Period("p2","physics","10:00",today.dayOfWeek.value,today.toString()),Period("p3","code","11:00",today.dayOfWeek.value,today.toString()))))
        }
    }
    @get:Rule val rules: RuleChain=RuleChain.outerRule(seed).around(compose)
    private fun picture(name: String) {
        compose.waitForIdle()
        val dir=File("build/screen-checks").apply{mkdirs()}
        compose.runOnUiThread {
            val view=compose.activity.window.decorView
            val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            File(dir,"$name.png").outputStream().use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
            bitmap.recycle()
        }
    }
    @Test fun renderPhoneScreens() {
        compose.waitUntil(15000){compose.onAllNodesWithText("85.7").fetchSemanticsNodes().isNotEmpty()}
        picture("03-day")
        compose.onNodeWithText("Subjects",substring=false).performClick()
        compose.onNodeWithText("Subjects.").assertExists();picture("04-subjects")
        compose.onNodeWithText("Week",substring=false).performClick()
        compose.onNodeWithText("The week.").assertExists();picture("05-week")
        compose.onNodeWithText("You",substring=false).performClick()
        compose.onNodeWithText("Your space.").assertExists();picture("06-profile")
    }
}
