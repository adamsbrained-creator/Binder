package app.binder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RawgTest {
    private val search = """{"count":2,"results":[
        {"id":3498,"slug":"grand-theft-auto-v","name":"Grand Theft Auto V","released":"2013-09-17","background_image":"https://media.rawg.io/media/games/456/gta5.jpg","rating":4.47,
         "platforms":[{"platform":{"id":4,"name":"PC"}},{"platform":{"id":187,"name":"PlayStation 5"}},{"platform":{"id":1,"name":"Xbox One"}}],
         "genres":[{"id":4,"name":"Action"},{"id":3,"name":"Adventure"}]},
        {"id":9,"slug":"tba","name":"Unreleased","released":null,"background_image":null,"platforms":null,"genres":[]}]}"""
    private val detail = """{"id":3498,"slug":"grand-theft-auto-v","name":"Grand Theft Auto V","released":"2013-09-17","background_image":"https://media.rawg.io/x.jpg","description_raw":"  An open world.  ","platforms":[{"platform":{"name":"PC"}}],"genres":[{"name":"Action"}]}"""

    private fun provider(urls: MutableList<String>, answer: String) =
        RawgProvider(key = { "K2" }, get = { urls.add(it); answer }, gate = RateGate(100, 1000))

    @Test fun parsesLinks() {
        assertEquals("grand-theft-auto-v", RawgLinks.parse("https://rawg.io/games/grand-theft-auto-v"))
        assertEquals("portal-2", RawgLinks.parse("rawg.io/games/portal-2?x=1"))
        assertNull(RawgLinks.parse("https://rawg.io/genres/action"))
        assertEquals(Source.RAWG, Sources.forLink("https://rawg.io/games/portal-2")?.source)
    }

    @Test fun searchResponseBecomesFound() {
        val urls = ArrayList<String>()
        val r = provider(urls, search).search("grand theft")
        assertEquals(2, r.size)
        val f = r[0]
        assertEquals("3498", f.sourceId)
        assertEquals("https://rawg.io/games/grand-theft-auto-v", f.sourceUrl)
        assertEquals("PC, PlayStation 5", f.artist) // Kind.subLabel for games is Platform
        assertEquals("PC, PlayStation 5, Xbox One", f.extra)
        assertEquals("2013", f.year)
        assertEquals("Action, Adventure", f.genre)
        assertEquals("https://media.rawg.io/media/games/456/gta5.jpg", f.coverUrl)
        assertEquals("", r[1].coverUrl)
        assertEquals("", r[1].year)
        assertTrue(urls.single().startsWith("https://api.rawg.io/api/games?key=K2"))
        assertTrue(urls.single().contains("search=grand+theft"))
    }

    @Test fun responseBecomesAnItem_withoutTheirRating() {
        val item = provider(ArrayList(), detail).details(Found("RAWG", "3498", "", "Grand Theft Auto V")).toItem(listOf("c.jpg"))
        assertEquals("An open world.", item.description)
        assertEquals("PC", item.sub)
        assertEquals("RAWG", item.sourceName)
        assertTrue(item.rating == 0f)
    }

    @Test fun detailsFromLinkUseTheSlug() {
        val urls = ArrayList<String>()
        val f = provider(urls, detail).fromLink("https://rawg.io/games/grand-theft-auto-v")!!
        assertEquals("Grand Theft Auto V", f.title)
        assertTrue(urls.single().startsWith("https://api.rawg.io/api/games/grand-theft-auto-v?key=K2"))
    }

    @Test fun blankKeySaysSo() {
        val p = RawgProvider(key = { "" }, get = { error("no network call without a key") })
        try { p.search("portal"); assertFalse(true) } catch (e: FetchFailure) { assertTrue(e.message!!.contains("no API key")) }
    }
}
