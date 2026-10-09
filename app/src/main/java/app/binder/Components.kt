package app.binder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ───────── text & frames ─────────

@Composable
fun Txt(
    text: String,
    size: Int = 15,
    color: Color = P.fg,
    modifier: Modifier = Modifier,
    font: FontFamily = BodyFont,
    weight: FontWeight = FontWeight.Normal,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign? = null,
    lh: TextUnit = TextUnit.Unspecified,
    strike: Boolean = false,
    ls: TextUnit = TextUnit.Unspecified,
) {
    Text(
        text = text, modifier = modifier, color = color, fontSize = size.sp, fontFamily = font,
        fontWeight = weight, textAlign = align, lineHeight = lh, letterSpacing = ls, maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textDecoration = if (strike) TextDecoration.LineThrough else null,
    )
}

@Composable
fun Cap(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text, modifier = modifier.padding(top = 16.dp, bottom = 8.dp), color = P.mute,
        fontSize = 11.sp, letterSpacing = 0.08.em, fontFamily = BodyFont,
    )
}

/** Standard screen frame: edge-to-edge, with room for the status bar (and the side rail when it is on). */
@Composable
fun Screen(rail: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val startPad = if (rail && LocalOpts.current.nav == NavStyle.RAIL) 60.dp else 0.dp
    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().padding(start = 16.dp + startPad, end = 16.dp),
        content = content,
    )
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

// ───────── buttons & inputs ─────────

@Composable
fun CircleBtn(icon: ImageVector, desc: String, bg: Color = P.surface, tint: Color = P.fg, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(20.dp)) }
}

@Composable
fun IconBtn(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = P.fg, modifier = Modifier.size(22.dp))
    }
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
fun RoundPlus(size: Dp = 52.dp, modifier: Modifier = Modifier, onHold: (() -> Unit)? = null, onClick: () -> Unit) {
    Box(
        modifier.size(size).clip(CircleShape).background(Coral).tapHold(onClick, onHold),
        contentAlignment = Alignment.Center,
    ) { Icon(Ic.Plus, "Add", tint = Color(0xFF111111), modifier = Modifier.size(24.dp)) }
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

/** Chips that wrap onto several lines. Pass -1 as selected for "none selected". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowChips(options: List<String>, selected: Int, modifier: Modifier = Modifier, onSelect: (Int) -> Unit) {
    FlowRow(
        modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEachIndexed { i, o ->
            val on = i == selected
            Box(
                Modifier.clip(CircleShape).background(if (on) P.primary else P.surface)
                    .clickable { onSelect(i) }.padding(horizontal = 14.dp, vertical = 9.dp)
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
            textStyle = TextStyle(color = P.fg, fontSize = 18.sp, fontFamily = BodyFont),
            cursorBrush = SolidColor(Coral),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        )
    }
}

// ───────── gestures ─────────

/** Tap to open, press and hold for a menu. Gives a little haptic buzz and a pressed look. */
@Composable
fun Modifier.tapHold(onTap: () -> Unit, onHold: (() -> Unit)? = null): Modifier {
    val haptic = LocalHapticFeedback.current
    val tap by rememberUpdatedState(onTap)
    val hold by rememberUpdatedState(onHold)
    var pressed by remember { mutableStateOf(false) }
    return this
        .graphicsLayer { alpha = if (pressed) 0.6f else 1f }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    pressed = true
                    tryAwaitRelease()
                    pressed = false
                },
                onLongPress = {
                    val h = hold
                    if (h != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        h()
                    }
                },
                onTap = { tap() },
            )
        }
        .semantics { onClick { tap(); true } }
}

