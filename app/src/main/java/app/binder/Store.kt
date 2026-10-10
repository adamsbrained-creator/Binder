package app.binder

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

sealed interface Route {
    data object Home : Route
    data object Search : Route
    data object More : Route
    data object NewList : Route
    data object About : Route
    data class Lists(val mode: Int) : Route // 0 = pinned, 1 = recently edited
    data class Lst(val id: String) : Route
    data class ItemEdit(val listId: String, val itemId: String?) : Route
}

class Nav {
    var stack by mutableStateOf<List<Route>>(listOf(Route.Home))
        private set
    var onChange: () -> Unit = {}
    val top: Route get() = stack.last()

    private fun go(s: List<Route>) { stack = s; onChange() }
    fun push(r: Route) = go(stack + r)
    fun pop() { if (stack.size > 1) go(stack.dropLast(1)) }
    fun back() { if (stack.size > 1) pop() else if (stack.first() != Route.Home) go(listOf(Route.Home)) }
    fun root(r: Route) = go(listOf(r))
    fun replaceTop(r: Route) = go(stack.dropLast(1) + r)
}

class Act(val label: String, val danger: Boolean = false, val run: () -> Unit)
class SheetData(val title: String, val acts: List<Act>)
class Undo(val label: String, val run: () -> Unit)

class BinderViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context = app
    private val file = File(app.filesDir, "binder.json")
    private val imgDir = File(app.filesDir, "images")
    private val prefs = app.getSharedPreferences("binder", Context.MODE_PRIVATE)

    val nav = Nav()

    var lists by mutableStateOf(load())
        private set
    var opts by mutableStateOf(loadOpts())
        private set

    // temporary screen state (not saved)
    var reorder by mutableStateOf(false)
    var drawer by mutableStateOf(false)
    var menu by mutableStateOf(false)
    var sheet by mutableStateOf<SheetData?>(null)
    var quick by mutableStateOf<String?>(null)
    var rename by mutableStateOf<BinderList?>(null)
    var undo by mutableStateOf<Undo?>(null)

    init {
        nav.onChange = { reorder = false; drawer = false; menu = false; sheet = null; quick = null }
    }

    // ───────── options ─────────

    private fun loadOpts(): Opts {
        val d = Opts()
        fun <T : Enum<T>> pick(key: String, all: List<T>, def: T): T =
            all.firstOrNull { it.name == prefs.getString(key, null) } ?: def
        return Opts(
            theme = prefs.getString("theme", d.theme) ?: d.theme,
            appName = prefs.getString("appName", d.appName) ?: d.appName,
            font = pick("font", FontChoice.entries, d.font),
            header = pick("header", HeaderStyle.entries, d.header),
            nav = pick("nav", NavStyle.entries, d.nav),
            home = pick("home", HomeStyle.entries, d.home),
            add = pick("add", AddStyle.entries, d.add),
            itemPage = pick("itemPage", ItemPage.entries, d.itemPage),
            fetch = prefs.getBoolean("fetch", d.fetch),
            listName = pick("listName", ListWord.entries, d.listName),
            emptySlots = prefs.getBoolean("emptySlots", d.emptySlots),
        )
    }

    fun change(f: (Opts) -> Opts) {
        val o = f(opts)
        opts = o
        prefs.edit()
            .putString("theme", o.theme).putString("appName", o.appName).putString("font", o.font.name)
            .putString("header", o.header.name).putString("nav", o.nav.name).putString("home", o.home.name)
            .putString("add", o.add.name).putString("itemPage", o.itemPage.name)
            .putBoolean("fetch", o.fetch).putString("listName", o.listName.name).putBoolean("emptySlots", o.emptySlots)
            .apply()
    }

    // ───────── lists ─────────

    fun addList(name: String, kind: Kind): String {
        val b = BinderList(name = name, kind = kind, layout = if (kind.isChecklist) Layout.LIST else Layout.GRID)
        commit(listOf(b) + lists)
        return b.id
    }
    fun renameList(id: String, name: String) = edit(id) { it.copy(name = name) }
    fun setLayout(id: String, layout: Layout) = edit(id) { it.copy(layout = layout) }
    fun setSort(id: String, sort: Sort) = edit(id) { it.copy(sort = sort) }
    fun togglePin(id: String) = edit(id) { it.copy(pinned = !it.pinned) }
    fun setColumns(id: String, n: Int) = edit(id) { it.copy(columns = n.coerceIn(2, 4)) }
    fun toggleUnder(id: String, u: Under) = edit(id) {
        it.copy(under = if (u in it.under) it.under - u else it.under + u)
    }

    fun moveList(id: String, delta: Int) {
        val i = lists.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j < 0 || j > lists.lastIndex) return
        val m = lists.toMutableList()
        m.add(j, m.removeAt(i))
        commit(m)
    }

    fun moveListTo(from: String, to: String) {
        val m = lists.toMutableList()
        val i = m.indexOfFirst { it.id == from }
        val j = m.indexOfFirst { it.id == to }
        if (i < 0 || j < 0 || i == j) return
        m.add(j, m.removeAt(i))
        commit(m)
    }

    fun deleteList(l: BinderList) {
        val idx = lists.indexOfFirst { it.id == l.id }
        commit(lists.filterNot { it.id == l.id })
        undo = Undo("Deleted “${l.name}”") {
            val m = lists.toMutableList()
            m.add(idx.coerceIn(0, m.size), l)
            commit(m)
        }
    }

    // ───────── items ─────────

    /** Adds the item (on top) or replaces it if it already exists. */
    fun saveItem(listId: String, item: Item) = edit(listId) { l ->
        if (l.items.any { it.id == item.id }) l.copy(items = l.items.map { if (it.id == item.id) item else it })
        else l.copy(items = listOf(item) + l.items)
    }

    fun deleteItem(listId: String, item: Item) {
        val l = lists.firstOrNull { it.id == listId } ?: return
        val idx = l.items.indexOfFirst { it.id == item.id }
        edit(listId) { x -> x.copy(items = x.items.filterNot { it.id == item.id }) }
        undo = Undo("Deleted “${item.title}”") {
            edit(listId) { x ->
                val m = x.items.toMutableList()
                m.add(idx.coerceIn(0, m.size), item)
                x.copy(items = m)
            }
        }
    }

    fun moveItemTo(listId: String, from: String, to: String) = edit(listId) { l ->
        val m = l.items.toMutableList()
        val i = m.indexOfFirst { it.id == from }
        val j = m.indexOfFirst { it.id == to }
        if (i < 0 || j < 0 || i == j) l else {
            m.add(j, m.removeAt(i))
            l.copy(items = m)
        }
    }

    /** Next status; after the last one it wraps back to the first. */
    fun advance(listId: String, item: Item) {
        val l = lists.firstOrNull { it.id == listId } ?: return
        val last = l.kind.stages.lastIndex
        saveItem(listId, item.copy(stage = if (item.stage >= last) 0 else item.stage + 1))
    }

    fun setStage(listId: String, item: Item, stage: Int) = saveItem(listId, item.copy(stage = stage))

    fun runUndo() {
        undo?.run?.invoke()
        undo = null
    }

    private fun edit(id: String, f: (BinderList) -> BinderList) = commit(lists.map { if (it.id == id) f(it) else it })

    private fun commit(new: List<BinderList>) {
        lists = new
        runCatching { file.writeText(listsToJson(new).toString()) }
    }

    // ───────── fetching details (only when Opts.fetch is on) ─────────

    /** Saves a search result as a normal item. The cover is downloaded first; if that fails the item is still saved. */
    fun addFound(listId: String, f: Found, extra: (Item) -> Item = { it }, done: () -> Unit) {
        viewModelScope.launch {
            val cover = if (f.coverUrl.isEmpty()) null else withContext(Dispatchers.IO) { downloadCover(ctx, f.coverUrl) }
            saveItem(listId, extra(f.toItem(listOfNotNull(cover))))
            done()
        }
    }

    /** No connection (or the lookup failed): keep what was typed and fetch the details later. */
    fun addPending(listId: String, title: String) {
        saveItem(listId, Item(title = title.trim(), detailsPending = true))
    }

    private var retrying = false

    /** Looks up items that were saved while offline. Runs when a list opens, only if fetching is on. */
    fun retryPending(listId: String) {
        val l = lists.firstOrNull { it.id == listId } ?: return
        val provider = Sources.forKind(l.kind) ?: return
        val todo = l.items.filter { it.detailsPending }
        if (!opts.fetch || todo.isEmpty() || retrying) return
        retrying = true
        viewModelScope.launch {
            try {
                for (item in todo.take(5)) { // a few at a time, to stay under the source's limit
                    val hit = withContext(Dispatchers.IO) {
                        runCatching {
                            val q = if (item.sub.isBlank()) item.title else "${item.title} ${item.sub}"
                            provider.search(q).firstOrNull()
                        }
                    }
                    val failure = hit.exceptionOrNull()
                    if (failure is FetchFailure && failure.offline) break // still offline: try again next time
                    val f = hit.getOrNull()
                    if (f == null) {
                        // nothing found, or the source said no: stop asking about this one
                        if (failure == null) markFound(listId, item.id, null)
                        continue
                    }
                    val cover = if (f.coverUrl.isEmpty()) null else withContext(Dispatchers.IO) { downloadCover(ctx, f.coverUrl) }
                    markFound(listId, item.id, f.toItem(listOfNotNull(cover)))
                }
            } finally {
                retrying = false
            }
        }
    }

    /** Fills in an item from a lookup. Keeps what the person typed or rated. */
    private fun markFound(listId: String, itemId: String, f: Item?) = edit(listId) { l ->
        l.copy(items = l.items.map { cur ->
            if (cur.id != itemId) cur
            else if (f == null) cur.copy(detailsPending = false)
            else cur.copy(
                sub = cur.sub.ifBlank { f.sub }, images = cur.images.ifEmpty { f.images },
                sourceName = f.sourceName, sourceId = f.sourceId, sourceUrl = f.sourceUrl,
                year = f.year, genre = f.genre, description = f.description, detailsPending = false,
            )
        })
    }

    // ───────── pictures ─────────

    fun importImages(uris: List<Uri>, done: (List<String>) -> Unit) {
        viewModelScope.launch {
            val names = withContext(Dispatchers.IO) { uris.mapNotNull { importImage(ctx, it) } }
            done(names)
        }
    }

    // ───────── zip backup ─────────

    fun exportZip(uri: Uri, done: (Boolean) -> Unit) {
        val snapshot = lists
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val out = ctx.contentResolver.openOutputStream(uri) ?: return@runCatching false
                    ZipOutputStream(out).use { z ->
                        z.putNextEntry(ZipEntry("binder.json"))
                        z.write(listsToJson(snapshot).toString(2).toByteArray(Charsets.UTF_8))
                        z.closeEntry()
                        val used = snapshot.flatMap { it.items }.flatMap { it.images }.toSet()
                        for (n in used) {
                            val f = File(imgDir, n)
                            if (f.exists()) {
                                z.putNextEntry(ZipEntry("images/$n"))
                                f.inputStream().use { it.copyTo(z) }
                                z.closeEntry()
                            }
                        }
                    }
                    true
                }.getOrDefault(false)
            }
            done(ok)
        }
    }

    fun importZip(uri: Uri, replace: Boolean, done: (Boolean) -> Unit) {
        viewModelScope.launch {
            val found = withContext(Dispatchers.IO) {
                runCatching {
                    imgDir.mkdirs()
                    var json: String? = null
                    val ins = ctx.contentResolver.openInputStream(uri) ?: return@runCatching null
                    ZipInputStream(ins).use { z ->
                        var e: ZipEntry? = z.nextEntry
                        while (e != null) {
                            val n = File(e.name).name
                            if (e.name == "binder.json") {
                                json = z.readBytes().toString(Charsets.UTF_8)
                            } else if (e.name.startsWith("images/") && n.isNotEmpty() && !e.isDirectory) {
                                File(imgDir, n).outputStream().use { o -> z.copyTo(o) }
                            }
                            e = z.nextEntry
                        }
                    }
                    json?.let { parseLists(it) }
                }.getOrNull()
            }
            if (found == null) {
                done(false)
            } else {
                commit(if (replace) found else lists + found.filter { n -> lists.none { it.id == n.id } })
                done(true)
            }
        }
    }

    // ───────── saving & loading ─────────

    private fun load(): List<BinderList> {
        if (!file.exists()) return emptyList()
        return runCatching { parseLists(file.readText()) }.getOrElse {
            runCatching { file.copyTo(File(file.parentFile, "binder.json.bak"), overwrite = true) }
            emptyList()
        }
    }
}
