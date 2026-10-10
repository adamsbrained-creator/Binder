package app.binder

import java.util.UUID

enum class Layout(val label: String) {
    GRID("Grid"), LIST("List"), SHELF("Shelf"), SECTIONS("Sections"), CARDS("Cards")
}

enum class Sort(val label: String) {
    MANUAL("Manual"), NEWEST("Newest"), TITLE("A–Z"), RATING("Rating")
}

/** Where details can be fetched from. Only Apple Music is wired up in 1.0.0. */
enum class Source(val label: String, val openLabel: String) {
    APPLE("Apple Music", "Open in Apple Music"),
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
    val square: Boolean = false, // album and game covers are square
    val source: Source? = null,  // where details come from, if anywhere
) {
    BOOKS("Books", "Title, author", "books", "book", "Author", listOf("To read", "Reading", "Done"), true),
    MOVIES("Movies", "Title, year", "movies", "movie", "Year", listOf("To watch", "Watching", "Watched"), true),
    SERIES("Series", "Seasons", "series", "series", "Seasons", listOf("To watch", "Watching", "Watched"), true),
    ALBUMS("Albums", "Artist, year", "albums", "album", "Artist", listOf("To listen", "Listening", "Heard"), true, square = true, source = Source.APPLE),
    GAMES("Games", "Platform", "games", "game", "Platform", listOf("To play", "Playing", "Played"), true, square = true),
    PLACES("Places", "City", "places", "place", "City", listOf("Want to go", "Planned", "Visited"), true),
    ANYTHING("Anything", "Your own", "things", "thing", "Details", listOf("Idea", "In progress", "Done"), true),
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

/** The word used for "lists" everywhere in the app. */
enum class ListWord(val plural: String, val single: String) {
    LISTS("Lists", "List"), BINDS("Binds", "Bind"), CATALOGS("Catalogs", "Catalog"), SLEEVES("Sleeves", "Sleeve")
}

data class Opts(
    val theme: String = "system",
    val appName: String = "binder", // "binder" or "catalog"
    val font: FontChoice = FontChoice.OUTFIT,
    val header: HeaderStyle = HeaderStyle.WORDMARK,
    val nav: NavStyle = NavStyle.NONE,
    val home: HomeStyle = HomeStyle.PINNED,
    val add: AddStyle = AddStyle.SHEET,
    val itemPage: ItemPage = ItemPage.STANDARD,
    val fetch: Boolean = false,           // fetch details from the internet (off = no network calls at all)
    val listName: ListWord = ListWord.LISTS,
    val emptySlots: Boolean = true,       // dashed "+" slots in empty places
)

/** "Lists" / "list" / "List" / "lists" in the word the person picked. */
val Opts.lists: String get() = listName.plural
val Opts.list: String get() = listName.single
val Opts.listsLower: String get() = listName.plural.lowercase()
val Opts.listLower: String get() = listName.single.lowercase()
