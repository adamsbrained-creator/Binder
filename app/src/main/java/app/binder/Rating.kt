package app.binder

import java.text.DecimalFormatSymbols
import java.util.Locale

/** Pure rating helpers (no Compose), so they can be tested on the JVM. */

/** How a rating is shown: five stars, or "4.75 ★". */
enum class RatingStyle(val label: String) { STARS("Stars"), NUMBER("Number") }

/** The most a rating can be, and the step the number style's slider moves in. */
const val RATING_MAX = 5f
const val RATING_STEP = 0.25f

/** Rating with float noise taken off (4.7999 becomes 4.8) and kept between 0 and 5. */
fun cleanRating(r: Float): Float {
    if (r.isNaN()) return 0f
    return (Math.round(r.coerceIn(0f, RATING_MAX) * 100f) / 100f)
}

/** How full each of the five stars is: 4.5 gives 1, 1, 1, 1, 0.5, 0. Any value is drawn in proportion. */
fun starFills(rating: Float): List<Float> {
    val r = cleanRating(rating)
    return List(5) { i -> Math.round((r - i).coerceIn(0f, 1f) * 100f) / 100f }
}

/**
 * The number style's text: up to two decimals, no trailing zeros, the device's decimal separator.
 * 4.75 gives "4.75", 4.0 gives "4", 4.7999 gives "4.8". Nothing (empty) when there is no rating.
 */
fun formatRating(rating: Float, locale: Locale = Locale.getDefault()): String {
    val r = cleanRating(rating)
    if (r <= 0f) return ""
    var s = String.format(Locale.ROOT, "%.2f", r.toDouble())
    s = s.trimEnd('0').trimEnd('.')
    if (s.isEmpty() || s == "0") return ""
    val sep = DecimalFormatSymbols.getInstance(locale).decimalSeparator
    return if (sep == '.') s else s.replace('.', sep)
}

/** A slider value snapped to the nearest quarter, between 0 and 5. */
fun snapRating(v: Float): Float = (Math.round(v.coerceIn(0f, RATING_MAX) / RATING_STEP) * RATING_STEP)
