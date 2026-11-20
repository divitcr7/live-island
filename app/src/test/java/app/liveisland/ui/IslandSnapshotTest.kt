package app.liveisland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.liveisland.core.Settings
import app.liveisland.core.ThemeId
import app.liveisland.island.IslandStyle
import app.liveisland.island.LensSide
import app.liveisland.ui.design.PaperTheme
import app.liveisland.ui.design.PaperLight
import org.junit.Rule
import org.junit.Test

/** Every island state in every theme, in a real phone top. */
class IslandSnapshotTest {
    @get:Rule val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6, maxPercentDifference = 1.0)

    private val states = listOf(Demo.REST, Demo.MUSIC, Demo.MUSIC_OPEN, Demo.STACKED, Demo.TIMER_OPEN, Demo.CALL, Demo.ON_CALL, Demo.MESSAGE, Demo.OTP, Demo.CHARGING, Demo.LIVE, Demo.NAV)

    private fun sheet(theme: ThemeId, side: LensSide, list: List<Demo>) = paparazzi.snapshot("${theme.name.lowercase()}_${side.name.lowercase()}") {
        PaperTheme {
            val s = Settings(theme = theme)
            Column(Modifier.background(PaperLight.bg).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                list.forEach { d -> PhoneTop(IslandStyle.of(s), d, s.placement, side, s.rest, s.features, height = if (d in setOf(Demo.MUSIC_OPEN, Demo.TIMER_OPEN, Demo.MESSAGE)) 150.dp else 80.dp) }
            }
        }
    }

    @Test fun signal_a() = sheet(ThemeId.SIGNAL, LensSide.CENTER, states.take(6))
    @Test fun coded() = sheet(ThemeId.CODED, LensSide.CENTER, states.drop(6))
    @Test fun instrument() = sheet(ThemeId.INSTRUMENT, LensSide.START, states.take(6))
    @Test fun clay() = sheet(ThemeId.CLAY, LensSide.END, states.take(6))
}
