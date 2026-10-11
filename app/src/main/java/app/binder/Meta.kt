package app.binder

import org.json.JSONObject
import java.net.URLEncoder

/**
 * Text of a field, or "" when it is missing or JSON null. (Android's org.json turns a JSON null into the text "null",
 * so optString alone is not safe for fields that sources fill with null.)
 */
internal fun JSONObject.str(key: String): String = if (isNull(key)) "" else optString(key, "")

/** What a source knows about one thing, before it becomes an item in a list. */
data class Found(
    val sourceName: String,
    val sourceId: String,
    val sourceUrl: String,
    val title: String,
    val artist: String = "",       // artist / author / year / seasons / platform / city: goes into Item.sub (see Kind.subLabel)
    val year: String = "",
    val genre: String = "",
    val description: String = "",
    val coverUrl: String = "",     // full-size picture (saved on the phone)
    val thumbUrl: String = "",     // small picture for the result rows
    val extra: String = "",        // optional: runtime, episodes, pages, volumes, platforms (see Kind.extraLabel)
)

/** Turns a result into a normal item. [images] are the file names of the downloaded cover. A source's own rating is never imported. */
fun Found.toItem(images: List<String> = emptyList(), pending: Boolean = false): Item = Item(
    title = title, sub = artist, images = images,
    sourceName = sourceName, sourceId = sourceId, sourceUrl = sourceUrl,
    year = year, genre = genre, description = description, detailsPending = pending, extra = extra,
)

/** The one thing every source has to do. All calls block, so run them off the main thread. */
interface MetadataProvider {
    val source: Source
    fun search(query: String): List<Found>
    /** True when [url] is a link this source understands. */
    fun handlesLink(url: String): Boolean
    /** Looks up a pasted link. Returns null if the link isn't one of ours or nothing was found. */
    fun fromLink(url: String): Found?
    /** Adds what a search result doesn't carry (runtime, seasons, genres ...) when one is picked. Default: nothing more. */
    fun details(f: Found): Found = f
    /** True when the source should only be asked when the person presses enter, not on every pause in typing. */
    val searchOnEnter: Boolean get() = false
}

/**
 * API keys, filled in at start-up from BuildConfig (see app/build.gradle.kts). Blank means the build has no key,
 * and the source that needs it says so and is hidden in More. Kept here so the providers can be tested without Android.
 */
object Keys {
    @Volatile var tmdb: String = ""
    @Volatile var rawg: String = ""

    fun forSource(s: Source): String = when (s) {
        Source.TMDB -> tmdb
        Source.RAWG -> rawg
        else -> ""
    }
}

/** Picks the provider for a list (by its kind and choice) and for a pasted link. */
object Sources {
    private val apple by lazy { AppleProvider() }
    private val tmdbMovie by lazy { TmdbProvider(TmdbProvider.Media.MOVIE) }
    private val tmdbTv by lazy { TmdbProvider(TmdbProvider.Media.TV) }
    private val rawg by lazy { RawgProvider() }
    private val aniAnime by lazy { AniListProvider(AniListProvider.Type.ANIME) }
    private val aniManga by lazy { AniListProvider(AniListProvider.Type.MANGA) }
    private val nominatim by lazy { NominatimProvider() }
    private val wikipedia by lazy { WikipediaProvider() }
    private val web by lazy { WebPageProvider() }
    private val googleBooks by lazy { GoogleBooksProvider() }
    private val openLibrary by lazy { OpenLibraryProvider(fallback = googleBooks) }

    /** Some sources work differently per kind (TMDB: movies or series), so the kind can be passed. */
    fun forSource(s: Source, kind: Kind? = null): MetadataProvider = when (s) {
        Source.APPLE -> apple
        Source.TMDB -> if (kind == Kind.SERIES) tmdbTv else tmdbMovie
        Source.RAWG -> rawg
        Source.OPENLIBRARY -> openLibrary
        Source.GOOGLEBOOKS -> googleBooks
        Source.ANILIST -> if (kind == Kind.MANGA) aniManga else aniAnime
        Source.NOMINATIM -> nominatim
        Source.WIKIPEDIA -> wikipedia
        Source.WEB -> web
    }

    /** False for a source that needs a key this build doesn't have. */
    fun isReady(s: Source): Boolean = !s.needsKey || Keys.forSource(s).isNotBlank()

    /** The sources a kind offers that this build can actually use. The first is the default. */
    fun readySources(k: Kind): List<Source> = k.sources.filter { isReady(it) }

    /** The source a list uses: its own choice if it is still available, else the first the kind offers. */
    fun sourceFor(l: BinderList): Source? {
        val ready = readySources(l.kind)
        return l.source?.takeIf { it in ready } ?: ready.firstOrNull()
    }

    fun forList(l: BinderList): MetadataProvider? = sourceFor(l)?.let { forSource(it, l.kind) }

    /** The source a saved item came from (items store its label). */
    fun byLabel(label: String): Source? = Source.entries.firstOrNull { it.label == label }

