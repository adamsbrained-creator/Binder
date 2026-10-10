package app.binder

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// ───────────────────────── menus (press and hold) ─────────────────────────

fun listMenu(vm: BinderViewModel, l: BinderList): SheetData = SheetData(
    l.name,
    buildList {
        add(Act(if (l.pinned) "Unpin" else "Pin to top") { vm.togglePin(l.id) })
        add(Act("Rename") { vm.rename = l })
        add(Act("Move up") { vm.moveList(l.id, -1) })
        add(Act("Move down") { vm.moveList(l.id, 1) })
        if (vm.opts.home == HomeStyle.ROWS || vm.opts.home == HomeStyle.PINNED) {
            add(Act("Reorder by dragging") { vm.reorder = true })
        }
        add(Act("Delete", danger = true) { vm.deleteList(l) })
    },
)

fun itemMenu(vm: BinderViewModel, l: BinderList, item: Item): SheetData = SheetData(
    item.title,
    buildList {
        add(Act("Edit") { vm.nav.push(Route.ItemEdit(l.id, item.id)) })
        l.kind.stages.forEachIndexed { i, s ->
            if (i != item.stage) add(Act("Mark as “$s”") { vm.setStage(l.id, item, i) })
        }
        add(Act("Delete", danger = true) { vm.deleteItem(l.id, item) })
    },
)

// ───────────────────────── 1 · Home ─────────────────────────

@Composable
fun HomeScreen(vm: BinderViewModel) {
    val o = vm.opts
    val nav = vm.nav
    var filter by rememberSaveable { mutableStateOf(0) }
    val lists = vm.lists
    val shown = lists.filter {
        when (filter) {
            1 -> !it.kind.isChecklist
            2 -> it.kind.isChecklist
            else -> true
        }
    }
    val pad = navHeight(o.nav) + if (showsPlus(o.nav)) 24 else 96
    val open = { l: BinderList -> nav.push(Route.Lst(l.id)) }
    val hold = { l: BinderList -> vm.sheet = listMenu(vm, l) }

    Box(Modifier.fillMaxSize()) {
        Screen(rail = true) {
            AppHeader(vm)
            TopTabs(vm, Route.Home)
            if (lists.isEmpty()) {
                EmptyBlock(
                    "No ${o.listsLower} yet",
                    "Make one for books, movies, albums or a quick checklist.",
                    "Create a ${o.listLower}",
                ) { nav.push(Route.NewList) }
            } else {
                when (o.home) {
                    HomeStyle.PINNED -> PinnedHome(vm, lists, pad, open, hold)
                    HomeStyle.BENTO -> BentoHome(vm, lists, pad, open, hold)
                    HomeStyle.TILES -> {
                        HomeTitle(o.lists, lists.size)
                        Chips(listOf("All", "Media", "Checklists"), filter) { filter = it }
                        TilesHome(shown, pad, open, hold)
                    }
                    HomeStyle.ROWS -> {
                        HomeTitle(o.lists, lists.size)
                        Chips(listOf("All", "Media", "Checklists"), filter) { filter = it }
                        RowsHome(vm, shown, pad, open, hold)
                    }
                    HomeStyle.STACKS -> {
                        HomeTitle(o.lists, lists.size)
                        Chips(listOf("All", "Media", "Checklists"), filter) { filter = it }
                        StacksHome(vm, shown, pad, open, hold)
                    }
                }
            }
        }
        if (!showsPlus(o.nav)) {
            MenuFab(vm, bottom = navHeight(o.nav), onAdd = { nav.push(Route.NewList) })
        }
    }
}

@Composable
private fun HomeTitle(title: String, count: Int) {
    Row(verticalAlignment = Alignment.Bottom) {
        Txt(title, 34, font = TitleFont)
        Txt("$count", 14, P.mute, Modifier.padding(start = 8.dp, bottom = 6.dp))
    }
}

@Composable
private fun SectionCap(left: String, right: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Txt(left, 11, P.mute, ls = 0.08.em)
        Txt(right, 13, P.mute)
    }
}

/** A list as a small card: how many things, its name and a progress bar. No pictures. */
@Composable
private fun ListCard(l: BinderList, modifier: Modifier, open: () -> Unit, hold: () -> Unit) {
    Column(
        modifier.height(120.dp).clip(RoundedCornerShape(24.dp)).background(P.surface)
            .tapHold(open, hold).padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Txt("${l.items.size}", 26, weight = FontWeight.Medium, ls = (-0.02).em)
        Column {
            Txt(l.name, 15, weight = FontWeight.Medium, maxLines = 1)
            Progress(l.progress(), Modifier.padding(top = 8.dp))
        }
    }
}

// ----- Tiles -----

