package me.centralhardware.znatoki.telegram.statistic.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** The mockup's line icons on a 24×24 grid, stroked in the content colour (rects and circles written as paths). */
object Icons {
    private fun icon(name: String, vararg paths: String): ImageVector {
        val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
        paths.forEach { d ->
            b.addPath(PathParser().parsePathString(d).toNodes(), stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
        return b.build()
    }

    private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"
    private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
        "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"

    val today = icon("today", rect(3.5f, 5f, 17f, 15f, 3f), "M3.5 10h17M8 3v4M16 3v4")
    val payments = icon("payments", rect(3f, 6f, 18f, 13f, 2.5f), "M3 10.5h18M7 15h4")
    val schedule = icon("schedule", circle(12f, 12f, 8.5f), "M12 7.5V12l3 2")
    val students = icon("students", circle(9f, 8f, 3.5f), "M2.5 20c.8-3.6 3.4-5.5 6.5-5.5s5.7 1.9 6.5 5.5", circle(17.5f, 9f, 2.6f), "M16.5 14.6c2.6.2 4.4 1.9 5 4.9")
    val tutors = icon("tutors", circle(12f, 8f, 3.5f), "M5 20c.8-3.6 3.6-5.5 7-5.5s6.2 1.9 7 5.5", "M18.5 4.5l1.5 1.5-1.5 1.5M5.5 4.5L4 6l1.5 1.5")
    val report = icon("report", rect(5f, 3.5f, 14f, 17f, 2f), "M9 8h6M9 12h6M9 16h3")
    val audit = icon("audit", "M12 7v5l3 2", "M3.5 12a8.5 8.5 0 1 0 2.5-6", "M3 4v4h4")
    val scale = icon("scale", "M3 19l5-13 5 13M4.8 14.5h6.4", "M14.5 19l3.5-9 3.5 9M15.7 16h4.6")
    val logout = icon("logout", "M14 4h4.5A1.5 1.5 0 0 1 20 5.5v13a1.5 1.5 0 0 1-1.5 1.5H14", "M10 8l-4 4 4 4M6 12h10")
    val search = icon("search", circle(11f, 11f, 6.5f), "M16 16l4.5 4.5")
}
