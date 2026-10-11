package app.binder

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Keeps the launcher entry (icon and label) in step with Opts.appName.
 * The manifest has two activity-aliases, LauncherBinder and LauncherCatalog. Exactly one is enabled.
 */
object Launcher {
    private const val BINDER = "app.binder.LauncherBinder"
    private const val CATALOG = "app.binder.LauncherCatalog"

    private fun isOn(pm: PackageManager, ctx: Context, cls: String, enabledInManifest: Boolean): Boolean =
        when (pm.getComponentEnabledSetting(ComponentName(ctx.packageName, cls))) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> true
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED -> false
            else -> enabledInManifest // "default" means whatever the manifest says
        }

    private fun set(pm: PackageManager, ctx: Context, cls: String, on: Boolean) {
        pm.setComponentEnabledSetting(
            ComponentName(ctx.packageName, cls),
            if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    /** Makes the alias for the chosen name the only enabled one. Safe to call any time; does nothing if already right. */
    fun sync(ctx: Context, catalog: Boolean) {
        runCatching {
            val pm = ctx.packageManager
            val want = if (catalog) CATALOG else BINDER
            val old = if (catalog) BINDER else CATALOG
            // The new entry first, then the old one, so the app never has no launcher icon.
            if (!isOn(pm, ctx, want, want == BINDER)) set(pm, ctx, want, true)
            if (isOn(pm, ctx, old, old == BINDER)) set(pm, ctx, old, false)
        }
    }
}