/** Swipe right / left on a row. Rows snap back after the swipe. */
@Composable
fun SwipeRow(
    enabled: Boolean,
    rightLabel: String,
    leftLabel: String,
    onRight: () -> Unit,
    onLeft: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }
    val x = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val rightCb by rememberUpdatedState(onRight)
    val leftCb by rememberUpdatedState(onLeft)
    Box(Modifier.fillMaxWidth()) {
        val toRight = x.value > 0f
        Box(
            Modifier.matchParentSize().clip(RoundedCornerShape(20.dp))
                .background(if (toRight) P.surface else Coral.copy(alpha = 0.18f))
                .padding(horizontal = 20.dp),
            contentAlignment = if (toRight) Alignment.CenterStart else Alignment.CenterEnd,
        ) {
            if (x.value != 0f) Txt(if (toRight) rightLabel else leftLabel, 14, if (toRight) P.fg else Coral)
        }
        Box(
            Modifier.offset { IntOffset(x.value.roundToInt(), 0) }
                .background(P.bg)
                .pointerInput(Unit) {
                    val limit = size.width * 0.42f
                    val trigger = size.width * 0.28f
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val v = x.value
                            if (v > trigger) rightCb() else if (v < -trigger) leftCb()
                            scope.launch { x.animateTo(0f) }
                        },
                        onDragCancel = { scope.launch { x.animateTo(0f) } },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            scope.launch { x.snapTo((x.value + amount).coerceIn(-limit, limit)) }
                        },
                    )
                }
        ) { content() }
    }
}

/** Drag-to-reorder for a LazyColumn. Rows grab by their right edge (where the grip icon is). */
class ReorderState(private val lazy: LazyListState, private val onMove: (String, String) -> Unit) {
    var key by mutableStateOf<String?>(null)
        private set
    var dy by mutableFloatStateOf(0f)
        private set
    private var waitFor: Int? = null

    fun start(k: String) {
        key = k
        dy = 0f
        waitFor = null
    }

    fun drag(delta: Float) {
        val k = key ?: return
        dy += delta
        val infos = lazy.layoutInfo.visibleItemsInfo
        val me = infos.firstOrNull { it.key == k } ?: return
        val w = waitFor
        if (w != null) {
            if (me.offset == w) return
            waitFor = null
        }
        val center = me.offset + me.size / 2f + dy
        val target = infos.firstOrNull { i ->
            val kk = i.key
            kk is String && kk != k && !kk.startsWith("~") && center >= i.offset && center <= i.offset + i.size
        } ?: return
        onMove(k, target.key as String)
        dy += (me.offset - target.offset).toFloat()
        waitFor = me.offset
    }

    fun end() {
        key = null
        dy = 0f
        waitFor = null
    }
}

fun Modifier.reorderRow(rs: ReorderState, id: String, enabled: Boolean): Modifier =
    if (!enabled) this else this
        .pointerInput(id) {
            val zone = 56.dp.toPx()
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (down.position.x >= size.width - zone) {
                    down.consume()
                    rs.start(id)
                    drag(down.id) { change ->
                        change.consume()
                        rs.drag(change.positionChange().y)
                    }
                    rs.end()
                }
            }
        }
        .zIndex(if (rs.key == id) 1f else 0f)
        .graphicsLayer { translationY = if (rs.key == id) rs.dy else 0f }

@Composable
fun Grip() {
    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Icon(Ic.Handle, "Drag to reorder", tint = P.mute, modifier = Modifier.size(20.dp))
    }
}

// ───────── covers ─────────

