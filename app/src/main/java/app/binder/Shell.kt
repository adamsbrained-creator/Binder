package app.binder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ───────── the bottom button: menu, and hold it for "+" ─────────

/** Black / white menu button. Tap opens the menu; press and hold pops a + out beside it. */
@Composable
fun BoxScope.MenuFab(
    vm: BinderViewModel,
    bottom: Int = 0,
    onAdd: () -> Unit,
    onAddHold: (() -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    LaunchedEffect(open) {
        if (open) {
            delay(4000)
            open = false
        }
    }
    Row(
        Modifier.align(Alignment.BottomEnd).navigationBarsPadding()
            .padding(end = 16.dp, bottom = (bottom + 18).dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = open,
            enter = fadeIn() + scaleIn(initialScale = 0.4f) + slideInHorizontally { it / 2 },
            exit = fadeOut() + scaleOut(targetScale = 0.4f),
        ) {
            RoundBtn(
                Ic.Plus, "Add", 58.dp, Modifier.border(1.dp, P.line, CircleShape),
                onHold = onAddHold?.let { h -> { open = false; h() } },
            ) { open = false; onAdd() }
        }
        RoundBtn(Ic.Menu, "Menu", 58.dp, onHold = { open = true }) {
            if (open) open = false else vm.menu = true
        }
    }
}

// ───────── menu sheet ─────────

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(CircleShape).background(P.surface)
            .clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Txt(label, 15, modifier = Modifier.weight(1f))
        Icon(Ic.Chevron, null, tint = P.mute, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun BoxScope.MenuSheet(vm: BinderViewModel) {
    val o = vm.opts
    val nav = vm.nav
    val go = { r: Route -> vm.menu = false; nav.push(r) }
    SheetFrame(onClose = { vm.menu = false }) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Txt("Menu", 22, weight = FontWeight.Medium, modifier = Modifier.weight(1f))
            IconBtn(Ic.Close, "Close") { vm.menu = false }
        }
        Column(Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
            Cap(o.lists.uppercase())
            MenuRow("All ${o.listsLower}") { vm.menu = false; nav.root(Route.Home) }
            MenuRow("Pinned") { go(Route.Lists(0)) }
            MenuRow("Recently edited") { go(Route.Lists(1)) }
            // the View sheet belongs to the Binds screen, so go there first
            MenuRow("View") { vm.menu = false; nav.root(Route.Home); vm.homeView = true }
            Cap("TOOLS")
            MenuRow("Search") { go(Route.Search) }
            Cap("APP")
            MenuRow("Settings") { go(Route.More) }
            MenuRow("About") { go(Route.About) }
        }
    }
}

// ───────── About ─────────

private const val CHANGELOG = """1.0.0
• New look: menu button, round header buttons, rings and progress bars
• One App name switch (Binder or Catalog) sets the name, the icon, the label under the icon and the word for your lists
• Launcher icon: ink on white, in Outfit Medium, with a themed (monochrome) version
• Albums can fetch cover, artist, year and genre from Apple Music (off by default)
• View options per list: columns and what shows under covers
• Items remember where their details came from
• More sources (each only when "Fetch details automatically" is on): TMDB for movies and series, RAWG for games, Open Library (with Google Books) for books and comics, AniList for manga and anime, Wikipedia for Anything, OpenStreetMap for places, and the title and picture of any other pasted web page
• New Manga and Comics lists
• A list can pick its source (list menu > Source, or the switcher in quick add)
• Details card also shows runtime, episodes, pages, volumes or platforms
• Ratings: fixed the part-filled star, which sat too far left. Stars are now drawn exactly. New Rating style option (Stars or a number like 4.75 ★) with a 0 to 5 slider in steps of 0.25
• View sheet for the home screen (layout, header, show) opened from the menu
• Bento and Stacks no longer overlap or show placeholder gradients
• Settings page rebuilt in the same style as Paperback's (More is now called Settings): a summary card, grouped rows with icons, and a page for each group
• Header sizes like Paperback (Compact, Regular, Large) with a small page title under the menu button; Compact has no page title on the home screen
• New option: hide the coral progress line at the top of a list (Settings > Items & ratings)

0.5
• Pictures, layouts, fonts, gestures, zip backup, Binder / Catalog name

0.1
• First version"""