@Composable
private fun ColumnScope.TilesHome(
    shown: List<BinderList>, pad: Int, open: (BinderList) -> Unit, hold: (BinderList) -> Unit,
) {
    LazyVerticalGrid(
        GridCells.Fixed(2),
        Modifier.weight(1f),
        contentPadding = PaddingValues(top = 6.dp, bottom = pad.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(shown, key = { it.id }) { l -> ListCard(l, Modifier.fillMaxWidth(), { open(l) }, { hold(l) }) }
    }
}

// ----- Rows (with swipe and drag) -----

@Composable
private fun ListRow(
    vm: BinderViewModel, l: BinderList, rs: ReorderState,
    open: (BinderList) -> Unit, hold: (BinderList) -> Unit,
) {
    val reorder = vm.reorder
    Box(Modifier.padding(bottom = 8.dp).reorderRow(rs, l.id, reorder)) {
        SwipeRow(
            enabled = !reorder,
            rightLabel = if (l.pinned) "Unpin" else "Pin to top",
            leftLabel = "Delete",
            onRight = { vm.togglePin(l.id) },
            onLeft = { vm.deleteList(l) },
        ) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(P.surface)
                    .tapHold({ open(l) }, { hold(l) })
                    .padding(start = 14.dp, end = if (reorder) 4.dp else 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                ProgressRing(l.progress(), 44.dp)
                Column(Modifier.weight(1f)) {
                    Txt(l.name, 15, weight = FontWeight.Medium, maxLines = 1)
                    Txt("${l.items.size} items · ${l.doneCount()} done", 12, P.mute, maxLines = 1)
                }
                if (reorder) Grip()
            }
        }
    }
}

@Composable
private fun ColumnScope.RowsHome(
    vm: BinderViewModel, shown: List<BinderList>, pad: Int,
    open: (BinderList) -> Unit, hold: (BinderList) -> Unit,
) {
    val state = rememberLazyListState()
    val rs = remember(state) { ReorderState(state) { f, t -> vm.moveListTo(f, t) } }
    LazyColumn(Modifier.weight(1f), state = state, contentPadding = PaddingValues(top = 6.dp, bottom = pad.dp)) {
        items(shown, key = { it.id }) { l -> ListRow(vm, l, rs, open, hold) }
    }
}

// ----- Pinned (small cards on top, rows below) -----

@Composable
private fun ColumnScope.PinnedHome(
    vm: BinderViewModel, lists: List<BinderList>, pad: Int,
    open: (BinderList) -> Unit, hold: (BinderList) -> Unit,
) {
    val o = vm.opts
    val state = rememberLazyListState()
    val rs = remember(state) { ReorderState(state) { f, t -> vm.moveListTo(f, t) } }
    val pinned = lists.filter { it.pinned }
    val unpinned = lists.filter { !it.pinned }
    // the dashed slot: choose a list to pin
    val pinOne = {
        vm.sheet = SheetData(
            "Pin a ${o.listLower}",
            unpinned.map { u -> Act(u.name) { vm.togglePin(u.id) } },
        )
    }
    LazyColumn(Modifier.weight(1f), state = state, contentPadding = PaddingValues(bottom = pad.dp)) {
        item(key = "~cap1") { SectionCap("PINNED", if (pinned.isEmpty()) "" else "swipe") }
        item(key = "~pins") {
            if (pinned.isEmpty() && !o.emptySlots) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(P.surface).padding(16.dp)) {
                    Txt("Press and hold a ${o.listLower} below, then choose “Pin to top” to keep it here.", 13, P.mute)
                }
            } else {
                // lets the cards run to the edges of the screen
                LazyRow(
                    Modifier.layout { m, c ->
                        val extra = 16.dp.roundToPx()
                        val p = m.measure(c.copy(maxWidth = c.maxWidth + extra * 2))
                        layout(c.maxWidth, p.height) { p.place(-extra, 0) }
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(pinned, key = { it.id }) { l ->
                        ListCard(l, Modifier.width(140.dp), { open(l) }, { hold(l) })
                    }
                    if (o.emptySlots && unpinned.isNotEmpty()) {
                        item(key = "~slot") {
                            DashedSlot(Modifier.width(110.dp).height(120.dp), 24.dp) { pinOne() }
                        }
                    }
                }
            }
        }
        item(key = "~cap2") { SectionCap("ALL ${o.lists.uppercase()}", "${lists.size}") }
        items(lists, key = { it.id }) { l -> ListRow(vm, l, rs, open, hold) }
    }
}

// ----- Stacks (real covers fanned out; empty lists show a dashed slot) -----