    /** Which source a pasted link belongs to, if any. Specific sources come first, so the patterns never clash. */
    fun forLink(url: String): MetadataProvider? = listOf(apple, tmdbMovie, rawg, openLibrary, googleBooks, aniAnime, nominatim, wikipedia, web).firstOrNull { it.handlesLink(url) }
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

// ───────── TMDB (movies and series) ─────────
// API: https://developer.themoviedb.org  (v3, key as the api_key parameter; about 40 requests a second allowed)
// Posters: https://image.tmdb.org/t/p/{size}{poster_path}
// Terms: free for non-commercial use with the attribution notice shown in About > Licences.

object TmdbLinks {
    // themoviedb.org/movie/603-the-matrix, themoviedb.org/tv/1396, themoviedb.org/en/movie/603 ...
    private val link = Regex("""themoviedb\.org/(?:[a-zA-Z]{2}(?:-[a-zA-Z]{2})?/)?(movie|tv)/(\d+)""")

    /** "movie" or "tv", and the id; or null. */
    fun parse(url: String): Pair<String, String>? {
        val m = link.find(url.trim()) ?: return null
        return m.groupValues[1] to m.groupValues[2]
    }
}

class TmdbProvider(
    private val media: Media,
    private val key: () -> String = { Keys.tmdb },
    private val get: (String) -> String = Http::get,
    private val gate: RateGate = RateGate(30, 10_000),
) : MetadataProvider {
    enum class Media(val path: String) { MOVIE("movie"), TV("tv") }

    override val source = Source.TMDB

    private fun base(path: String, extra: String = ""): String {
        val k = key()
        if (k.isBlank()) throw FetchFailure("TMDB isn't set up in this build (no API key).")
        return "https://api.themoviedb.org/3/$path?api_key=${URLEncoder.encode(k, "UTF-8")}$extra"
    }

    override fun search(query: String): List<Found> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val url = base("search/${media.path}", "&include_adult=false&query=${URLEncoder.encode(q, "UTF-8")}")
        gate.check()
        return parseSearch(get(url), media)
    }

    override fun handlesLink(url: String): Boolean = TmdbLinks.parse(url) != null

    override fun fromLink(url: String): Found? {
        val (type, id) = TmdbLinks.parse(url) ?: return null
        return fetch(if (type == "tv") Media.TV else Media.MOVIE, id)
    }

    /** The search result has no runtime, seasons or genres, so they are fetched once when a result is picked. */
    override fun details(f: Found): Found {
        val (type, id) = f.sourceId.split("/").takeIf { it.size == 2 } ?: return f
        return fetch(if (type == "tv") Media.TV else Media.MOVIE, id) ?: f
    }

    private fun fetch(m: Media, id: String): Found? {
        val url = base("${m.path}/${URLEncoder.encode(id, "UTF-8")}")
        gate.check()
        return parseDetails(get(url), m)
    }

    companion object {
        const val IMAGE = "https://image.tmdb.org/t/p/w500"

        private fun poster(path: String): String = if (path.isBlank() || path == "null") "" else IMAGE + path
        private fun yearOf(date: String): String = date.take(4).takeIf { it.length == 4 && it.all(Char::isDigit) } ?: ""

        /** 136 gives "2 h 16 min", 45 gives "45 min", 120 gives "2 h". */
        fun runtime(minutes: Int): String = when {
            minutes <= 0 -> ""
            minutes < 60 -> "$minutes min"
            minutes % 60 == 0 -> "${minutes / 60} h"
            else -> "${minutes / 60} h ${minutes % 60} min"
        }

        private fun plural(n: Int, one: String, many: String) = "$n ${if (n == 1) one else many}"

        private fun found(o: JSONObject, m: Media): Found? {
            val id = o.optLong("id", 0L)
            val title = (if (m == Media.TV) o.str("name") else o.str("title")).trim()
            if (id == 0L || title.isBlank()) return null
            val year = yearOf(if (m == Media.TV) o.str("first_air_date") else o.str("release_date"))
            val art = poster(o.str("poster_path"))
            return Found(
                sourceName = Source.TMDB.label,
                sourceId = "${m.path}/$id",
                sourceUrl = "https://www.themoviedb.org/${m.path}/$id",
                title = title,
                artist = year,                      // Movies: sub is the year (Kind.subLabel); Series get "N seasons" from the details
                year = year,
                description = o.str("overview").trim(),
                coverUrl = art, thumbUrl = art,
            )
        }

        /** The JSON of search/movie or search/tv. Rating fields are ignored on purpose: the rating is yours. */
        fun parseSearch(json: String, m: Media): List<Found> {
            val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
            val out = ArrayList<Found>()
            for (i in 0 until results.length()) {
                if (out.size >= 10) break
                val o = results.optJSONObject(i) ?: continue
                found(o, m)?.let { out.add(it) }
            }
            return out
        }

        /** The JSON of movie/{id} or tv/{id}: adds genres, runtime, seasons and episodes. */
        fun parseDetails(json: String, m: Media): Found? {
            val o = JSONObject(json)
            val base = found(o, m) ?: return null
            val genres = o.optJSONArray("genres")?.let { arr ->
                (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.str("name")?.takeIf { n -> n.isNotBlank() } }
            }.orEmpty().joinToString(", ")
            return if (m == Media.MOVIE) {
                base.copy(genre = genres, extra = runtime(o.optInt("runtime", 0)))
            } else {
                val seasons = o.optInt("number_of_seasons", 0)
                val episodes = o.optInt("number_of_episodes", 0)
                base.copy(
                    genre = genres,
                    artist = if (seasons > 0) plural(seasons, "season", "seasons") else base.artist,
                    extra = if (episodes > 0) episodes.toString() else "",
                )
            }
        }
    }
}

