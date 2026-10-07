package app.binder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ───────────────────────── 1 · Lists (home) ─────────────────────────

@Composable
fun HomeScreen(vm: BinderViewModel) {
    var filter by rememberSaveable { mutableStateOf(0) }
    val shown = vm.lists.filter {
        when (filter) {
            1 -> !it.kind.isChecklist
            2 -> it.kind.isChecklist
            else -> true
        }
    }
    val open = { l: BinderList -> vm.nav.push(Route.Lst(l.id)) }

    Screen {
        TopBar("binder", right = { CircleBtn(Ic.Plus, "New list") { vm.nav.push(Route.NewList) } })
        Row(verticalAlignment = Alignment.Bottom) {
            Txt("Lists", 34, font = Serif)
            Txt("${vm.lists.size}", 14, P.mute, Modifier.padding(start = 8.dp, bottom = 6.dp))
        }
        if (vm.lists.isEmpty()) {
            EmptyBlock("No lists yet", "Make one for books, movies, albums or a quick checklist.", "Create a list") {
                vm.nav.push(Route.NewList)
            }
        } else {
            Chips(listOf("All", "Media", "Checklists"), filter) { filter = it }
            if (vm.homeRows) {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 130.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(shown, key = { it.id }) { l -> RowTile(l) { open(l) } }
                }
            } else {
                LazyVerticalGrid(
                    GridCells.Fixed(2),
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 130.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(shown, key = { it.id }) { l -> Tile(l.name, l.countLabel(), l.stack(), false) { open(l) } }
                }
            }
        }
    }
}

@Composable
fun RowTile(l: BinderList, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(P.surface)
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Txt(l.name, 16, weight = FontWeight.Medium, maxLines = 1)
            Txt(l.countLabel(), 13, P.mute)
        }
        CardStack(l.stack(), 0.6f)
    }
}

// ───────────────────────── 2 · One list ─────────────────────────

