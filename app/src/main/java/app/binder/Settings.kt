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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.em
import kotlin.math.roundToInt
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

// ───────────────────────── Settings (same look as Paperback's) ─────────────────────────

private val GroupShape = RoundedCornerShape(28.dp)
private val CardShape = RoundedCornerShape(24.dp)

@Composable
private fun isDark(): Boolean = P.bg == DarkPal.bg

/** Small spaced capitals above a group, like READING / LOOK in Paperback. */
@Composable
private fun SetSection(label: String) {
    Txt(
        label, 12, P.mute, Modifier.padding(start = 4.dp, top = 24.dp, bottom = 10.dp),
        weight = FontWeight.Medium, ls = 0.14.em, maxLines = 1,
    )
}

/** One rounded card that holds a few rows. */
@Composable
private fun SetGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(GroupShape).background(P.surface), content = content)
}

/** An icon tile, the name, a short value on the right and an arrow. A thin line separates it from the row above. */
@Composable
private fun SetRow(icon: ImageVector, title: String, value: String = "", divider: Boolean = false, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(P.bg), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = P.fg, modifier = Modifier.size(22.dp))
            }
            Txt(title, 16, weight = FontWeight.Medium, maxLines = 1, modifier = Modifier.weight(1f))
            if (value.isNotEmpty()) Txt(value, 14, P.mute, maxLines = 1)
            Icon(Ic.Chevron, null, tint = P.mute, modifier = Modifier.size(18.dp))
        }
        if (divider) {
            Box(Modifier.align(Alignment.TopStart).padding(start = 70.dp, end = 16.dp).fillMaxWidth().height(1.dp).background(P.line))
        }
    }
}

/** The dark card on top (Paperback's "Daily goal"): a ring with how much is done, and a button. */
@Composable
private fun SummaryCard(o: Opts, lists: List<BinderList>, onOpen: () -> Unit) {
    val total = lists.sumOf { it.items.size }
    val done = lists.sumOf { it.doneCount() }
    val frac = if (total == 0) 0f else done.toFloat() / total
    val pct = (frac * 100).roundToInt()
    val bg = if (isDark()) Color(0xFF1F1F1F) else Color(0xFF111111)
    Row(
        Modifier.padding(top = 16.dp).fillMaxWidth().clip(GroupShape).background(bg)
            .padding(start = 18.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val w = 7.dp.toPx()
                val tl = Offset(w / 2f, w / 2f)
                val box = Size(size.width - w, size.height - w)
                drawArc(Color(0xFF3A3A3A), 0f, 360f, false, tl, box, style = Stroke(w))
                if (frac > 0f) {
                    drawArc(Coral, -90f, 360f * frac, false, tl, box, style = Stroke(w, cap = StrokeCap.Round))
                } else {
                    drawCircle(Coral, 4.5.dp.toPx(), Offset(size.width / 2f, 5.dp.toPx()))
                }
            }
            Txt("$pct%", 13, Color.White, weight = FontWeight.Medium, maxLines = 1)
        }
        Column(Modifier.weight(1f)) {
            Txt(o.lists, 17, Color.White, weight = FontWeight.Medium, maxLines = 1, ls = (-0.01).em)
            Txt(
                if (total == 0) "No items yet" else "$done of $total items done",
                13, Color(0xFFC4C4C4), Modifier.padding(top = 2.dp), maxLines = 1,
            )
        }
        Box(
            Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(Color.White)
                .clickable(onClick = onOpen).padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) { Txt("Back up", 14, Color(0xFF161616), weight = FontWeight.Medium, maxLines = 1) }
    }
}

// ───────── pieces for the pages inside Settings ─────────

/** One sentence under the header that says what the page is for. */
@Composable
private fun SetIntro(text: String) {
    Txt(text, 15, P.mute, Modifier.padding(start = 4.dp, end = 4.dp, top = 18.dp), lh = 22.sp)
}

/** A small grey note under a card. */
@Composable
private fun SetNote(text: String, top: Int = 10) {
    Txt(text, 12, P.mute, Modifier.padding(start = 4.dp, end = 4.dp, top = top.dp), lh = 17.sp)
}

