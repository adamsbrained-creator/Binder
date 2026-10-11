package app.binder

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class RatingTest {
    private fun fills(r: Float) = starFills(r)

    @Test fun fillsForHalfStars() {
        assertEquals(listOf(1f, 1f, 1f, 1f, 0.5f), fills(4.5f))
    }

    @Test fun fillsAreProportional() {
        assertEquals(listOf(0f, 0f, 0f, 0f, 0f), fills(0f))
        assertEquals(listOf(0.25f, 0f, 0f, 0f, 0f), fills(0.25f))
        assertEquals(listOf(0.5f, 0f, 0f, 0f, 0f), fills(0.5f))
        assertEquals(listOf(0.75f, 0f, 0f, 0f, 0f), fills(0.75f))
        assertEquals(listOf(1f, 0f, 0f, 0f, 0f), fills(1f))
        assertEquals(listOf(1f, 1f, 0.5f, 0f, 0f), fills(2.5f))
        assertEquals(listOf(1f, 1f, 1f, 1f, 0.75f), fills(4.75f))
        assertEquals(listOf(1f, 1f, 1f, 1f, 1f), fills(5f))
    }

    @Test fun fillsStayInRangeAndIgnoreFloatNoise() {
        assertEquals(listOf(1f, 1f, 1f, 1f, 1f), fills(7f))
        assertEquals(listOf(0f, 0f, 0f, 0f, 0f), fills(-1f))
        assertEquals(listOf(1f, 1f, 1f, 1f, 0.8f), fills(4.7999f))
        assertEquals(listOf(0f, 0f, 0f, 0f, 0f), fills(Float.NaN))
    }

    @Test fun formatsUpToTwoDecimalsWithoutTrailingZeros() {
        assertEquals("4.75", formatRating(4.75f, Locale.US))
        assertEquals("4", formatRating(4.0f, Locale.US))
        assertEquals("4.5", formatRating(4.5f, Locale.US))
        assertEquals("2.25", formatRating(2.25f, Locale.US))
        assertEquals("5", formatRating(5f, Locale.US))
    }

    @Test fun noFloatNoise() {
        assertEquals("4.8", formatRating(4.7999f, Locale.US))
        assertEquals("0.3", formatRating(0.1f + 0.2f, Locale.US))
    }

    @Test fun unratedShowsNothing() {
        assertEquals("", formatRating(0f, Locale.US))
        assertEquals("", formatRating(-2f, Locale.US))
        assertEquals("", formatRating(0.001f, Locale.US))
        assertEquals("", formatRating(Float.NaN, Locale.US))
    }

    @Test fun usesTheDevicesDecimalSeparator() {
        assertEquals("4,75", formatRating(4.75f, Locale.GERMANY))
        assertEquals("4,75", formatRating(4.75f, Locale.FRANCE))
        assertEquals("4", formatRating(4f, Locale.GERMANY))
    }

    @Test fun sliderSnapsToQuarters() {
        assertEquals(0f, snapRating(0.1f))
        assertEquals(0.25f, snapRating(0.2f))
        assertEquals(4.75f, snapRating(4.8f))
        assertEquals(5f, snapRating(9f))
        assertEquals(0f, snapRating(-3f))
    }
}
