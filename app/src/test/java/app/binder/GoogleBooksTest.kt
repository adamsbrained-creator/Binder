package app.binder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleBooksTest {
    private val answer = """{"items":[
        {"id":"abcDEF123","volumeInfo":{"title":"Fox","subtitle":"A tale","authors":["Dahl","Blake"],"publishedDate":"1988-05-01","pageCount":80,"categories":["Fiction"],"description":"A fox.","imageLinks":{"thumbnail":"http://books.google.com/x.jpg"},"infoLink":"http://books.google.com/books?id=abcDEF123"}},
        {"id":"zzz999","volumeInfo":{"title":"Bare"}},
        {"id":"nope"}]}"""

    private fun provider(urls: MutableList<String>) = GoogleBooksProvider(get = { urls.add(it); answer }, gate = RateGate(100, 1000))

    @Test fun parsesLinks() {
        assertEquals("abcDEF123", GoogleBooksLinks.parse("https://books.google.com/books?id=abcDEF123&hl=en"))
        assertEquals("abcDEF123", GoogleBooksLinks.parse("https://www.google.com/books/edition/Fox/abcDEF123?hl=en&gbpv=1"))
        assertNull(GoogleBooksLinks.parse("https://www.google.com/search?q=fox"))
        assertEquals(Source.GOOGLEBOOKS, Sources.forLink("https://books.google.com/books?id=abcDEF123")?.source)
    }

    @Test fun searchResponseBecomesFound() {
        val urls = ArrayList<String>()
        val r = provider(urls).search("fox")
        assertEquals(2, r.size)
        val f = r[0]
        assertEquals("abcDEF123", f.sourceId)
        assertEquals("Fox: A tale", f.title)
        assertEquals("Dahl, Blake", f.artist)
        assertEquals("1988", f.year)
        assertEquals("Fiction", f.genre)
        assertEquals("80", f.extra)
        assertEquals("https://books.google.com/x.jpg", f.coverUrl) // https, not http
        assertEquals("", r[1].coverUrl)
        assertTrue(urls.single().startsWith("https://www.googleapis.com/books/v1/volumes?"))
        assertTrue(!urls.single().contains("key="))
    }

    @Test fun responseBecomesAnItem() {
        val item = provider(ArrayList()).search("fox")[0].toItem()
        assertEquals("Google Books", item.sourceName)
        assertEquals("Dahl, Blake", item.sub)
        assertTrue(item.rating == 0f)
    }

    @Test fun isbnSearch() {
        val urls = ArrayList<String>()
        provider(urls).search("9780140328721")
        assertTrue(urls.single().contains("q=isbn%3A9780140328721"))
    }
}
