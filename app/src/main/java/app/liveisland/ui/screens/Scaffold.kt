package app.liveisland.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.liveisland.island.Glyph
import app.liveisland.ui.Demo
import app.liveisland.ui.design.Chip
import app.liveisland.ui.design.Icon
import app.liveisland.ui.design.P
import app.liveisland.ui.design.Rule
import app.liveisland.ui.design.Txt

/**
 * Every screen: back + title, an optional preview pinned to the top (so you always see
 * the result of what you're changing), then the settings scrolling underneath.
 */
@Composable
fun Page(
    title: String,
    onBack: (() -> Unit)?,
    preview: (@Composable () -> Unit)? = null,
    content: LazyListScope.() -> Unit,
) {
    val c = P.c
    Column(Modifier.fillMaxSize().background(c.bg).windowInsetsPadding(WindowInsets.safeDrawing)) {
        Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp, top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).clickable(remember { MutableInteractionSource() }, null) { onBack() },
                contentAlignment = Alignment.Center,
            ) { Icon(Glyph.BACK, 20.dp) } else Spacer(Modifier.size(8.dp))
            Txt(title, P.t.title, modifier = Modifier.padding(start = 4.dp))
        }
        if (preview != null) {
            Box(Modifier.padding(horizontal = 20.dp).padding(top = 8.dp)) { preview() }
            Rule()
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), content = {
            content()
            item { Spacer(Modifier.height(40.dp)) }
        })
    }
}

/** Row of state chips under a preview: "show me what Music looks like". */
@Composable
fun DemoPicker(options: List<Demo>, selected: Demo, onSelect: (Demo) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) { options.forEach { d -> Chip(d.label, d == selected, { onSelect(d) }) } }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = P.c
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surface).border(1.dp, c.line, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}
