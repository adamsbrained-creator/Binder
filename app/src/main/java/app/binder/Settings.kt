package app.binder

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ───────────────────────── New list ─────────────────────────

@Composable
fun NewListScreen(vm: BinderViewModel) {
    var name by rememberSaveable { mutableStateOf("") }
    var k by rememberSaveable { mutableStateOf(0) }
    val kind = Kind.entries[k]

    Screen {
        TopBar("New list", left = { CircleBtn(Ic.Back, "Back") { vm.nav.pop() } })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Field("List name", name, Modifier.padding(top = 6.dp)) { name = it }
            Cap("START FROM")
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Kind.entries.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { kd ->
                            Tile(kd.label, kd.hint, kd.stack.map { Art(Covers[it]) }, kd == kind, Modifier.weight(1f)) {
                                k = kd.ordinal
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        BigButton("Create list", Modifier.navigationBarsPadding().padding(top = 8.dp, bottom = 18.dp)) {
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
    val pad = navHeight(vm.opts.nav) + 40
    Screen(rail = true) {
        TopBar(
            "Search",
            left = {
                if (vm.nav.stack.size > 1) CircleBtn(Ic.Back, "Back") { vm.nav.pop() }
                else Spacer(Modifier.width(44.dp))
            },
        )
        TopTabs(vm, Route.Search)
        Field("Search every list", q) { q = it }
        Spacer(Modifier.height(8.dp))
        if (q.isBlank()) {
            EmptyBlock("Find anything", "Titles, authors, artists and notes from all your lists.")
        } else if (hits.isEmpty()) {
            EmptyBlock("No matches", "Nothing found for “$q”.")
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = pad.dp)) {
                items(hits, key = { it.second.id }) { (l, item) ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.nav.push(Route.ItemEdit(l.id, item.id)) }.padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CoverView(item.title, item.art(), Modifier.width(40.dp), size = 8, px = 150, showTitle = false)
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

// ───────────────────────── More (settings) ─────────────────────────

@Composable
private fun <T> OptionGroup(title: String, all: List<T>, selected: T, label: (T) -> String, onPick: (T) -> Unit) {
    Cap(title)
    FlowChips(all.map(label), all.indexOf(selected)) { onPick(all[it]) }
}

@Composable
fun MoreScreen(vm: BinderViewModel) {
    val o = vm.opts
    var msg by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<Uri?>(null) }
    val stamp = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.exportZip(uri) { ok -> msg = if (ok) "Backup saved." else "Could not save the backup." }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pending = uri
    }

    Screen(rail = true) {
        TopBar(
            "",
            left = {
                if (vm.nav.stack.size > 1) CircleBtn(Ic.Back, "Back") { vm.nav.pop() }
                else Spacer(Modifier.width(44.dp))
            },
        )
        TopTabs(vm, Route.More)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                LogoMark(44.dp)
                Txt("More", 34, font = TitleFont)
            }

            Cap("APP NAME")
            Seg(listOf("Binder", "Catalog"), if (o.appName == "catalog") 1 else 0) { i ->
                vm.change { it.copy(appName = if (i == 1) "catalog" else "binder") }
            }
            Txt("Only changes the name and logo text at the top.", 12, P.mute, Modifier.padding(top = 6.dp))

            Cap("THEME")
            val themes = listOf("system", "light", "dark")
            Seg(listOf("System", "Light", "Dark"), themes.indexOf(o.theme).coerceAtLeast(0)) { i ->
                vm.change { it.copy(theme = themes[i]) }
            }

            OptionGroup("HOME LAYOUT", HomeStyle.entries, o.home, { it.label }) { v -> vm.change { it.copy(home = v) } }
            OptionGroup("HEADER", HeaderStyle.entries, o.header, { it.label }) { v -> vm.change { it.copy(header = v) } }
            OptionGroup("NAVIGATION", NavStyle.entries, o.nav, { it.label }) { v -> vm.change { it.copy(nav = v) } }
            OptionGroup("ADDING ITEMS (+ BUTTON)", AddStyle.entries, o.add, { it.label }) { v -> vm.change { it.copy(add = v) } }
            Txt("Press and hold the + button for the other way.", 12, P.mute, Modifier.padding(top = 6.dp))
            OptionGroup("ITEM PAGE", ItemPage.entries, o.itemPage, { it.label }) { v -> vm.change { it.copy(itemPage = v) } }
            OptionGroup("FONT", FontChoice.entries, o.font, { it.label }) { v -> vm.change { it.copy(font = v) } }

            Cap("BACKUP")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    BigButton("Save backup") { exportLauncher.launch("${o.appName}-backup-$stamp.zip") }
                }
                Box(Modifier.weight(1f)) {
                    Box(
                        Modifier.fillMaxWidth().height(52.dp).clip(CircleShape).background(P.surface)
                            .clickable { importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
                        contentAlignment = Alignment.Center,
                    ) { Txt("Restore", 16, weight = FontWeight.Medium) }
                }
            }
            Txt("A .zip with all your lists, text and pictures.", 12, P.mute, Modifier.padding(top = 6.dp))
            if (msg.isNotEmpty()) Txt(msg, 13, Coral, Modifier.padding(top = 6.dp))

            Cap("ABOUT")
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(P.surface).padding(16.dp)) {
                Txt("${o.appName.replaceFirstChar { it.uppercase() }} 0.5", 15, weight = FontWeight.Medium)
                Txt(
                    "Everything is kept on this phone: the lists in binder.json and the pictures in a folder next to it. Use Save backup to keep a copy.",
                    13, P.mute, Modifier.padding(top = 4.dp),
                )
            }
            Spacer(Modifier.height(navHeight(o.nav).dp + 40.dp))
        }
    }

    val p = pending
    if (p != null) {
        AlertDialog(
            onDismissRequest = { pending = null },
            containerColor = P.bg,
            title = { Txt("Restore backup", 20) },
            text = { Txt("Replace everything with the backup, or add its lists to what you have now?", 14, P.mute) },
            confirmButton = {
                TextButton({
                    pending = null
                    vm.importZip(p, replace = true) { ok -> msg = if (ok) "Backup restored." else "That file is not a backup." }
                }) { Txt("Replace", 15, Coral) }
            },
            dismissButton = {
                TextButton({
                    pending = null
                    vm.importZip(p, replace = false) { ok -> msg = if (ok) "Lists added." else "That file is not a backup." }
                }) { Txt("Add", 15, P.fg) }
            },
        )
    }
}
