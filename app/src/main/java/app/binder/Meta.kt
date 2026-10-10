package app.binder

import org.json.JSONObject
import java.net.URLEncoder

/** What a source knows about one thing, before it becomes an item in a list. */
data class Found(
    val sourceName: String,
    val sourceId: String,
    val sourceUrl: String,
    val title: String,
    val artist: String = "",       // artist / author / director: goes into Item.sub
    val year: String = "",
    val genre: String = "",
    val description: String = "",
    val coverUrl: String = "",     // full-size picture (saved on the phone)
    val thumbUrl: String = "",     // small picture for the result rows
)

/** Turns a result into a normal item. [images] are the file names of the downloaded cover. */
fun Found.toItem(images: List<String> = emptyList(), pending: Boolean = false): Item = Item(
    title = title, sub = artist, images = images,
    sourceName = sourceName, sourceId = sourceId, sourceUrl = sourceUrl,
    year = year, genre = genre, description = description, detailsPending = pending,
)

/** The one thing every source has to do. Both calls block, so run them off the main thread. */
interface MetadataProvider {
    val source: Source
    fun search(query: String): List<Found>
    /** True when [url] is a link this source understands. */
    fun handlesLink(url: String): Boolean
    /** Looks up a pasted link. Returns null if the link isn't one of ours or nothing was found. */
    fun fromLink(url: String): Found?
}

/** Picks the provider for a list (by its kind) and for a pasted link. */
object Sources {
    private val apple by lazy { AppleProvider() }

    fun forSource(s: Source): MetadataProvider = when (s) {
        Source.APPLE -> apple
    }

    fun forKind(k: Kind): MetadataProvider? = k.source?.let { forSource(it) }

    /** Which source a pasted link belongs to, if any. */
    fun forLink(url: String): MetadataProvider? = Source.entries.map { forSource(it) }.firstOrNull { it.handlesLink(url) }
}

// ───────── Apple Music (iTunes Search API) ─────────

object AppleLinks {
    // music.apple.com/{country}/album/{slug}/{id}  (the slug can be missing; ?i=… points at a song inside the album)
    private val album = Regex("""(?:music|itunes)\.apple\.com/([a-zA-Z]{2})/album/(?:[^/?#\s]+/)?(?:id)?(\d+)""")

    /** Country (two letters, lower case) and album id, or null. */
    fun parse(url: String): Pair<String, String>? {
        val m = album.find(url.trim()) ?: return null
        return m.groupValues[1].lowercase() to m.groupValues[2]
    }
}

class AppleProvider(
    private val get: (String) -> String = Http::get,
    private val gate: RateGate = RateGate(15, 60_000), // Apple allows about 20 a minute
) : MetadataProvider {
    override val source = Source.APPLE

    override fun search(query: String): List<Found> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        gate.check()
        val term = URLEncoder.encode(q, "UTF-8")
        return parse(get("https://itunes.apple.com/search?term=$term&entity=album&limit=10"))
    }

    override fun handlesLink(url: String): Boolean = AppleLinks.parse(url) != null

    override fun fromLink(url: String): Found? {
        val (country, id) = AppleLinks.parse(url) ?: return null
        gate.check()
        return parse(get("https://itunes.apple.com/lookup?id=$id&country=$country")).firstOrNull()
    }

    companion object {
        /** Apple gives 100x100 pictures; the same address with another size gives a bigger one. */
        fun artwork(url100: String, size: Int): String =
            Regex("""/\d+x\d+(bb|cc|sr)?\.(jpg|png|webp)$""").replace(url100) { "/${size}x${size}${it.groupValues[1]}.${it.groupValues[2]}" }

        /** Turns the JSON of a search or lookup into results. Skips anything that isn't an album. */
        fun parse(json: String): List<Found> {
            val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
            val out = ArrayList<Found>()
            for (i in 0 until results.length()) {
                val o = results.optJSONObject(i) ?: continue
                val id = o.optLong("collectionId", 0L)
                val name = o.optString("collectionName", "")
                if (id == 0L || name.isBlank()) continue
                if (o.has("wrapperType") && o.optString("wrapperType") != "collection") continue
                val art = o.optString("artworkUrl100", "")
                val tracks = o.optInt("trackCount", 0)
                out.add(
                    Found(
                        sourceName = Source.APPLE.label,
                        sourceId = id.toString(),
                        sourceUrl = o.optString("collectionViewUrl", ""),
                        title = name,
                        artist = o.optString("artistName", ""),
                        year = o.optString("releaseDate", "").take(4).takeIf { it.length == 4 && it.all(Char::isDigit) } ?: "",
                        genre = o.optString("primaryGenreName", ""),
                        description = if (tracks > 0) "$tracks ${if (tracks == 1) "song" else "songs"}" else "",
                        coverUrl = if (art.isEmpty()) "" else artwork(art, 600),
                        thumbUrl = art,
                    )
                )
            }
            return out
        }
    }
}
