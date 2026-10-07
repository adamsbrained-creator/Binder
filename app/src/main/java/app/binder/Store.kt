package app.binder

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

sealed interface Route {
    data object Home : Route
    data object Search : Route
    data object More : Route
    data object NewList : Route
    data class Lst(val id: String) : Route
    data class ItemEdit(val listId: String, val itemId: String?) : Route
}

class Nav {
    var stack by mutableStateOf<List<Route>>(listOf(Route.Home))
        private set
    val top: Route get() = stack.last()
    fun push(r: Route) { stack = stack + r }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }
    fun root(r: Route) { stack = listOf(r) }
    fun replaceTop(r: Route) { stack = stack.dropLast(1) + r }
}

class BinderViewModel(app: Application) : AndroidViewModel(app) {
    private val file = File(app.filesDir, "binder.json")
    private val prefs = app.getSharedPreferences("binder", Context.MODE_PRIVATE)

    val nav = Nav()

    var lists by mutableStateOf(load())
        private set
    var theme by mutableStateOf(prefs.getString("theme", "system") ?: "system")
        private set
    var homeRows by mutableStateOf(prefs.getBoolean("rows", false))
        private set

    fun chooseTheme(t: String) { theme = t; prefs.edit().putString("theme", t).apply() }
    fun setRows(b: Boolean) { homeRows = b; prefs.edit().putBoolean("rows", b).apply() }

    fun addList(name: String, kind: Kind): String {
        val b = BinderList(name = name, kind = kind, layout = if (kind.isChecklist) Layout.LIST else Layout.GRID)
        commit(listOf(b) + lists)
        return b.id
    }
    fun renameList(id: String, name: String) = edit(id) { it.copy(name = name) }
    fun setLayout(id: String, layout: Layout) = edit(id) { it.copy(layout = layout) }
    fun deleteList(id: String) = commit(lists.filterNot { it.id == id })

    /** Adds the item (on top) or replaces it if it already exists. */
    fun saveItem(listId: String, item: Item) = edit(listId) { l ->
        if (l.items.any { it.id == item.id }) l.copy(items = l.items.map { if (it.id == item.id) item else it })
        else l.copy(items = listOf(item) + l.items)
    }
    fun deleteItem(listId: String, itemId: String) = edit(listId) { l -> l.copy(items = l.items.filterNot { it.id == itemId }) }

    private fun edit(id: String, f: (BinderList) -> BinderList) = commit(lists.map { if (it.id == id) f(it) else it })

    private fun commit(new: List<BinderList>) {
        lists = new
        runCatching { file.writeText(toJson(new).toString()) }
    }

    private fun load(): List<BinderList> {
        if (!file.exists()) return emptyList()
        return runCatching { parse(file.readText()) }.getOrElse {
            runCatching { file.copyTo(File(file.parentFile, "binder.json.bak"), overwrite = true) }
            emptyList()
        }
    }

    private fun toJson(all: List<BinderList>): JSONArray {
        val out = JSONArray()
        for (b in all) {
            val items = JSONArray()
            for (i in b.items) {
                items.put(
                    JSONObject().put("id", i.id).put("title", i.title).put("sub", i.sub).put("stage", i.stage)
                        .put("rating", i.rating.toDouble()).put("note", i.note).put("added", i.added).put("cover", i.cover)
                )
            }
            out.put(
                JSONObject().put("id", b.id).put("name", b.name).put("kind", b.kind.name)
                    .put("layout", b.layout.name).put("created", b.created).put("items", items)
            )
        }
        return out
    }

    private fun parse(s: String): List<BinderList> {
        val arr = JSONArray(s)
        return List(arr.length()) { n ->
            val o = arr.getJSONObject(n)
            val its = o.getJSONArray("items")
            val items = List(its.length()) { m ->
                val i = its.getJSONObject(m)
                Item(
                    id = i.getString("id"), title = i.getString("title"), sub = i.optString("sub"),
                    stage = i.optInt("stage"), rating = i.optDouble("rating", 0.0).toFloat(),
                    note = i.optString("note"), added = i.optLong("added"), cover = i.optInt("cover", -1),
                )
            }
            BinderList(
                id = o.getString("id"), name = o.getString("name"),
                kind = runCatching { Kind.valueOf(o.getString("kind")) }.getOrDefault(Kind.BOOKS),
                layout = runCatching { Layout.valueOf(o.getString("layout")) }.getOrDefault(Layout.GRID),
                created = o.optLong("created"), items = items,
            )
        }
    }
}