/** A card with a title, an optional line under it, and (optionally) the controls. */
@Composable
private fun SetCard(title: String, sub: String = "", content: (@Composable ColumnScope.() -> Unit)? = null) {
    Column(Modifier.padding(top = 16.dp).fillMaxWidth().clip(CardShape).background(P.surface).padding(16.dp)) {
        Txt(title, 16, weight = FontWeight.Medium)
        if (sub.isNotEmpty()) Txt(sub, 13, P.mute, Modifier.padding(top = 2.dp), lh = 18.sp)
        if (content != null) {
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

/** A card with an on / off switch at the right. */
@Composable
private fun SetToggleCard(title: String, sub: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.padding(top = 16.dp).fillMaxWidth().clip(CardShape).background(P.surface).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Txt(title, 16, weight = FontWeight.Medium)
            Txt(sub, 13, P.mute, Modifier.padding(top = 2.dp), lh = 18.sp)
        }
        Toggle(on, onChange)
    }
}

/** The 2 to 3 choice switch inside a card: a darker track with a lighter pill on the choice. */
@Composable
private fun SetSeg(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val dark = isDark()
    val track = if (dark) Color(0xFF0B0B0B) else Color(0xFFE7E7E7)
    val chosen = if (dark) Color(0xFF2C2C2C) else Color.White
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(track).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(18.dp))
                    .background(if (on) chosen else Color.Transparent).clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Txt(label, 15, if (on) P.fg else P.mute, weight = if (on) FontWeight.Medium else FontWeight.Normal, maxLines = 1)
            }
        }
    }
}

/** Choices that wrap onto several lines (for the longer lists), drawn to sit on a card. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetChips(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.clip(CircleShape).background(if (on) P.primary else P.bg)
                    .clickable { onSelect(i) }.padding(horizontal = 14.dp, vertical = 9.dp)
            ) { Txt(label, 14, if (on) P.onPrimary else P.fg) }
        }
    }
}

/** Frame for a page inside Settings: the app header, the page name with a back arrow, then the content. */
@Composable
private fun SubPage(vm: BinderViewModel, title: String, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Screen {
            AppHeader(vm)
            PageLabel(title) { vm.nav.pop() }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                content()
                Spacer(Modifier.height(112.dp))
            }
        }
        MenuFab(vm, onAdd = { vm.nav.push(Route.NewList) })
    }
}

// ───────── the Settings page ─────────

@Composable
fun MoreScreen(vm: BinderViewModel) {
    val o = vm.opts
    val nav = vm.nav
    val go = { p: SettingsPage -> nav.push(Route.Setting(p)) }
    val themeLabel = when (o.theme) {
        "light" -> "Light"
        "dark" -> "Dark"
        else -> "Match system"
    }

    Box(Modifier.fillMaxSize()) {
        Screen(rail = true) {
            AppHeader(vm)
            TopTabs(vm, Route.More)
            PageLabel("Settings")
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                SummaryCard(o, vm.lists) { go(SettingsPage.BACKUP) }

                SetSection(o.lists.uppercase())
                SetGroup {
                    SetRow(Ic.Globe, "Fetch details", if (o.fetch) "On" else "Off") { go(SettingsPage.FETCH) }
                    SetRow(Ic.Star, "Items & ratings", "${o.itemPage.label} · ${o.ratingStyle.label}", divider = true) { go(SettingsPage.ITEMS) }
                }

                SetSection("LOOK")
                SetGroup {
                    SetRow(Ic.Palette, "Appearance", themeLabel) { go(SettingsPage.APPEARANCE) }
                    SetRow(Ic.AppStyle, "App style", "Header: ${o.headerSize.label}", divider = true) { go(SettingsPage.APP_STYLE) }
                    SetRow(Ic.NameTag, "App name", o.appLabel, divider = true) { go(SettingsPage.APP_NAME) }
                }

                SetSection("DATA")
                SetGroup {
                    SetRow(Ic.Backup, "Backup & restore") { go(SettingsPage.BACKUP) }
                }

                Spacer(Modifier.height(24.dp))
                SetGroup {
                    SetRow(Ic.Info, "About ${o.appLabel}", BuildConfig.VERSION_NAME) { nav.push(Route.About) }
                }
                Spacer(Modifier.height(navHeight(o.nav).dp + 112.dp))
            }
        }
        if (!showsPlus(o.nav)) {
            MenuFab(vm, bottom = navHeight(o.nav), onAdd = { nav.push(Route.NewList) })
        }
    }
}