// ───────── RAWG (games) ─────────
// API: https://api.rawg.io/api/games?key=…&search=…  (one key parameter; the free plan allows 20,000 requests a month)
// Terms: free for non-commercial use if RAWG is credited and linked wherever its data is shown (About > Licences).
// Note: RAWG's own ratings are never imported.

object RawgLinks {
    // rawg.io/games/grand-theft-auto-v  (the slug works as the id in the API)
    private val link = Regex("""rawg\.io/games/([A-Za-z0-9][A-Za-z0-9_\-]*)""")

    fun parse(url: String): String? = link.find(url.trim())?.groupValues?.get(1)
}

class RawgProvider(
    private val key: () -> String = { Keys.rawg },
    private val get: (String) -> String = Http::get,
    private val gate: RateGate = RateGate(10, 10_000),
) : MetadataProvider {
    override val source = Source.RAWG

    private fun url(path: String, extra: String = ""): String {
        val k = key()
        if (k.isBlank()) throw FetchFailure("RAWG isn't set up in this build (no API key).")
        return "https://api.rawg.io/api/$path?key=${URLEncoder.encode(k, "UTF-8")}$extra"
    }

    override fun search(query: String): List<Found> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val u = url("games", "&page_size=10&search=${URLEncoder.encode(q, "UTF-8")}")
        gate.check()
        return parseSearch(get(u))
    }

    override fun handlesLink(url: String): Boolean = RawgLinks.parse(url) != null

    override fun fromLink(url: String): Found? {
        val slug = RawgLinks.parse(url) ?: return null
        return fetch(slug)
    }

    /** The search result has no description, so it is fetched once when a result is picked. */
    override fun details(f: Found): Found {
        val full = fetch(f.sourceId) ?: return f
        return full.copy(coverUrl = full.coverUrl.ifEmpty { f.coverUrl })
    }

    private fun fetch(idOrSlug: String): Found? {
        val u = url("games/${URLEncoder.encode(idOrSlug, "UTF-8")}")
        gate.check()
        return parseGame(JSONObject(get(u)))
    }

    companion object {
        private fun names(arr: org.json.JSONArray?, inner: String? = null): List<String> {
            if (arr == null) return emptyList()
            return (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                (if (inner == null) o else o.optJSONObject(inner))?.str("name")?.takeIf { it.isNotBlank() }
            }
        }

        /** One game object, from the list or from games/{id}. */
        fun parseGame(o: JSONObject): Found? {
            val id = o.optLong("id", 0L)
            val name = o.str("name").trim()
            if (id == 0L || name.isBlank()) return null
            val platforms = names(o.optJSONArray("platforms"), "platform")
            val art = o.str("background_image").takeIf { it.isNotBlank() && it != "null" } ?: ""
            val slug = o.str("slug")
            return Found(
                sourceName = Source.RAWG.label,
                sourceId = id.toString(),
                sourceUrl = if (slug.isBlank()) "" else "https://rawg.io/games/$slug",
                title = name,
                artist = platforms.take(2).joinToString(", "),   // Kind.subLabel for games is Platform
                year = o.str("released").take(4).takeIf { it.length == 4 && it.all(Char::isDigit) } ?: "",
                genre = names(o.optJSONArray("genres")).joinToString(", "),
                description = o.str("description_raw").trim(),
                coverUrl = art, thumbUrl = art,
                extra = platforms.joinToString(", "),
            )
        }

        fun parseSearch(json: String): List<Found> {
            val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
            return (0 until results.length()).mapNotNull { results.optJSONObject(it)?.let(::parseGame) }
        }
    }
}

// ───────── Open Library (books), with Google Books as the fallback ─────────
// Search: https://openlibrary.org/search.json?q=…&fields=…   Covers: https://covers.openlibrary.org/b/id/{cover_i}-L.jpg
// Rules (openlibrary.org/developers/api): identify the app in the User-Agent (3 requests a second then, 1 without),
// cache, don't crawl. Cover lookups by cover id are not rate limited; a link back to Open Library is appreciated.

object OpenLibraryLinks {
    private val work = Regex("""openlibrary\.org/works/(OL\d+W)""")
    private val edition = Regex("""openlibrary\.org/books/(OL\d+M)""")
    private val isbnLink = Regex("""openlibrary\.org/isbn/([0-9Xx\-]{10,17})""")