@Composable
private fun ColumnScope.StacksHome(
    vm: BinderViewModel, shown: List<BinderList>, pad: Int, open: (BinderList) -> Unit, hold: (BinderList) -> Unit,
) {
    LazyColumn(
        Modifier.weight(1f),
        contentPadding = PaddingValues(top = 6.dp, bottom = pad.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(shown, key = { it.id }) { l ->
            val arts = l.items.take(4).map { it.art() }
            val more = l.items.size - 4
            val slots = maxOf(1, arts.size + if (more > 0) 1 else 0)
            val rot = listOf(-4f, 2f, -2f, 3f, 3f)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).tapHold({ open(l) }, { hold(l) }).padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.width((78 + 54 * (slots - 1)).dp).height(118.dp)) {
                    if (arts.isEmpty()) {
                        Box(
                            Modifier.offset(y = 5.dp).width(78.dp).height(108.dp).clip(RoundedCornerShape(14.dp))
                                .border(2.dp, P.line, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Ic.Plus, null, tint = P.mute, modifier = Modifier.size(22.dp)) }
                    }
                    arts.forEachIndexed { i, a ->
                        ArtBox(
                            a,
                            Modifier.offset(x = (54 * i).dp, y = 5.dp).width(78.dp).height(108.dp).rotate(rot[i])
                                .border(2.dp, P.bg, RoundedCornerShape(14.dp)),
                            14.dp, 300,
                        )
                    }
                    if (more > 0) {
                        Box(
                            Modifier.offset(x = (54 * arts.size).dp, y = 5.dp).width(78.dp).height(108.dp).rotate(3f)
                                .clip(RoundedCornerShape(14.dp)).background(P.surface)
                                .border(2.dp, P.bg, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Txt("+$more", 22, weight = FontWeight.Medium) }
                    }
                }
                Txt(l.name, 22, font = TitleFont, modifier = Modifier.padding(top = 10.dp), align = TextAlign.Center)
                Txt("${l.items.size} items · ${dateLabel(l.activity())}", 12, P.mute, Modifier.padding(top = 2.dp))
            }
        }
    }
}

// ----- Bento -----