@Composable
fun SettingsPageScreen(vm: BinderViewModel, page: SettingsPage) {
    when (page) {
        SettingsPage.FETCH -> FetchPage(vm)
        SettingsPage.ITEMS -> ItemsPage(vm)
        SettingsPage.APPEARANCE -> AppearancePage(vm)
        SettingsPage.APP_STYLE -> AppStylePage(vm)
        SettingsPage.APP_NAME -> AppNamePage(vm)
        SettingsPage.BACKUP -> BackupPage(vm)
    }
}

// ───────── Fetch details ─────────

@Composable
private fun FetchPage(vm: BinderViewModel) {
    val o = vm.opts
    SubPage(vm, "Fetch details") {
        SetIntro("Look up covers and details for what you add. This is off until you turn it on.")
        SetToggleCard("Fetch details automatically", "Only the text you search for is sent.", o.fetch) { v ->
            vm.change { it.copy(fetch = v) }
        }
        SetNote(
            if (o.fetch) "On: in a ${o.listLower} with a source, typing a title or pasting a link looks up the cover and details. Only what you type or paste is sent."
            else "Off: ${o.appLabel} makes no internet calls at all.",
        )

        SetSection("SOURCES")
        // only sources this build can really use; one that needs a key the build doesn't have is hidden
        val rows = Kind.entries.filter { it.sources.isNotEmpty() }.mapNotNull { k ->
            val ready = Sources.readySources(k)
            if (ready.isEmpty()) null else k.label to ready.joinToString(", ") { it.label }
        }
        SetGroup {
            rows.forEachIndexed { i, (k, v) ->
                if (i > 0) Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().height(1.dp).background(P.line))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Txt(k, 15)
                    Txt(v, 14, P.mute, Modifier.padding(start = 16.dp), align = TextAlign.End)
                }
            }
        }
        SetNote("Paste a link from one of these, or any web page, to fill in the title and picture. Your own rating is never replaced.")
    }
}

// ───────── Items & ratings ─────────

@Composable
private fun ItemsPage(vm: BinderViewModel) {
    val o = vm.opts
    SubPage(vm, "Items & ratings") {
        SetIntro("How an item looks when you open it, how its rating is shown, and what a ${o.listLower} shows at the top.")
        SetCard("Item page") {
            SetSeg(ItemPage.entries.map { it.label }, o.itemPage.ordinal) { i ->
                vm.change { it.copy(itemPage = ItemPage.entries[i]) }
            }
        }
        SetCard("Rating style", "The number style shows “4.75 ★” and sets the rating from 0 to 5 in steps of 0.25.") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RatingStyle.entries.forEach { st ->
                    val on = o.ratingStyle == st
                    val shape = RoundedCornerShape(18.dp)
                    Column(
                        Modifier.weight(1f).clip(shape).background(P.bg)
                            .border(2.dp, if (on) Coral else Color.Transparent, shape)
                            .clickable { vm.change { it.copy(ratingStyle = st) } }
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Txt(st.label, 15, weight = if (on) FontWeight.Medium else FontWeight.Normal)
                        RatingText(4.75f, 14, style = st)
                    }
                }
            }
        }
        SetToggleCard("Progress line", "The coral line under a ${o.listLower}'s header.", o.listProgress) { v ->
            vm.change { it.copy(listProgress = v) }
        }
        SetToggleCard("Show empty slots", "Dashed “+” where something will go.", o.emptySlots) { v ->
            vm.change { it.copy(emptySlots = v) }
        }
    }
}

