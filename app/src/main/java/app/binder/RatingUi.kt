package app.binder

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ───────── the star shape ─────────
// Stars are drawn, not typed, so a part-filled star always starts at its own left edge and the
// fill is the same share of the star's width at any size or font scale.

private const val HALF_WIDTH = 0.95106f   // half the width of a unit-radius five-point star (sin 72°)
private const val DROP = 0.80902f         // how far the lowest points sit below the centre (cos 36°)

private fun starPath(cx: Float, cy: Float, r: Float): Path {
    val p = Path()
    val inner = r * 0.4f
    for (i in 0 until 10) {
        val a = -PI / 2 + i * PI / 5
        val rad = if (i % 2 == 0) r else inner
        val x = cx + (rad * cos(a)).toFloat()
        val y = cy + (rad * sin(a)).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    return p
}

/**
 * One star whose box is [left, left + width] and which is centred vertically in [height].
 * [fill] 0..1 is the share of the star's own width, measured from its left edge.
 */
private fun DrawScope.drawStar(left: Float, width: Float, fill: Float, on: Color, off: Color) {
    val r = width / (2 * HALF_WIDTH)
    val cx = left + width / 2f
    val cy = size.height / 2f + r * (1f - DROP) / 2f     // centres the star's real height
    val path = starPath(cx, cy, r)
    drawPath(path, off)
    val f = fill.coerceIn(0f, 1f)
    if (f > 0f) {
        // clip from the star's own start edge, never from the middle of its slot
        val right = left + width * f + if (f >= 1f) 1f else 0f
        clipRect(left = left - 1f, top = 0f, right = right, bottom = size.height) { drawPath(path, on) }
    }
}

@Composable
private fun Sp(size: Int): Dp = with(LocalDensity.current) { size.sp.toDp() }

/** Five stars, drawn proportionally. Not tappable. */
@Composable
fun StarRow(value: Float, star: Dp, modifier: Modifier = Modifier, gap: Dp = 2.dp) {
    val fills = starFills(value)
    val off = P.mute.copy(alpha = 0.3f)
    Canvas(modifier.size(star * 5 + gap * 4, star)) {
        val s = star.toPx()
        val g = gap.toPx()
        for (i in 0 until 5) drawStar(i * (s + g), s, fills[i], Coral, off)
    }
}

/** A single coral star, for after the number. */
@Composable
fun StarGlyph(size: Dp, modifier: Modifier = Modifier, color: Color = Coral) {
    Canvas(modifier.size(size)) { drawStar(0f, this.size.width, 1f, color, color) }
}

/**
 * The rating as the option says: five stars, or "4.75 ★" (number first, coral star after it).
 * Draws nothing when there is no rating.
 */
@Composable
fun RatingText(
    rating: Float,
    size: Int = 13,
    modifier: Modifier = Modifier,
    style: RatingStyle = LocalOpts.current.ratingStyle,
    color: Color = P.fg,
) {
    if (cleanRating(rating) <= 0f) return
    val px = Sp(size)
    when (style) {
        RatingStyle.STARS -> StarRow(rating, px, modifier)
        RatingStyle.NUMBER -> {
            val text = formatRating(rating)
            if (text.isEmpty()) return
            Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                Txt(text, size, color, weight = FontWeight.Medium)
                StarGlyph(px * 0.85f, Modifier.padding(start = 3.dp))
            }
        }
    }
}

// ───────── setting a rating ─────────

/**
 * Stars style: tap the left half of a star for a half, the right half for a whole one, and tap the
 * same value again to clear. Any stored value is drawn in proportion.
 */
@Composable
fun Stars(value: Float, onChange: (Float) -> Unit) {
    val star = 26.dp
    val gap = 8.dp
    val fills = starFills(value)
    val off = P.mute.copy(alpha = 0.3f)
    Canvas(
        Modifier.size(star * 5 + gap * 4, star + 8.dp).pointerInput(value) {
            detectTapGestures { pos ->
                val slot = (star + gap).toPx()
                val i = (pos.x / slot).toInt().coerceIn(0, 4)
                val within = pos.x - i * slot
                val v = i + if (within < slot / 2f) 0.5f else 1f
                onChange(if (v == value) 0f else v)
            }
        }
    ) {
        val s = star.toPx()
        val g = gap.toPx()
        for (i in 0 until 5) drawStar(i * (s + g), s, fills[i], Coral, off)
    }
}

/** The rating field on the item page and in quick add, following Opts.ratingStyle. */
@Composable
fun RatingInput(vm: BinderViewModel, value: Float, onChange: (Float) -> Unit) {
    when (vm.opts.ratingStyle) {
        RatingStyle.STARS -> Stars(value, onChange)
        RatingStyle.NUMBER -> {
            val text = formatRating(value)
            Row(
                Modifier.clip(CircleShape).clickable { vm.ratingEdit = RatingEdit(value, onChange) }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (text.isEmpty()) {
                    Txt("Add a rating", 15, P.mute)
                } else {
                    Txt(text, 40, font = TitleFont, ls = (-0.02).em)
                    StarGlyph(30.dp, Modifier.padding(start = 6.dp, top = 4.dp))
                }
            }
        }
    }
}

/** A small sheet to set the number style's rating from 0 to 5 in steps of 0.25. */
@Composable
fun BoxScope.RatingSheet(vm: BinderViewModel, e: RatingEdit) {
    var v by remember(e) { mutableFloatStateOf(snapRating(e.value)) }
    val close = { vm.ratingEdit = null }
    SheetFrame(close) {
        Txt("Rating", 22, weight = FontWeight.Medium)
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val t = formatRating(v)
            if (t.isEmpty()) Txt("Not rated", 20, P.mute)
            else {
                Txt(t, 44, font = TitleFont)
                StarGlyph(34.dp, Modifier.padding(start = 8.dp, top = 4.dp))
            }
        }
        Slider(
            value = v,
            onValueChange = { raw ->
                val s = snapRating(raw)
                if (s != v) { v = s; e.onChange(s) }
            },
            valueRange = 0f..RATING_MAX,
            steps = 19, // 20 steps of 0.25
            colors = SliderDefaults.colors(
                thumbColor = Coral, activeTrackColor = Coral, inactiveTrackColor = P.line,
                activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Txt("0", 12, P.mute)
            Txt("5", 12, P.mute)
        }
        Spacer(Modifier.size(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (v > 0f) Box(Modifier.weight(1f)) { Pill("Clear") { v = 0f; e.onChange(0f) } }
            Box(Modifier.weight(2f)) { BigButton("Done") { close() } }
        }
    }
}