    /** The Solr query for a pasted Open Library link, or null. */
    fun query(url: String): String? {
        val u = url.trim()
        work.find(u)?.let { return "key:/works/${it.groupValues[1]}" }
        edition.find(u)?.let { return "edition_key:${it.groupValues[1]}" }
        isbnLink.find(u)?.let { return "isbn:${it.groupValues[1].replace("-", "")}" }
        return null
    }

    /** 10 or 13 digits (a trailing X is allowed for ISBN-10), with or without dashes and spaces. */
    fun isbn(text: String): String? {
        val t = text.trim().replace("-", "").replace(" ", "")
        return t.takeIf { Regex("""\d{13}|\d{9}[0-9Xx]""").matches(it) }?.uppercase()
    }
}

class OpenLibraryProvider(
    private val fallback: MetadataProvider? = null,
    private val get: (String) -> String = Http::get,
    private val gate: RateGate = RateGate(3, 3_000), // 1 a second is allowed without a contact in the User-Agent
) : MetadataProvider {
    override val source = Source.OPENLIBRARY

    private fun searchUrl(q: String) =
        "https://openlibrary.org/search.json?limit=10&fields=$FIELDS&q=${URLEncoder.encode(q, "UTF-8")}"

    override fun search(query: String): List<Found> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val solr = OpenLibraryLinks.isbn(q)?.let { "isbn:$it" } ?: q
        return withFallback({ ask(solr) }) { it.search(q) }
    }

    override fun handlesLink(url: String): Boolean = OpenLibraryLinks.query(url) != null

    override fun fromLink(url: String): Found? {
        val q = OpenLibraryLinks.query(url) ?: return null
        return ask(q).firstOrNull()
    }

    private fun ask(q: String): List<Found> {
        gate.check()
        return parse(get(searchUrl(q)))
    }

    /** Our own answer first; if the source is down or knows nothing, the fallback gets a go. Offline is not retried. */
    private fun withFallback(own: () -> List<Found>, other: (MetadataProvider) -> List<Found>): List<Found> {
        val fb = fallback
        val r = runCatching(own)
        val err = r.exceptionOrNull()
        if (err is FetchFailure && err.offline) throw err
        val list = r.getOrNull()
        if (!list.isNullOrEmpty() || fb == null) return list ?: throw (err ?: FetchFailure("Couldn't read the answer."))
        return runCatching { other(fb) }.getOrElse { if (err != null) throw err else emptyList() }
    }

    companion object {
        const val FIELDS = "key,title,author_name,first_publish_year,cover_i,number_of_pages_median"

        /** The JSON of search.json. */
        fun parse(json: String): List<Found> {
            val docs = JSONObject(json).optJSONArray("docs") ?: return emptyList()
            val out = ArrayList<Found>()
            for (i in 0 until docs.length()) {
                val o = docs.optJSONObject(i) ?: continue
                val key = o.str("key")            // "/works/OL45804W"
                val title = o.str("title").trim()
                if (key.isBlank() || title.isBlank()) continue
                val authors = o.optJSONArray("author_name")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() } }.orEmpty()
                val cover = o.optLong("cover_i", 0L)
                val pages = o.optInt("number_of_pages_median", 0)
                out.add(
                    Found(
                        sourceName = Source.OPENLIBRARY.label,
                        sourceId = key.removePrefix("/works/"),
                        sourceUrl = "https://openlibrary.org$key",
                        title = title,
                        artist = authors.take(3).joinToString(", "),   // Kind.subLabel for books is Author
                        year = o.optInt("first_publish_year", 0).takeIf { it in 1000..2999 }?.toString() ?: "",
                        coverUrl = if (cover > 0) "https://covers.openlibrary.org/b/id/$cover-L.jpg" else "",
                        thumbUrl = if (cover > 0) "https://covers.openlibrary.org/b/id/$cover-M.jpg" else "",
                        extra = if (pages > 0) pages.toString() else "",
                    )
                )
            }
            return out
        }
    }
}

// ───────── Google Books (fallback for books, and the source for comics) ─────────
// https://www.googleapis.com/books/v1/volumes?q=…   Public searches work without a key but share a small anonymous quota.

object GoogleBooksLinks {
    private val a = Regex("""books\.google\.[a-z.]+/books\?(?:[^#\s]*&)?id=([A-Za-z0-9_\-]{6,})""")
    private val b = Regex("""google\.[a-z.]+/books/edition/[^/\s]*/([A-Za-z0-9_\-]{6,})""")

    fun parse(url: String): String? {
        val u = url.trim()
        return (a.find(u) ?: b.find(u))?.groupValues?.get(1)
    }
}

