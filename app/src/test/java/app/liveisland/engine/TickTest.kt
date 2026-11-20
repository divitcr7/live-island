package app.liveisland.engine

import app.liveisland.core.IslandItem
import app.liveisland.core.Kind
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/** The island must only wake the CPU as often as what's on screen actually needs. */
class TickTest {
    private val t = 1_700_000_000_000L // a whole second

    @After fun clear() = IslandEngine.removeWhere { true }

    @Test fun idleNoRotationSleepsAMinute() = assertEquals(60_000L, IslandEngine.nextTickIn(t, null, false))
    @Test fun idleRotatesFacts() = assertEquals(4_000L, IslandEngine.nextTickIn(t, 4_000L, false))
    @Test fun runningTimerTicksEverySecond() {
        IslandEngine.post(IslandItem("t", Kind.TIMER, endsAt = t + 60_000, totalMs = 60_000))
        assertEquals(1_000L, IslandEngine.nextTickIn(t, 4_000L, false))
    }
    @Test fun pausedMusicDoesNotTick() {
        IslandEngine.post(IslandItem("m", Kind.MEDIA, playing = false))
        assertEquals(60_000L, IslandEngine.nextTickIn(t, 4_000L, false))
    }
    @Test fun wakesForExpiry() {
        IslandEngine.post(IslandItem("c", Kind.CHARGING, expiresAt = t + 4_500))
        assertEquals(4_500L, IslandEngine.nextTickIn(t, null, false)) // exact, not rounded
    }
    @Test fun alignsToSecond() = assertEquals(59_700L, IslandEngine.nextTickIn(t + 300, null, false))
}