@Composable
private fun BentoSmall(l: BinderList, modifier: Modifier, open: () -> Unit, hold: () -> Unit) {
    Box(
        modifier.height(110.dp).clip(RoundedCornerShape(24.dp)).background(P.surface)
            .tapHold(open, hold).padding(14.dp)
    ) {
        Column(Modifier.align(Alignment.BottomStart)) {
            Txt(l.name, 15, weight = FontWeight.Medium, maxLines = 1)
            Txt(l.countLabel(), 12, P.mute, Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
private fun ColumnScope.BentoHome(
    vm: BinderViewModel, lists: List<BinderList>, pad: Int,
    open: (BinderList) -> Unit, hold: (BinderList) -> Unit,
) {
    val big = lists[0]
    val second = lists.getOrNull(1)
    val totalDone = lists.sumOf { it.doneCount() }
    val next = lists.firstNotNullOfOrNull { l ->
        if (l.kind.isChecklist) null else l.items.firstOrNull { it.stage == 0 }?.let { l to it }
    }
    LazyColumn(
        Modifier.weight(1f),
        contentPadding = PaddingValues(top = 6.dp, bottom = pad.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "~top") {
            Row(Modifier.fillMaxWidth().height(230.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(24.dp)).background(P.surface)
                        .tapHold({ open(big) }, { hold(big) })
                ) {
                    val first = big.items.firstOrNull()
                    if (first != null) {
                        ArtBox(
                            first.art(),
                            Modifier.offset(18.dp, 18.dp).width(84.dp).height(118.dp).rotate(-6f)
                                .border(2.dp, P.surface, RoundedCornerShape(12.dp)),
                            12.dp, 300,
                        )
                    }
                    Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                        Txt("${big.items.size}", 44, font = TitleFont, ls = (-0.02).em)
                        Txt(big.name, 15, weight = FontWeight.Medium, maxLines = 1)
                        Txt("${big.doneCount()} done", 12, P.mute)
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Coral).padding(14.dp)) {
                        Column(Modifier.align(Alignment.BottomStart)) {
                            Txt("$totalDone", 40, Color(0xFF111111), font = TitleFont, ls = (-0.02).em)
                            Txt("done", 15, Color(0xFF111111), weight = FontWeight.Medium)
                            Txt("across all ${vm.opts.listsLower}", 12, Color(0xFF111111))
                        }
                    }
                    if (second != null) {
                        BentoSmall(second, Modifier.weight(1f).fillMaxWidth(), { open(second) }, { hold(second) })
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        if (next != null) {
            item(key = "~next") {
                val (nl, ni) = next
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(P.surface)
                        .tapHold({ open(nl) }, { hold(nl) }).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ArtBox(ni.art(), Modifier.width(40.dp).height(56.dp), 8.dp, 200)
                    Column(Modifier.weight(1f)) {
                        Txt("NEXT UP", 11, P.mute, ls = 0.08.em)
                        Txt(ni.title, 15, weight = FontWeight.Medium, maxLines = 1)
                    }
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).clickable { vm.advance(nl.id, ni) },
                        contentAlignment = Alignment.Center,
                    ) { Box(Modifier.size(26.dp).border(2.dp, P.mute, CircleShape)) }
                }
            }
        }
        items(lists.drop(2).chunked(2)) { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { l -> BentoSmall(l, Modifier.weight(1f), { open(l) }, { hold(l) }) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ----- Pinned / recently edited (from the menu) -----

@Composable
fun ListsScreen(vm: BinderViewModel, mode: Int) {
    val o = vm.opts
    val nav = vm.nav
    val all = vm.lists
    val shown = if (mode == 0) all.filter { it.pinned } else all.sortedByDescending { it.activity() }.take(12)
    val state = rememberLazyListState()
    val rs = remember(state, mode) { ReorderState(state) { f, t -> if (mode == 0) vm.moveListTo(f, t) } }
    Box(Modifier.fillMaxSize()) {
        Screen {
            Header2(
                if (mode == 0) "Pinned" else "Recently edited",
                "${shown.size} ${if (shown.size == 1) o.listLower else o.listsLower}",
                onBack = { nav.pop() },
            )
            if (shown.isEmpty()) {
                EmptyBlock(
                    if (mode == 0) "Nothing pinned" else "Nothing yet",
                    if (mode == 0) "Press and hold a ${o.listLower}, then choose “Pin to top”." else "Make a ${o.listLower} to see it here.",
                )
            } else {
                LazyColumn(Modifier.weight(1f), state = state, contentPadding = PaddingValues(top = 6.dp, bottom = 100.dp)) {
                    items(shown, key = { it.id }) { l ->
                        ListRow(vm, l, rs, { nav.push(Route.Lst(it.id)) }, { vm.sheet = listMenu(vm, it) })
                    }
                }
            }
        }
        MenuFab(vm, onAdd = { nav.push(Route.NewList) })
    }
}

// ───────────────────────── 2 · One list ─────────────────────────

private fun nextLabel(l: BinderList, item: Item): String {
    val last = l.kind.stages.lastIndex
    return "→ " + l.kind.stages[if (item.stage >= last) 0 else item.stage + 1]
}

private fun starText(r: Float): String = "★".repeat(r.toInt()) + if (r % 1f >= 0.5f) "½" else ""

@Composable
private fun StatusDot(done: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(26.dp).clip(CircleShape)
                .background(if (done) Coral else Color.Transparent)
                .border(2.dp, if (done) Coral else P.mute, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Ic.Check, null, tint = Color(0xFF111111), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun CheckRow(
    vm: BinderViewModel, l: BinderList, item: Item, handles: Boolean, rs: ReorderState,
    open: (Item) -> Unit, hold: (Item) -> Unit,
) {
    val done = item.stage == l.kind.stages.lastIndex
    Box(Modifier.reorderRow(rs, item.id, handles)) {
        SwipeRow(
            enabled = !handles,
            rightLabel = if (done) "To do" else "Done",
            leftLabel = "Delete",
            onRight = { vm.setStage(l.id, item, if (done) 0 else l.kind.stages.lastIndex) },
            onLeft = { vm.deleteItem(l.id, item) },
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StatusDot(done) { vm.setStage(l.id, item, if (done) 0 else l.kind.stages.lastIndex) }
                Txt(
                    item.title, 16, if (done) P.mute else P.fg,
                    Modifier.weight(1f).tapHold({ open(item) }, { hold(item) }).padding(horizontal = 6.dp, vertical = 12.dp),
                    strike = done,
                )
                if (handles) Grip()
            }
        }
    }
}

/** What an item shows while its details are still to be fetched. */
@Composable
private fun PendingCover(square: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier.aspectRatio(if (square) 1f else 2f / 3f).dashedBorder(12.dp).padding(6.dp),
        contentAlignment = Alignment.Center,
    ) { Txt("Details pending", 11, P.mute, align = TextAlign.Center) }
}

@Composable
private fun MediaRow(
    vm: BinderViewModel, l: BinderList, item: Item, handles: Boolean, rs: ReorderState,
    open: (Item) -> Unit, hold: (Item) -> Unit,
) {
    Box(Modifier.reorderRow(rs, item.id, handles)) {
        SwipeRow(
            enabled = !handles,
            rightLabel = nextLabel(l, item),
            leftLabel = "Delete",
            onRight = { vm.advance(l.id, item) },
            onLeft = { vm.deleteItem(l.id, item) },
        ) {
            Row(
                Modifier.fillMaxWidth().tapHold({ open(item) }, { hold(item) }).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.detailsPending && item.images.isEmpty()) PendingCover(l.kind.square, Modifier.width(48.dp))
                else CoverView(item.title, item.art(), Modifier.width(48.dp), px = 200, showTitle = false, square = l.kind.square)
                Column(Modifier.weight(1f)) {
                    Txt(item.title, 16, maxLines = 2)
                    if (item.sub.isNotBlank()) Txt(item.sub, 13, P.mute, maxLines = 1)
                    Txt(
                        if (item.detailsPending) "Details pending" else l.kind.stages[item.stage], 12,
                        if (item.stage > 0 && !item.detailsPending) Coral else P.mute,
                    )
                }
                if (handles) Grip()
                else if (l.kind.hasRating && item.rating > 0f) Txt("★ ${fmtRating(item.rating)}", 13, Coral)
            }
        }
    }
}

@Composable
private fun SectionRow(
    vm: BinderViewModel, l: BinderList, item: Item, handles: Boolean, rs: ReorderState,
    open: (Item) -> Unit, hold: (Item) -> Unit,
) {
    val last = l.kind.stages.lastIndex
    val done = item.stage == last
    Box(Modifier.reorderRow(rs, item.id, handles)) {
        SwipeRow(
            enabled = !handles,
            rightLabel = nextLabel(l, item),
            leftLabel = "Delete",
            onRight = { vm.advance(l.id, item) },
            onLeft = { vm.deleteItem(l.id, item) },
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                StatusDot(done) { vm.setStage(l.id, item, if (done) 0 else last) }
                Row(
                    Modifier.weight(1f).tapHold({ open(item) }, { hold(item) }).padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoverView(item.title, item.art(), Modifier.width(38.dp), px = 150, showTitle = false, square = l.kind.square)
                    Column(Modifier.weight(1f)) {
                        Txt(item.title, 17, if (done) P.mute else P.fg, font = TitleFont, maxLines = 1)
                        if (item.sub.isNotBlank()) Txt(item.sub, 12, P.mute, maxLines = 1)
                    }
                    if (!handles && l.kind.hasRating && item.rating > 0f) Txt(starText(item.rating), 13, Coral)
                }
                if (handles) Grip()
            }
        }
    }
}

/** One cover in the grid, with whatever the list is set to show under it. */
@Composable
private fun GridCell(l: BinderList, item: Item, px: Int, open: (Item) -> Unit, hold: (Item) -> Unit) {
    val last = l.kind.stages.lastIndex
    val done = item.stage == last && !l.kind.isChecklist
    Column(Modifier.tapHold({ open(item) }, { hold(item) })) {
        if (item.detailsPending && item.images.isEmpty()) {
            PendingCover(l.kind.square, Modifier.fillMaxWidth())
        } else {
            CoverView(item.title, item.art(), Modifier.fillMaxWidth(), px = px, square = l.kind.square) {
                if (Under.PROGRESS in l.under && item.stage in 1 until last) {
                    Box(
                        Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(8.dp).height(4.dp)
                            .clip(CircleShape).background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(item.stage.toFloat() / last).background(Coral))
                    }
                }
                if (done) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(Coral),
                        contentAlignment = Alignment.Center,
                    ) { Icon(Ic.Check, null, tint = Color(0xFF111111), modifier = Modifier.size(14.dp)) }
                }
            }
        }
        if (Under.TITLE in l.under) {
            Txt(item.title, 13, modifier = Modifier.padding(top = 6.dp), weight = FontWeight.Medium, maxLines = 2, lh = 15.sp)
            if (item.sub.isNotBlank()) Txt(item.sub, 12, P.mute, maxLines = 1)
        }
        if (Under.RATING in l.under && l.kind.hasRating && item.rating > 0f) {
            Txt(starText(item.rating), 12, Coral, Modifier.padding(top = 2.dp))
        }
        if (Under.ADDED in l.under) Txt(dateLabel(item.added), 11, P.mute, Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun ItemsBody(
    vm: BinderViewModel, l: BinderList, layout: Layout, shown: List<Item>, handles: Boolean, pad: Int,
    addBar: Boolean, onAdd: () -> Unit, open: (Item) -> Unit, hold: (Item) -> Unit,
) {
    val o = vm.opts
    val state = rememberLazyListState()
    val rs = remember(state, l.id) { ReorderState(state) { f, t -> vm.moveItemTo(l.id, f, t) } }
    val rowKind = l.kind.isChecklist || layout == Layout.LIST

    when {
        shown.isEmpty() -> Column(Modifier.fillMaxSize()) {
            if (l.items.isEmpty()) {
                val hint = if (addBar) "Type below to add your first ${l.kind.single}"
                else "Hold the menu button to add your first ${l.kind.single}"
                if (o.emptySlots && !l.kind.isChecklist) {
                    val cols = if (layout == Layout.CARDS) 2 else l.columns
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DashedSlot(Modifier.weight(1f).aspectRatio(if (l.kind.square) 1f else 2f / 3f), 12.dp) { onAdd() }
                        repeat(cols - 1) { Spacer(Modifier.weight(1f)) }
                    }
                    Txt(hint, 14, P.mute, Modifier.fillMaxWidth().padding(top = 40.dp), align = TextAlign.Center)
                } else {
                    EmptyBlock("Nothing here yet", hint)
                }
            } else {
                EmptyBlock("Nothing in this view", "Try another filter above.")
            }
        }
        layout == Layout.SECTIONS -> LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(bottom = pad.dp)) {
            l.kind.stages.forEachIndexed { si, sname ->
                val group = shown.filter { it.stage == si }
                if (group.isNotEmpty()) {
                    item(key = "~sec$si") {
                        Row(
                            Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Txt(sname, 20, font = TitleFont)
                            Txt("${group.size}", 13, P.mute)
                        }
                    }
                    items(group, key = { it.id }) { item ->
                        if (l.kind.isChecklist) CheckRow(vm, l, item, handles, rs, open, hold)
                        else SectionRow(vm, l, item, handles, rs, open, hold)
                    }
                }
            }
        }
        rowKind -> LazyColumn(Modifier.fillMaxSize(), state = state, contentPadding = PaddingValues(bottom = pad.dp)) {
            items(shown, key = { it.id }) { item ->
                if (l.kind.isChecklist) CheckRow(vm, l, item, handles, rs, open, hold)
                else MediaRow(vm, l, item, handles, rs, open, hold)
            }
        }
        layout == Layout.SHELF -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = pad.dp)) {
            items(shown.chunked(4)) { row ->
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { item ->
                            CoverView(
                                item.title, item.art(),
                                Modifier.weight(1f).tapHold({ open(item) }, { hold(item) }), size = 10,
                                square = l.kind.square,
                            )
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(P.surface))
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
        layout == Layout.CARDS -> LazyVerticalGrid(
            GridCells.Fixed(2),
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = pad.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(shown, key = { it.id }) { item ->
                Column(
                    Modifier.clip(RoundedCornerShape(24.dp)).background(P.surface)
                        .tapHold({ open(item) }, { hold(item) }).padding(10.dp)
                ) {
                    if (item.detailsPending && item.images.isEmpty()) PendingCover(l.kind.square, Modifier.fillMaxWidth())
                    else CoverView(item.title, item.art(), Modifier.fillMaxWidth(), size = 16, px = 600, square = l.kind.square)
                    Txt(item.title, 15, weight = FontWeight.Medium, modifier = Modifier.padding(top = 10.dp), maxLines = 2)
                    if (item.sub.isNotBlank()) Txt(item.sub, 12, P.mute, maxLines = 1)
                    Txt(l.kind.stages[item.stage], 12, if (item.stage > 0) Coral else P.mute, Modifier.padding(top = 2.dp))
                }
            }
        }
        else -> LazyVerticalGrid(
            GridCells.Fixed(l.columns),
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = pad.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(shown, key = { it.id }) { item -> GridCell(l, item, if (l.columns == 2) 600 else 400, open, hold) }
        }
    }
}

@Composable
private fun BoxScope.ViewSheet(vm: BinderViewModel, l: BinderList, onClose: () -> Unit) {
    val unders = Under.entries
    SheetFrame(onClose) {
        Txt("View", 22, weight = FontWeight.Medium)
        Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
            Cap("LAYOUT")
            FlowChips(l.kind.layouts.map { it.label }, l.kind.layouts.indexOf(l.layout)) { vm.setLayout(l.id, l.kind.layouts[it]) }
            Cap("SORT")
            FlowChips(Sort.entries.map { it.label }, l.sort.ordinal) { vm.setSort(l.id, Sort.entries[it]) }
            if (!l.kind.isChecklist) {
                Cap("SHOW UNDER COVERS")
                MultiChips(unders.map { it.label }, unders.indices.filter { unders[it] in l.under }.toSet()) {
                    vm.toggleUnder(l.id, unders[it])
                }
                Cap("COLUMNS")
                Seg(listOf("2", "3", "4"), l.columns - 2) { vm.setColumns(l.id, it + 2) }
            }
            Txt("Swipe a row right to move it to the next status, left to delete. Press and hold for more.", 12, P.mute, Modifier.padding(top = 14.dp, bottom = 12.dp))
        }
        BigButton("Done") { onClose() }
    }
}

@Composable
fun ListScreen(vm: BinderViewModel, id: String) {
    val nav = vm.nav
    val o = vm.opts
    var stage by rememberSaveable { mutableStateOf(-1) } // -1 = all
    var showView by remember { mutableStateOf(false) }

    val l = vm.lists.firstOrNull { it.id == id }
    if (l == null) {
        LaunchedEffect(Unit) { nav.pop() }
        return
    }
    // items saved while offline get their details when the list opens (only if fetching is on)
    LaunchedEffect(id, o.fetch) { vm.retryPending(id) }

    val layout = if (l.layout in l.kind.layouts) l.layout else l.kind.layouts.first()
    val shown = l.sorted().filter { stage < 0 || it.stage == stage }
    val handles = vm.reorder && l.sort == Sort.MANUAL
    val addBar = l.kind.isChecklist || o.add == AddStyle.BAR
    val open = { item: Item -> nav.push(Route.ItemEdit(l.id, item.id)) }
    val hold = { item: Item -> vm.sheet = itemMenu(vm, l, item) }
    val openEditor = { nav.push(Route.ItemEdit(l.id, null)) }
    val openQuick = { vm.quick = l.id }
    val addDefault = { if (o.add == AddStyle.SHEET) openQuick() else openEditor() }

    Box(Modifier.fillMaxSize()) {
        Screen {
            Header2(
                l.name, l.countLabel(), onBack = { nav.pop() },
                right = {
                    CircleBtn(Ic.Grid, "View options") { showView = true }
                    CircleBtn(Ic.Dots, "${o.list} options") {
                        vm.sheet = SheetData(
                            l.name,
                            buildList {
                                add(Act(if (l.pinned) "Unpin" else "Pin to top") { vm.togglePin(l.id) })
                                add(Act("Rename") { vm.rename = l })
                                if (layout == Layout.LIST || layout == Layout.SECTIONS) {
                                    add(Act("Reorder by dragging") {
                                        if (l.sort != Sort.MANUAL) vm.setSort(l.id, Sort.MANUAL)
                                        vm.reorder = true
                                    })
                                }
                                add(Act("Delete ${o.listLower}", danger = true) { vm.deleteList(l); nav.pop() })
                            },
                        )
                    }
                },
            )
            Progress(l.progress(), Modifier.padding(top = 6.dp, bottom = 2.dp))
            Chips(listOf("All") + l.kind.stages, stage + 1) { stage = it - 1 }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                ItemsBody(vm, l, layout, shown, handles, if (addBar) 24 else 100, addBar, { addDefault() }, open, hold)
            }
            if (addBar) {
                AddBar(
                    if (l.kind.isChecklist) "Add an item…" else "Add a ${l.kind.single}…",
                    onAdd = { t -> vm.saveItem(l.id, Item(title = t)) },
                    onFull = { openEditor() },
                )
            }
        }
        if (!addBar) {
            MenuFab(
                vm, onAdd = { addDefault() },
                onAddHold = { if (o.add == AddStyle.SHEET) openEditor() else openQuick() },
            )
        }
        if (showView) ViewSheet(vm, l) { showView = false }
    }
}

