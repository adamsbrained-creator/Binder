package app.binder

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

val Coral = Color(0xFFF46C4E)

// ───────── fonts: Outfit is the standard one ─────────

val Outfit = FontFamily(Font(R.font.outfit_regular, FontWeight.Normal), Font(R.font.outfit_bold, FontWeight.Bold))
private val Lora = FontFamily(Font(R.font.lora_regular, FontWeight.Normal), Font(R.font.lora_bold, FontWeight.Bold))
private val Crimson = FontFamily(Font(R.font.crimsonpro_regular, FontWeight.Normal), Font(R.font.crimsonpro_bold, FontWeight.Bold))
private val InstSerif = FontFamily(Font(R.font.instrumentserif_regular, FontWeight.Normal))
private val InstSans = FontFamily(Font(R.font.instrumentsans_regular, FontWeight.Normal), Font(R.font.instrumentsans_bold, FontWeight.Bold))
private val WorkSans = FontFamily(Font(R.font.worksans_regular, FontWeight.Normal), Font(R.font.worksans_bold, FontWeight.Bold))

fun FontChoice.body(): FontFamily = when (this) {
    FontChoice.INSTSANS -> InstSans
    FontChoice.WORKSANS -> WorkSans
    else -> Outfit
}

fun FontChoice.title(): FontFamily = when (this) {
    FontChoice.OUTFIT -> Outfit
    FontChoice.LORA -> Lora
    FontChoice.CRIMSON -> Crimson
    FontChoice.INSTRUMENT -> InstSerif
    FontChoice.INSTSANS -> InstSans
    FontChoice.WORKSANS -> WorkSans
}

val LocalOpts = staticCompositionLocalOf { Opts() }

val BodyFont: FontFamily
    @Composable get() = LocalOpts.current.font.body()
val TitleFont: FontFamily
    @Composable get() = LocalOpts.current.font.title()

// ───────── colours ─────────

@Immutable
class Pal(
    val bg: Color, val surface: Color, val fg: Color, val mute: Color,
    val line: Color, val primary: Color, val onPrimary: Color,
)

val LightPal = Pal(Color(0xFFFFFFFF), Color(0xFFF2F2F2), Color(0xFF111111), Color(0xFF6B6B6B), Color(0xFFE3E3E3), Color(0xFF111111), Color(0xFFFFFFFF))
val DarkPal = Pal(Color(0xFF000000), Color(0xFF161616), Color(0xFFF4F4F4), Color(0xFF9A9A9A), Color(0xFF262626), Color(0xFFF4F4F4), Color(0xFF0A0A0A))

val LocalPal = staticCompositionLocalOf { LightPal }
val P: Pal
    @Composable get() = LocalPal.current

@Composable
fun BinderTheme(dark: Boolean, content: @Composable () -> Unit) {
    val pal = if (dark) DarkPal else LightPal
    val scheme = if (dark) darkColorScheme(background = pal.bg, surface = pal.surface, onSurface = pal.fg)
    else lightColorScheme(background = pal.bg, surface = pal.surface, onSurface = pal.fg)
    CompositionLocalProvider(LocalPal provides pal) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

// ───────── cover colours & pictures ─────────

class Cover(val top: Color, val bottom: Color, val ink: Color)

val Covers = listOf(
    Cover(Color(0xFF2A4A7A), Color(0xFF142540), Color.White),
    Cover(Color(0xFFB5432E), Color(0xFF7F2A1E), Color.White),
    Cover(Color(0xFF52704C), Color(0xFF34502F), Color.White),
    Cover(Color(0xFFE6CD92), Color(0xFFC49A52), Color(0xFF111111)),
    Cover(Color(0xFF333333), Color(0xFF0C0C0C), Color.White),
    Cover(Color(0xFFEEEEEE), Color(0xFFCDCDCD), Color(0xFF111111)),
)

/** A cover: a colour, and optionally a picture on top of it. */
class Art(val c: Cover, val img: String? = null)

fun coverFor(key: String): Cover = Covers[(key.hashCode() and 0x7fffffff) % Covers.size]
fun Item.look(): Cover = if (cover in Covers.indices) Covers[cover] else coverFor(title)
fun Item.art(): Art = Art(look(), images.firstOrNull())
fun BinderList.stack(): List<Art> = (items.take(3).map { it.art() } + kind.stack.map { Art(Covers[it]) }).take(3)
