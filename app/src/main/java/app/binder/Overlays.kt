package app.binder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

/** Press-and-hold menus: a list of actions in a bottom sheet. */
@Composable
fun BoxScope.ActionSheet(vm: BinderViewModel, s: SheetData) {
    SheetFrame(onClose = { vm.sheet = null }) {
        Txt(s.title, 20, font = TitleFont, maxLines = 1, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        s.acts.forEach { a ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .clickable { vm.sheet = null; a.run() }.padding(horizontal = 8.dp, vertical = 14.dp)
            ) { Txt(a.label, 16, if (a.danger) Coral else P.fg) }
        }
    }
}

@Composable
private fun ToggleChip(label: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(CircleShape).background(if (on) P.primary else P.surface)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp)
    ) { Txt(label, 14, if (on) P.onPrimary else P.fg) }
}

private fun looksLink(q: String): Boolean = q.startsWith("http", true) || q.contains("music.apple.com", true)

/** What the search under the title is doing. */
private sealed interface Lookup {
    data object Idle : Lookup
    data object Loading : Lookup
    data class Results(val list: List<Found>, val fromLink: Boolean) : Lookup
    data class Failed(val message: String, val offline: Boolean) : Lookup
}

@Composable
private fun Thumb(url: String, size: Dp, modifier: Modifier = Modifier) {
    val bmp = rememberRemoteImage(url, 200)
    Box(modifier.size(size).clip(RoundedCornerShape(10.dp)).background(P.line)) {
        if (bmp != null) Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
    }
}

/** Quick add: pick the list, type a title, optionally add an author / rating / note. With fetching on, Albums lists search as you type. */
@Composable
fun BoxScope.QuickAdd(vm: BinderViewModel, preset: String) {
    val lists = vm.lists
    val o = vm.opts
    var target by remember { mutableStateOf(preset.ifEmpty { lists.firstOrNull()?.id ?: "" }) }
    var title by remember { mutableStateOf("") }
    var sub by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0f) }
    var showSub by remember { mutableStateOf(false) }
    var showRating by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    var lookup by remember { mutableStateOf<Lookup>(Lookup.Idle) }
    var saving by remember { mutableStateOf(false) }
    val l = lists.firstOrNull { it.id == target }
    // a source only exists for some kinds, and only when fetching is switched on
    val provider = if (o.fetch && l != null) Sources.forKind(l.kind) else null
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(250)
        runCatching { focus.requestFocus() }
    }

    // search as you type (about 400 ms after the last key), or look up a pasted link
    LaunchedEffect(title, target, o.fetch) {
        lookup = Lookup.Idle
        val q = title.trim()
        if (provider == null || q.length < 2) return@LaunchedEffect
        val isLink = looksLink(q)
        if (isLink && !provider.handlesLink(q)) {
            lookup = Lookup.Failed("That link isn't from ${provider.source.label}.", false)
            return@LaunchedEffect
        }
        delay(400)
        lookup = Lookup.Loading
        val r = withContext(Dispatchers.IO) {
            runCatching { if (isLink) listOfNotNull(provider.fromLink(q)) else provider.search(q) }
        }
        val err = r.exceptionOrNull()
        lookup = when {
            err is FetchFailure -> Lookup.Failed(err.message ?: "Lookup failed.", err.offline)
            err != null -> Lookup.Failed("Couldn't read the answer.", false)
            else -> Lookup.Results(r.getOrDefault(emptyList()), isLink)
        }
    }

    fun withExtras(x: Item): Item = x.copy(rating = rating, note = note.trim(), sub = x.sub.ifBlank { sub.trim() })

    fun pick(f: Found) {
        if (l == null || saving) return
        saving = true
        vm.addFound(l.id, f, ::withExtras) { vm.quick = null }
    }

    fun submit() {
        if (l == null || title.isBlank() || saving) return
        val lk = lookup
        val q = title.trim()
        if (provider != null && looksLink(q)) {
            // a pasted link only adds once it has been found
            if (lk is Lookup.Results && lk.list.isNotEmpty()) pick(lk.list.first())
            return
        }
        if (provider != null && lk is Lookup.Failed) {
            // offline or the source said no: keep what was typed, fetch the details later
            vm.addPending(l.id, q)
            vm.quick = null
            return
        }
        vm.saveItem(l.id, Item(title = q, sub = sub.trim(), rating = rating, note = note.trim()))
        vm.quick = null
    }

    SheetFrame(onClose = { vm.quick = null }) {
        Txt("ADD TO", 11, P.mute, ls = 0.08.em)
        Spacer(Modifier.height(8.dp))
        FlowChips(lists.map { it.name }, lists.indexOfFirst { it.id == target }) { target = lists[it].id }
        BasicTextField(
            value = title,
            onValueChange = { title = it },
            singleLine = true,
            textStyle = TextStyle(color = P.fg, fontSize = 26.sp, fontFamily = TitleFont),
            cursorBrush = SolidColor(Coral),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).focusRequester(focus),
            decorationBox = { inner ->
                Box {
                    if (title.isEmpty()) {
                        val hint = if (provider != null) "${l?.kind?.single?.replaceFirstChar { it.uppercase() }} title or ${provider.source.label} link"
                        else if (l != null) "New ${l.kind.single}" else "Add something…"
                        Txt(hint, 26, P.mute, font = TitleFont, maxLines = 1)
                    }
                    inner()
                }
            },
        )

        // ---- what the source found ----
        if (provider != null) {
            when (val lk = lookup) {
                Lookup.Idle -> {}
                Lookup.Loading -> Txt("Searching ${provider.source.label}…", 13, P.mute, Modifier.padding(bottom = 12.dp))
                is Lookup.Failed -> Txt(
                    if (lk.offline) "${lk.message} Press Add to keep it and fetch the details later." else lk.message,
                    13, Coral, Modifier.padding(bottom = 12.dp),
                )
                is Lookup.Results -> {
                    if (lk.list.isEmpty()) {
                        Txt("Nothing found. You can still add it as typed.", 13, P.mute, Modifier.padding(bottom = 12.dp))
                    } else if (lk.fromLink) {
                        val f = lk.list.first()
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(P.surface).padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Thumb(f.thumbUrl, 92.dp)
                            Column(Modifier.weight(1f)) {
                                Txt(f.title, 18, maxLines = 2)
                                Txt(f.artist, 13, P.mute, Modifier.padding(top = 3.dp), maxLines = 1)
                                Txt(listOf(f.year, f.genre, f.description).filter { it.isNotBlank() }.joinToString(" · "), 13, P.mute, maxLines = 1)
                                Box(Modifier.padding(top = 8.dp).clip(CircleShape).background(P.bg).padding(horizontal = 10.dp, vertical = 5.dp)) {
                                    Txt(f.sourceName, 12)
                                }
                            }
                        }
                        Txt("The cover and details are saved on your phone.", 12, P.mute, Modifier.padding(start = 4.dp, top = 12.dp))
                    } else {
                        Column(
                            Modifier.fillMaxWidth().heightIn(max = 250.dp).clip(RoundedCornerShape(22.dp)).background(P.surface)
                                .verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            lk.list.forEachIndexed { i, f ->
                                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(P.line))
                                Row(
                                    Modifier.fillMaxWidth().clickable { pick(f) }.padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Thumb(f.thumbUrl, 54.dp)
                                    Column(Modifier.weight(1f)) {
                                        Txt(f.title, 15, weight = FontWeight.Medium, maxLines = 1)
                                        Txt(listOf(f.artist, f.year).filter { it.isNotBlank() }.joinToString(" · "), 13, P.mute, maxLines = 1)
                                    }
                                }
                            }
                        }
                        Txt("Results from ${provider.source.label}", 12, P.mute, Modifier.fillMaxWidth().padding(top = 10.dp), align = TextAlign.Center)
                    }
                }
            }
        }

        if (l != null && !l.kind.isChecklist) {
            Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (l.kind.subLabel.isNotEmpty()) ToggleChip(l.kind.subLabel, showSub) { showSub = !showSub }
                ToggleChip("Rating", showRating) { showRating = !showRating }
                ToggleChip("Note", showNote) { showNote = !showNote }
            }
            if (showSub) Field(l.kind.subLabel, sub, Modifier.padding(top = 10.dp)) { sub = it }
            if (showRating) Box(Modifier.padding(top = 10.dp)) { Stars(rating) { rating = it } }
            if (showNote) Field("Note", note, Modifier.padding(top = 10.dp), singleLine = false) { note = it }
        }
        Spacer(Modifier.height(16.dp))
        val isLinkInput = provider != null && looksLink(title.trim())
        val ready = !isLinkInput || (lookup as? Lookup.Results)?.list?.isNotEmpty() == true
        if (ready) BigButton(if (saving) "Saving…" else "Add to ${o.listLower}") { submit() }
    }
}

