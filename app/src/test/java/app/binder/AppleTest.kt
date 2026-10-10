package app.binder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AppleTest {

    // The shape of a real iTunes Search / Lookup answer (trimmed to the fields Binder reads).
    private val sample = """
    {"resultCount":3,"results":[
      {"wrapperType":"collection","collectionType":"Album","collectionId":1441164426,"artistName":"The Beatles",
       "collectionName":"Abbey Road (Remastered)",
       "collectionViewUrl":"https://music.apple.com/us/album/abbey-road-remastered/1441164426?uo=4",
       "artworkUrl100":"https://is1-ssl.mzstatic.com/image/thumb/Music115/v4/aa/bb/cc/source/100x100bb.jpg",
       "trackCount":17,"releaseDate":"1969-09-26T07:00:00Z","primaryGenreName":"Rock","country":"USA"},
      {"wrapperType":"collection","collectionType":"Album","collectionId":7,"artistName":"Nobody",
       "collectionName":"No Art, One Song","trackCount":1,"releaseDate":"bad","primaryGenreName":""},
      {"wrapperType":"track","trackId":99,"trackName":"Come Together"}
    ]}
    """.trimIndent()

    @Test fun parsesAnAlbum() {
        val r = AppleProvider.parse(sample)
        assertEquals(2, r.size) // the track is skipped
        val a = r[0]
        assertEquals("Apple Music", a.sourceName)
        assertEquals("1441164426", a.sourceId)
        assertEquals("Abbey Road (Remastered)", a.title)
        assertEquals("The Beatles", a.artist)
        assertEquals("1969", a.year)
        assertEquals("Rock", a.genre)
        assertEquals("17 songs", a.description)
        assertTrue(a.sourceUrl.startsWith("https://music.apple.com/us/album/"))
    }

    @Test fun artworkGetsBigger() {
        val a = AppleProvider.parse(sample)[0]
        assertTrue(a.coverUrl.endsWith("/600x600bb.jpg"))
        assertTrue(a.thumbUrl.endsWith("/100x100bb.jpg"))
    }

    @Test fun missingPartsAreEmptyNotBroken() {
        val b = AppleProvider.parse(sample)[1]
        assertEquals("", b.year)
        assertEquals("", b.genre)
        assertEquals("", b.coverUrl)
        assertEquals("1 song", b.description)
    }

    @Test fun turnsIntoAnItem() {
        val i = AppleProvider.parse(sample)[0].toItem(listOf("cover.jpg"))
        assertEquals("Abbey Road (Remastered)", i.title)
        assertEquals("The Beatles", i.sub)
        assertEquals(listOf("cover.jpg"), i.images)
        assertEquals("1441164426", i.sourceId)
        assertEquals("1969", i.year)
        assertFalse(i.detailsPending)
        assertTrue(AppleProvider.parse(sample)[0].toItem(pending = true).detailsPending)
    }

    @Test fun emptyAnswers() {
        assertEquals(0, AppleProvider.parse("""{"resultCount":0,"results":[]}""").size)
        assertEquals(0, AppleProvider.parse("""{"resultCount":0}""").size)
    }

    @Test fun readsAppleMusicLinks() {
        assertEquals("us" to "1441164426", AppleLinks.parse("https://music.apple.com/us/album/abbey-road-remastered/1441164426"))
        assertEquals("gb" to "1441164426", AppleLinks.parse("music.apple.com/GB/album/abbey-road/1441164426?i=1441164430"))
        assertEquals("cz" to "123", AppleLinks.parse("  https://music.apple.com/cz/album/123  "))
        assertEquals("us" to "55", AppleLinks.parse("https://itunes.apple.com/us/album/some-name/id55"))
        assertNull(AppleLinks.parse("https://music.apple.com/us/playlist/best/pl.123"))
        assertNull(AppleLinks.parse("abbey road"))
        assertNull(AppleLinks.parse("https://example.com/us/album/x/123"))
    }

    @Test fun searchAsksTheRightAddress() {
        var asked = ""
        val p = AppleProvider(get = { asked = it; sample })
        val r = p.search("abbey road")
        assertEquals(2, r.size)
        assertEquals("https://itunes.apple.com/search?term=abbey+road&entity=album&limit=10", asked)
    }

    @Test fun lookupUsesTheLinksCountry() {
        var asked = ""
        val p = AppleProvider(get = { asked = it; sample })
        val f = p.fromLink("https://music.apple.com/gb/album/abbey-road/1441164426")
        assertEquals("Abbey Road (Remastered)", f?.title)
        assertEquals("https://itunes.apple.com/lookup?id=1441164426&country=gb", asked)
        assertNull(p.fromLink("https://example.com/nothing"))
    }

    @Test fun tooShortSearchMakesNoCall() {
        val p = AppleProvider(get = { fail("must not call"); "" })
        assertEquals(0, p.search(" a ").size)
    }

    @Test fun rateGateStopsTooManyCalls() {
        var t = 0L
        val g = RateGate(3, 60_000) { t }
        repeat(3) { g.check() }
        try { g.check(); fail("should stop") } catch (e: FetchFailure) { /* expected */ }
        t = 61_000
        g.check() // the window has passed
    }

    @Test fun routerPicksTheSource() {
        assertEquals(Source.APPLE, Sources.forKind(Kind.ALBUMS)?.source)
        assertNull(Sources.forKind(Kind.CHECKLIST))
        assertNull(Sources.forKind(Kind.ANYTHING))
        assertEquals(Source.APPLE, Sources.forLink("https://music.apple.com/us/album/x/42")?.source)
        assertNull(Sources.forLink("https://example.com"))
    }
}
