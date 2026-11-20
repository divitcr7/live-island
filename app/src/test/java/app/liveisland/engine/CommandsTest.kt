package app.liveisland.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandsTest {
    @Test fun minutesByDefault() = assertEquals(600_000L, Commands.parseDuration("10"))
    @Test fun seconds() = assertEquals(90_000L, Commands.parseDuration("90s"))
    @Test fun minutesSuffix() = assertEquals(300_000L, Commands.parseDuration("5 min"))
    @Test fun hours() = assertEquals(5_400_000L, Commands.parseDuration("1.5h"))
    @Test fun rejectsZero() = assertNull(Commands.parseDuration("0"))
    @Test fun rejectsText() = assertNull(Commands.parseDuration("soon"))
    @Test fun format() { assertEquals("10 min", Commands.fmt(600_000)); assertEquals("45s", Commands.fmt(45_000)); assertEquals("1h 30m", Commands.fmt(5_400_000)) }

    private val sys = object : SystemActions {
        override fun torch() = true
        override fun wifiPanel() {}
        override fun bluetoothSettings() {}
        override fun internetPanel() {}
        override fun launch(pkg: String) {}
        override fun pinNote(text: String) {}
        override fun openShelf() {}
        override fun apps() = listOf("Spotify" to "com.spotify.music", "Google Maps" to "com.google.android.apps.maps")
    }

    @Test fun timerSuggestion() = assertTrue(Commands.suggest("timer 10", sys).first().title.contains("10 min"))
    @Test fun bareNumberIsTimer() = assertTrue(Commands.suggest("5", sys).first().title.contains("5 min"))
    @Test fun focus() = assertTrue(Commands.suggest("focus 25", sys).first().title.startsWith("Focus"))
    @Test fun note() = assertTrue(Commands.suggest("note door 4471", sys).first().title.contains("door 4471"))
    @Test fun appSearch() = assertTrue(Commands.suggest("maps", sys).any { it.title == "Google Maps" })
    @Test fun torch() = assertEquals("Torch", Commands.suggest("tor", sys).first().title)
}
