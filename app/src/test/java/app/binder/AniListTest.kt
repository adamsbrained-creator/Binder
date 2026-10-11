package app.binder

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AniListTest {
    private val anime = """{"data":{"Page":{"media":[
        {"id":16498,"title":{"romaji":"Shingeki no Kyojin","english":"Attack on Titan","native":"進撃の巨人"},"format":"TV","startDate":{"year":2013},"episodes":25,"chapters":null,"volumes":null,
         "coverImage":{"extraLarge":"https://s4.anilist.co/xl.jpg","large":"https://s4.anilist.co/l.jpg","medium":"https://s4.anilist.co/m.jpg"},
         "description":"Humanity<br>lives in <i>fear</i>.","genres":["Action","Drama","Fantasy","Mystery"],"siteUrl":"https://anilist.co/anime/16498","averageScore":84,"staff":{"nodes":[]}},
        {"id":2,"title":{"romaji":"Only Romaji","english":null,"native":null},"format":null,"startDate":{"year":null},"coverImage":null,"genres":[],"siteUrl":null}]}}}"""
    private val manga = """{"data":{"Media":{"id":30013,"title":{"romaji":"One Piece","english":"One Piece","native":"ONE PIECE"},"format":"MANGA","startDate":{"year":1997},"episodes":null,"chapters":1100,"volumes":107,
        "coverImage":{"large":"https://s4.anilist.co/l.jpg"},"description":"Pirates.","genres":["Adventure"],"siteUrl":"https://anilist.co/manga/30013","staff":{"nodes":[{"name":{"full":"Eiichiro Oda"}}]}}}}"""

    @Test fun parsesLinks() {
        assertEquals("anime" to "16498", AniListLinks.parse("https://anilist.co/anime/16498/Shingeki-no-Kyojin/"))
        assertEquals("manga" to "30013", AniListLinks.parse("anilist.co/manga/30013"))
        assertNull(AniListLinks.parse("https://anilist.co/user/someone"))
        assertEquals(Source.ANILIST, Sources.forLink("https://anilist.co/manga/30013")?.source)
    }

    @Test fun searchPostsAGraphQlQuery() {
        var url = ""; var body = ""
        val p = AniListProvider(AniListProvider.Type.ANIME, post = { u, b -> url = u; body = b; anime }, gate = RateGate(100, 1000))
        val r = p.search("attack on titan")
        assertEquals("https://graphql.anilist.co", url)
        val j = JSONObject(body)
        assertEquals("attack on titan", j.getJSONObject("variables").getString("search"))
        assertEquals("ANIME", j.getJSONObject("variables").getString("type"))
        assertTrue(j.getString("query").contains("media(search: \$search, type: \$type)"))
        assertEquals(2, r.size)
    }

    @Test fun animeResponseBecomesFound() {
        val f = AniListProvider.parse(anime)[0]
        assertEquals("16498", f.sourceId)
        assertEquals("Attack on Titan", f.title)           // English first
        assertEquals("TV", f.artist)
        assertEquals("2013", f.year)
        assertEquals("Action, Drama, Fantasy", f.genre)    // three at most
        assertEquals("25", f.extra)
        assertEquals("Humanity lives in fear.", f.description) // no HTML
        assertEquals("https://s4.anilist.co/xl.jpg", f.coverUrl)
        assertEquals("https://s4.anilist.co/m.jpg", f.thumbUrl)
        val bare = AniListProvider.parse(anime)[1]
        assertEquals("Only Romaji", bare.title)
        assertEquals("", bare.coverUrl)
        assertEquals("https://anilist.co/anime/2", bare.sourceUrl)
    }

    @Test fun mangaResponseBecomesAnItem_withoutTheirScore() {
        val item = AniListProvider.parse(manga).single().toItem()
        assertEquals("Eiichiro Oda", item.sub)   // Kind.subLabel for manga is Author
        assertEquals("107", item.extra)           // volumes
        assertEquals("AniList", item.sourceName)
        assertTrue(item.rating == 0f)
    }

    @Test fun linkLookupAsksForThatId() {
        var body = ""
        val p = AniListProvider(AniListProvider.Type.MANGA, post = { _, b -> body = b; manga }, gate = RateGate(100, 1000))
        val f = p.fromLink("https://anilist.co/manga/30013/One-Piece")!!
        assertEquals("One Piece", f.title)
        val v = JSONObject(body).getJSONObject("variables")
        assertEquals(30013, v.getInt("id"))
        assertEquals("MANGA", v.getString("type"))
    }

    @Test fun graphQlErrorsBecomeAFailure() {
        try { AniListProvider.parse("""{"errors":[{"message":"Bad"}],"data":null}"""); assertTrue(false) } catch (e: FetchFailure) { assertTrue(e.message!!.contains("AniList")) }
        assertEquals(0, AniListProvider.parse("""{"data":{"Page":{"media":[]}}}""").size)
    }
}
