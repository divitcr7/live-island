package app.liveisland.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.liveisland.core.Anchor
import app.liveisland.core.Appearance
import app.liveisland.core.SettingsStore
import app.liveisland.island.LensSide
import app.liveisland.service.Cutout
import app.liveisland.service.LensSpot
import app.liveisland.ui.design.P
import app.liveisland.ui.design.PaperDark
import app.liveisland.ui.design.PaperLight
import app.liveisland.ui.design.PaperTheme
import app.liveisland.ui.screens.Access
import app.liveisland.ui.screens.ActivitiesScreen
import app.liveisland.ui.screens.AppsScreen
import app.liveisland.ui.screens.AutomationScreen
import app.liveisland.ui.screens.HomeScreen
import app.liveisland.ui.screens.LookScreen
import app.liveisland.ui.screens.MoreScreen
import app.liveisland.ui.screens.PositionScreen
import app.liveisland.ui.screens.PrivacyScreen
import app.liveisland.ui.screens.RestScreen
import app.liveisland.ui.screens.Sub
import app.liveisland.ui.screens.Tab
import app.liveisland.ui.screens.TabBar

class MainActivity : ComponentActivity() {
    private var spot by mutableStateOf<LensSpot?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val store = SettingsStore.get(this)
        window.decorView.setOnApplyWindowInsetsListener { v, insets ->
            spot = Cutout.detect(this, insets.displayCutout)
            v.onApplyWindowInsets(insets)
        }
        setContent {
            val s by store.settings.collectAsState()
            val dark = when (s.appearance) { Appearance.DARK -> true; Appearance.LIGHT -> false; Appearance.SYSTEM -> isSystemInDarkTheme() }
            // status and nav bar icons follow the app's own theme, not just the phone's
            LaunchedEffect(dark) {
                val bar = if (dark) SystemBarStyle.dark(PaperDark.bg.toArgb()) else SystemBarStyle.light(PaperLight.bg.toArgb(), PaperDark.bg.toArgb())
                enableEdgeToEdge(bar, bar)
            }
            PaperTheme(dark) { App(store) }
        }
    }

    @Composable
    private fun App(store: SettingsStore) {
        val s by store.settings.collectAsState()
        var tab by remember { mutableStateOf(Tab.HOME) }
        var sub by remember { mutableStateOf<Sub?>(null) }
        var subFrom by remember { mutableStateOf(Tab.MORE) }
        var access by remember { mutableStateOf(readAccess()) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { access = readAccess() }

        val lens = spot ?: Cutout.detect(this, null)
        val side = lens.side(s.placement.anchor)
        val detected = if (!lens.detected) "no notch found (centre)" else when (lens.side(Anchor.AUTO)) {
            LensSide.START -> "left"; LensSide.END -> "right"; else -> "centre"
        }
        val open: (Sub) -> Unit = { subFrom = tab; sub = it }
        val closeSub: () -> Unit = { tab = subFrom; sub = null }

        // Back: close a page, else go Home, else leave the app.
        BackHandler(sub != null || tab != Tab.HOME) { if (sub != null) closeSub() else tab = Tab.HOME }

        Column(Modifier.fillMaxSize().background(P.c.bg)) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    targetState = sub?.name ?: tab.name,
                    transitionSpec = {
                        val isSub = Sub.entries.any { it.name == targetState }
                        val wasSub = Sub.entries.any { it.name == initialState }
                        when {
                            isSub -> (slideInHorizontally(tween(240)) { it / 5 } + fadeIn(tween(200))) togetherWith fadeOut(tween(120))
                            wasSub -> fadeIn(tween(200)) togetherWith (slideOutHorizontally(tween(220)) { it / 5 } + fadeOut(tween(160)))
                            else -> fadeIn(tween(180)) togetherWith fadeOut(tween(120))
                        }
                    },
                    label = "nav",
                ) { key ->
                    when (key) {
                        Sub.REST.name -> RestScreen(s, side, store::update, closeSub)
                        Sub.APPS.name -> AppsScreen(s, store::update, closeSub)
                        Sub.AUTOMATION.name -> AutomationScreen(s, store::update, closeSub)
                        Sub.PRIVACY.name -> PrivacyScreen(closeSub)
                        Tab.LOOK.name -> LookScreen(s, side, store::update)
                        Tab.ACTIVITIES.name -> ActivitiesScreen(s, side, access.dnd, store::update)
                        Tab.POSITION.name -> PositionScreen(s, side, detected, store::update)
                        Tab.MORE.name -> MoreScreen(s, store::update, open)
                        else -> HomeScreen(s, side, access, store::update, open, P.c.dark) { access = readAccess() }
                    }
                }
            }
            TabBar(tab) { t -> sub = null; tab = t }
        }
    }

    private fun readAccess() = Access(
        overlay = Permissions.overlay(this),
        notifications = Permissions.notifications(this),
        runtime = Permissions.runtimeGranted(this),
        dnd = Permissions.dnd(this),
    )
}

