package app.binder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

@Composable
fun Txt(
    text: String,
    size: Int = 15,
    color: Color = P.fg,
    modifier: Modifier = Modifier,
    font: FontFamily = Outfit,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
    lh: TextUnit = TextUnit.Unspecified,
    strike: Boolean = false,
) {
    Text(
        text = text, modifier = modifier, color = color, fontSize = size.sp, fontFamily = font,
        fontWeight = weight, textAlign = align, lineHeight = lh, maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textDecoration = if (strike) TextDecoration.LineThrough else null,
    )
}

@Composable
fun Cap(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text, modifier = modifier.padding(top = 16.dp, bottom = 8.dp), color = P.mute,
        fontSize = 11.sp, letterSpacing = 0.08.em, fontFamily = Outfit,
    )
}

/** Standard screen frame: edge-to-edge, with room for the status bar. */
@Composable
fun Screen(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().padding(horizontal = 16.dp), content = content)
}

@Composable
fun TopBar(
    center: String = "",
    left: @Composable () -> Unit = { Spacer(Modifier.size(44.dp)) },
    right: @Composable () -> Unit = { Spacer(Modifier.size(44.dp)) },
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        left()
        Txt(center, 20, weight = FontWeight.Medium)
        right()
    }
}

@Composable
fun CircleBtn(icon: ImageVector, desc: String, bg: Color = P.surface, tint: Color = P.fg, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(20.dp)) }
}

@Composable
fun Pill(text: String, filled: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier.height(44.dp).clip(CircleShape).background(if (filled) P.primary else P.surface)
            .clickable(onClick = onClick).padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) { Txt(text, 14, if (filled) P.onPrimary else P.fg, weight = FontWeight.Medium) }
}

@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.fillMaxWidth().height(52.dp).clip(CircleShape).background(P.primary).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Txt(text, 16, P.onPrimary, weight = FontWeight.Medium) }
}

@Composable
fun Seg(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    Row(modifier.fillMaxWidth().clip(CircleShape).background(P.surface).padding(4.dp)) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Box(
                Modifier.weight(1f).clip(CircleShape)
                    .background(if (on) P.primary else Color.Transparent)
                    .clickable { onSelect(i) }.padding(vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) { Txt(o, 14, if (on) P.onPrimary else P.fg) }
        }
    }
}

@Composable
fun Chips(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Box(
                Modifier.clip(CircleShape).background(if (on) P.primary else P.surface)
                    .clickable { onSelect(i) }.padding(horizontal = 14.dp, vertical = 8.dp)
            ) { Txt(o, 14, if (on) P.onPrimary else P.fg) }
        }
    }
}

@Composable
fun Field(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    imeAction: ImeAction = ImeAction.Default,
    onDone: () -> Unit = {},
    onChange: (String) -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(P.surface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Txt(label, 12, P.mute)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            textStyle = TextStyle(color = P.fg, fontSize = 18.sp, fontFamily = Outfit),
            cursorBrush = SolidColor(Coral),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        )
    }
}

@Composable
fun CoverView(title: String, c: Cover, modifier: Modifier = Modifier, size: Int = 13) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.aspectRatio(2f / 3f).clip(shape)
            .background(Brush.verticalGradient(listOf(c.top, c.bottom)))
            .border(1.dp, P.line, shape).padding(8.dp)
    ) { Txt(title, size, c.ink, font = Serif, maxLines = 4, lh = (size + 2).sp) }
}

/** The little fanned-out stack of three cards used as a list's icon. */
@Composable
fun CardStack(covers: List<Cover>, scale: Float = 1f, modifier: Modifier = Modifier) {
    val rot = listOf(-9f, 2f, 10f)
    Box(modifier.height((60 * scale).dp).width((100 * scale).dp)) {
        covers.take(3).forEachIndexed { i, c ->
            val shape = RoundedCornerShape(8.dp)
            Box(
                Modifier.offset(x = (i * 26 * scale).dp).size((38 * scale).dp, (54 * scale).dp)
                    .rotate(rot[i]).clip(shape)
                    .background(Brush.verticalGradient(listOf(c.top, c.bottom)))
                    .border(2.dp, P.surface, shape)
            )
        }
    }
}

@Composable
fun Tile(
    title: String, sub: String, covers: List<Cover>, selected: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier.fillMaxWidth().height(158.dp).clip(shape).background(P.surface)
            .border(2.dp, if (selected) Coral else Color.Transparent, shape)
            .clickable(onClick = onClick).padding(14.dp)
    ) {
        CardStack(covers, 1f, Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 4.dp))
        Column(Modifier.align(Alignment.BottomStart)) {
            Txt(title, 15, weight = FontWeight.Medium, maxLines = 2)
            Txt(sub, 12, P.mute, Modifier.padding(top = 2.dp), maxLines = 1)
        }
    }
}

/** Tap the left half of a star for a half-star, the right half for a full one. Tap the same value again to clear. */
@Composable
fun Stars(value: Float, onChange: (Float) -> Unit) {
    val w: Dp = 34.dp
    Row {
        for (i in 0 until 5) {
            val fill = (value - i).coerceIn(0f, 1f)
            Box(
                Modifier.size(w).pointerInput(value) {
                    detectTapGestures { off ->
                        val v = i + if (off.x < size.width / 2f) 0.5f else 1f
                        onChange(if (v == value) 0f else v)
                    }
                }
            ) {
                Txt("★", 28, P.mute.copy(alpha = 0.3f), Modifier.fillMaxSize(), align = TextAlign.Center)
                if (fill > 0f) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(fill).clipToBounds()) {
                        Txt("★", 28, Coral, Modifier.requiredWidth(w), align = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnScope.EmptyBlock(title: String, body: String, button: String? = null, onClick: () -> Unit = {}) {
    Column(
        Modifier.weight(1f).fillMaxWidth().padding(bottom = 80.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CardStack(listOf(Covers[0], Covers[3], Covers[1]), 1.3f, Modifier.padding(bottom = 28.dp))
        Txt(title, 24, font = Serif, align = TextAlign.Center)
        Txt(body, 14, P.mute, Modifier.padding(top = 6.dp, bottom = 20.dp), align = TextAlign.Center)
        if (button != null) Pill(button, filled = true, onClick = onClick)
    }
}

fun fmtRating(r: Float): String = if (r % 1f == 0f) r.toInt().toString() else r.toString()

fun Modifier.statusBarsPaddingCompat(): Modifier = this.statusBarsPadding().imePadding()
