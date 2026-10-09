package app.binder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em

/** Height the bottom bar takes, so lists can leave room for it. */
fun navHeight(n: NavStyle): Int = when (n) {
    NavStyle.DOCK, NavStyle.MINI -> 72
    NavStyle.PILL -> 84
    NavStyle.SPLIT, NavStyle.SEARCH -> 82
    NavStyle.NOTCH -> 70
    else -> 0
}

/** These bars carry their own "new" button, so the floating one is hidden. */
fun showsPlus(n: NavStyle): Boolean = n == NavStyle.SPLIT || n == NavStyle.NOTCH || n == NavStyle.SEARCH

// ───────── header ─────────

@Composable
fun AppHeader(vm: BinderViewModel) {
    val o = vm.opts
    val h = if (o.nav == NavStyle.DRAWER && (o.header == HeaderStyle.WORDMARK || o.header == HeaderStyle.CIRCLES))
        HeaderStyle.MENU_LEFT else o.header
    val name = o.appName
    val search = { vm.nav.push(Route.Search) }
    val more = { vm.nav.push(Route.More) }
    val menu = { vm.drawer = true }
    when (h) {
        HeaderStyle.WORDMARK -> Row(
            Modifier.fillMaxWidth().height(60.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Txt(name, 28, weight = FontWeight.Medium, modifier = Modifier.weight(1f), ls = (-0.03).em)
            CircleBtn(Ic.Search, "Search") { search() }
            CircleBtn(Ic.Dots, "More") { more() }
        }
        HeaderStyle.CIRCLES -> Box(Modifier.fillMaxWidth().height(60.dp)) {
            Box(Modifier.align(Alignment.CenterStart)) { CircleBtn(Ic.Search, "Search") { search() } }
            Txt(name, 22, weight = FontWeight.Medium, modifier = Modifier.align(Alignment.Center), ls = (-0.02).em)
            Box(Modifier.align(Alignment.CenterEnd)) { CircleBtn(Ic.Dots, "More") { more() } }
        }
        HeaderStyle.MENU_CENTER -> Box(Modifier.fillMaxWidth().height(60.dp)) {
            Box(Modifier.align(Alignment.CenterStart)) { IconBtn(Ic.Menu, "Menu") { menu() } }
            Txt(name, 22, weight = FontWeight.Medium, modifier = Modifier.align(Alignment.Center), ls = (-0.02).em)
            Row(Modifier.align(Alignment.CenterEnd)) {
                IconBtn(Ic.Search, "Search") { search() }
                IconBtn(Ic.Dots, "More") { more() }
            }
        }
        HeaderStyle.MENU_LEFT -> Row(
            Modifier.fillMaxWidth().height(60.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBtn(Ic.Menu, "Menu") { menu() }
            Txt(name, 22, weight = FontWeight.Medium, modifier = Modifier.weight(1f).padding(start = 4.dp), ls = (-0.02).em)
            IconBtn(Ic.Search, "Search") { search() }
            IconBtn(Ic.Dots, "More") { more() }
        }
    }
}

/** Underlined tabs under the header (only when the navigation style is "Top tabs"). */
@Composable
fun TopTabs(vm: BinderViewModel, cur: Route) {
    if (vm.opts.nav != NavStyle.TABS) return
    val tabs = listOf<Pair<Route, String>>(Route.Home to "Lists", Route.Search to "Search", Route.More to "More")
    Row(Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
        tabs.forEach { (r, label) ->
            val on = r == cur
            Column(Modifier.width(IntrinsicSize.Max).clickable { vm.nav.root(r) }.padding(top = 8.dp)) {
                Txt(label, 15, if (on) P.fg else P.mute, weight = if (on) FontWeight.Medium else FontWeight.Normal)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.fillMaxWidth().height(3.dp).clip(CircleShape)
                        .background(if (on) Coral else Color.Transparent)
                )
            }
        }
    }
}

// ───────── bottom navigation ─────────

private class Tab(val route: Route, val icon: ImageVector, val label: String)

private val TABS = listOf(
    Tab(Route.Home, Ic.List, "Lists"),
    Tab(Route.Search, Ic.Search, "Search"),
    Tab(Route.More, Ic.Dots, "More"),
)

/** mode: 0 = icons only, 1 = label on the selected one, 2 = every label */
@Composable
private fun NavPill(cur: Route, vm: BinderViewModel, mode: Int) {
    Row(
        Modifier.clip(CircleShape).background(P.surface).border(1.dp, P.line, CircleShape).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        TABS.forEach { t ->
            val on = t.route == cur
            val label = mode == 2 || (mode == 1 && on)
            Row(
                Modifier.height(44.dp).clip(CircleShape)
                    .background(if (on) P.primary else Color.Transparent)
                    .clickable { vm.nav.root(t.route) }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(t.icon, t.label, tint = if (on) P.onPrimary else P.mute, modifier = Modifier.size(20.dp))
                if (label) Txt(t.label, 14, if (on) P.onPrimary else P.mute)
            }
        }
    }
}

@Composable
private fun BoxScope.Fade(content: @Composable () -> Unit) {
    Box(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, P.bg)))
            .navigationBarsPadding().padding(top = 36.dp, bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun NotchTab(t: Tab, cur: Route, vm: BinderViewModel) {
    val on = t.route == cur
    Box(Modifier.size(48.dp).clip(CircleShape).clickable { vm.nav.root(t.route) }, contentAlignment = Alignment.Center) {
        Icon(t.icon, t.label, tint = if (on) P.fg else P.mute, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun BoxScope.NavBar(vm: BinderViewModel, cur: Route) {
    val newList = { vm.nav.push(Route.NewList) }
    when (vm.opts.nav) {
        NavStyle.DOCK -> Fade { NavPill(cur, vm, 1) }
        NavStyle.MINI -> Fade { NavPill(cur, vm, 0) }
        NavStyle.PILL -> {
            val shape = RoundedCornerShape(28.dp)
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                    .clip(shape).background(P.surface).border(1.dp, P.line, shape).padding(8.dp)
            ) {
                TABS.forEach { t ->
                    val on = t.route == cur
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                            .background(if (on) P.primary else Color.Transparent)
                            .clickable { vm.nav.root(t.route) }.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Icon(t.icon, t.label, tint = if (on) P.onPrimary else P.mute, modifier = Modifier.size(20.dp))
                        Txt(t.label, 11, if (on) P.onPrimary else P.mute)
                    }
                }
            }
        }
        NavStyle.SPLIT -> Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavPill(cur, vm, 1)
            RoundPlus(52.dp) { newList() }
        }
        NavStyle.NOTCH -> Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(top = 28.dp).background(P.surface).navigationBarsPadding()) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(P.line))
                Row(
                    Modifier.fillMaxWidth().height(64.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NotchTab(TABS[0], cur, vm)
                    NotchTab(TABS[1], cur, vm)
                    Spacer(Modifier.width(66.dp))
                    NotchTab(TABS[2], cur, vm)
                }
            }
            Box(Modifier.align(Alignment.TopCenter).size(66.dp).clip(CircleShape).background(P.bg).padding(5.dp)) {
                RoundPlus(56.dp) { newList() }
            }
        }
        NavStyle.SEARCH -> Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                Modifier.weight(1f).height(52.dp).clip(CircleShape).background(P.surface)
                    .border(1.dp, P.line, CircleShape).padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    Modifier.weight(1f).clickable { vm.nav.root(Route.Search) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Ic.Search, "Search", tint = P.mute, modifier = Modifier.size(20.dp))
                    Txt("Search ${vm.opts.appName}…", 14, P.mute, maxLines = 1)
                }
                IconBtn(Ic.List, "Lists") { vm.nav.root(Route.Home) }
                IconBtn(Ic.Dots, "More") { vm.nav.root(Route.More) }
            }
            RoundPlus(52.dp) { newList() }
        }
        NavStyle.RAIL -> Column(
            Modifier.align(Alignment.CenterStart).padding(start = 12.dp)
                .clip(CircleShape).background(P.surface).border(1.dp, P.line, CircleShape).padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TABS.forEach { t ->
                val on = t.route == cur
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(if (on) P.primary else Color.Transparent)
                        .clickable { vm.nav.root(t.route) },
                    contentAlignment = Alignment.Center,
                ) { Icon(t.icon, t.label, tint = if (on) P.onPrimary else P.mute, modifier = Modifier.size(20.dp)) }
            }
        }
        NavStyle.TABS, NavStyle.DRAWER, NavStyle.NONE -> {}
    }
}

