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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: BinderViewModel = viewModel()
            val dark = when (vm.opts.theme) {
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
    val top = nav.top
    val overlay = vm.drawer || vm.sheet != null || vm.quick != null
    BackHandler(enabled = overlay || vm.reorder || top != Route.Home || nav.stack.size > 1) {
        when {
            vm.sheet != null -> vm.sheet = null
            vm.quick != null -> vm.quick = null
            vm.drawer -> vm.drawer = false
            vm.reorder -> vm.reorder = false
            else -> nav.back()
        }
    }

    CompositionLocalProvider(LocalOpts provides vm.opts) {
        Box(Modifier.fillMaxSize().background(P.bg)) {
            Crossfade(targetState = top, label = "route") { r ->
                when (r) {
                    Route.Home -> HomeScreen(vm)
                    Route.Search -> SearchScreen(vm)
                    Route.More -> MoreScreen(vm)
                    Route.NewList -> NewListScreen(vm)
                    is Route.Lst -> ListScreen(vm, r.id)
                    is Route.ItemEdit -> ItemScreen(vm, r.listId, r.itemId)
                }
            }
            val topLevel = top == Route.Home || top == Route.Search || top == Route.More
            if (topLevel) NavBar(vm, top)
            val gap = if (topLevel) navHeight(vm.opts.nav) else 0
            val u = vm.undo
            if (vm.reorder) ReorderBar(vm, gap) else if (u != null) UndoBar(vm, u, gap)
            if (vm.drawer) Drawer(vm, top)
            val sh = vm.sheet
            if (sh != null) ActionSheet(vm, sh)
            val q = vm.quick
            if (q != null) QuickAdd(vm, q)
        }
        val rn = vm.rename
        if (rn != null) RenameDialog(vm, rn)
    }
}
