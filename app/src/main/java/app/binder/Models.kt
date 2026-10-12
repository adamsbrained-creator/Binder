package app.binder

import java.util.UUID

enum class Layout(val label: String) {
    GRID("Grid"), LIST("List"), SHELF("Shelf"), SECTIONS("Sections"), CARDS("Cards")
}

enum class Sort(val label: String) {
    MANUAL("Manual"), NEWEST("Newest"), TITLE("A–Z"), RATING("Rating")
}

/**
 * Where details can be fetched from. [needsKey] sources only work in a build that has their key
 * (see Keys in Meta.kt); without it they say so and are hidden in More.
 */
enum class Source(val label: String, val openLabel: String, val needsKey: Boolean = false) {
    APPLE("Apple Music", "Open in Apple Music"),
    TMDB("TMDB", "Open on TMDB", needsKey = true),
    RAWG("RAWG", "Open on RAWG", needsKey = true),
    OPENLIBRARY("Open Library", "Open on Open Library"),
    GOOGLEBOOKS("Google Books", "Open in Google Books"),
    ANILIST("AniList", "Open on AniList"),
    NOMINATIM("OpenStreetMap", "Open on OpenStreetMap"),
    WIKIPEDIA("Wikipedia", "Open on Wikipedia"),
    WEB("Web page", "Open page"),     // any other pasted link (Open Graph tags); not a source you pick for a list
}

/** The kinds of list you can start from. Add a new one here and it shows up everywhere. */
enum class Kind(
    val label: String,
    val hint: String,
    val noun: String,
    val single: String,
    val subLabel: String,
    val stages: List<String>,
    val hasRating: Boolean,
    val square: Boolean = false,                 // album and game covers are square
    val sources: List<Source> = emptyList(),     // where details can come from; the first one is the default
    val extraLabel: String = "Details",          // what Item.extra holds for this kind (runtime, seasons, pages ...)
) {
    BOOKS("Books", "Title, author", "books", "book", "Author", listOf("To read", "Reading", "Done"), true, sources = listOf(Source.OPENLIBRARY, Source.GOOGLEBOOKS), extraLabel = "Pages"),
    MANGA("Manga", "Title, author", "manga", "manga", "Author", listOf("To read", "Reading", "Done"), true, sources = listOf(Source.ANILIST), extraLabel = "Volumes"),
    COMICS("Comics", "Title, writer", "comics", "comic", "Writer", listOf("To read", "Reading", "Done"), true, sources = listOf(Source.GOOGLEBOOKS), extraLabel = "Pages"),
    MOVIES("Movies", "Title, year", "movies", "movie", "Year", listOf("To watch", "Watching", "Watched"), true, sources = listOf(Source.TMDB), extraLabel = "Runtime"),
    SERIES("Series", "Seasons", "series", "series", "Seasons", listOf("To watch", "Watching", "Watched"), true, sources = listOf(Source.TMDB, Source.ANILIST), extraLabel = "Episodes"),
    ALBUMS("Albums", "Artist, year", "albums", "album", "Artist", listOf("To listen", "Listening", "Heard"), true, square = true, sources = listOf(Source.APPLE)),
    GAMES("Games", "Platform", "games", "game", "Platform", listOf("To play", "Playing", "Played"), true, square = true, sources = listOf(Source.RAWG), extraLabel = "Platforms"),
    PLACES("Places", "City", "places", "place", "City", listOf("Want to go", "Planned", "Visited"), true, sources = listOf(Source.NOMINATIM), extraLabel = "Address"),
    ANYTHING("Anything", "Your own", "things", "thing", "Details", listOf("Idea", "In progress", "Done"), true, sources = listOf(Source.WIKIPEDIA)),
    CHECKLIST("Checklist", "Items, done", "items", "item", "", listOf("To do", "Done"), false);

    val isChecklist: Boolean get() = this == CHECKLIST
    val layouts: List<Layout> get() = if (isChecklist) listOf(Layout.LIST, Layout.SECTIONS) else Layout.entries
}

/** What can be shown under a cover in the grid. */
enum class Under(val label: String) { TITLE("Title"), RATING("Rating"), ADDED("Added"), PROGRESS("Progress") }

data class Item(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val sub: String = "",
    val stage: Int = 0,
    val rating: Float = 0f,
    val note: String = "",
    val added: Long = System.currentTimeMillis(),
    val cover: Int = -1, // -1 = automatic colour
    val images: List<String> = emptyList(), // file names in files/images, the first one is the cover
    // filled in when details are fetched (all optional, old files simply have none)
    val sourceName: String = "",
    val sourceId: String = "",
    val sourceUrl: String = "",
    val year: String = "",
    val genre: String = "",
    val description: String = "",
    val detailsPending: Boolean = false, // saved offline, details still to be fetched
    val extra: String = "",              // optional: runtime, seasons, pages, volumes, platforms ... (see Kind.extraLabel)
)

