package app.liveisland.island

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Our own icon set (24x24 grid). No Material icons anywhere in the app. */
enum class Glyph(private val d: String, private val stroke: Boolean, private val width: Float = 1.8f) {
    PLAY("M8 5v14l11-7z", false),
    PAUSE("M7 5h3.5v14H7zM13.5 5H17v14h-3.5z", false),
    PREV("M6 5h2v14H6zM20 5v14L9 12z", false),
    NEXT("M16 5h2v14h-2zM4 5v14l11-7z", false),
    HEART("M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z", true),
    TURN("M6 20v-7a4 4 0 0 1 4-4h8M14 5l4 4-4 4", true, 2.2f),
    PHONE("M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a1 1 0 0 1-1 1A16 16 0 0 1 4 5a1 1 0 0 1 1-1z", false),
    BOLT("M13 2 4 14h7l-1 8 9-12h-7z", false),
    HOURGLASS("M7 3h10M7 21h10M8 3c0 5 8 5 8 9s-8 4-8 9M16 3c0 5-8 5-8 9s8 4 8 9", true),
    STOPWATCH("M12 21a8 8 0 1 0 0-16 8 8 0 0 0 0 16zM12 9v4l2.5 2M10 2h4", true),
    BT("M7 7l10 10-5 4V3l5 4L7 17", true, 1.9f),
    CAL("M4 5h16v15H4zM4 10h16M9 3v4M15 3v4", true),
    BATTERY("M3 7h16v10H3zM21 10.5v3", true),
    CLOUD("M7 18h10a4 4 0 0 0 .5-8A6 6 0 0 0 6 11a3.5 3.5 0 0 0 1 7z", true),
    DOWN("M12 4v12M6 11l6 6 6-6M5 20h14", true, 2f),
    UP("M12 20V8M6 13l6-6 6 6M5 4h14", true, 2f),
    SPEED("M4 15a8 8 0 1 1 16 0M12 15l4-5", true, 2f),
    CAMERA("M3 8a2 2 0 0 1 2-2h2l2-2h6l2 2h2a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2zM12 16.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7z", true),
    MIC("M9 6a3 3 0 0 1 6 0v5a3 3 0 0 1-6 0zM5.5 11a6.5 6.5 0 0 0 13 0M12 17.5V21", true),
    KEY("M14 10a4 4 0 1 0-3.5 4L8 16.5V19h2.5v-2h2v-2l1.5-1.5A4 4 0 0 0 14 10zM15.5 8.5h.01", true),
    COPY("M9 9h11v11H9zM5 15H4V4h11v1", true),
    PIN("M12 21s-6-5.5-6-11a6 6 0 0 1 12 0c0 5.5-6 11-6 11zM12 12.5a2.5 2.5 0 1 0 0-5 2.5 2.5 0 0 0 0 5z", true),
    FLAG("M5 21V4M5 4h11l-2 4 2 4H5", true),
    FOCUS("M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18zM12 16a4 4 0 1 0 0-8 4 4 0 0 0 0 8z", true),
    TRAY("M4 13h4l2 3h4l2-3h4M4 13l2-8h12l2 8v6H4z", true),
    SEARCH("M11 17.5a6.5 6.5 0 1 0 0-13 6.5 6.5 0 0 0 0 13zM16 16l4.5 4.5", true, 2f),
    TORCH("M8 2h8v4l-2 3v13h-4V9L8 6z", true),
    WIFI("M2 9a15 15 0 0 1 20 0M5 12.5a10 10 0 0 1 14 0M8.5 16a5 5 0 0 1 7 0M12 19.5h.01", true, 2f),
    MOON("M20 14A8 8 0 1 1 10 4a6.5 6.5 0 0 0 10 10z", true),
    SEND("M4 12 20 4l-6 16-2.5-6.5z", true),
    SHARE("M12 3v12M7 8l5-5 5 5M5 13v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6", true, 1.9f),
    CLOSE("M6 6l12 12M18 6 6 18", true, 2f),
    PLUS("M12 5v14M5 12h14", true, 2.2f),
    STOP("M6 6h12v12H6z", false),
    BOX("M3 8l9-5 9 5v8l-9 5-9-5zM3 8l9 5 9-5M12 13v8", true),
    DOT("M12 15a3 3 0 1 0 0-6 3 3 0 0 0 0 6z", false),
    BELL("M6 16V11a6 6 0 0 1 12 0v5l2 2H4zM10 21h4", true),
    APP("M4 4h16v16H4z", true),
    CHECK("M5 12.5 10 17 19 7", true, 2.2f),
    CHEVRON("M9 5l7 7-7 7", true, 2f),
    BACK("M15 5l-7 7 7 7", true, 2f),
    SLIDERS("M4 7h10M18 7h2M4 17h4M12 17h8M14 4v6M8 14v6", true),
    MOVE("M12 3v18M3 12h18M12 3l-3 3M12 3l3 3M12 21l-3-3M12 21l3-3M3 12l3-3M3 12l3 3M21 12l-3-3M21 12l-3 3", true),
    SHIELD("M12 3l8 3v6c0 5-3.5 8-8 9-4.5-1-8-4-8-9V6z", true),
    PLUG("M9 2v5M15 2v5M6 7h12v4a6 6 0 0 1-12 0zM12 17v5", true),
    SPARK("M12 3v4M12 17v4M3 12h4M17 12h4M6 6l2.5 2.5M15.5 15.5 18 18M6 18l2.5-2.5M15.5 8.5 18 6", true);

    val vector: ImageVector by lazy {
        ImageVector.Builder(name = name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
            .addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = if (stroke) null else SolidColor(Color.Black),
                stroke = if (stroke) SolidColor(Color.Black) else null,
                strokeLineWidth = if (stroke) width else 0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ).build()
    }
}