// ───────── Appearance ─────────

@Composable
private fun AppearancePage(vm: BinderViewModel) {
    val o = vm.opts
    val themes = listOf("system", "light", "dark")
    SubPage(vm, "Appearance") {
        SetIntro("Choose the colours and the type.")
        SetCard("Theme", "Match system follows your phone.") {
            SetSeg(listOf("System", "Light", "Dark"), themes.indexOf(o.theme).coerceAtLeast(0)) { i ->
                vm.change { it.copy(theme = themes[i]) }
            }
        }
        SetCard("Font", "Outfit is the standard one.") {
            SetChips(FontChoice.entries.map { it.label }, o.font.ordinal) { i ->
                vm.change { it.copy(font = FontChoice.entries[i]) }
            }
        }
    }
}

// ───────── App style ─────────

@Composable
private fun AppStylePage(vm: BinderViewModel) {
    val o = vm.opts
    SubPage(vm, "App style") {
        SetIntro("Choose how much room the top bar takes, and how the navigation and adding look on every screen.")
        SetCard("Header size", "Compact gives your ${o.listsLower} the most space.") {
            SetSeg(HeaderSize.entries.map { it.label }, o.headerSize.ordinal) { i ->
                vm.change { it.copy(headerSize = HeaderSize.entries[i]) }
            }
        }
        SetNote("Compact: no circles, the name sits next to the menu button and the home screen has no page title. Regular and Large: round buttons, with a small page title just under the menu button.")
        SetCard("Header style", "The bar at the top of every screen.") {
            SetChips(HeaderStyle.entries.map { it.label }, o.header.ordinal) { i ->
                vm.change { it.copy(header = HeaderStyle.entries[i]) }
            }
        }
        SetCard("Navigation", "None keeps everything in the menu button.") {
            SetChips(NavStyle.entries.map { it.label }, o.nav.ordinal) { i ->
                vm.change { it.copy(nav = NavStyle.entries[i]) }
            }
        }
        SetCard("Adding items", "Tap the menu button to open the menu. Press and hold it to pop out a + for adding.") {
            SetChips(AddStyle.entries.map { it.label }, o.add.ordinal) { i ->
                vm.change { it.copy(add = AddStyle.entries[i]) }
            }
        }
    }
}

// ───────── App name ─────────

@Composable
private fun AppNamePage(vm: BinderViewModel) {
    val o = vm.opts
    SubPage(vm, "App name") {
        SetIntro("Binder or Catalog: one switch for the name, the icon and the word for your lists.")
        SetCard("Name", "Changes the name, the icon, the label under the icon and the word for your ${o.listsLower}.") {
            SetSeg(listOf("Binder", "Catalog"), if (o.appName == "catalog") 1 else 0) { i ->
                vm.change { it.copy(appName = if (i == 1) "catalog" else "binder") }
            }
        }
        SetNote("Your launcher may take a moment to update, and you may need to re-add the icon to your home screen.")
    }
}

// ───────── Backup & restore ─────────

@Composable
private fun BackupPage(vm: BinderViewModel) {
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

    val count = vm.lists.size
    val items = vm.lists.sumOf { it.items.size }
    SubPage(vm, "Backup & restore") {
        SetCard(
            "Your ${o.listsLower}",
            "$count ${if (count == 1) o.listLower else o.listsLower} and $items ${if (items == 1) "item" else "items"}, with their pictures.",
        )
        BigButton("Save a backup file", Modifier.padding(top = 16.dp)) {
            exportLauncher.launch("${o.appName}-backup-$stamp.zip")
        }
        Box(
            Modifier.padding(top = 10.dp).fillMaxWidth().height(52.dp).clip(CircleShape).background(P.surface)
                .clickable { importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
            contentAlignment = Alignment.Center,
        ) { Txt("Restore from a backup file", 16, weight = FontWeight.Medium) }
        if (msg.isNotEmpty()) Txt(msg, 13, Coral, Modifier.padding(start = 4.dp, top = 12.dp))
        SetNote("A .zip with all your ${o.listsLower}, text and pictures.")
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
