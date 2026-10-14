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
    val Photo = icon("photo", "M4 5h16v14H4z", "M4 16l5-5 4 4 3-3 4 4")

    // Settings icons (same shapes as the ones in Paperback's Settings)
    val Globe = icon(
        "globe",
        "M21 12 a9 9 0 1 1 -18 0 a9 9 0 1 1 18 0 z", "M3 12h18",
        "M12 3 c2.5 2.7 3.8 5.7 3.8 9 s-1.3 6.3 -3.8 9 c-2.5 -2.7 -3.8 -5.7 -3.8 -9 s1.3 -6.3 3.8 -9 z",
    )
    val Star = icon("star", "M12 3.5 L14.6 8.9 L20.5 9.7 L16.2 13.8 L17.2 19.7 L12 16.9 L6.8 19.7 L7.8 13.8 L3.5 9.7 L9.4 8.9 z")
    val Palette = icon(
        "palette",
        "M12 3 a9 9 0 1 0 0 18 c1.4 0 2 -1 1.5 -2.2 c-.5 -1.2 .2 -2.3 1.5 -2.3 H17 a4 4 0 0 0 4 -4 c0 -5 -4 -9.5 -9 -9.5 z",
        "M7.5 11h.01", "M10 7h.01", "M15 7.5h.01",
    )
    val AppStyle = icon(
        "appstyle",
        "M4 7 a3 3 0 0 1 3 -3 h10 a3 3 0 0 1 3 3 v10 a3 3 0 0 1 -3 3 H7 a3 3 0 0 1 -3 -3 z", "M4 10h16", "M8 7h5",
    )
    val NameTag = icon("nametag", "M5 7V5h14v2", "M12 5v14", "M9 19h6")
    val Backup = icon(
        "backup",
        "M7 18 a4 4 0 0 1 0 -8 a5 5 0 0 1 9.6 -1 A4.5 4.5 0 0 1 17 18 z", "M12 16v-5", "M9.5 13.5 L12 11 l2.5 2.5",
    )
    val Info = icon("info", "M12 3.5 a8.5 8.5 0 1 0 0 17 a8.5 8.5 0 1 0 0 -17 z", "M12 11v5", "M12 8h.01")
}
