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
    val Menu = icon("menu", "M4 6h16M4 12h16M4 18h16")
    val Grid = icon("grid", "M4 4h6v6H4zM14 4h6v6h-6zM4 14h6v6H4zM14 14h6v6h-6z")
    val Handle = icon("handle", "M5 9h14M5 15h14")
    val Close = icon("close", "M6 6l12 12M18 6L6 18")
    val Chevron = icon("chevron", "M9 5l7 7-7 7")
    val Out = icon("out", "M8 16L16 8M9 8h7v7")
    val Layers = icon("layers", "M12 3l9 5-9 5-9-5 9-5z", "M3 13l9 5 9-5")
    val Sliders = icon(
        "sliders", "M4 7h9M17 7h3M4 17h3M11 17h9",
        "M17 7 a2 2 0 1 1 -4 0 a2 2 0 1 1 4 0 z", "M11 17 a2 2 0 1 1 -4 0 a2 2 0 1 1 4 0 z",
    )
    val Download = icon("download", "M12 4v11M7 11l5 5 5-5M5 20h14")
    val Info = icon("info", "M21 12 a9 9 0 1 1 -18 0 a9 9 0 1 1 18 0 z", "M12 11v5M12 8h0.01")
    val Photo = icon("photo", "M4 5h16v14H4z", "M4 16l5-5 4 4 3-3 4 4")
}
