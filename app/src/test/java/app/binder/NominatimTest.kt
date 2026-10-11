package app.binder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NominatimTest {
    private val answer = """[
        {"place_id":1,"osm_type":"relation","osm_id":435514,"lat":"50.0875","lon":"14.4213","category":"boundary","type":"administrative","display_name":"Prague, Czechia","name":"Prague",
         "address":{"city":"Prague","country":"Czechia"}},
        {"place_id":2,"osm_type":"way","osm_id":42,"category":"tourism","type":"museum","display_name":"National Museum, Wenceslas Square, Nové Město, Prague, Czechia","name":"National Museum",
         "address":{"road":"Wenceslas Square","suburb":"Nové Město","city":"Prague","country":"Czechia"}},
        {"place_id":3,"osm_type":"node","osm_id":7,"category":"place","type":"yes","display_name":"Tiny Place, Somewhere","name":"","address":{"village":"Tiny Village"}},
        {"osm_type":"node","osm_id":0,"display_name":"Broken"}]"""

    private fun provider(urls: MutableList<String>) = NominatimProvider(get = { urls.add(it); answer }, gate = RateGate(100, 1000))

    @Test fun parsesLinks() {
        assertEquals("R435514", OsmLinks.parse("https://www.openstreetmap.org/relation/435514"))
        assertEquals("W42", OsmLinks.parse("openstreetmap.org/way/42#map=17/50/14"))
        assertNull(OsmLinks.parse("https://www.openstreetmap.org/#map=5/50/14"))
        assertEquals(Source.NOMINATIM, Sources.forLink("https://www.openstreetmap.org/node/7")?.source)
    }

    @Test fun waitsForEnter() {
        assertTrue(NominatimProvider().searchOnEnter)
        assertTrue(!AppleProvider().searchOnEnter)
    }

    @Test fun searchResponseBecomesFound() {
        val urls = ArrayList<String>()
        val r = provider(urls).search("national museum prague")
        assertEquals(3, r.size) // the entry with no id is skipped
        assertEquals("Prague", r[0].title)
        assertEquals("relation/435514", r[0].sourceId)
        assertEquals("Czechia", r[0].artist)                // the city equals the name, so only the country is left
        assertEquals("Administrative", r[0].genre)
        assertEquals("National Museum", r[1].title)
        assertEquals("Prague, Czechia", r[1].artist)        // Kind.subLabel for places is City
        assertEquals("Museum", r[1].genre)
        assertEquals("National Museum, Wenceslas Square, Nové Město, Prague, Czechia", r[1].extra)
        assertEquals("Tiny Place", r[2].title)              // no name: the first part of the address
        assertEquals("Tiny Village", r[2].artist)
        assertEquals("", r[0].coverUrl)                     // no pictures
        assertEquals("https://www.openstreetmap.org/way/42", r[1].sourceUrl)
        assertTrue(urls.single().startsWith("https://nominatim.openstreetmap.org/search?format=jsonv2"))
        assertTrue(urls.single().contains("q=national+museum+prague"))
    }

    @Test fun responseBecomesAnItem() {
        val item = provider(ArrayList()).search("xx")[1].toItem()
        assertEquals("OpenStreetMap", item.sourceName)
        assertEquals("Prague, Czechia", item.sub)
        assertTrue(item.rating == 0f)
    }

    @Test fun linkLookupAsksForTheOsmId() {
        val urls = ArrayList<String>()
        provider(urls).fromLink("https://www.openstreetmap.org/way/42")
        assertTrue(urls.single().contains("/lookup?") && urls.single().endsWith("osm_ids=W42"))
    }

    @Test fun neverMoreThanOneRequestASecond() {
        var t = 0L
        val p = NominatimProvider(get = { answer }, gate = RateGate(1, 1_100, now = { t }))
        p.search("a1")
        try { p.search("a2"); assertTrue(false) } catch (e: FetchFailure) { }
        t = 1_200
        p.search("a3")
    }
}