@Composable
private fun AddBar(hint: String, onAdd: (String) -> Unit, onFull: () -> Unit) {
    var text by remember { mutableStateOf("") }
    fun submit() {
        if (text.isNotBlank()) {
            onAdd(text.trim())
            text = ""
        }
    }
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 8.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.weight(1f).height(52.dp).clip(CircleShape).background(P.surface).padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (text.isEmpty()) Txt(hint, 15, P.mute, maxLines = 1)
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(color = P.fg, fontSize = 15.sp, fontFamily = BodyFont),
                cursorBrush = SolidColor(Coral),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        RoundBtn(Ic.Plus, "Add", 52.dp, onHold = onFull) { submit() }
    }
}

// ───────────────────────── 3 · Item (add / edit) ─────────────────────────

private fun openLink(ctx: android.content.Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
fun ItemScreen(vm: BinderViewModel, listId: String, itemId: String?) {
    val l = vm.lists.firstOrNull { it.id == listId } ?: return
    val existing = l.items.firstOrNull { it.id == itemId }
    val collage = vm.opts.itemPage == ItemPage.COLLAGE

    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var sub by rememberSaveable { mutableStateOf(existing?.sub ?: "") }
    var stage by rememberSaveable { mutableStateOf(existing?.stage ?: 0) }
    var rating by rememberSaveable { mutableStateOf(existing?.rating ?: 0f) }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var cover by rememberSaveable { mutableStateOf(existing?.cover ?: -1) }
    var imgs by rememberSaveable { mutableStateOf(existing?.images?.joinToString("|") ?: "") }
    var left by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }
    val imgList = imgs.split("|").filter { it.isNotEmpty() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(8)) { uris ->
        if (uris.isNotEmpty()) {
            vm.importImages(uris) { names -> imgs = (imgList + names).joinToString("|") }
        }
    }

    // Changes are kept automatically when you leave (back button, Done, or the system back).
    fun leave() {
        if (left) return
        left = true
        if (title.isNotBlank()) {
            val base = existing ?: Item(title = "")
            vm.saveItem(
                listId,
                base.copy(
                    title = title.trim(), sub = sub.trim(), stage = stage, rating = rating,
                    note = note.trim(), cover = cover, images = imgList,
                ),
            )
        }
        vm.nav.pop()
    }
    androidx.activity.compose.BackHandler { leave() }

    val look = if (cover in Covers.indices) Covers[cover] else coverFor(title)
    val art = Art(look, imgList.firstOrNull())
    val shownTitle = title.ifBlank { "New ${l.kind.single}" }

    val ctx = LocalContext.current
    val info = listOf(existing?.year ?: "", existing?.genre ?: "", existing?.description ?: "")
        .filter { it.isNotBlank() && it.length <= 24 }.joinToString(" · ")
    val longText = existing?.description?.takeIf { it.length > 24 } ?: ""
    val link = existing?.sourceUrl ?: ""
    val src = Source.entries.firstOrNull { it.label == existing?.sourceName }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Header2(
            shownTitle, l.kind.label, onBack = { leave() },
            right = {
                if (existing == null) {
                    Pill("Done", filled = true) { leave() }
                } else {
                    if (link.isNotEmpty()) CircleBtn(Ic.Out, "Open link") { openLink(ctx, link) }
                    CircleBtn(Ic.Dots, "More") {
                        vm.sheet = SheetData(existing.title, listOf(Act("Delete", danger = true) { confirm = true }))
                    }
                }
            },
        )
        if (collage) {
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverView(shownTitle, art, Modifier.width(112.dp).rotate(-4f), size = 15, px = 600, square = l.kind.square)
                Column(Modifier.weight(1f).rotate(2f).clip(RoundedCornerShape(20.dp)).background(P.surface).padding(14.dp)) {
                    Txt(note.ifBlank { "Write a note below and it shows up here." }, 17, if (note.isBlank()) P.mute else P.fg, font = TitleFont, maxLines = 6)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.rotate(-3f).clip(CircleShape).background(Coral).padding(horizontal = 14.dp, vertical = 7.dp)) {
                    Txt(l.kind.stages[stage], 13, Color(0xFF111111), weight = FontWeight.Medium)
                }
                if (l.kind.hasRating) Stars(rating) { rating = it }
            }
        } else if (l.kind.square) {
            Column(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                CoverView(shownTitle, art, Modifier.width(168.dp), size = 15, px = 800, square = true)
                if (sub.isNotBlank()) Txt(sub, 20, weight = FontWeight.Medium, modifier = Modifier.padding(top = 14.dp), maxLines = 1)
                if (info.isNotEmpty()) Txt(info, 14, P.mute, Modifier.padding(top = 2.dp), maxLines = 1)
                if (l.kind.hasRating) Stars(rating) { rating = it }
            }
        } else {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                CoverView(shownTitle, art, Modifier.width(110.dp).rotate(-4f), size = 15, px = 600)
                Column(Modifier.weight(1f)) {
                    if (sub.isNotBlank()) Txt(sub, 16, weight = FontWeight.Medium, maxLines = 2)
                    if (info.isNotEmpty()) Txt(info, 14, P.mute, Modifier.padding(top = 2.dp), maxLines = 2)
                    if (l.kind.hasRating) Stars(rating) { rating = it }
                }
            }
        }
        Seg(l.kind.stages, stage, Modifier.padding(vertical = 14.dp)) { stage = it }
        if (existing != null && existing.detailsPending) {
            Txt(
                "Details pending. They are fetched when you open this ${vm.opts.listLower} with fetching turned on.",
                12, P.mute, Modifier.padding(bottom = 10.dp),
            )
        }
        if (existing != null && (existing.year.isNotEmpty() || existing.genre.isNotEmpty() || existing.sourceName.isNotEmpty())) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(P.surface).padding(horizontal = 16.dp, vertical = 4.dp)) {
                val rows = listOf("Released" to existing.year, "Genre" to existing.genre, "Source" to existing.sourceName)
                    .filter { it.second.isNotEmpty() }
                rows.forEachIndexed { i, (k, v) ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(P.line))
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Txt(k, 15)
                        Txt(v, 15, P.mute, Modifier.padding(start = 16.dp), maxLines = 1)
                    }
                }
            }
        }
        if (longText.isNotEmpty()) Txt(longText, 14, P.mute, Modifier.padding(top = 12.dp), lh = 20.sp)
        if (link.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp).height(52.dp).clip(CircleShape).background(P.surface)
                    .clickable { openLink(ctx, link) },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt(src?.openLabel ?: "Open link", 15, weight = FontWeight.Medium)
                Icon(Ic.Out, null, tint = P.fg, modifier = Modifier.padding(start = 8.dp).size(18.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Field("Title", title) { title = it }
        if (l.kind.subLabel.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Field(l.kind.subLabel, sub) { sub = it }
        }
        Spacer(Modifier.height(10.dp))
        Field("Notes", note, singleLine = false) { note = it }

        Cap("PHOTOS")
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            imgList.forEachIndexed { i, name ->
                Box(Modifier.size(width = 78.dp, height = 104.dp)) {
                    ArtBox(
                        Art(look, name),
                        Modifier.fillMaxSize().clickable {
                            if (i != 0) imgs = (listOf(name) + imgList.filter { it != name }).joinToString("|")
                        },
                        12.dp, 300,
                    )
                    if (i == 0) {
                        Box(
                            Modifier.align(Alignment.BottomStart).padding(6.dp).clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 8.dp, vertical = 3.dp)
                        ) { Txt("Cover", 10, Color.White) }
                    }
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(4.dp).size(26.dp).clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { imgs = imgList.filter { it != name }.joinToString("|") },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Ic.Close, "Remove photo", tint = Color.White, modifier = Modifier.size(14.dp)) }
                }
            }
            Box(
                Modifier.size(width = 78.dp, height = 104.dp).clip(RoundedCornerShape(12.dp))
                    .border(1.dp, P.line, RoundedCornerShape(12.dp))
                    .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Ic.Photo, "Add photos", tint = P.mute, modifier = Modifier.size(22.dp))
                    Txt("Add", 12, P.mute)
                }
            }
        }
        if (imgList.size > 1) Txt("Tap a photo to make it the cover.", 12, P.mute, Modifier.padding(top = 8.dp))

        if (imgList.isEmpty() && !l.kind.isChecklist) {
            Cap("COVER COLOUR")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape).border(2.dp, if (cover < 0) Coral else P.line, CircleShape)
                        .clickable { cover = -1 },
                    contentAlignment = Alignment.Center,
                ) { Txt("Auto", 10, P.mute) }
                Covers.forEachIndexed { i, c ->
                    Box(
                        Modifier.size(38.dp).clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(c.top, c.bottom)))
                            .border(2.dp, if (cover == i) Coral else Color.Transparent, CircleShape)
                            .clickable { cover = i }
                    )
                }
            }
        }
        if (existing != null) {
            Txt("Delete item", 15, Coral, Modifier.clickable { confirm = true }.padding(vertical = 28.dp))
        }
        Spacer(Modifier.height(40.dp))
    }

    if (confirm && existing != null) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = P.bg,
            title = { Txt("Delete “${existing.title}”?", 20) },
            confirmButton = {
                TextButton({
                    confirm = false
                    left = true
                    vm.deleteItem(listId, existing)
                    vm.nav.pop()
                }) { Txt("Delete", 15, Coral) }
            },
            dismissButton = { TextButton({ confirm = false }) { Txt("Cancel", 15, P.mute) } },
        )
    }
}
