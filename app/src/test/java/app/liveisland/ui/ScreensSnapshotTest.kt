package app.liveisland.ui

import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.app.ActivityOptionsCompat
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.liveisland.core.Settings
import app.liveisland.core.ThemeId
import app.liveisland.island.LensSide
import app.liveisland.ui.design.PaperTheme
import app.liveisland.ui.screens.Access
import app.liveisland.ui.screens.ActivitiesScreen
import app.liveisland.ui.screens.HomeScreen
import app.liveisland.ui.screens.LookScreen
import app.liveisland.ui.screens.PositionScreen
import app.liveisland.ui.screens.PrivacyScreen
import app.liveisland.ui.screens.RestScreen
import com.android.resources.NightMode
import org.junit.Rule
import org.junit.Test

/** Renders real screens to PNG so the design can be reviewed without a device. */
class ScreensSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, maxPercentDifference = 1.0)

    private val noRegistry = object : ActivityResultRegistryOwner {
        override val activityResultRegistry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(requestCode: Int, contract: ActivityResultContract<I, O>, input: I, options: ActivityOptionsCompat?) = Unit
        }
    }

    private fun shot(name: String, content: @Composable () -> Unit) = paparazzi.snapshot(name) {
        CompositionLocalProvider(LocalActivityResultRegistryOwner provides noRegistry) { PaperTheme(content = content) }
    }

    private val s = Settings()

    @Test fun home_setup() = shot("home_setup") { HomeScreen(s, LensSide.CENTER, Access(false, false, false, false), {}, {}, false, {}) }
    @Test fun home_ready() = shot("home_ready") { HomeScreen(s.copy(theme = ThemeId.CODED), LensSide.CENTER, Access(true, true, true, true), {}, {}, false, {}) }
    @Test fun look() = shot("look") { LookScreen(s, LensSide.CENTER, {}, {}) }
    @Test fun look_clay() = shot("look_clay") { LookScreen(s.copy(theme = ThemeId.CLAY), LensSide.CENTER, {}, {}) }
    @Test fun position_left() = shot("position_left") { PositionScreen(s, LensSide.START, "left", {}, {}) }
    @Test fun rest() = shot("rest") { RestScreen(s, LensSide.CENTER, {}, {}) }
    @Test fun activities() = shot("activities") { ActivitiesScreen(s.copy(theme = ThemeId.INSTRUMENT), LensSide.CENTER, false, {}, {}) }
    @Test fun privacy() = shot("privacy") { PrivacyScreen {} }
}

class DarkScreensSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6.copy(nightMode = NightMode.NIGHT), maxPercentDifference = 1.0)
    @Test fun look_dark() = paparazzi.snapshot("look_dark") {
        PaperTheme { LookScreen(Settings(theme = ThemeId.INSTRUMENT), LensSide.CENTER, {}, {}) }
    }
}
