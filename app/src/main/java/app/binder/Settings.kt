package app.binder

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ───────────────────────── New list ─────────────────────────

@Composable
private fun KindTile(kd: Kind, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier.height(92.dp).clip(shape).background(P.surface)
            .border(2.dp, if (selected) Coral else Color.Transparent, shape)
            .clickable(onClick = onClick).padding(12.dp)
    ) {
        Column(Modifier.align(Alignment.BottomStart)) {
            Txt(kd.label, 15, weight = FontWeight.Medium, maxLines = 1)
            Txt(kd.hint, 11, P.mute, Modifier.padding(top = 2.dp), maxLines = 2, lh = 13.sp)
        }
    }
}

@Composable
fun NewListScreen(vm: BinderViewModel) {
    val o = vm.opts
    var name by rememberSaveable { mutableStateOf("") }
    var k by rememberSaveable { mutableStateOf(0) }
    val kind = Kind.entries[k]

    Screen {
        Header2("New ${o.listLower}", onBack = { vm.nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Field("${o.list} name", name, Modifier.padding(top = 6.dp)) { name = it }
            Cap("START FROM")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Kind.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { kd -> KindTile(kd, kd == kind, Modifier.weight(1f)) { k = kd.ordinal } }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        BigButton("Create ${o.listLower}", Modifier.navigationBarsPadding().padding(top = 8.dp, bottom = 18.dp)) {
            val id = vm.addList(name.trim().ifBlank { kind.label }, kind)
            vm.nav.replaceTop(Route.Lst(id))
        }
    }
}

// ───────────────────────── Search ─────────────────────────

@Composable
fun SearchScreen(vm: BinderViewModel) {
    var q by rememberSaveable { mutableStateOf("") }
    val o = vm.opts
    val hits: List<Pair<BinderList, Item>> = if (q.isBlank()) emptyList() else vm.lists.flatMap { l ->
        l.items.filter {
            it.title.contains(q, true) || it.sub.contains(q, true) || it.note.contains(q, true) || it.genre.contains(q, true)
        }.map { l to it }
    }
    val pad = navHeight(o.nav) + 40
    Screen(rail = true) {
        Header2(
            "Search",
            onBack = if (vm.nav.stack.size > 1) ({ vm.nav.pop() }) else null,
        )
        TopTabs(vm, Route.Search)
        Field("Search every ${o.listLower}", q) { q = it }
        Spacer(Modifier.height(8.dp))
        if (q.isBlank()) {
            EmptyBlock("Find anything", "Titles, authors, artists and notes from all your ${o.listsLower}.")
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
                        CoverView(item.title, item.art(), Modifier.width(40.dp), size = 8, px = 150, showTitle = false, square = l.kind.square)
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
private fun SwitchRow(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(P.surface).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Txt(title, 15)
            Txt(sub, 12, P.mute, Modifier.padding(top = 2.dp))
        }
        Toggle(on, onChange)
    }
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

    // "Backup and restore" in the floating menu opens More already scrolled down to the Backup section
    val scroll = rememberScrollState()
    LaunchedEffect(vm.moreJump) {
        if (vm.moreJump == "backup") {
            withTimeoutOrNull(600) { snapshotFlow { scroll.maxValue }.first { it > 0 } }
            scroll.animateScrollTo(scroll.maxValue)
            vm.moreJump = null
        }
    }

    Screen(rail = true) {
        Header2("More", onBack = if (vm.nav.stack.size > 1) ({ vm.nav.pop() }) else null)
        TopTabs(vm, Route.More)
        Column(Modifier.weight(1f).verticalScroll(scroll)) {
            Cap("DETAILS")
            SwitchRow("Fetch details automatically", "Only the text you search for is sent.", o.fetch) { v ->
                vm.change { it.copy(fetch = v) }
            }
            Txt(
                if (o.fetch) "On: in a ${o.listLower} with a source, typing a title or pasting a link looks up the cover and details. Only what you type or paste is sent."
                else "Off: ${o.appLabel} makes no internet calls at all.",
                12, P.mute, Modifier.padding(top = 6.dp),
            )

            Cap("SOURCES")
            // only sources this build can really use; one that needs a key the build doesn't have is hidden
            val rows = Kind.entries.filter { it.sources.isNotEmpty() }.mapNotNull { k ->
                val ready = Sources.readySources(k)
                if (ready.isEmpty()) null else k.label to ready.joinToString(", ") { it.label }
            }
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(P.surface).padding(horizontal = 16.dp, vertical = 4.dp)) {
                rows.forEachIndexed { i, (k, v) ->
                    if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(P.line))
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Txt(k, 15)
                        Txt(v, 15, P.mute, Modifier.padding(start = 16.dp), align = TextAlign.End)
                    }
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(P.line))
                Txt(
                    "Paste a link from one of these, or any web page, to fill in the title and picture. Your own rating is never replaced.",
                    13, P.mute, Modifier.padding(vertical = 12.dp),
                )
            }

            Cap("APP NAME")
            Seg(listOf("Binder", "Catalog"), if (o.appName == "catalog") 1 else 0) { i ->
                vm.change { it.copy(appName = if (i == 1) "catalog" else "binder") }
            }
            Txt(
                "Changes the name, the icon, the label under the icon and the word for your ${o.listsLower}.",
                12, P.mute, Modifier.padding(top = 6.dp),
            )
            Txt(
                "Your launcher may take a moment to update, and you may need to re-add the icon to your home screen.",
                12, P.mute, Modifier.padding(top = 4.dp),
            )

            Cap("THEME")
            val themes = listOf("system", "light", "dark")
            Seg(listOf("System", "Light", "Dark"), themes.indexOf(o.theme).coerceAtLeast(0)) { i ->
                vm.change { it.copy(theme = themes[i]) }
            }

            Cap("HEADER SIZE")
            Seg(HeaderSize.entries.map { it.label }, o.headerSize.ordinal) { i ->
                vm.change { it.copy(headerSize = HeaderSize.entries[i]) }
            }
            Txt(
                "Compact gives your ${o.listsLower} the most space: no circles, name on the left. " +
                    "Large is the original. It sets the top bar of every page, and the Wordmark header on the home screen.",
                12, P.mute, Modifier.padding(top = 6.dp),
            )

            Cap("MENU STYLE")
            Seg(MenuStyle.entries.map { it.label }, o.menuStyle.ordinal) { i ->
                vm.change { it.copy(menuStyle = MenuStyle.entries[i]) }
            }
            Txt(
                "Floating: quick buttons to add an item, make a new ${o.listLower} or search, then Settings, Backup and About. " +
                    "Classic is the original and also has Pinned, Recently edited and View.",
                12, P.mute, Modifier.padding(top = 6.dp),
            )

            OptionGroup("NAVIGATION", NavStyle.entries, o.nav, { it.label }) { v -> vm.change { it.copy(nav = v) } }
            OptionGroup("ADDING ITEMS", AddStyle.entries, o.add, { it.label }) { v -> vm.change { it.copy(add = v) } }
            Txt("Tap the menu button to open the menu. Press and hold it to pop out a + for adding.", 12, P.mute, Modifier.padding(top = 6.dp))
            OptionGroup("ITEM PAGE", ItemPage.entries, o.itemPage, { it.label }) { v -> vm.change { it.copy(itemPage = v) } }
            Cap("RATING STYLE")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RatingStyle.entries.forEach { st ->
                    val on = o.ratingStyle == st
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(P.surface)
                            .border(2.dp, if (on) Coral else Color.Transparent, RoundedCornerShape(22.dp))
                            .clickable { vm.change { it.copy(ratingStyle = st) } }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Txt(st.label, 15, weight = if (on) FontWeight.Medium else FontWeight.Normal)
                        RatingText(4.75f, 14, style = st)
                    }
                }
            }
            Txt("The number style shows “4.75 ★” and sets the rating from 0 to 5 in steps of 0.25.", 12, P.mute, Modifier.padding(top = 6.dp))

            OptionGroup("FONT", FontChoice.entries, o.font, { it.label }) { v -> vm.change { it.copy(font = v) } }

            Cap("EMPTY SLOTS")
            SwitchRow("Show empty slots", "Dashed “+” where something will go.", o.emptySlots) { v ->
                vm.change { it.copy(emptySlots = v) }
            }

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
            Txt("A .zip with all your ${o.listsLower}, text and pictures.", 12, P.mute, Modifier.padding(top = 6.dp))
            if (msg.isNotEmpty()) Txt(msg, 13, Coral, Modifier.padding(top = 6.dp))

            Cap("ABOUT")
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(P.surface)
                    .clickable { vm.nav.push(Route.About) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Txt("${o.appLabel} ${BuildConfig.VERSION_NAME}", 15, weight = FontWeight.Medium)
                    Txt(
                        "${o.lists} and pictures stay on this phone. Only the text you search for is sent when fetching is on.",
                        13, P.mute, Modifier.padding(top = 4.dp),
                    )
                }
                Icon(Ic.Chevron, null, tint = P.mute, modifier = Modifier.size(18.dp))
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
            text = { Txt("Replace everything with the backup, or add its ${o.listsLower} to what you have now?", 14, P.mute) },
            confirmButton = {
                TextButton({
                    pending = null
                    vm.importZip(p, replace = true) { ok -> msg = if (ok) "Backup restored." else "That file is not a backup." }
                }) { Txt("Replace", 15, Coral) }
            },
            dismissButton = {
                TextButton({
                    pending = null
                    vm.importZip(p, replace = false) { ok -> msg = if (ok) "${o.lists} added." else "That file is not a backup." }
                }) { Txt("Add", 15, P.fg) }
            },
        )
    }
}