class GoogleBooksProvider(
    private val get: (String) -> String = Http::get,
    private val gate: RateGate = RateGate(10, 10_000),
) : MetadataProvider {
    override val source = Source.GOOGLEBOOKS

    override fun search(query: String): List<Found> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val term = OpenLibraryLinks.isbn(q)?.let { "isbn:$it" } ?: q
        gate.check()
        return parse(get("https://www.googleapis.com/books/v1/volumes?maxResults=10&printType=books&q=${URLEncoder.encode(term, "UTF-8")}"))
    }

    override fun handlesLink(url: String): Boolean = GoogleBooksLinks.parse(url) != null

    override fun fromLink(url: String): Found? {
        val id = GoogleBooksLinks.parse(url) ?: return null
        gate.check()
        return parseVolume(JSONObject(get("https://www.googleapis.com/books/v1/volumes/${URLEncoder.encode(id, "UTF-8")}")))
    }

    companion object {
        private fun https(u: String) = if (u.startsWith("http://")) "https://" + u.removePrefix("http://") else u

        fun parseVolume(o: JSONObject): Found? {
            val id = o.str("id")
            val v = o.optJSONObject("volumeInfo") ?: return null
            val title = v.str("title").trim()
            if (id.isBlank() || title.isBlank()) return null
            fun list(key: String) = v.optJSONArray(key)?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() } }.orEmpty()
            val img = https(v.optJSONObject("imageLinks")?.str("thumbnail").orEmpty())
            val pages = v.optInt("pageCount", 0)
            val sub = v.str("subtitle").trim()
            return Found(
                sourceName = Source.GOOGLEBOOKS.label,
                sourceId = id,
                sourceUrl = https(v.str("infoLink")).ifBlank { "https://books.google.com/books?id=$id" },
                title = if (sub.isEmpty()) title else "$title: $sub",
                artist = list("authors").take(3).joinToString(", "),
                year = v.str("publishedDate").take(4).takeIf { it.length == 4 && it.all(Char::isDigit) } ?: "",
                genre = list("categories").take(2).joinToString(", "),
                description = v.str("description").trim(),
                coverUrl = img, thumbUrl = img,
                extra = if (pages > 0) pages.toString() else "",
            )
        }

        fun parse(json: String): List<Found> {
            val items = JSONObject(json).optJSONArray("items") ?: return emptyList()
            return (0 until items.length()).mapNotNull { items.optJSONObject(it)?.let(::parseVolume) }
        }
    }
}

// ───────── AniList (anime and manga, GraphQL, no key) ─────────
// POST https://graphql.anilist.co with {"query": …, "variables": …}. 90 requests a minute.
// Terms (docs.anilist.co/guide/terms-of-use): free for non-commercial use; no using it as a data backup or for mass
// collection; not for competing anime/manga list trackers. Their score is never imported.

object AniListLinks {
    private val link = Regex("""anilist\.co/(anime|manga)/(\d+)""")

    /** "anime" or "manga", and the id; or null. */
    fun parse(url: String): Pair<String, String>? {
        val m = link.find(url.trim()) ?: return null
        return m.groupValues[1] to m.groupValues[2]
    }
}