private const val LICENCES = """Fonts: Outfit, Lora, Crimson Pro, Instrument Serif, Instrument Sans and Work Sans are used under the SIL Open Font License 1.1. The licence texts are in the FONT-LICENSES folder of the project.

Album details and covers come from the Apple Music (iTunes) search service when fetching is on. Apple and Apple Music are trademarks of Apple Inc. Binder is not affiliated with or endorsed by Apple.

Movie and series details and posters come from TMDB. This product uses the TMDB API but is not endorsed or certified by TMDB.

Game details and pictures come from RAWG (rawg.io).

Book details and covers come from Open Library (openlibrary.org), a project of the Internet Archive, and from Google Books when Open Library has nothing.

Place details come from OpenStreetMap (© OpenStreetMap contributors, ODbL, openstreetmap.org/copyright) through Nominatim.

Anime and manga details and covers come from AniList (anilist.co).

Text and pictures from Wikipedia are available under the Creative Commons Attribution-ShareAlike licence; each item keeps a link to its article. Other web pages are read through the title, picture and description they publish themselves (Open Graph tags)."""

@Composable
private fun InfoRow(title: String, trailing: String = "", body: String) {
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(RoundedCornerShape(26.dp)).background(P.surface)
            .clickable { open = !open }.padding(horizontal = 20.dp, vertical = 15.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Txt(title, 15, modifier = Modifier.weight(1f))
            if (trailing.isNotEmpty()) Txt(trailing, 14, P.mute, Modifier.padding(end = 8.dp))
            Icon(Ic.Chevron, null, tint = P.mute, modifier = Modifier.size(18.dp))
        }
        if (open) Txt(body, 13, P.mute, Modifier.padding(top = 10.dp), lh = 19.sp)
    }
}

@Composable
fun AboutScreen(vm: BinderViewModel) {
    val o = vm.opts
    val ctx = LocalContext.current
    Screen {
        Header2("About", onBack = { vm.nav.pop() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.size(104.dp).clip(RoundedCornerShape(26.dp)).background(Color.White)
                        .border(1.dp, P.line, RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    // the real launcher artwork, so About always shows the icon that is active
                    Image(
                        painterResource(if (o.isCatalog) R.drawable.ic_launcher_catalog_foreground else R.drawable.ic_launcher_foreground),
                        contentDescription = o.appLabel,
                        modifier = Modifier.size(104.dp),
                    )
                }
                Txt("Version ${BuildConfig.VERSION_NAME}", 22, weight = FontWeight.Medium, modifier = Modifier.padding(top = 16.dp))
                Txt(
                    "${o.lists} and pictures stay on this phone. Only the text you search for is sent when fetching is on.",
                    14, P.mute, Modifier.padding(top = 6.dp, start = 12.dp, end = 12.dp), align = TextAlign.Center, lh = 20.sp,
                )
            }
            InfoRow(
                "Data", "On this phone",
                "Your ${o.listsLower} are saved in a file on this phone and your pictures in a folder next to it. Nothing is sent anywhere unless you turn on “Fetch details automatically” in Settings. Then only the words you type to search (or a link you paste) go to the source.",
            )
            InfoRow("Changelog", body = CHANGELOG)
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(CircleShape).background(P.surface)
                    .clickable { vm.nav.push(Route.Setting(SettingsPage.BACKUP)) }.padding(horizontal = 20.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt("Backup and restore", 15, modifier = Modifier.weight(1f))
                Icon(Ic.Chevron, null, tint = P.mute, modifier = Modifier.size(18.dp))
            }
            InfoRow("Licences", body = LICENCES)
            // RAWG asks for an active link back to RAWG wherever its data is used
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp).clip(CircleShape).background(P.surface)
                    .clickable { openLink(ctx, "https://rawg.io") }.padding(horizontal = 20.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Txt("Data from RAWG", 15, modifier = Modifier.weight(1f))
                Icon(Ic.Out, null, tint = P.mute, modifier = Modifier.size(18.dp))
            }
            Cap("PART OF THE FAMILY")
            FlowChips(listOf("Paperback", "Scrapbook", "Binder"), 2) { }
            Spacer(Modifier.height(40.dp))
        }
    }
}
