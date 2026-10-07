package app.binder

import android.graphics.Color as AColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: BinderViewModel = viewModel()
            val dark = when (vm.theme) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(AColor.TRANSPARENT, AColor.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            BinderTheme(dark) { App(vm) }
        }
    }
}

@Composable
fun App(vm: BinderViewModel) {
    val nav = vm.nav
    BackHandler(enabled = nav.stack.size > 1) { nav.pop() }

    Box(Modifier.fillMaxSize().background(P.bg)) {
        Crossfade(targetState = nav.top, label = "route") { r ->
            when (r) {
                Route.Home -> HomeScreen(vm)
                Route.Search -> SearchScreen(vm)
                Route.More -> MoreScreen(vm)
                Route.NewList -> NewListScreen(vm)
                is Route.Lst -> ListScreen(vm, r.id)
                is Route.ItemEdit -> ItemScreen(vm, r.listId, r.itemId)
            }
        }
        val top = nav.top
        if (top == Route.Home || top == Route.Search || top == Route.More) {
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, P.bg)))
                    .navigationBarsPadding().padding(top = 36.dp, bottom = 14.dp),
                contentAlignment = Alignment.Center,
            ) { Dock(top, vm) }
        }
    }
}

@Composable
fun Dock(current: Route, vm: BinderViewModel) {
    val tabs = listOf(
        Triple(Route.Home, Ic.List, "Lists"),
        Triple(Route.Search, Ic.Search, "Search"),
        Triple(Route.More, Ic.Dots, "More"),
    )
    Row(
        Modifier.clip(CircleShape).background(P.surface).border(1.dp, P.line, CircleShape).padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEach { (route, ic, label) ->
            val on = route == current
            Row(
                Modifier.height(44.dp).clip(CircleShape)
                    .background(if (on) P.primary else Color.Transparent)
                    .clickable { vm.nav.root(route) }.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(ic, label, tint = if (on) P.onPrimary else P.mute, modifier = Modifier.size(20.dp))
                if (on) Txt(label, 14, P.onPrimary)
            }
        }
    }
}