/** "Deleted … Undo" bar. Goes away by itself after a few seconds. */
@Composable
fun BoxScope.UndoBar(vm: BinderViewModel, u: Undo, bottom: Int) {
    LaunchedEffect(u) {
        delay(5000)
        if (vm.undo === u) vm.undo = null
    }
    Row(
        Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = (bottom + 16).dp)
            .clip(CircleShape).background(P.primary).padding(start = 20.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(u.label, 14, P.onPrimary, Modifier.weight(1f, fill = false), maxLines = 1)
        Box(
            Modifier.height(44.dp).clip(CircleShape).clickable { vm.runUndo() }.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Txt("Undo", 14, Coral, weight = FontWeight.Medium) }
    }
}

@Composable
fun BoxScope.ReorderBar(vm: BinderViewModel, bottom: Int) {
    Row(
        Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = (bottom + 16).dp)
            .clip(CircleShape).background(P.primary).padding(start = 20.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt("Drag the grips to reorder", 14, P.onPrimary, Modifier.weight(1f, fill = false), maxLines = 1)
        Box(
            Modifier.height(44.dp).clip(CircleShape).clickable { vm.reorder = false }.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) { Txt("Done", 14, Coral, weight = FontWeight.Medium) }
    }
}

@Composable
fun RenameDialog(vm: BinderViewModel, l: BinderList) {
    var nm by remember { mutableStateOf(l.name) }
    AlertDialog(
        onDismissRequest = { vm.rename = null },
        containerColor = P.bg,
        title = { Txt("Rename ${vm.opts.listLower}", 20) },
        text = { Field("${vm.opts.list} name", nm) { nm = it } },
        confirmButton = {
            TextButton({
                if (nm.isNotBlank()) vm.renameList(l.id, nm.trim())
                vm.rename = null
            }) { Txt("Save", 15, Coral) }
        },
        dismissButton = { TextButton({ vm.rename = null }) { Txt("Cancel", 15, P.mute) } },
    )
}