@Composable
fun ListScreen(vm: BinderViewModel, id: String) {
    val nav = vm.nav
    var stage by rememberSaveable { mutableStateOf(-1) } // -1 = all
    var menu by remember { mutableStateOf(false) }
    var dlg by remember { mutableStateOf(0) } // 1 rename, 2 delete
    var draft by remember { mutableStateOf("") }

    val l = vm.lists.firstOrNull { it.id == id }
    if (l == null) {
        LaunchedEffect(Unit) { nav.pop() }
        return
    }
    val shown = l.items.filter { stage < 0 || it.stage == stage }
    val open = { item: Item -> nav.push(Route.ItemEdit(l.id, item.id)) }

    Screen {
        TopBar(
            left = { CircleBtn(Ic.Back, "Back") { nav.pop() } },
            right = {
                Box {
                    CircleBtn(Ic.Dots, "List options") { menu = true }
                    DropdownMenu(menu, { menu = false }, Modifier.background(P.surface)) {
                        DropdownMenuItem(text = { Txt("Rename", 15) }, onClick = { menu = false; dlg = 1 })
                        DropdownMenuItem(text = { Txt("Delete list", 15, Coral) }, onClick = { menu = false; dlg = 2 })
                    }
                }
            },
        )
        Txt(l.name, 34, font = Serif, maxLines = 2, lh = 38.sp, modifier = Modifier.padding(top = 8.dp))
        Txt(l.countLabel(), 14, P.mute, Modifier.padding(top = 2.dp, bottom = 12.dp))
        if (!l.kind.isChecklist) {
            Seg(Layout.entries.map { it.label }, l.layout.ordinal) { vm.setLayout(l.id, Layout.entries[it]) }
        }
        Chips(listOf("All") + l.kind.stages, stage + 1) { stage = it - 1 }
        if (l.kind.isChecklist) {
            Field("Add an item", draft, Modifier.padding(bottom = 8.dp), imeAction = ImeAction.Done, onDone = {
                if (draft.isNotBlank()) { vm.saveItem(l.id, Item(title = draft.trim())); draft = "" }
            }) { draft = it }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                l.items.isEmpty() && !l.kind.isChecklist ->
                    Column(Modifier.fillMaxSize()) { EmptyBlock("Nothing here yet", "Tap the + button to add your first one.") }
                shown.isEmpty() ->
                    Column(Modifier.fillMaxSize()) { EmptyBlock("Nothing in this view", "Try another filter above.") }
                l.kind.isChecklist -> LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(shown, key = { it.id }) { item ->
                        val done = item.stage == 1
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(44.dp).clip(CircleShape)
                                    .clickable { vm.saveItem(l.id, item.copy(stage = if (done) 0 else 1)) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier.size(26.dp).clip(CircleShape)
                                        .background(if (done) Coral else Color.Transparent)
                                        .border(2.dp, if (done) Coral else P.mute, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (done) Icon(Ic.Check, null, tint = Color(0xFF111111), modifier = Modifier.size(16.dp))
                                }
                            }
                            Txt(
                                item.title, 16, if (done) P.mute else P.fg,
                                Modifier.weight(1f).clickable { open(item) }.padding(horizontal = 6.dp, vertical = 12.dp),
                                strike = done,
                            )
                        }
                    }
                }
                l.layout == Layout.LIST -> LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                    items(shown, key = { it.id }) { item ->
                        Row(
                            Modifier.fillMaxWidth().clickable { open(item) }.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CoverView(item.title, item.look(), Modifier.width(48.dp), size = 9)
                            Column(Modifier.weight(1f)) {
                                Txt(item.title, 16, maxLines = 2)
                                if (item.sub.isNotBlank()) Txt(item.sub, 13, P.mute, maxLines = 1)
                                Txt(l.kind.stages[item.stage], 12, if (item.stage > 0) Coral else P.mute)
                            }
                            if (l.kind.hasRating && item.rating > 0f) Txt("★ ${fmtRating(item.rating)}", 13, Coral)
                        }
                    }
                }
                l.layout == Layout.SHELF -> LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                    items(shown.chunked(4)) { row ->
                        Column {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { item ->
                                    CoverView(item.title, item.look(), Modifier.weight(1f).clickable { open(item) }, size = 10)
                                }
                                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                            Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(P.surface))
                            Spacer(Modifier.height(18.dp))
                        }
                    }
                }
                else -> LazyVerticalGrid(
                    GridCells.Fixed(3),
                    contentPadding = PaddingValues(bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(shown, key = { it.id }) { item ->
                        Column(Modifier.clickable { open(item) }) {
                            CoverView(item.title, item.look(), Modifier.fillMaxWidth())
                            Txt(item.title, 13, modifier = Modifier.padding(top = 6.dp), weight = FontWeight.Medium, maxLines = 2, lh = 15.sp)
                            if (item.sub.isNotBlank()) Txt(item.sub, 12, P.mute, maxLines = 1)
                        }
                    }
                }
            }
            if (!l.kind.isChecklist) {
                Box(
                    Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(bottom = 18.dp)
                        .size(56.dp).clip(CircleShape).background(Coral)
                        .clickable { nav.push(Route.ItemEdit(l.id, null)) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Ic.Plus, "Add item", tint = Color(0xFF111111), modifier = Modifier.size(24.dp)) }
            }
        }
    }

    if (dlg == 1) {
        var nm by remember { mutableStateOf(l.name) }
        AlertDialog(
            onDismissRequest = { dlg = 0 }, containerColor = P.bg,
            title = { Txt("Rename list", 20) },
            text = { Field("List name", nm) { nm = it } },
            confirmButton = { TextButton({ if (nm.isNotBlank()) vm.renameList(l.id, nm.trim()); dlg = 0 }) { Txt("Save", 15, Coral) } },
            dismissButton = { TextButton({ dlg = 0 }) { Txt("Cancel", 15, P.mute) } },
        )
    }
    if (dlg == 2) {
        AlertDialog(
            onDismissRequest = { dlg = 0 }, containerColor = P.bg,
            title = { Txt("Delete “${l.name}”?", 20) },
            text = { Txt("The list and everything in it will be removed.", 14, P.mute) },
            confirmButton = { TextButton({ dlg = 0; vm.deleteList(l.id); nav.pop() }) { Txt("Delete", 15, Coral) } },
            dismissButton = { TextButton({ dlg = 0 }) { Txt("Cancel", 15, P.mute) } },
        )
    }
}

// ───────────────────────── 3 · Item (add / edit) ─────────────────────────