class AniListProvider(
    private val type: Type,
    private val post: (String, String) -> String = { url, body -> Http.postJson(url, body, mapOf("Accept" to "application/json")) },
    private val gate: RateGate = RateGate(40, 60_000),
) : MetadataProvider {
    enum class Type(val gql: String) { ANIME("ANIME"), MANGA("MANGA") }

    override val source = Source.ANILIST

    override fun search(query: String): List<Found> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val vars = JSONObject().put("search", q).put("type", type.gql)
        return parse(ask(SEARCH, vars))
    }

    override fun handlesLink(url: String): Boolean = AniListLinks.parse(url) != null

    override fun fromLink(url: String): Found? {
        val (kind, id) = AniListLinks.parse(url) ?: return null
        val vars = JSONObject().put("id", id.toInt()).put("type", if (kind == "manga") "MANGA" else "ANIME")
        return parse(ask(BY_ID, vars)).firstOrNull()
    }

    private fun ask(query: String, vars: JSONObject): String {
        gate.check()
        return post(ENDPOINT, JSONObject().put("query", query).put("variables", vars).toString())
    }

    companion object {
        const val ENDPOINT = "https://graphql.anilist.co"
        private const val FIELDS = "id title { romaji english native } format startDate { year } episodes chapters volumes " +
            "coverImage { extraLarge large medium } description(asHtml: false) genres siteUrl staff(perPage: 3) { nodes { name { full } } }"
        const val SEARCH = "query (\$search: String, \$type: MediaType) { Page(page: 1, perPage: 10) { media(search: \$search, type: \$type) { $FIELDS } } }"
        const val BY_ID = "query (\$id: Int, \$type: MediaType) { Media(id: \$id, type: \$type) { $FIELDS } }"

        private val tags = Regex("<[^>]+>")
        private fun plain(s: String) = tags.replace(s.replace("<br>", " ").replace("<br/>", " "), "").replace(Regex("\\s+"), " ").trim()

        private fun pretty(format: String): String = when (format) {
            "" -> ""
            "TV", "OVA", "ONA" -> format
            "TV_SHORT" -> "TV short"
            "MOVIE" -> "Movie"
            "SPECIAL" -> "Special"
            "MUSIC" -> "Music"
            "MANGA" -> "Manga"
            "NOVEL" -> "Novel"
            "ONE_SHOT" -> "One shot"
            else -> format.lowercase().replaceFirstChar { it.uppercase() }
        }

        private fun one(o: JSONObject): Found? {
            val id = o.optLong("id", 0L)
            val t = o.optJSONObject("title")
            val title = listOf(t?.str("english").orEmpty(), t?.str("romaji").orEmpty(), t?.str("native").orEmpty()).firstOrNull { it.isNotBlank() }?.trim()
            if (id == 0L || title == null) return null
            val isManga = o.str("siteUrl").contains("/manga/") || o.str("format").let { it == "MANGA" || it == "NOVEL" || it == "ONE_SHOT" }
            val cover = o.optJSONObject("coverImage")
            val art = listOf(cover?.str("extraLarge").orEmpty(), cover?.str("large").orEmpty(), cover?.str("medium").orEmpty()).firstOrNull { it.isNotBlank() } ?: ""
            val thumb = listOf(cover?.str("medium").orEmpty(), art).firstOrNull { it.isNotBlank() } ?: ""
            val genres = o.optJSONArray("genres")?.let { a -> (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() } }.orEmpty()
            val staff = o.optJSONObject("staff")?.optJSONArray("nodes")?.let { a ->
                (0 until a.length()).mapNotNull { a.optJSONObject(it)?.optJSONObject("name")?.str("full")?.takeIf { n -> n.isNotBlank() } }
            }.orEmpty()
            val year = o.optJSONObject("startDate")?.optInt("year", 0) ?: 0
            val episodes = o.optInt("episodes", 0)
            val volumes = o.optInt("volumes", 0)
            val chapters = o.optInt("chapters", 0)
            return Found(
                sourceName = Source.ANILIST.label,
                sourceId = id.toString(),
                sourceUrl = o.str("siteUrl").ifBlank { "https://anilist.co/${if (isManga) "manga" else "anime"}/$id" },
                title = title,
                // manga: Kind.subLabel is Author; anime has no seasons here, so the format (TV, Movie, OVA …) goes in the sub
                artist = if (isManga) staff.take(2).joinToString(", ") else pretty(o.str("format")),
                year = if (year in 1000..2999) year.toString() else "",
                genre = genres.take(3).joinToString(", "),
                description = plain(o.str("description")),
                coverUrl = art, thumbUrl = thumb,
                extra = when {
                    !isManga && episodes > 0 -> episodes.toString()
                    isManga && volumes > 0 -> volumes.toString()
                    isManga && chapters > 0 -> "$chapters chapters"
                    else -> ""
                },
            )
        }

        /** The JSON answer of either query. A GraphQL error becomes a FetchFailure. */
        fun parse(json: String): List<Found> {
            val root = JSONObject(json)
            val data = root.optJSONObject("data")
            if (data == null || data.isNull("Page") && data.isNull("Media")) {
                if (root.optJSONArray("errors") != null) throw FetchFailure("AniList couldn't answer that.")
                return emptyList()
            }
            val page = data.optJSONObject("Page")?.optJSONArray("media")
            if (page != null) return (0 until page.length()).mapNotNull { page.optJSONObject(it)?.let(::one) }
            return listOfNotNull(data.optJSONObject("Media")?.let(::one))
        }
    }
}

// ───────── Wikipedia (Anything) ─────────
// Search: https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=…&format=json
// Summary: https://{lang}.wikipedia.org/api/rest_v1/page/summary/{title}  (title, description, extract, thumbnail, originalimage)
// Text is CC BY-SA; the item keeps a link back to the article. Wikimedia asks API clients to put contact info in the User-Agent.

object WikipediaLinks {
    private val link = Regex("""https?://([a-z\-]{2,12})(?:\.m)?\.wikipedia\.org/wiki/([^?#\s]+)""", RegexOption.IGNORE_CASE)

    /** Language code and the (still URL-encoded) article title, or null. */
    fun parse(url: String): Pair<String, String>? {
        val m = link.find(url.trim()) ?: return null
        return m.groupValues[1].lowercase() to m.groupValues[2]
    }
}

