package app.binder

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
import androidx.compose.foundation.layout.fillMaxHeight
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
                EmptyBlock("No lists yet", "Make one for books, movies, albums or a quick checklist.", "Create a list") {
                    nav.push(Route.NewList)
                }
            } else {
                when (o.home) {
                    HomeStyle.PINNED -> PinnedHome(vm, lists, pad, open, hold)
                    HomeStyle.BENTO -> BentoHome(vm, lists, pad, open, hold)
                    HomeStyle.TILES -> {
                        HomeTitle(lists.size)
                        Chips(listOf("All", "Media", "Checklists"), filter) { filter = it }
                        TilesHome(shown, pad, open, hold)
                    }
                    HomeStyle.ROWS -> {
                        HomeTitle(lists.size)
                        Chips(listOf("All", "Media", "Checklists"), filter) { filter = it }
                        RowsHome(vm, shown, pad, open, hold)
                    }
                    HomeStyle.STACKS -> {
                        HomeTitle(lists.size)
                        Chips(listOf("All", "Media", "Checklists"), filter) { filter = it }
                        StacksHome(shown, pad, open, hold)
                    }
                }
            }
        }
        if (!showsPlus(o.nav)) {
            RoundPlus(
                60.dp,
                Modifier.align(Alignment.BottomEnd).navigationBarsPadding()
                    .padding(end = 16.dp, bottom = (navHeight(o.nav) + 18).dp),
            ) { nav.push(Route.NewList) }
        }
    }
}

