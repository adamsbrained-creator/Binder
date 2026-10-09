package app.binder

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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

/** Quick add: pick the list, type a title, optionally add an author / rating / note. */
@Composable
fun BoxScope.QuickAdd(vm: BinderViewModel, preset: String) {
    val lists = vm.lists
    var target by remember { mutableStateOf(preset.ifEmpty { lists.firstOrNull()?.id ?: "" }) }
    var title by remember { mutableStateOf("") }
    var sub by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf(0f) }
    var showSub by remember { mutableStateOf(false) }
    var showRating by remember { mutableStateOf(false) }
    var showNote by remember { mutableStateOf(false) }
    val l = lists.firstOrNull { it.id == target }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(250)
        runCatching { focus.requestFocus() }
    }

    fun submit() {
        if (l != null && title.isNotBlank()) {
            vm.saveItem(l.id, Item(title = title.trim(), sub = sub.trim(), rating = rating, note = note.trim()))
            vm.quick = null
        }
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
                    if (title.isEmpty()) Txt(if (l != null) "New ${l.kind.single}" else "Add something…", 26, P.mute, font = TitleFont)
                    inner()
                }
            },
        )
        if (l != null && !l.kind.isChecklist) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (l.kind.subLabel.isNotEmpty()) ToggleChip(l.kind.subLabel, showSub) { showSub = !showSub }
                ToggleChip("Rating", showRating) { showRating = !showRating }
                ToggleChip("Note", showNote) { showNote = !showNote }
            }
            if (showSub) Field(l.kind.subLabel, sub, Modifier.padding(top = 10.dp)) { sub = it }
            if (showRating) Box(Modifier.padding(top = 10.dp)) { Stars(rating) { rating = it } }
            if (showNote) Field("Note", note, Modifier.padding(top = 10.dp), singleLine = false) { note = it }
        }
        Spacer(Modifier.height(16.dp))
        BigButton("Add to list") { submit() }
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
        title = { Txt("Rename list", 20) },
        text = { Field("List name", nm) { nm = it } },
        confirmButton = {
            TextButton({
                if (nm.isNotBlank()) vm.renameList(l.id, nm.trim())
                vm.rename = null
            }) { Txt("Save", 15, Coral) }
        },
        dismissButton = { TextButton({ vm.rename = null }) { Txt("Cancel", 15, P.mute) } },
    )
}