class WikipediaProvider(
    private val get: (String) -> String = Http::get,
    private val gate: RateGate = RateGate(10, 10_000),
) : MetadataProvider {
    override val source = Source.WIKIPEDIA

    override fun search(query: String): List<Found> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        gate.check()
        return parseSearch(get("https://en.wikipedia.org/w/api.php?action=query&list=search&srlimit=8&format=json&srsearch=${URLEncoder.encode(q, "UTF-8")}"))
    }

    override fun handlesLink(url: String): Boolean = WikipediaLinks.parse(url) != null

    override fun fromLink(url: String): Found? {
        val (lang, title) = WikipediaLinks.parse(url) ?: return null
        return summary(lang, title)
    }

    /** A search hit has only a title and a snippet, so the summary (text, picture) is fetched when one is picked. */
    override fun details(f: Found): Found {
        val (lang, title) = f.sourceId.split(":", limit = 2).takeIf { it.size == 2 } ?: return f
        return summary(lang, title) ?: f
    }

    private fun summary(lang: String, encodedTitle: String): Found? {
        gate.check()
        return parseSummary(get("https://$lang.wikipedia.org/api/rest_v1/page/summary/$encodedTitle"), lang)
    }

    companion object {
        private val tags = Regex("<[^>]+>")

        fun parseSearch(json: String): List<Found> {
            val hits = JSONObject(json).optJSONObject("query")?.optJSONArray("search") ?: return emptyList()
            val out = ArrayList<Found>()
            for (i in 0 until hits.length()) {
                val o = hits.optJSONObject(i) ?: continue
                val title = o.str("title").trim()
                if (title.isBlank()) continue
                val snippet = Html.decode(tags.replace(o.str("snippet"), "")).replace(Regex("\\s+"), " ").trim().take(80)
                out.add(
                    Found(
                        sourceName = Source.WIKIPEDIA.label,
                        sourceId = "en:" + URLEncoder.encode(title.replace(' ', '_'), "UTF-8").replace("+", "%20"),
                        sourceUrl = "https://en.wikipedia.org/wiki/" + URLEncoder.encode(title.replace(' ', '_'), "UTF-8").replace("+", "%20"),
                        title = title,
                        artist = snippet,
                    )
                )
            }
            return out
        }

        /** The JSON of page/summary/{title}. Null for a page that doesn't exist. */
        fun parseSummary(json: String, lang: String = "en"): Found? {
            val o = JSONObject(json)
            val title = o.str("title").trim()
            if (title.isBlank()) return null
            val thumb = o.optJSONObject("thumbnail")?.str("source").orEmpty()
            val orig = o.optJSONObject("originalimage")
            val origUrl = orig?.str("source").orEmpty()
            // the original can be huge; use it only when it is of a sensible size
            val big = if (origUrl.isNotBlank() && (orig?.optInt("width", 0) ?: 0) in 1..2400 && (orig?.optInt("height", 0) ?: 0) in 1..2400) origUrl else thumb
            val page = o.optJSONObject("content_urls")?.optJSONObject("desktop")?.str("page").orEmpty()
            val extract = o.str("extract").trim()
            val desc = o.str("description").trim()
            return Found(
                sourceName = Source.WIKIPEDIA.label,
                sourceId = "$lang:" + URLEncoder.encode(title.replace(' ', '_'), "UTF-8").replace("+", "%20"),
                sourceUrl = page.ifBlank { "https://$lang.wikipedia.org/wiki/" + URLEncoder.encode(title.replace(' ', '_'), "UTF-8").replace("+", "%20") },
                title = title,
                artist = desc,                       // Kind.subLabel for Anything is Details: the short description
                description = if (o.str("type") == "disambiguation") "$extract (A disambiguation page: several things share this name.)".trim() else extract,
                coverUrl = big.ifBlank { thumb }, thumbUrl = thumb.ifBlank { big },
            )
        }
    }
}

// ───────── Any other web page (Open Graph tags) ─────────
// Reads og:title, og:image, og:description (og:site_name, og:url) from the page's own <meta> tags (ogp.me).
// Only used when you paste a link that no other source recognises, and only with "Fetch details automatically" on.

/** Just enough HTML text handling for meta tags. */
object Html {
    private val named = mapOf("amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ", "ndash" to "–", "mdash" to "—", "hellip" to "…", "rsquo" to "’", "lsquo" to "‘", "rdquo" to "”", "ldquo" to "“")

    fun decode(s: String): String = Regex("&(#x[0-9a-fA-F]+|#\\d+|[a-zA-Z]+);").replace(s) { m ->
        val g = m.groupValues[1]
        when {
            g.startsWith("#x") || g.startsWith("#X") -> g.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) } ?: m.value
            g.startsWith("#") -> g.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) } ?: m.value
            else -> named[g] ?: m.value
        }
    }
}

object OpenGraph {
    private val metaTag = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val attr = Regex("([a-zA-Z:\\-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)')")
    private val titleTag = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))

    /** property (lower case, e.g. "og:title") to content, first one wins. Attribute order doesn't matter. */
    fun tags(html: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (m in metaTag.findAll(html)) {
            val attrs = attr.findAll(m.value).associate { it.groupValues[1].lowercase() to (it.groupValues[2].ifEmpty { it.groupValues[3] }) }
            val key = (attrs["property"] ?: attrs["name"])?.lowercase() ?: continue
            val content = attrs["content"] ?: continue
            if (key !in out) out[key] = Html.decode(content).trim()
        }
        return out
    }

    fun pageTitle(html: String): String = titleTag.find(html)?.groupValues?.get(1)?.let { Html.decode(it).replace(Regex("\\s+"), " ").trim() } ?: ""

    /** Makes an og:image address absolute against the page's own address. */
    fun absolute(base: String, ref: String): String = when {
        ref.isBlank() -> ""
        ref.startsWith("http://", true) || ref.startsWith("https://", true) -> ref
        ref.startsWith("//") -> "https:$ref"
        else -> runCatching { java.net.URI(base).resolve(ref).toString() }.getOrDefault("")
    }
}