@Composable
private fun HomeTitle(count: Int) {
    Row(verticalAlignment = Alignment.Bottom) {
        Txt("Lists", 34, font = TitleFont)
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

// ----- Tiles -----

@Composable
private fun ColumnScope.TilesHome(
    shown: List<BinderList>, pad: Int, open: (BinderList) -> Unit, hold: (BinderList) -> Unit,
) {
    LazyVerticalGrid(
        GridCells.Fixed(2),
        Modifier.weight(1f),
        contentPadding = PaddingValues(bottom = pad.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(shown, key = { it.id }) { l ->
            Tile(l.name, l.countLabel(), l.stack(), false, onHold = { hold(l) }) { open(l) }
        }
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
            val a = l.stack()
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(P.surface)
                    .tapHold({ open(l) }, { hold(l) })
                    .padding(start = 12.dp, end = if (reorder) 4.dp else 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(P.bg)) {
                    ArtBox(
                        a[0],
                        Modifier.offset(9.dp, 10.dp).width(20.dp).height(30.dp).rotate(-8f)
                            .border(1.5.dp, P.bg, RoundedCornerShape(6.dp)),
                        5.dp, 120,
                    )
                    ArtBox(
                        a[1],
                        Modifier.offset(20.dp, 11.dp).width(20.dp).height(30.dp).rotate(7f)
                            .border(1.5.dp, P.bg, RoundedCornerShape(6.dp)),
                        5.dp, 120,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Txt(l.name, 15, weight = FontWeight.Medium, maxLines = 1)
                    Txt("${l.items.size} items · ${l.doneCount()} done", 12, P.mute, maxLines = 1)
                    Progress(l.progress(), Modifier.padding(top = 6.dp))
                }
                if (reorder) Grip() else Txt("${(l.progress() * 100).roundToInt()}%", 13, P.mute)
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
    LazyColumn(Modifier.weight(1f), state = state, contentPadding = PaddingValues(bottom = pad.dp)) {
        items(shown, key = { it.id }) { l -> ListRow(vm, l, rs, open, hold) }
    }
}

// ----- Pinned (cards on top, rows below) -----

@Composable
private fun PinCard(l: BinderList, open: () -> Unit, hold: () -> Unit) {
    val a = l.stack()
    Box(
        Modifier.width(150.dp).height(220.dp).clip(RoundedCornerShape(26.dp)).background(P.surface)
            .tapHold(open, hold)
    ) {
        ArtBox(
            a[1],
            Modifier.offset(58.dp, 24.dp).width(68.dp).height(96.dp).rotate(7f)
                .border(2.dp, P.surface, RoundedCornerShape(10.dp)),
            10.dp, 300,
        )
        ArtBox(
            a[0],
            Modifier.offset(16.dp, 16.dp).width(68.dp).height(96.dp).rotate(-6f)
                .border(2.dp, P.surface, RoundedCornerShape(10.dp)),
            10.dp, 300,
        )
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            Txt("${l.items.size}", 34, weight = FontWeight.Medium, ls = (-0.02).em)
            Txt(l.name, 15, weight = FontWeight.Medium, maxLines = 1)
            Txt("${l.doneCount()} done", 12, P.mute)
            Progress(l.progress(), Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun ColumnScope.PinnedHome(
    vm: BinderViewModel, lists: List<BinderList>, pad: Int,
    open: (BinderList) -> Unit, hold: (BinderList) -> Unit,
) {
    val state = rememberLazyListState()
    val rs = remember(state) { ReorderState(state) { f, t -> vm.moveListTo(f, t) } }
    val pinned = lists.filter { it.pinned }
    LazyColumn(Modifier.weight(1f), state = state, contentPadding = PaddingValues(bottom = pad.dp)) {
        item(key = "~cap1") { SectionCap("PINNED", if (pinned.isEmpty()) "" else "swipe") }
        item(key = "~pins") {
            if (pinned.isEmpty()) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(P.surface).padding(16.dp)) {
                    Txt("Press and hold a list below, then choose “Pin to top” to keep it here.", 13, P.mute)
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
                    items(pinned, key = { it.id }) { l -> PinCard(l, { open(l) }, { hold(l) }) }
                }
            }
        }
        item(key = "~cap2") { SectionCap("ALL LISTS", "${lists.size}") }
        items(lists, key = { it.id }) { l -> ListRow(vm, l, rs, open, hold) }
    }
}

// ----- Stacks -----

@Composable
private fun ColumnScope.StacksHome(
    shown: List<BinderList>, pad: Int, open: (BinderList) -> Unit, hold: (BinderList) -> Unit,
) {
    LazyColumn(
        Modifier.weight(1f),
        contentPadding = PaddingValues(top = 6.dp, bottom = pad.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(shown, key = { it.id }) { l ->
            val arts = if (l.items.isEmpty()) l.stack() else l.items.take(4).map { it.art() }
            val more = l.items.size - 4
            val slots = arts.size + if (more > 0) 1 else 0
            val rot = listOf(-4f, 2f, -2f, 3f, 3f)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).tapHold({ open(l) }, { hold(l) }).padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.width((78 + 54 * (slots - 1)).dp).height(118.dp)) {
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
                    ArtBox(
                        big.stack()[0],
                        Modifier.offset(18.dp, 18.dp).width(84.dp).height(118.dp).rotate(-6f)
                            .border(2.dp, P.surface, RoundedCornerShape(12.dp)),
                        12.dp, 300,
                    )
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
                            Txt("across all lists", 12, Color(0xFF111111))
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
                CoverView(item.title, item.art(), Modifier.width(48.dp), px = 200, showTitle = false)
                Column(Modifier.weight(1f)) {
                    Txt(item.title, 16, maxLines = 2)
                    if (item.sub.isNotBlank()) Txt(item.sub, 13, P.mute, maxLines = 1)
                    Txt(l.kind.stages[item.stage], 12, if (item.stage > 0) Coral else P.mute)
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
                    CoverView(item.title, item.art(), Modifier.width(38.dp), px = 150, showTitle = false)
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

@Composable
private fun ItemsBody(
    vm: BinderViewModel, l: BinderList, layout: Layout, shown: List<Item>, handles: Boolean, pad: Int,
    open: (Item) -> Unit, hold: (Item) -> Unit,
) {
    val state = rememberLazyListState()
    val rs = remember(state, l.id) { ReorderState(state) { f, t -> vm.moveItemTo(l.id, f, t) } }
    val rowKind = l.kind.isChecklist || layout == Layout.LIST

    when {
        shown.isEmpty() -> Column(Modifier.fillMaxSize()) {
            if (l.items.isEmpty()) EmptyBlock("Nothing here yet", "Add your first one with the + button.")
            else EmptyBlock("Nothing in this view", "Try another filter above.")
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
                    CoverView(item.title, item.art(), Modifier.fillMaxWidth(), size = 16, px = 600)
                    Txt(item.title, 15, weight = FontWeight.Medium, modifier = Modifier.padding(top = 10.dp), maxLines = 2)
                    if (item.sub.isNotBlank()) Txt(item.sub, 12, P.mute, maxLines = 1)
                    Txt(l.kind.stages[item.stage], 12, if (item.stage > 0) Coral else P.mute, Modifier.padding(top = 2.dp))
                }
            }
        }
        else -> LazyVerticalGrid(
            GridCells.Fixed(3),
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = pad.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(shown, key = { it.id }) { item ->
                Column(Modifier.tapHold({ open(item) }, { hold(item) })) {
                    CoverView(item.title, item.art(), Modifier.fillMaxWidth(), px = 400)
                    Txt(item.title, 13, modifier = Modifier.padding(top = 6.dp), weight = FontWeight.Medium, maxLines = 2, lh = 15.sp)
                    if (item.sub.isNotBlank()) Txt(item.sub, 12, P.mute, maxLines = 1)
                }
            }
        }
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
        RoundPlus(52.dp, onHold = onFull) { submit() }
    }
}

@Composable
private fun BoxScope.ViewSheet(vm: BinderViewModel, l: BinderList, onClose: () -> Unit) {
    SheetFrame(onClose) {
        Txt("View", 20, font = TitleFont)
        Cap("LAYOUT")
        FlowChips(l.kind.layouts.map { it.label }, l.kind.layouts.indexOf(l.layout)) { vm.setLayout(l.id, l.kind.layouts[it]) }
        Cap("SORT")
        FlowChips(Sort.entries.map { it.label }, l.sort.ordinal) { vm.setSort(l.id, Sort.entries[it]) }
        Txt("Swipe a row right to move it to the next status, left to delete. Press and hold for more.", 12, P.mute, Modifier.padding(top = 14.dp, bottom = 16.dp))
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
    val layout = if (l.layout in l.kind.layouts) l.layout else l.kind.layouts.first()
    val shown = l.sorted().filter { stage < 0 || it.stage == stage }
    val handles = vm.reorder && l.sort == Sort.MANUAL
    val addBar = l.kind.isChecklist || o.add == AddStyle.BAR
    val open = { item: Item -> nav.push(Route.ItemEdit(l.id, item.id)) }
    val hold = { item: Item -> vm.sheet = itemMenu(vm, l, item) }
    val openEditor = { nav.push(Route.ItemEdit(l.id, null)) }
    val openQuick = { vm.quick = l.id }

    Box(Modifier.fillMaxSize()) {
        Screen {
            TopBar(
                left = { CircleBtn(Ic.Back, "Back") { nav.pop() } },
                right = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircleBtn(Ic.Grid, "View options") { showView = true }
                        CircleBtn(Ic.Dots, "List options") {
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
                                    add(Act("Delete list", danger = true) { vm.deleteList(l); nav.pop() })
                                },
                            )
                        }
                    }
                },
            )
            Txt(l.name, 34, font = TitleFont, maxLines = 2, lh = 38.sp, modifier = Modifier.padding(top = 8.dp))
            Txt(l.countLabel(), 14, P.mute, Modifier.padding(top = 2.dp, bottom = 8.dp))
            if (layout == Layout.SECTIONS || l.kind.isChecklist) Progress(l.progress(), Modifier.padding(bottom = 4.dp))
            Chips(listOf("All") + l.kind.stages, stage + 1) { stage = it - 1 }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                ItemsBody(vm, l, layout, shown, handles, if (addBar) 24 else 100, open, hold)
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
            RoundPlus(
                60.dp,
                Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 16.dp, bottom = 18.dp),
                onHold = { if (o.add == AddStyle.SHEET) openEditor() else openQuick() },
            ) { if (o.add == AddStyle.SHEET) openQuick() else openEditor() }
        }
        if (showView) ViewSheet(vm, l) { showView = false }
    }
}

// ───────────────────────── 3 · Item (add / edit) ─────────────────────────

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

    Column(
        Modifier.fillMaxSize().statusBarsPadding().imePadding().padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        TopBar(
            left = { CircleBtn(Ic.Back, "Back") { leave() } },
            right = { Pill("Done", filled = true) { leave() } },
        )
        Txt(l.name.uppercase(), 11, P.mute, ls = 0.08.em)
        if (collage) {
            Txt(shownTitle, 34, font = TitleFont, align = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 14.dp), maxLines = 3, lh = 38.sp)
            Box(
                Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp).width(72.dp).height(4.dp)
                    .rotate(-2f).clip(CircleShape).background(Coral)
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CoverView(shownTitle, art, Modifier.width(112.dp).rotate(-4f), size = 15, px = 600)
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
        } else {
            Row(
                Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                CoverView(shownTitle, art, Modifier.width(110.dp).rotate(-4f), size = 15, px = 600)
                Column(Modifier.weight(1f)) {
                    Txt(shownTitle, 26, font = TitleFont, maxLines = 3, lh = 29.sp)
                    if (sub.isNotBlank()) Txt(sub, 14, P.mute, Modifier.padding(top = 2.dp), maxLines = 1)
                    if (l.kind.hasRating) Stars(rating) { rating = it }
                }
            }
        }
        Seg(l.kind.stages, stage, Modifier.padding(vertical = 14.dp)) { stage = it }
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
