package app.binder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WikipediaTest {
    private val search = """{"query":{"search":[
        {"ns":0,"title":"Albert Einstein","pageid":736,"snippet":"<span class=\"searchmatch\">Albert</span> Einstein (14 March 1879 &ndash; 18 April 1955) was a German-born physicist"},
        {"title":"  "}]}}"""
    private val summary = """{"type":"standard","title":"Albert Einstein","description":"German-born physicist (1879–1955)","extract":"Albert Einstein was a physicist.",
        "thumbnail":{"source":"https://upload.wikimedia.org/thumb.jpg","width":320,"height":400},
        "originalimage":{"source":"https://upload.wikimedia.org/orig.jpg","width":1800,"height":2200},
        "content_urls":{"desktop":{"page":"https://en.wikipedia.org/wiki/Albert_Einstein"}}}"""

    private fun provider(urls: MutableList<String>, answer: String) =
        WikipediaProvider(get = { urls.add(it); answer }, gate = RateGate(100, 1000))

    @Test fun parsesLinks() {
        assertEquals("en" to "Albert_Einstein", WikipediaLinks.parse("https://en.wikipedia.org/wiki/Albert_Einstein#Early_life"))
        assertEquals("cs" to "Praha", WikipediaLinks.parse("https://cs.m.wikipedia.org/wiki/Praha?x=1"))
        assertNull(WikipediaLinks.parse("https://example.com/wiki/Foo"))
        assertEquals(Source.WIKIPEDIA, Sources.forLink("https://en.wikipedia.org/wiki/Prague")?.source)
    }

    @Test fun searchResponseBecomesFound() {
        val urls = ArrayList<String>()
        val r = provider(urls, search).search("einstein")
        assertEquals(1, r.size)
        assertEquals("Albert Einstein", r[0].title)
        assertEquals("en:Albert_Einstein", r[0].sourceId)
        assertEquals("https://en.wikipedia.org/wiki/Albert_Einstein", r[0].sourceUrl)
        assertTrue(r[0].artist.startsWith("Albert Einstein (14 March 1879 – 18 April 1955)")) // tags and entities removed
        assertTrue(urls.single().contains("list=search") && urls.single().contains("srsearch=einstein"))
    }

    @Test fun summaryBecomesAnItem() {
        val f = WikipediaProvider.parseSummary(summary)!!
        assertEquals("German-born physicist (1879–1955)", f.artist)
        assertEquals("Albert Einstein was a physicist.", f.description)
        assertEquals("https://upload.wikimedia.org/orig.jpg", f.coverUrl)   // a sensible size, so the original is used
        assertEquals("https://upload.wikimedia.org/thumb.jpg", f.thumbUrl)
        assertEquals("https://en.wikipedia.org/wiki/Albert_Einstein", f.sourceUrl)
        val item = f.toItem()
        assertEquals("Wikipedia", item.sourceName)
        assertTrue(item.rating == 0f)
    }

    @Test fun hugeOriginalsFallBackToTheThumbnail() {
        val json = summary.replace("\"width\":1800,\"height\":2200", "\"width\":9000,\"height\":7000")
        assertEquals("https://upload.wikimedia.org/thumb.jpg", WikipediaProvider.parseSummary(json)!!.coverUrl)
    }

    @Test fun pickingFetchesTheSummaryOfThatLanguage() {
        val urls = ArrayList<String>()
        provider(urls, summary).details(Found("Wikipedia", "cs:Praha", "", "Praha"))
        assertEquals("https://cs.wikipedia.org/api/rest_v1/page/summary/Praha", urls.single())
    }

    @Test fun disambiguationIsSaidSo() {
        val f = WikipediaProvider.parseSummary("""{"type":"disambiguation","title":"Mercury","extract":"Mercury may refer to:"}""")!!
        assertTrue(f.description.contains("disambiguation"))
    }
}
