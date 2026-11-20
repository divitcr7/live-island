package app.liveisland.ui.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.liveisland.island.Geist
import app.liveisland.island.GeistMono

/**
 * "Paper" — Live Island's own design language for the settings app.
 * Warm paper, ink type, hairline rules, inverted selection. Not Material, not iOS, not One UI.
 */
@Immutable
data class Paper(
    val bg: Color, val surface: Color, val raised: Color, val line: Color,
    val fg: Color, val muted: Color, val dim: Color, val inv: Color,
    val ok: Color, val warn: Color, val dark: Boolean,
)

val PaperLight = Paper(
    bg = Color(0xFFEEEDEA), surface = Color(0xFFF7F6F3), raised = Color(0xFFFFFFFF), line = Color(0xFFD9D7D2),
    fg = Color(0xFF1B1B1D), muted = Color(0xFF6C6A70), dim = Color(0xFF9A989E), inv = Color(0xFFF7F6F3),
    ok = Color(0xFF2F8F5B), warn = Color(0xFFD2512F), dark = false,
)
val PaperDark = Paper(
    bg = Color(0xFF111113), surface = Color(0xFF18181B), raised = Color(0xFF1F1F23), line = Color(0xFF2A2A2E),
    fg = Color(0xFFE9E7E2), muted = Color(0xFF929097), dim = Color(0xFF646268), inv = Color(0xFF111113),
    ok = Color(0xFF6CC79A), warn = Color(0xFFF2603C), dark = true,
)

@Immutable
data class Type(
    val display: TextStyle, val title: TextStyle, val heading: TextStyle, val body: TextStyle,
    val small: TextStyle, val label: TextStyle, val num: TextStyle,
)

private val type = Type(
    display = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, lineHeight = 36.sp, letterSpacing = (-1.1).sp),
    title = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 28.sp, letterSpacing = (-0.6).sp),
    heading = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp),
    body = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 14.5.sp, lineHeight = 20.sp),
    small = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 12.5.sp, lineHeight = 17.sp),
    label = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Normal, fontSize = 10.5.sp, letterSpacing = 0.9.sp),
    num = TextStyle(fontFamily = GeistMono, fontWeight = FontWeight.Medium, fontSize = 13.sp, fontFeatureSettings = "tnum"),
)

val LocalPaper = staticCompositionLocalOf { PaperLight }
val LocalType = staticCompositionLocalOf { type }

object P {
    val c: Paper @Composable get() = LocalPaper.current
    val t: Type @Composable get() = LocalType.current
}

@Composable
fun PaperTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalPaper provides if (dark) PaperDark else PaperLight,
        LocalType provides type,
        content = content,
    )
}