data class BinderList(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kind: Kind,
    val layout: Layout = Layout.GRID,
    val sort: Sort = Sort.MANUAL,
    val pinned: Boolean = false,
    val items: List<Item> = emptyList(),
    val created: Long = System.currentTimeMillis(),
    val columns: Int = 3,                         // 2, 3 or 4 in the grid layout
    val under: Set<Under> = setOf(Under.TITLE, Under.RATING),
    val source: Source? = null,                   // optional: which source to fetch from (null = the first one the kind offers)
)

fun BinderList.doneCount(): Int = items.count { it.stage == kind.stages.lastIndex }
fun BinderList.progress(): Float = if (items.isEmpty()) 0f else doneCount().toFloat() / items.size
fun BinderList.activity(): Long = maxOf(created, items.maxOfOrNull { it.added } ?: 0L)

fun BinderList.countLabel(): String =
    if (kind.isChecklist) "${doneCount()} of ${items.size} done"
    else "${items.size} ${if (items.size == 1) kind.single else kind.noun}"

fun BinderList.sorted(): List<Item> = when (sort) {
    Sort.MANUAL -> items
    Sort.NEWEST -> items.sortedByDescending { it.added }
    Sort.TITLE -> items.sortedBy { it.title.lowercase() }
    Sort.RATING -> items.sortedByDescending { it.rating }
}

// ───────── look & feel options ─────────

enum class HeaderStyle(val label: String) {
    WORDMARK("Wordmark"), CIRCLES("Circles"), MENU_CENTER("Menu, centred"), MENU_LEFT("Menu, left")
}

/** How much room the top bar takes. LARGE is the original header, unchanged. */
enum class HeaderSize(val label: String) { COMPACT("Compact"), REGULAR("Regular"), LARGE("Large") }

/** The menu opened from the menu button. CLASSIC is the original sheet, unchanged. */
enum class MenuStyle(val label: String) { CLASSIC("Classic"), FLOATING("Floating") }

enum class NavStyle(val label: String) {
    NONE("None"), DOCK("Dock"), SPLIT("Split dock"), NOTCH("Notch bar"), SEARCH("Search bar"),
    TABS("Top tabs"), MINI("Mini dock"), PILL("Labelled pill"), RAIL("Side rail"), DRAWER("Drawer")
}

enum class HomeStyle(val label: String) {
    PINNED("Pinned"), TILES("Tiles"), ROWS("Rows"), STACKS("Stacks"), BENTO("Bento")
}

enum class AddStyle(val label: String) {
    SHEET("Quick sheet"), EDITOR("Full editor"), BAR("Add bar")
}

enum class ItemPage(val label: String) { STANDARD("Standard"), COLLAGE("Collage") }

enum class FontChoice(val label: String) {
    OUTFIT("Outfit"), LORA("Outfit + Lora"), CRIMSON("Outfit + Crimson"),
    INSTRUMENT("Outfit + Instrument Serif"), INSTSANS("Instrument Sans"), WORKSANS("Work Sans")
}

data class Opts(
    val theme: String = "system",
    val appName: String = "binder", // "binder" or "catalog"
    val font: FontChoice = FontChoice.OUTFIT,
    val header: HeaderStyle = HeaderStyle.WORDMARK,
    val headerSize: HeaderSize = HeaderSize.LARGE, // saved as the optional key "headerSize"; LARGE = the original look
    val menuStyle: MenuStyle = MenuStyle.CLASSIC, // saved as the optional key "menuStyle"; CLASSIC = the original menu
    val nav: NavStyle = NavStyle.NONE,
    val home: HomeStyle = HomeStyle.PINNED,
    val add: AddStyle = AddStyle.SHEET,
    val itemPage: ItemPage = ItemPage.STANDARD,
    val fetch: Boolean = false,           // fetch details from the internet (off = no network calls at all)
    val emptySlots: Boolean = true,       // dashed "+" slots in empty places
    val ratingStyle: RatingStyle = RatingStyle.STARS, // saved as the optional key "ratingStyle"
)

/** True when the app is set to Catalog (name, icon and wording all follow this one switch). */
val Opts.isCatalog: Boolean get() = appName == "catalog"

/** "Binder" or "Catalog": the app name as shown (wordmark, About, label). */
val Opts.appLabel: String get() = if (isCatalog) "Catalog" else "Binder"

/** The word for one list and for many: Bind/Binds, or Catalog/Catalogs. Follows [Opts.appName]. */
val Opts.lists: String get() = if (isCatalog) "Catalogs" else "Binds"
val Opts.list: String get() = if (isCatalog) "Catalog" else "Bind"
val Opts.listsLower: String get() = lists.lowercase()
val Opts.listLower: String get() = list.lowercase()