@Composable
fun ItemScreen(vm: BinderViewModel, listId: String, itemId: String?) {
    val l = vm.lists.firstOrNull { it.id == listId } ?: return
    val existing = l.items.firstOrNull { it.id == itemId }

    var title by rememberSaveable { mutableStateOf(existing?.title ?: "") }
    var sub by rememberSaveable { mutableStateOf(existing?.sub ?: "") }
    var stage by rememberSaveable { mutableStateOf(existing?.stage ?: 0) }
    var rating by rememberSaveable { mutableStateOf(existing?.rating ?: 0f) }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var cover by rememberSaveable { mutableStateOf(existing?.cover ?: -1) }
    var left by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf(false) }

    // Changes are kept automatically when you leave (back button, Done, or the system back).
    fun leave() {
        if (left) return
        left = true
        if (title.isNotBlank()) {
            val base = existing ?: Item(title = "")
            vm.saveItem(
                listId,
                base.copy(title = title.trim(), sub = sub.trim(), stage = stage, rating = rating, note = note.trim(), cover = cover),
            )
        }
        vm.nav.pop()
    }
    BackHandler { leave() }

    val look = if (cover in Covers.indices) Covers[cover] else coverFor(title)

    Column(Modifier.fillMaxSize().statusBarsPaddingCompat().padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
        TopBar(
            left = { CircleBtn(Ic.Back, "Back") { leave() } },
            right = { Pill("Done", filled = true) { leave() } },
        )
        Cap(l.name.uppercase(), Modifier.fillMaxWidth().padding(top = 0.dp))
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            CoverView(title.ifBlank { "Untitled" }, look, Modifier.width(110.dp).rotate(-4f), size = 15)
            Column(Modifier.weight(1f)) {
                Txt(title.ifBlank { "New ${l.kind.noun.dropLast(1)}" }, 26, font = Serif, maxLines = 3, lh = 29.sp)
                if (sub.isNotBlank()) Txt(sub, 14, P.mute, Modifier.padding(top = 2.dp), maxLines = 1)
                if (l.kind.hasRating) Stars(rating) { rating = it }
            }
        }
        Seg(l.kind.stages, stage, Modifier.padding(vertical = 8.dp)) { stage = it }
        Spacer(Modifier.height(8.dp))
        Field("Title", title) { title = it }
        if (l.kind.subLabel.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Field(l.kind.subLabel, sub) { sub = it }
        }
        Spacer(Modifier.height(10.dp))
        Field("Notes", note, singleLine = false) { note = it }
        if (!l.kind.isChecklist) {
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
            onDismissRequest = { confirm = false }, containerColor = P.bg,
            title = { Txt("Delete “${existing.title}”?", 20) },
            confirmButton = {
                TextButton({
                    confirm = false; left = true
                    vm.deleteItem(listId, existing.id); vm.nav.pop()
                }) { Txt("Delete", 15, Coral) }
            },
            dismissButton = { TextButton({ confirm = false }) { Txt("Cancel", 15, P.mute) } },
        )
    }
}

// ───────────────────────── 4 · New list ─────────────────────────

@Composable
fun NewListScreen(vm: BinderViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var k by rememberSaveable { mutableStateOf(0) }
    val kind = Kind.entries[k]

    Screen {
        TopBar("New list", left = { CircleBtn(Ic.Back, "Back") { vm.nav.pop() } })
        Field("List name", name, Modifier.padding(top = 6.dp)) { name = it }
        Cap("START FROM")
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Kind.entries.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { kd ->
                        Tile(kd.label, kd.hint, kd.stack.map { Covers[it] }, kd == kind, Modifier.weight(1f)) { k = kd.ordinal }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        BigButton("Create list", Modifier.navigationBarsPadding().padding(bottom = 18.dp)) {
            val id = vm.addList(name.trim().ifBlank { kind.label }, kind)
            vm.nav.replaceTop(Route.Lst(id))
        }
    }
}

// ───────────────────────── Search ─────────────────────────

@Composable
fun SearchScreen(vm: BinderViewModel) {
    var q by rememberSaveable { mutableStateOf("") }
    val hits: List<Pair<BinderList, Item>> = if (q.isBlank()) emptyList() else vm.lists.flatMap { l ->
        l.items.filter {
            it.title.contains(q, true) || it.sub.contains(q, true) || it.note.contains(q, true)
        }.map { l to it }
    }
    Screen {
        TopBar("Search")
        Field("Search every list", q) { q = it }
        Spacer(Modifier.height(8.dp))
        if (q.isBlank()) {
            EmptyBlock("Find anything", "Titles, authors, artists and notes from all your lists.")
        } else if (hits.isEmpty()) {
            EmptyBlock("No matches", "Nothing found for “$q”.")
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 130.dp)) {
                items(hits, key = { it.second.id }) { (l, item) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.nav.push(Route.ItemEdit(l.id, item.id)) }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverView(item.title, item.look(), Modifier.width(40.dp), size = 8)
                        Column(Modifier.weight(1f)) {
                            Txt(item.title, 16, maxLines = 1)
                            Txt("in ${l.name}", 13, P.mute, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────── More ─────────────────────────

@Composable
fun MoreScreen(vm: BinderViewModel) {
    Screen {
        TopBar("binder")
        Txt("More", 34, font = Serif)
        Cap("THEME")
        val themes = listOf("system", "light", "dark")
        Seg(listOf("System", "Light", "Dark"), themes.indexOf(vm.theme).coerceAtLeast(0)) { vm.chooseTheme(themes[it]) }
        Cap("HOME LOOK")
        Seg(listOf("Tiles", "Rows"), if (vm.homeRows) 1 else 0) { vm.setRows(it == 1) }
        Cap("ABOUT")
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(P.surface).padding(16.dp)) {
            Txt("Binder 0.1", 15, weight = FontWeight.Medium)
            Txt("Everything is kept on this phone, in a single file (binder.json).", 13, P.mute, Modifier.padding(top = 4.dp))
        }
    }
}
