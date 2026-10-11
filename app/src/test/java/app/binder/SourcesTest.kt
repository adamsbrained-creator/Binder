package app.binder

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SourcesTest {
    @After fun reset() { Keys.tmdb = ""; Keys.rawg = "" }

    @Test fun mangaAndComicsAreLikeBooks() {
        assertEquals(Kind.BOOKS.stages, Kind.MANGA.stages)
        assertEquals(Kind.BOOKS.stages, Kind.COMICS.stages)
        assertFalse(Kind.MANGA.square); assertFalse(Kind.COMICS.square)
        assertEquals(listOf(Source.ANILIST), Kind.MANGA.sources)
        assertEquals(listOf(Source.GOOGLEBOOKS), Kind.COMICS.sources)
    }

    @Test fun firstSourceIsTheDefault() {
        assertEquals(Source.OPENLIBRARY, Kind.BOOKS.sources.first())
        assertEquals(Source.OPENLIBRARY, Sources.sourceFor(BinderList(name = "b", kind = Kind.BOOKS)))
        assertEquals(Source.GOOGLEBOOKS, Sources.sourceFor(BinderList(name = "b", kind = Kind.BOOKS, source = Source.GOOGLEBOOKS)))
    }

    @Test fun aSourceThatIsNotOfferedByTheKindIsIgnored() {
        assertEquals(Source.OPENLIBRARY, Sources.sourceFor(BinderList(name = "b", kind = Kind.BOOKS, source = Source.ANILIST)))
    }

    @Test fun keyedSourcesAreHiddenWithoutAKey() {
        assertTrue(Sources.readySources(Kind.MOVIES).isEmpty())
        assertNull(Sources.forList(BinderList(name = "m", kind = Kind.MOVIES)))
        assertEquals(listOf(Source.ANILIST), Sources.readySources(Kind.SERIES)) // TMDB needs a key, AniList doesn't
        Keys.tmdb = "abc"
        assertEquals(listOf(Source.TMDB), Sources.readySources(Kind.MOVIES))
        assertEquals(listOf(Source.TMDB, Source.ANILIST), Sources.readySources(Kind.SERIES))
        assertEquals(Source.TMDB, Sources.forList(BinderList(name = "m", kind = Kind.MOVIES))?.source)
    }

    @Test fun gamesNeedTheRawgKey() {
        assertTrue(Sources.readySources(Kind.GAMES).isEmpty())
        Keys.rawg = "k"
        assertEquals(listOf(Source.RAWG), Sources.readySources(Kind.GAMES))
    }

    @Test fun seriesAndMoviesGetTheirOwnTmdbEndpoint() {
        Keys.tmdb = "abc"
        val urls = ArrayList<String>()
        val movie = TmdbProvider(TmdbProvider.Media.MOVIE, key = { Keys.tmdb }, get = { urls.add(it); """{"results":[]}""" }, gate = RateGate(10, 1000))
        movie.search("matrix")
        assertTrue(urls[0].contains("/search/movie"))
        assertFalse(Sources.forSource(Source.TMDB, Kind.SERIES) === Sources.forSource(Source.TMDB, Kind.MOVIES))
    }

    @Test fun checklistsHaveNoSource() {
        assertNull(Sources.forList(BinderList(name = "c", kind = Kind.CHECKLIST)))
        assertTrue(Kind.CHECKLIST.sources.isEmpty())
    }

    @Test fun everySourceCanBeBuilt() {
        for (s in Source.entries) assertEquals(s, Sources.forSource(s, Kind.SERIES).source)
    }

    @Test fun itemsFindTheirSourceByLabel() {
        for (s in Source.entries) assertEquals(s, Sources.byLabel(s.label))
        assertNull(Sources.byLabel("Nope"))
    }

    @Test fun sourceLabelsAreUnique() {
        assertEquals(Source.entries.size, Source.entries.map { it.label }.toSet().size)
    }
}