@Composable
fun ArtBox(art: Art, modifier: Modifier = Modifier, radius: Dp = 10.dp, px: Int = 300) {
    val bmp = rememberImage(art.img, px)
    Box(
        modifier.clip(RoundedCornerShape(radius))
            .background(Brush.verticalGradient(listOf(art.c.top, art.c.bottom)))
    ) {
        if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

@Composable
fun CoverView(
    title: String, art: Art, modifier: Modifier = Modifier, size: Int = 13, px: Int = 500, showTitle: Boolean = true,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(modifier.aspectRatio(2f / 3f).clip(shape).border(1.dp, P.line, shape)) {
        ArtBox(art, Modifier.fillMaxSize(), 12.dp, px)
        if (art.img == null && showTitle) {
            Txt(title, size, art.c.ink, Modifier.padding(8.dp), font = TitleFont, maxLines = 4, lh = (size + 2).sp)
        }
    }
}

/** The little fanned-out stack of three cards used as a list's icon. */
@Composable
fun CardStack(arts: List<Art>, scale: Float = 1f, modifier: Modifier = Modifier) {
    val rot = listOf(-9f, 2f, 10f)
    Box(modifier.height((60 * scale).dp).width((100 * scale).dp)) {
        arts.take(3).forEachIndexed { i, a ->
            ArtBox(
                a,
                Modifier.offset(x = (i * 26 * scale).dp).size((38 * scale).dp, (54 * scale).dp)
                    .rotate(rot[i]).border(2.dp, P.surface, RoundedCornerShape(8.dp)),
                8.dp, 200,
            )
        }
    }
}

@Composable
fun Tile(
    title: String, sub: String, arts: List<Art>, selected: Boolean,
    modifier: Modifier = Modifier, onHold: (() -> Unit)? = null, onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier.fillMaxWidth().height(158.dp).clip(shape).background(P.surface)
            .border(2.dp, if (selected) Coral else Color.Transparent, shape)
            .tapHold(onClick, onHold).padding(14.dp)
    ) {
        CardStack(arts, 1f, Modifier.align(Alignment.TopStart).padding(start = 4.dp, top = 4.dp))
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
fun Progress(frac: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(P.line)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(frac.coerceIn(0f, 1f)).background(Coral))
    }
}

@Composable
fun ColumnScope.EmptyBlock(title: String, body: String, button: String? = null, onClick: () -> Unit = {}) {
    Column(
        Modifier.weight(1f).fillMaxWidth().padding(bottom = 80.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CardStack(listOf(Art(Covers[0]), Art(Covers[3]), Art(Covers[1])), 1.3f, Modifier.padding(bottom = 28.dp))
        Txt(title, 24, font = TitleFont, align = TextAlign.Center)
        Txt(body, 14, P.mute, Modifier.padding(top = 6.dp, bottom = 20.dp), align = TextAlign.Center)
        if (button != null) Pill(button, filled = true, onClick = onClick)
    }
}

/** The app's mark: a coral card behind a dark one. */
@Composable
fun LogoMark(size: Dp = 40.dp) {
    Box(Modifier.size(size)) {
        Box(
            Modifier.align(Alignment.TopStart).padding(start = size * 0.04f, top = size * 0.08f)
                .size(size * 0.58f, size * 0.74f).rotate(-10f)
                .clip(RoundedCornerShape(size * 0.14f)).background(Coral)
        )
        Box(
            Modifier.align(Alignment.BottomEnd).padding(end = size * 0.04f, bottom = size * 0.04f)
                .size(size * 0.62f, size * 0.78f).rotate(6f)
                .clip(RoundedCornerShape(size * 0.15f)).background(P.fg).padding(size * 0.07f)
        ) {
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(size * 0.1f)).background(P.bg))
        }
    }
}

// ───────── bottom sheet ─────────

@Composable
fun BoxScope.SheetFrame(onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val st = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).pointerInput(Unit) { detectTapGestures { onClose() } })
    AnimatedVisibility(
        visibleState = st,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically { it },
    ) {
        val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        Column(
            Modifier.fillMaxWidth().clip(shape).background(P.bg).border(1.dp, P.line, shape)
                .pointerInput(Unit) { detectTapGestures { } }
                .navigationBarsPadding().imePadding()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 20.dp)
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(CircleShape).background(P.line))
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

fun fmtRating(r: Float): String = if (r % 1f == 0f) r.toInt().toString() else r.toString()

fun dateLabel(ms: Long): String = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(ms))
