package app.binder

import org.junit.Assert.assertEquals
import org.junit.Test

class OptsTest {
    @Test fun binderWords() {
        val o = Opts(appName = "binder")
        assertEquals("Binds", o.lists); assertEquals("Bind", o.list)
        assertEquals("binds", o.listsLower); assertEquals("bind", o.listLower)
        assertEquals("Binder", o.appLabel)
    }

    @Test fun catalogWords() {
        val o = Opts(appName = "catalog")
        assertEquals("Catalogs", o.lists); assertEquals("Catalog", o.list)
        assertEquals("catalogs", o.listsLower); assertEquals("catalog", o.listLower)
        assertEquals("Catalog", o.appLabel)
    }

    @Test fun unknownNameFallsBackToBinder() {
        assertEquals("Binds", Opts(appName = "whatever").lists)
    }
}
