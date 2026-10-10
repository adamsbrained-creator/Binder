package app.binder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistTest {

    // A file as version 0.1 wrote it: a plain list, no format marker, few fields.
    private val v01 = """[{"id":"a","name":"Books","kind":"BOOKS","items":[{"id":"i1","title":"Moby-Dick","sub":"Melville","stage":1,"rating":4.5,"note":"","added":1}],"created":5}]"""

    // A file as version 0.5 wrote it: an object with "version":2, images, layout, sort, pinned.
    private val v05 = """{"format":"binder","version":2,"lists":[{"id":"b","name":"Albums","kind":"ALBUMS","layout":"CARDS","sort":"TITLE","pinned":true,"created":9,
        "items":[{"id":"i2","title":"Abbey Road","sub":"The Beatles","stage":2,"rating":5,"note":"classic","added":2,"cover":3,"images":["x.jpg"]}]}]}"""

    @Test fun readsAVersion01File() {
        val l = parseLists(v01).single()
        assertEquals("Books", l.name)
        assertEquals(Kind.BOOKS, l.kind)
        assertEquals(3, l.columns)                              // default
        assertEquals(setOf(Under.TITLE, Under.RATING), l.under) // default
        val i = l.items.single()
        assertEquals("Moby-Dick", i.title)
        assertEquals(4.5f, i.rating)
        assertEquals("", i.year)
        assertFalse(i.detailsPending)
        assertTrue(i.images.isEmpty())
    }

    @Test fun readsAVersion05File() {
        val l = parseLists(v05).single()
        assertEquals(Kind.ALBUMS, l.kind)
        assertEquals(Layout.CARDS, l.layout)
        assertEquals(Sort.TITLE, l.sort)
        assertTrue(l.pinned)
        val i = l.items.single()
        assertEquals(listOf("x.jpg"), i.images)
        assertEquals(3, i.cover)
        assertEquals("", i.sourceName)
        assertEquals("", i.genre)
    }

    @Test fun newFieldsSurviveASaveAndLoad() {
        val item = Item(
            id = "z", title = "Abbey Road", sub = "The Beatles", sourceName = "Apple Music", sourceId = "1441164426",
            sourceUrl = "https://music.apple.com/us/album/x/1441164426", year = "1969", genre = "Rock",
            description = "17 songs", detailsPending = true, images = listOf("c.jpg"),
        )
        val list = BinderList(
            id = "L", name = "Albums", kind = Kind.ALBUMS, columns = 4,
            under = setOf(Under.ADDED, Under.PROGRESS), items = listOf(item),
        )
        val back = parseLists(listsToJson(listOf(list)).toString()).single()
        assertEquals(4, back.columns)
        assertEquals(setOf(Under.ADDED, Under.PROGRESS), back.under)
        assertEquals(item, back.items.single())
    }

    @Test fun anEmptyUnderSetStaysEmpty() {
        val list = BinderList(id = "L", name = "Books", kind = Kind.BOOKS, under = emptySet())
        assertEquals(emptySet<Under>(), parseLists(listsToJson(listOf(list)).toString()).single().under)
    }

    @Test fun columnsAreKeptInRange() {
        val j = """{"format":"binder","version":3,"lists":[{"id":"q","name":"X","kind":"BOOKS","columns":9,"items":[]}]}"""
        assertEquals(4, parseLists(j).single().columns)
    }
}
