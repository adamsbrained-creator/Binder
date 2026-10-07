package app.binder

import java.util.UUID

enum class Layout(val label: String) { GRID("Grid"), LIST("List"), SHELF("Shelf") }

/** The kinds of list you can start from. Add a new one here and it shows up everywhere. */
enum class Kind(
    val label: String,
    val hint: String,
    val noun: String,
    val subLabel: String,
    val stages: List<String>,
    val hasRating: Boolean,
    val stack: List<Int>, // which cover colours the tile shows when the list is empty
) {
    BOOKS("Books", "Title and author", "books", "Author", listOf("To read", "Reading", "Done"), true, listOf(0, 3, 1)),
    MOVIES("Movies", "Title, year, rating", "movies", "Year", listOf("To watch", "Watching", "Watched"), true, listOf(4, 1, 5)),
    ALBUMS("Albums", "Artist, year, rating", "albums", "Artist", listOf("To listen", "Listening", "Heard"), true, listOf(2, 3, 0)),
    CHECKLIST("Checklist", "Items and done", "items", "", listOf("To do", "Done"), false, listOf(5, 3, 4));

    val isChecklist: Boolean get() = this == CHECKLIST
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
)

data class BinderList(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val kind: Kind,
    val layout: Layout = Layout.GRID,
    val items: List<Item> = emptyList(),
    val created: Long = System.currentTimeMillis(),
)

fun BinderList.countLabel(): String =
    if (kind.isChecklist) "${items.count { it.stage == 1 }} of ${items.size} done"
    else "${items.size} ${if (items.size == 1) kind.noun.dropLast(1) else kind.noun}"
