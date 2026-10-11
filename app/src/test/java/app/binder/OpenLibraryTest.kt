package app.binder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenLibraryTest {
    private val answer = """{"start":0,"num_found":2,"docs":[
        {"key":"/works/OL45804W","title":"Fantastic Mr Fox","author_name":["Roald Dahl"],"first_publish_year":1970,"cover_i":8739161,"number_of_pages_median":96},
        {"key":"/works/OL1W","title":"No Cover","author_name":["A","B","C","D"]},
        {"title":"No key"}]}"""
    private val gbAnswer = """{"items":[{"id":"abcDEF123","volumeInfo":{"title":"Fox","authors":["Dahl"],"publishedDate":"1988-05-01","pageCount":80,"categories":["Fiction"],"description":"A fox.","imageLinks":{"thumbnail":"http://books.google.com/x.jpg"},"infoLink":"http://books.google.com/books?id=abcDEF123"}}]}"""

    private fun ol(urls: MutableList<String>, fallback: MetadataProvider? = null, answer: (String) -> String) =
        OpenLibraryProvider(fallback, get = { urls.add(it); answer(it) }, gate = RateGate(100, 1000))

    @Test fun parsesLinks() {
        assertEquals("key:/works/OL45804W", OpenLibraryLinks.query("https://openlibrary.org/works/OL45804W/Fantastic_Mr_Fox"))
        assertEquals("edition_key:OL7353617M", OpenLibraryLinks.query("https://openlibrary.org/books/OL7353617M/x"))
        assertEquals("isbn:9780140328721", OpenLibraryLinks.query("https://openlibrary.org/isbn/978-0-14-032872-1"))
        assertNull(OpenLibraryLinks.query("https://openlibrary.org/authors/OL34184A"))
        assertEquals(Source.OPENLIBRARY, Sources.forLink("https://openlibrary.org/works/OL45804W")?.source)
    }

    @Test fun recognisesIsbns() {
        assertEquals("9780140328721", OpenLibraryLinks.isbn("978-0-14-032872-1"))
        assertEquals("043942089X", OpenLibraryLinks.isbn("0-439-42089-x"))
        assertNull(OpenLibraryLinks.isbn("harry potter"))
        assertNull(OpenLibraryLinks.isbn("12345"))
    }

    @Test fun searchResponseBecomesFound() {
        val urls = ArrayList<String>()
        val r = ol(urls) { answer }.search("fantastic mr fox")
        assertEquals(2, r.size) // the entry without a key is skipped
        val f = r[0]
        assertEquals("OL45804W", f.sourceId)
        assertEquals("https://openlibrary.org/works/OL45804W", f.sourceUrl)
        assertEquals("Roald Dahl", f.artist) // Kind.subLabel for books is Author
        assertEquals("1970", f.year)
        assertEquals("96", f.extra)
        assertEquals("https://covers.openlibrary.org/b/id/8739161-L.jpg", f.coverUrl)
        assertEquals("https://covers.openlibrary.org/b/id/8739161-M.jpg", f.thumbUrl)
        assertEquals("", r[1].coverUrl)
        assertEquals("A, B, C", r[1].artist)
        assertTrue(urls.single().startsWith("https://openlibrary.org/search.json?"))
        assertTrue(urls.single().contains("q=fantastic+mr+fox"))
    }

    @Test fun anIsbnIsLookedUpByIsbn() {
        val urls = ArrayList<String>()
        ol(urls) { answer }.search("978-0-14-032872-1")
        assertTrue(urls.single().contains("q=isbn%3A9780140328721"))
    }

    @Test fun responseBecomesAnItem() {
        val item = OpenLibraryProvider.parse(answer)[0].toItem(listOf("c.jpg"))
        assertEquals("Fantastic Mr Fox", item.title)
        assertEquals("Roald Dahl", item.sub)
        assertEquals("Open Library", item.sourceName)
        assertTrue(item.rating == 0f)
    }

    @Test fun fallsBackToGoogleBooksWhenNothingIsFound() {
        val urls = ArrayList<String>()
        val gb = GoogleBooksProvider(get = { urls.add(it); gbAnswer }, gate = RateGate(100, 1000))
        val r = ol(urls, gb) { """{"docs":[]}""" }.search("fox")
        assertEquals("Google Books", r.single().sourceName)
        assertTrue(urls.last().startsWith("https://www.googleapis.com/books/v1/volumes"))
    }

    @Test fun fallsBackWhenOpenLibraryFails_butNotWhenOffline() {
        val gb = GoogleBooksProvider(get = { gbAnswer }, gate = RateGate(100, 1000))
        val busy = OpenLibraryProvider(gb, get = { throw FetchFailure("The source is busy.") }, gate = RateGate(100, 1000))
        assertEquals("Google Books", busy.search("fox").single().sourceName)
        val offline = OpenLibraryProvider(gb, get = { throw FetchFailure("Couldn't reach the internet.", offline = true) }, gate = RateGate(100, 1000))
        try { offline.search("fox"); assertTrue(false) } catch (e: FetchFailure) { assertTrue(e.offline) }
    }
}