// ───────── slide-out drawer ─────────

@Composable
private fun DrawerItem(icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(if (on) P.primary else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, label, tint = if (on) P.onPrimary else P.mute, modifier = Modifier.size(20.dp))
        Txt(label, 15, if (on) P.onPrimary else P.fg)
    }
}

@Composable
fun BoxScope.Drawer(vm: BinderViewModel, cur: Route) {
    val st = remember { MutableTransitionState(false).apply { targetState = true } }
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f))
            .pointerInput(Unit) { detectTapGestures { vm.drawer = false } }
    )
    AnimatedVisibility(
        visibleState = st,
        modifier = Modifier.align(Alignment.CenterStart),
        enter = slideInHorizontally { -it },
    ) {
        Column(
            Modifier.fillMaxHeight().fillMaxWidth(0.72f)
                .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)).background(P.surface)
                .statusBarsPadding().navigationBarsPadding().padding(16.dp)
        ) {
            Row(
                Modifier.padding(start = 6.dp, top = 8.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LogoMark(36.dp)
                Txt(vm.opts.appName, 24, weight = FontWeight.Medium, ls = (-0.02).em)
            }
            DrawerItem(Ic.List, "Lists", cur == Route.Home) { vm.drawer = false; vm.nav.root(Route.Home) }
            DrawerItem(Ic.Search, "Search", cur == Route.Search) { vm.drawer = false; vm.nav.root(Route.Search) }
            DrawerItem(Ic.Plus, "New list", false) { vm.drawer = false; vm.nav.push(Route.NewList) }
            DrawerItem(Ic.Dots, "More", cur == Route.More) { vm.drawer = false; vm.nav.root(Route.More) }
        }
    }
}
