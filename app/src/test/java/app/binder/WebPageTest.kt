package app.binder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebPageTest {
    private val page = """<!doctype html><html><head><title>Fallback &amp; title</title>
        <meta property="og:title" content="The &quot;Great&quot; Article &amp; more">
        <meta content="https://example.com/a.jpg" property="og:image" />
        <meta property='og:description' content='A short &#39;summary&#39;.'>
        <meta property="og:site_name" content="Example News">
        <meta property="og:url" content="https://example.com/canonical">
        </head><body>…</body></html>"""

    @Test fun anyHttpLinkIsHandled_butSpecificSourcesComeFirst() {
        assertTrue(WebPageProvider().handlesLink("https://example.com/a?b=1"))
        assertFalse(WebPageProvider().handlesLink("not a link"))
        assertFalse(WebPageProvider().handlesLink("example.com"))
        assertEquals(Source.WEB, Sources.forLink("https://example.com/post")?.source)
        assertEquals(Source.TMDB, Sources.forLink("https://www.themoviedb.org/movie/603")?.source)
        assertEquals(Source.WIKIPEDIA, Sources.forLink("https://en.wikipedia.org/wiki/Prague")?.source)
        assertEquals(Source.APPLE, Sources.forLink("https://music.apple.com/us/album/x/42")?.source)
    }

    @Test fun readsOpenGraphTags_inAnyAttributeOrder() {
        val f = WebPageProvider.parse("https://example.com/post", page)!!
        assertEquals("The \"Great\" Article & more", f.title)
        assertEquals("https://example.com/a.jpg", f.coverUrl)
        assertEquals("A short 'summary'.", f.description)
        assertEquals("Example News", f.artist)
        assertEquals("https://example.com/canonical", f.sourceUrl)
        assertEquals("Web page", f.sourceName)
    }

    @Test fun fallsBackToTheTitleTagAndResolvesRelativeImages() {
        val f = WebPageProvider.parse("https://example.com/dir/post", """<head><title>Plain &amp; simple</title><meta property="og:image" content="/img/x.png"></head>""")!!
        assertEquals("Plain & simple", f.title)
        assertEquals("https://example.com/img/x.png", f.coverUrl)
        assertEquals("https://example.com/dir/post", f.sourceUrl)
    }

    @Test fun aPageWithNoTitleGivesNothing() {
        assertNull(WebPageProvider.parse("https://example.com", "<html><body>hi</body></html>"))
    }

    @Test fun fromLinkFetchesThePage() {
        var asked = ""
        val p = WebPageProvider(get = { asked = it; page }, gate = RateGate(100, 1000))
        assertEquals("The \"Great\" Article & more", p.fromLink("  https://example.com/post ")!!.title)
        assertEquals("https://example.com/post", asked)
    }

    @Test fun decodesEntities() {
        assertEquals("a & b < c “q” – é", Html.decode("a &amp; b &lt; c &ldquo;q&rdquo; &ndash; &#233;"))
        assertEquals("😀", Html.decode("&#x1F600;"))
    }
}
