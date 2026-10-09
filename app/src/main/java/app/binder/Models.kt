package app.binder

import java.util.UUID

enum class Layout(val label: String) {
    GRID("Grid"), LIST("List"), SHELF("Shelf"), SECTIONS("Sections"), CARDS("Cards")
}

enum class Sort(val label: String) {
    MANUAL("Manual"), NEWEST("Newest"), TITLE("A–Z"), RATING("Rating")
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
    val stack: List<Int>, // cover colours shown on an empty list's tile
) {
    BOOKS("Books", "Title and author", "books", "book", "Author", listOf("To read", "Reading", "Done"), true, listOf(0, 3, 1)),
    MOVIES("Movies", "Title, year, rating", "movies", "movie", "Year", listOf("To watch", "Watching", "Watched"), true, listOf(4, 1, 5)),
    SERIES("Series", "Title, seasons, rating", "series", "series", "Seasons", listOf("To watch", "Watching", "Watched"), true, listOf(1, 4, 3)),
    ALBUMS("Albums", "Artist, year, rating", "albums", "album", "Artist", listOf("To listen", "Listening", "Heard"), true, listOf(2, 3, 0)),
    GAMES("Games", "Title, platform, rating", "games", "game", "Platform", listOf("To play", "Playing", "Played"), true, listOf(4, 0, 2)),
    PLACES("Places", "Place, city, rating", "places", "place", "City", listOf("Want to go", "Planned", "Visited"), true, listOf(2, 3, 5)),
    ANYTHING("Anything", "Your own collection", "things", "thing", "Details", listOf("Idea", "In progress", "Done"), true, listOf(5, 1, 0)),
    CHECKLIST("Checklist", "Items and done", "items", "item", "", listOf("To do", "Done"), false, listOf(5, 3, 4));

    val isChecklist: Boolean get() = this == CHECKLIST
    val layouts: List<Layout> get() = if (isChecklist) listOf(Layout.LIST, Layout.SECTIONS) else Layout.entries
}

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

data class Opts(
    val theme: String = "system",
    val appName: String = "binder", // "binder" or "catalog"
    val font: FontChoice = FontChoice.OUTFIT,
    val header: HeaderStyle = HeaderStyle.WORDMARK,
    val nav: NavStyle = NavStyle.NONE,
    val home: HomeStyle = HomeStyle.PINNED,
    val add: AddStyle = AddStyle.SHEET,
    val itemPage: ItemPage = ItemPage.STANDARD,
)
