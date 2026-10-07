package app.binder

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private fun icon(name: String, vararg paths: String): ImageVector {
    val b = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    for (p in paths) {
        b.addPath(
            pathData = addPathNodes(p),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return b.build()
}

object Ic {
    val Search = icon("search", "M17 11 a6 6 0 1 1 -12 0 a6 6 0 1 1 12 0 z", "M16 16 l4 4")
    val Plus = icon("plus", "M12 5v14M5 12h14")
    val Back = icon("back", "M15 5l-7 7 7 7")
    val Dots = icon("dots", "M12 6h0.01M12 12h0.01M12 18h0.01")
    val List = icon("list", "M9 6h11M9 12h11M9 18h11M4 6h0.01M4 12h0.01M4 18h0.01")
    val Check = icon("check", "M5 12l5 5L20 7")
}
