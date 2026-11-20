package app.liveisland.engine

import app.liveisland.core.Fact
import app.liveisland.core.FactValue
import app.liveisland.core.IslandItem
import app.liveisland.core.Kind
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** What the island is doing right now, independent of which activities exist. */
enum class Mode { AUTO, EXPANDED, COMMAND, SHELF, SHRUNK }

data class IslandUi(
    val mode: Mode = Mode.AUTO,
    /** Activity the user swiped to. Null = highest priority wins. */
    val focusedId: String? = null,
)

/**
 * The in-memory brain. Sources post items, the overlay renders [items] + [ui].
 * Process-wide singleton because the accessibility service, notification listener
 * and the app UI all live in one process.
 */
object IslandEngine {
    private val _items = MutableStateFlow<Map<String, IslandItem>>(emptyMap())
    val items: StateFlow<Map<String, IslandItem>> = _items.asStateFlow()

    private val _ui = MutableStateFlow(IslandUi())
    val ui: StateFlow<IslandUi> = _ui.asStateFlow()

    private val _facts = MutableStateFlow<Map<Fact, FactValue>>(emptyMap())
    val facts: StateFlow<Map<Fact, FactValue>> = _facts.asStateFlow()

    /** Ticks once a second while the overlay is alive; drives countdown text. */
    val now = MutableStateFlow(System.currentTimeMillis())

    /** Set by the overlay so engine-level events can buzz. */
    var haptic: ((strong: Boolean) -> Unit)? = null

    fun post(item: IslandItem) = _items.update { it + (item.id to item) }

    fun remove(id: String) {
        _items.update { it - id }
        _ui.update { if (it.focusedId == id) it.copy(focusedId = null, mode = Mode.AUTO) else it }
    }

    fun removeWhere(pred: (IslandItem) -> Boolean) = _items.update { m -> m.filterValues { !pred(it) } }

    fun fact(f: Fact, v: FactValue?) = _facts.update { if (v == null) it - f else it + (f to v) }

    fun setMode(mode: Mode) = _ui.update { it.copy(mode = mode) }

    fun focus(id: String?) = _ui.update { it.copy(focusedId = id) }

    /** Ordered list, highest priority first. */
    fun ordered(map: Map<String, IslandItem> = _items.value): List<IslandItem> =
        map.values.sortedWith(compareByDescending<IslandItem> { it.priority }.thenByDescending { it.positionAt })

    fun primary(map: Map<String, IslandItem>, ui: IslandUi): IslandItem? {
        val list = ordered(map)
        val urgent = list.firstOrNull { it.kind == Kind.CALL || it.kind == Kind.OTP || it.kind == Kind.TIMER_DONE }
        return urgent ?: ui.focusedId?.let { map[it] } ?: list.firstOrNull()
    }

    /** Swipe through running activities. */
    fun cycle(dir: Int) {
        val list = ordered().filter { it.kind != Kind.SHELF }
        if (list.size < 2) return
        val cur = primary(_items.value, _ui.value)
        val i = list.indexOf(cur).coerceAtLeast(0)
        val next = list[(i + dir + list.size) % list.size]
        _ui.update { it.copy(focusedId = next.id, mode = Mode.AUTO) }
    }

    /** Drop anything past its expiry. Called by the overlay's ticker. */
    fun sweep(nowMs: Long) {
        val dead = _items.value.values.filter { (it.expiresAt ?: Long.MAX_VALUE) <= nowMs }
        if (dead.isNotEmpty()) dead.forEach { remove(it.id) }
    }
}