class WebPageProvider(
    private val get: (String) -> String = Http::get,
    private val gate: RateGate = RateGate(10, 60_000),
) : MetadataProvider {
    override val source = Source.WEB

    /** There is nothing to search: this source only reads pasted links. */
    override fun search(query: String): List<Found> = emptyList()

    override fun handlesLink(url: String): Boolean = Regex("""^https?://\S+$""", RegexOption.IGNORE_CASE).matches(url.trim())

    override fun fromLink(url: String): Found? {
        val u = url.trim()
        if (!handlesLink(u)) return null
        gate.check()
        return parse(u, get(u))
    }

    companion object {
        /** Builds a result from a page's HTML. Null when the page gives no title at all. */
        fun parse(url: String, html: String): Found? {
            val head = html.take(400_000) // the tags are in <head>; no need to look at a whole long page
            val t = OpenGraph.tags(head)
            val title = (t["og:title"] ?: t["twitter:title"]).orEmpty().ifBlank { OpenGraph.pageTitle(head) }.trim()
            if (title.isBlank()) return null
            val img = OpenGraph.absolute(url, (t["og:image"] ?: t["og:image:url"] ?: t["twitter:image"]).orEmpty())
            return Found(
                sourceName = Source.WEB.label,
                sourceId = url,
                sourceUrl = t["og:url"].orEmpty().takeIf { it.startsWith("http", true) } ?: url,
                title = title,
                artist = t["og:site_name"].orEmpty(),
                description = (t["og:description"] ?: t["description"]).orEmpty(),
                coverUrl = img, thumbUrl = img,
            )
        }
    }
}

// ───────── Nominatim / OpenStreetMap (places) ─────────
// https://nominatim.openstreetmap.org/search?q=…&format=jsonv2   Policy: operations.osmfoundation.org/policies/nominatim
// At most 1 request a second, an identifying User-Agent, no search-as-you-type (so this source waits for enter),
// and "© OpenStreetMap contributors" wherever the data is shown. No pictures.

object OsmLinks {
    private val link = Regex("""openstreetmap\.org/(node|way|relation)/(\d+)""")

    /** "N123", "W123" or "R123" (the form Nominatim's lookup wants), or null. */
    fun parse(url: String): String? {
        val m = link.find(url.trim()) ?: return null
        return m.groupValues[1].first().uppercaseChar().toString() + m.groupValues[2]
    }
}

class NominatimProvider(
    private val get: (String) -> String = Http::get,
    private val gate: RateGate = RateGate(1, 1_100),
) : MetadataProvider {
    override val source = Source.NOMINATIM
    override val searchOnEnter: Boolean get() = true

    override fun search(query: String): List<Found> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        gate.check()
        return parse(get("https://nominatim.openstreetmap.org/search?format=jsonv2&addressdetails=1&limit=8&q=${URLEncoder.encode(q, "UTF-8")}"))
    }

    override fun handlesLink(url: String): Boolean = OsmLinks.parse(url) != null

    override fun fromLink(url: String): Found? {
        val id = OsmLinks.parse(url) ?: return null
        gate.check()
        return parse(get("https://nominatim.openstreetmap.org/lookup?format=jsonv2&addressdetails=1&osm_ids=$id")).firstOrNull()
    }

    companion object {
        private fun pretty(s: String) = s.replace('_', ' ').replaceFirstChar { it.uppercase() }

        /** The JSON array of search or lookup (format=jsonv2). */
        fun parse(json: String): List<Found> {
            val arr = org.json.JSONArray(json)
            val out = ArrayList<Found>()
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val type = o.str("osm_type")
                val id = o.optLong("osm_id", 0L)
                val display = o.str("display_name").trim()
                val name = o.str("name").trim().ifBlank { display.substringBefore(",").trim() }
                if (type.isBlank() || id == 0L || name.isBlank()) continue
                val addr = o.optJSONObject("address")
                // Kind.subLabel for places is City: town and village stand in when there is no city
                val city = listOf("city", "town", "village", "municipality", "hamlet", "suburb", "county", "state")
                    .firstNotNullOfOrNull { k -> addr?.str(k)?.takeIf { it.isNotBlank() } }.orEmpty()
                val country = addr?.str("country").orEmpty()
                val cat = o.str("type").takeIf { it.isNotBlank() && it != "yes" } ?: o.str("category")
                out.add(
                    Found(
                        sourceName = Source.NOMINATIM.label,
                        sourceId = "$type/$id",
                        sourceUrl = "https://www.openstreetmap.org/$type/$id",
                        title = name,
                        artist = listOf(city, country).filter { it.isNotBlank() && it != name }.joinToString(", "),
                        genre = if (cat.isBlank()) "" else pretty(cat),
                        extra = display,
                    )
                )
            }
            return out
        }
    }
}
