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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.liveisland.core.Anchor
import app.liveisland.core.SettingsStore
import app.liveisland.service.Cutout
import app.liveisland.service.LensSpot
import app.liveisland.ui.design.P
import app.liveisland.ui.design.PaperLight
import app.liveisland.ui.design.PaperTheme
import app.liveisland.ui.screens.Access
import app.liveisland.ui.screens.ActivitiesScreen
import app.liveisland.ui.screens.AppsScreen
import app.liveisland.ui.screens.AutomationScreen
import app.liveisland.ui.screens.HomeScreen
import app.liveisland.ui.screens.LookScreen
import app.liveisland.ui.screens.PositionScreen
import app.liveisland.ui.screens.PrivacyScreen
import app.liveisland.ui.screens.RestScreen
import app.liveisland.ui.screens.Route

class MainActivity : ComponentActivity() {
    private var spot by mutableStateOf<LensSpot?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(PaperLight.bg.toArgb(), 0xFF111113.toInt()),
            navigationBarStyle = SystemBarStyle.auto(PaperLight.bg.toArgb(), 0xFF111113.toInt()),
        )
        super.onCreate(savedInstanceState)
        val store = SettingsStore.get(this)
        window.decorView.setOnApplyWindowInsetsListener { v, insets ->
            spot = Cutout.detect(this, insets.displayCutout)
            v.onApplyWindowInsets(insets)
        }
        setContent { PaperTheme { App(store) } }
    }

    @Composable
    private fun App(store: SettingsStore) {
        val s by store.settings.collectAsState()
        val stack = remember { mutableStateListOf(Route.Home) }
        var access by remember { mutableStateOf(readAccess()) }
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { access = readAccess() }
        val lens = spot ?: Cutout.detect(this, null)
        val side = lens.side(s.placement.anchor)
        val detected = if (!lens.detected) "no notch found (centre)" else when (lens.side(Anchor.AUTO)) {
            app.liveisland.island.LensSide.START -> "left"; app.liveisland.island.LensSide.END -> "right"; else -> "centre"
        }
        val go: (Route) -> Unit = { stack.add(it) }
        val back: () -> Unit = { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
        BackHandler(stack.size > 1) { back() }

        Box(Modifier.fillMaxSize().background(P.c.bg)) {
            AnimatedContent(
                targetState = stack.last(),
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal || initialState == Route.Home
                    val dir = if (targetState == Route.Home) -1 else if (forward) 1 else -1
                    (slideInHorizontally(tween(260)) { it / 6 * dir } + fadeIn(tween(220))) togetherWith
                        (slideOutHorizontally(tween(220)) { -it / 8 * dir } + fadeOut(tween(160)))
                },
                label = "nav",
            ) { route ->
                when (route) {
                    Route.Home -> HomeScreen(s, side, access, store::update, go) { access = readAccess() }
                    Route.Look -> LookScreen(s, side, store::update, back)
                    Route.Position -> PositionScreen(s, side, detected, store::update, back)
                    Route.Rest -> RestScreen(s, side, store::update, back)
                    Route.Activities -> ActivitiesScreen(s, side, access.dnd, store::update, back)
                    Route.Apps -> AppsScreen(s, store::update, back)
                    Route.Automation -> AutomationScreen(s, store::update, back)
                    Route.Privacy -> PrivacyScreen(back)
                }
            }
        }
    }

    private fun readAccess() = Access(
        overlay = Permissions.overlay(this),
        notifications = Permissions.notifications(this),
        runtime = Permissions.runtimeGranted(this),
        dnd = Permissions.dnd(this),
    )
}
