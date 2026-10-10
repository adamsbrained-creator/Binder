package app.binder

import org.json.JSONArray
import org.json.JSONObject

// Saving and loading the lists as JSON. Kept apart from the ViewModel so it can be tested on a plain JVM.

fun listsToJson(all: List<BinderList>): JSONObject {
    val arr = JSONArray()
    for (b in all) {
        val items = JSONArray()
        for (i in b.items) {
            items.put(
                JSONObject().put("id", i.id).put("title", i.title).put("sub", i.sub).put("stage", i.stage)
                    .put("rating", i.rating.toDouble()).put("note", i.note).put("added", i.added)
                    .put("cover", i.cover).put("images", JSONArray(i.images))
                    .put("sourceName", i.sourceName).put("sourceId", i.sourceId).put("sourceUrl", i.sourceUrl)
                    .put("year", i.year).put("genre", i.genre).put("description", i.description)
                    .put("detailsPending", i.detailsPending)
            )
        }
        arr.put(
            JSONObject().put("id", b.id).put("name", b.name).put("kind", b.kind.name)
                .put("layout", b.layout.name).put("sort", b.sort.name).put("pinned", b.pinned)
                .put("created", b.created).put("columns", b.columns)
                .put("under", JSONArray(b.under.map { it.name })).put("items", items)
        )
    }
    return JSONObject().put("format", "binder").put("version", 3).put("lists", arr)
}

/** Reads both the new format and the old version 0.1 file (a plain list). */
fun parseLists(s: String): List<BinderList> {
    val t = s.trim()
    val arr = if (t.startsWith("[")) JSONArray(t) else JSONObject(t).getJSONArray("lists")
    return List(arr.length()) { n ->
        val o = arr.getJSONObject(n)
        val its = o.getJSONArray("items")
        val items = List(its.length()) { m ->
            val i = its.getJSONObject(m)
            val imgs = i.optJSONArray("images")
            Item(
                id = i.getString("id"), title = i.getString("title"), sub = i.optString("sub"),
                stage = i.optInt("stage"), rating = i.optDouble("rating", 0.0).toFloat(),
                note = i.optString("note"), added = i.optLong("added"), cover = i.optInt("cover", -1),
                images = if (imgs == null) emptyList() else List(imgs.length()) { k -> imgs.getString(k) },
                sourceName = i.optString("sourceName"), sourceId = i.optString("sourceId"),
                sourceUrl = i.optString("sourceUrl"), year = i.optString("year"),
                genre = i.optString("genre"), description = i.optString("description"),
                detailsPending = i.optBoolean("detailsPending", false),
            )
        }
        val underArr = o.optJSONArray("under")
        val under = if (underArr == null) setOf(Under.TITLE, Under.RATING) else
            (0 until underArr.length()).mapNotNull { k -> Under.entries.firstOrNull { it.name == underArr.optString(k) } }.toSet()
        BinderList(
            id = o.getString("id"), name = o.getString("name"),
            kind = runCatching { Kind.valueOf(o.getString("kind")) }.getOrDefault(Kind.BOOKS),
            layout = runCatching { Layout.valueOf(o.getString("layout")) }.getOrDefault(Layout.GRID),
            sort = runCatching { Sort.valueOf(o.getString("sort")) }.getOrDefault(Sort.MANUAL),
            pinned = o.optBoolean("pinned", false),
            created = o.optLong("created"), items = items,
            columns = o.optInt("columns", 3).coerceIn(2, 4), under = under,
        )
    }
}
