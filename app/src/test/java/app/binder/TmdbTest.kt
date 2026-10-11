package app.binder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TmdbTest {
    private val movieSearch = """{"page":1,"results":[
        {"id":603,"title":"The Matrix","release_date":"1999-03-30","poster_path":"/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg","overview":"A hacker learns the truth.","vote_average":8.2,"genre_ids":[28,878]},
        {"id":0,"title":"Broken"},
        {"id":604,"title":"No Poster","release_date":"","poster_path":null,"overview":""}],"total_results":3}"""
    private val movieDetails = """{"id":603,"title":"The Matrix","release_date":"1999-03-30","runtime":136,"poster_path":"/m.jpg","overview":"A hacker learns the truth.","genres":[{"id":28,"name":"Action"},{"id":878,"name":"Science Fiction"}],"vote_average":8.2}"""
    private val tvDetails = """{"id":1396,"name":"Breaking Bad","first_air_date":"2008-01-20","number_of_seasons":5,"number_of_episodes":62,"poster_path":"/bb.jpg","overview":"A teacher.","genres":[{"id":18,"name":"Drama"}]}"""

    private fun provider(media: TmdbProvider.Media, urls: MutableList<String>, answer: (String) -> String) =
        TmdbProvider(media, key = { "KEY1" }, get = { urls.add(it); answer(it) }, gate = RateGate(100, 1000))

    @Test fun parsesMovieLinks() {
        assertEquals("movie" to "603", TmdbLinks.parse("https://www.themoviedb.org/movie/603-the-matrix"))
        assertEquals("movie" to "603", TmdbLinks.parse("https://www.themoviedb.org/en-US/movie/603"))
        assertEquals("tv" to "1396", TmdbLinks.parse("themoviedb.org/tv/1396-breaking-bad?language=en"))
        assertNull(TmdbLinks.parse("https://www.themoviedb.org/person/6384"))
        assertNull(TmdbLinks.parse("https://music.apple.com/us/album/x/1"))
    }

    @Test fun linkRoutingDoesNotClash() {
        assertTrue(Sources.forLink("https://www.themoviedb.org/movie/603")?.source == Source.TMDB)
        assertTrue(Sources.forLink("https://music.apple.com/us/album/abbey-road/1441164426")?.source == Source.APPLE)
    }

    @Test fun searchResponseBecomesFound() {
        val urls = ArrayList<String>()
        val r = provider(TmdbProvider.Media.MOVIE, urls) { movieSearch }.search("the matrix")
        assertEquals(2, r.size) // the broken entry is skipped
        val f = r[0]
        assertEquals("movie/603", f.sourceId)
        assertEquals("https://www.themoviedb.org/movie/603", f.sourceUrl)
        assertEquals("The Matrix", f.title)
        assertEquals("1999", f.year)
        assertEquals("1999", f.artist) // Kind.subLabel for movies is Year
        assertEquals("https://image.tmdb.org/t/p/w500/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg", f.coverUrl)
        assertEquals("", r[1].coverUrl)
        assertTrue(urls.single().startsWith("https://api.themoviedb.org/3/search/movie?api_key=KEY1"))
        assertTrue(urls.single().contains("query=the+matrix"))
    }

    @Test fun seriesUsesTheTvEndpoint() {
        val urls = ArrayList<String>()
        provider(TmdbProvider.Media.TV, urls) { """{"results":[{"id":1396,"name":"Breaking Bad","first_air_date":"2008-01-20","poster_path":"/b.jpg"}]}""" }.search("breaking")
        assertTrue(urls.single().contains("/3/search/tv?"))
    }

    @Test fun responseBecomesAnItem_withoutImportingTheirRating() {
        val f = TmdbProvider.parseDetails(movieDetails, TmdbProvider.Media.MOVIE)!!
        val item = f.toItem(listOf("c.jpg"))
        assertEquals("The Matrix", item.title)
        assertEquals("1999", item.sub)
        assertEquals("Action, Science Fiction", item.genre)
        assertEquals("2 h 16 min", item.extra)
        assertEquals("TMDB", item.sourceName)
        assertTrue(item.rating == 0f) // vote_average is never imported
    }

    @Test fun seriesDetailsGiveSeasonsAndEpisodes() {
        val item = TmdbProvider.parseDetails(tvDetails, TmdbProvider.Media.TV)!!.toItem()
        assertEquals("5 seasons", item.sub)
        assertEquals("62", item.extra)
        assertEquals("tv/1396", item.sourceId)
        assertEquals("Drama", item.genre)
    }

    @Test fun pickingAResultFetchesTheDetails() {
        val urls = ArrayList<String>()
        val p = provider(TmdbProvider.Media.MOVIE, urls) { movieDetails }
        val full = p.details(Found("TMDB", "movie/603", "", "The Matrix"))
        assertEquals("2 h 16 min", full.extra)
        assertTrue(urls.single().startsWith("https://api.themoviedb.org/3/movie/603?api_key=KEY1"))
    }

    @Test fun linkLookupUsesTheIdFromTheLink() {
        val urls = ArrayList<String>()
        val f = provider(TmdbProvider.Media.MOVIE, urls) { tvDetails }.fromLink("https://www.themoviedb.org/tv/1396-breaking-bad")!!
        assertEquals("Breaking Bad", f.title)
        assertTrue(urls.single().contains("/3/tv/1396?"))
    }

    @Test fun runtimeFormat() {
        assertEquals("45 min", TmdbProvider.runtime(45))
        assertEquals("2 h", TmdbProvider.runtime(120))
        assertEquals("", TmdbProvider.runtime(0))
    }

    @Test fun blankKeySaysSo() {
        val p = TmdbProvider(TmdbProvider.Media.MOVIE, key = { "" }, get = { error("no network call without a key") })
        try { p.search("matrix"); assertFalse(true) } catch (e: FetchFailure) { assertTrue(e.message!!.contains("no API key")) }
    }
}
