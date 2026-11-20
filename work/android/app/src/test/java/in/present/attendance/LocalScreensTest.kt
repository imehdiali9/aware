package `in`.present.attendance

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import java.io.File

@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
// Custom width qualifiers trigger Robolectric issue #8460 for dialog text fields.
// Keep interaction tests at the default size; render phone previews separately.
@org.robolectric.annotation.Config(sdk = [35])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class LocalScreensTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val freshStore = object : ExternalResource() {
        override fun before() {
            Store(InstrumentationRegistry.getInstrumentation().targetContext).save(AppData())
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(freshStore).around(compose)
    private fun saved(predicate: (AppData) -> Boolean) {
        compose.waitForIdle()
        compose.waitUntil(15000) {
            compose.waitForIdle()
            predicate(Store(InstrumentationRegistry.getInstrumentation().targetContext).load())
        }
        compose.waitForIdle()
    }
    private fun picture(name: String) {
        compose.waitForIdle()
        val dir = File("build/journey-checks").apply{mkdirs()}
        compose.runOnUiThread {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            File(dir,"$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            bitmap.recycle()
        }
    }
    @Test fun onboardingTimetableMarkingNavigationAndPersistence() {
        compose.waitUntil(15000){compose.onAllNodesWithText("First name").fetchSemanticsNodes().isNotEmpty()}
        picture("01-introduction")
        compose.onNodeWithText("First name").performTextInput("Alex")
        compose.onNodeWithText("College / campus").performScrollTo().performTextInput("School of Engineering, CUSAT")
        compose.onNodeWithText("Department").performScrollTo().performTextInput("Computer Science")
        compose.onNodeWithText("Admission year").performScrollTo().performTextInput("2026")
        compose.onNodeWithText("Continue").performScrollTo().performClick()
        compose.onNodeWithText("+  Add subject").performClick()
        compose.onNodeWithText("Subject name").performTextInput("Engineering Mathematics")
        compose.onNodeWithText("Attended").performTextReplacement("8")
        compose.onNodeWithText("Held").performTextReplacement("10")
        compose.onNodeWithText("Save subject").assertIsEnabled().performClick()
        saved{it.subjects.size==1}
        picture("02-semester-setup")
        compose.onNodeWithText("Open my day").performScrollTo().performClick()
        saved{it.profile!=null}
        compose.onNodeWithText("80.0").assertExists()
        compose.onNodeWithText("Week",substring=false).performClick()
        compose.onNodeWithText("+  Period").performClick()
        compose.onNodeWithText("Save period").performClick()
        saved{it.periods.size==1}
        picture("05-week")
        compose.onNodeWithText("Day",substring=false).performClick()
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Present"))
        compose.onNodeWithText("Present",substring=false).performClick()
        saved{it.entries.singleOrNull()?.status=="present"}
        assertEquals(Totals(9,11),totals(Store(compose.activity).load()))
        compose.onNodeWithText("Absent",substring=false).performClick()
        saved{it.entries.singleOrNull()?.status=="absent"}
        assertEquals(Totals(8,11),totals(Store(compose.activity).load()))
        compose.onNodeWithText("Absent",substring=false).performClick()
        saved{it.entries.isEmpty()}
        compose.onNodeWithText("Present",substring=false).performClick()
        saved{it.entries.singleOrNull()?.status=="present"}
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToIndex(0)
        picture("03-day")
        compose.onNodeWithText("Subjects",substring=false).performClick()
        compose.onNodeWithText("81.8").assertExists()
        picture("04-subjects")
        compose.onNodeWithText("You",substring=false).performClick()
        compose.onNodeWithText("Your space.").assertExists()
        picture("06-profile")
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText("Attendance target"))
        compose.onNodeWithText("Attendance target").performClick()
        compose.onNodeWithText("Target (%)").performTextReplacement("85")
        compose.onNodeWithText("Save target").performClick()
        saved{it.target==85}
        compose.activityRule.scenario.recreate()
        compose.waitUntil(15000){compose.onAllNodesWithText("85%").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("85%").assertExists()
        compose.onNodeWithText("Day",substring=false).performClick()
        compose.onNodeWithText("81.8").assertExists()
        val original=Store(compose.activity).load()
        assertEquals(original,decode(encode(original)))
    }
}


