package me.centralhardware.znatoki.telegram.statistic.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.decodeToImageBitmap

// The app's building blocks, named after the mockup's CSS classes they replace.

val PALETTE = listOf(Color(0xFF2A8BD8), Color(0xFF2F9E62), Color(0xFFC45AB3), Color(0xFFD4772C), Color(0xFF5B6FD6), Color(0xFF2A9D9D), Color(0xFFB8913A), Color(0xFFD64545))

@Composable fun Title(text: String, modifier: Modifier = Modifier) =
    Text(text, modifier.padding(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 10.dp), style = MaterialTheme.typography.titleLarge)

/** A section heading in small caps; [trailing] sits at its right edge. */
@Composable fun Sec(text: String, top: Dp = 16.dp, trailing: (@Composable RowScope.() -> Unit)? = null) =
    Row(Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = top, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), Modifier.weight(1f), color = colors.hint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
        trailing?.invoke(this)
    }

@Composable fun Lock(text: String) {
    if (text.isNotEmpty()) Text(text, Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp), color = colors.hint, fontSize = 12.sp, lineHeight = 17.sp)
}

@Composable fun Empty(text: String) =
    Text(text, Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 22.dp), color = colors.hint, textAlign = TextAlign.Center, fontSize = 13.sp)

/** A small status label; [kind] is ok / grp / half / del / wait / src or empty. */
@Composable fun Chip(text: String, kind: String = "") {
    val c = colors
    val (fg, bg) = when (kind) {
        "ok" -> c.ok to c.okBg
        "grp" -> c.accent to c.accentBg
        "half", "wait" -> c.warn to c.warnBg
        "del" -> c.bad to c.badBg
        else -> c.hint to c.bg2
    }
    Text(text, Modifier.clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 6.dp, vertical = 1.dp), color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
}

@Composable fun Spinner() = Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
    CircularProgressIndicator(Modifier.size(26.dp), strokeWidth = 3.dp)
}

@Composable fun ErrorBox(e: Throwable) =
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(12.dp)).background(colors.badBg).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(e.message ?: "Ошибка", Modifier.weight(1f), color = colors.bad)
        Btn("Повторить", "mini ghost") { App.refresh() }
    }

/**
 * A button; [kind] holds the mockup's class tokens: primary / secondary / danger / ghost, plus mini (compact)
 * or iconbtn (a bare text button).
 */
@Composable fun Btn(label: String, kind: String = "secondary", enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val k = kind.split(" ")
    val mini = "mini" in k
    val pad = if (mini) PaddingValues(horizontal = 10.dp, vertical = 4.dp) else PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    val min = Modifier.heightIn(min = if (mini) 30.dp else 42.dp)
    val shape = RoundedCornerShape(if (mini) 8.dp else 12.dp)
    val text = @Composable { Text(label, fontSize = if (mini) 12.sp else 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    when {
        "iconbtn" in k -> TextButton(onClick, modifier.then(Modifier.heightIn(min = 32.dp)), enabled, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) { Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
        "primary" in k -> Button(onClick, modifier.then(min), enabled, shape = shape, contentPadding = pad) { text() }
        "danger" in k || "del" in k -> OutlinedButton(onClick, modifier.then(min), enabled, shape = shape, contentPadding = pad,
            colors = ButtonDefaults.outlinedButtonColors(containerColor = colors.badBg, contentColor = colors.bad), border = null) { text() }
        "ghost" in k -> OutlinedButton(onClick, modifier.then(min), enabled, shape = shape, contentPadding = pad,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent)) { text() }
        else -> Button(onClick, modifier.then(min), enabled, shape = shape, contentPadding = pad,
            colors = ButtonDefaults.buttonColors(containerColor = if (mini) colors.accent else colors.accentBg, contentColor = if (mini) colors.accentFg else colors.accent)) { text() }
    }
}

/** A row of buttons sharing the width equally. */
@Composable fun Actions(top: Dp = 12.dp, content: @Composable () -> Unit) = EqualRow(Modifier.padding(top = top), 8.dp, content)

/** Lays its children out in one row, each the same width. */
@Composable fun EqualRow(modifier: Modifier = Modifier, gap: Dp = 8.dp, content: @Composable () -> Unit) =
    Layout(content, modifier.fillMaxWidth()) { ms, cs ->
        if (ms.isEmpty()) return@Layout layout(cs.maxWidth, 0) {}
        val g = gap.roundToPx()
        val w = ((cs.maxWidth - g * (ms.size - 1)) / ms.size).coerceAtLeast(0)
        val ps = ms.map { it.measure(Constraints(minWidth = w, maxWidth = w)) }
        val h = ps.maxOf { it.height }
        layout(cs.maxWidth, h) { ps.forEachIndexed { i, p -> p.placeRelative(i * (w + g), (h - p.height) / 2) } }
    }

/** Segmented control: equal segments when they fit, a scrolling strip when there are many. */
@Composable fun <V> Seg(options: List<Pair<V, String>>, value: V, modifier: Modifier = Modifier, onPick: (V) -> Unit) {
    val c = colors
    val seg = @Composable { v: V, t: String, m: Modifier ->
        val on = v == value
        Box(m.clip(RoundedCornerShape(9.dp)).background(if (on) c.bg else Color.Transparent).clickable { onPick(v) }.padding(horizontal = 10.dp, vertical = 7.dp), contentAlignment = Alignment.Center) {
            Text(t, color = if (on) c.fg else c.hint, fontSize = 13.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    val box = modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(11.dp)).background(c.line.copy(alpha = .55f)).padding(2.dp)
    if (options.size <= 4) EqualRow(box, 2.dp) { options.forEach { (v, t) -> seg(v, t, Modifier) } }
    else Row(box.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) { options.forEach { (v, t) -> seg(v, t, Modifier) } }
}

/** One choice among options laid out by [Opts]. */
@Composable fun Opt(label: String, pressed: Boolean, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = colors
    Box(
        modifier.alpha(if (enabled) 1f else .4f).clip(RoundedCornerShape(10.dp))
            .background(if (pressed) c.accentBg else c.bg)
            .border(1.5.dp, if (pressed) c.accent else c.line, RoundedCornerShape(10.dp))
            .clickable(enabled) { onClick() }.padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (pressed) c.accent else c.fg, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable fun Opts(top: Dp = 0.dp, content: @Composable FlowRowScope.() -> Unit) =
    FlowRow(Modifier.fillMaxWidth().padding(top = top), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)

/** A white rounded block whose children are separated by hairlines (the mockup's .group / .summary). */
@Composable fun Group(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val line = colors.line
    // Child boundaries from the last layout; cuts[0] holds their count. Not state: re-placing children redraws this node anyway.
    val cuts = remember { IntArray(65) }
    Layout(
        content,
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.bg).drawBehind {
            for (i in 1..cuts[0]) drawLine(line, Offset(0f, cuts[i] - .5f), Offset(size.width, cuts[i] - .5f), 1f)
        },
    ) { ms, cs ->
        val ps = ms.map { it.measure(cs.copy(minHeight = 0)) }
        layout(cs.maxWidth, ps.sumOf { it.height }) {
            var y = 0
            cuts[0] = minOf(ps.size - 1, cuts.size - 1).coerceAtLeast(0)
            ps.forEachIndexed { i, p -> p.placeRelative(0, y); y += p.height; if (i < cuts[0]) cuts[i + 1] = y }
        }
    }
}

/** A padded block with its own background (the mockup's .card). */
@Composable fun Card(modifier: Modifier = Modifier, accent: Color? = null, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val m = modifier.fillMaxWidth().padding(top = 10.dp).clip(RoundedCornerShape(14.dp)).background(colors.bg)
        .let { if (onClick != null) it.clickable { onClick() } else it }
        .let { if (accent != null) it.drawBehind { drawRect(accent, size = size.copy(width = 3.dp.toPx())) } else it }
        .padding(horizontal = 14.dp, vertical = 12.dp)
    Column(m, content = content)
}

/** One line in a [Group]: an optional leading slot, the main column, trailing values. */
@Composable fun ListRow(onClick: (() -> Unit)? = null, dim: Boolean = false, top: Boolean = false, content: @Composable RowScope.() -> Unit) =
    Row(
        Modifier.fillMaxWidth().let { if (onClick != null) it.clickable { onClick() } else it }.alpha(if (dim) .5f else 1f).padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = if (top) Alignment.Top else Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )

@Composable fun RowScope.Main(content: @Composable ColumnScope.() -> Unit) = Column(Modifier.weight(1f), content = content)

@Composable fun Name(text: String) = Text(text, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)

/** A name followed by chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun NameLine(content: @Composable FlowRowScope.() -> Unit) =
    FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), itemVerticalAlignment = Alignment.CenterVertically, content = content)

/** The grey second line: text pieces and chips, wrapping. */
@OptIn(ExperimentalLayoutApi::class)
@Composable fun Meta(top: Dp = 2.dp, content: @Composable FlowRowScope.() -> Unit) =
    FlowRow(Modifier.padding(top = top), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(2.dp), itemVerticalAlignment = Alignment.CenterVertically, content = content)

@Composable fun MetaText(text: String, color: Color = colors.hint) = Text(text, color = color, fontSize = 12.sp, lineHeight = 16.sp)

/** A figure at a row's right edge. */
@Composable fun Num(text: String, color: Color = Color.Unspecified, modifier: Modifier = Modifier) =
    Text(text, modifier, style = mono, color = if (color == Color.Unspecified) colors.fg else color, textAlign = TextAlign.End, maxLines = 1)

/** The time column at a row's left. */
@Composable fun TimeCol(text: String, width: Dp = 44.dp, small: Boolean = false) =
    Text(text, Modifier.width(width), style = mono.copy(fontSize = if (small) 12.sp else 14.sp), color = colors.hint)

@Composable fun Right(content: @Composable ColumnScope.() -> Unit) = Column(horizontalAlignment = Alignment.End, content = content)

@Composable fun SummaryRow(label: String, value: String, num: Boolean = false) = ListRow {
    Text(label, Modifier.weight(1f))
    if (num) Num(value) else Text(value, Modifier.widthIn(max = 220.dp), textAlign = TextAlign.End)
}

/** A label and anything on the right, as the summary rows with chips. */
@Composable fun LabelRow(label: String, right: @Composable () -> Unit) = ListRow { Text(label, Modifier.weight(1f)); right() }

@Composable fun Avatar(id: Long, fio: String) {
    val p = fio.split(" ")
    Box(Modifier.size(36.dp).clip(CircleShape).background(PALETTE[(id % PALETTE.size).toInt()]), contentAlignment = Alignment.Center) {
        Text((p.getOrNull(0)?.take(1) ?: "?") + (p.getOrNull(1)?.take(1) ?: ""), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}
@Composable fun Avatar(id: Int, fio: String) = Avatar(id.toLong(), fio)

/** The statistic tiles across the top of a screen. */
@Composable fun Stats(content: @Composable () -> Unit) = EqualRow(Modifier.padding(top = 4.dp), 8.dp, content)

@Composable fun StatCard(value: String, label: String, num: Boolean = false) =
    Column(Modifier.clip(RoundedCornerShape(14.dp)).background(colors.bg).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(value, style = if (num) mono.copy(fontSize = 20.sp) else TextStyle(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold), maxLines = 1)
        Text(label, color = colors.hint, fontSize = 11.sp, maxLines = 2, lineHeight = 14.sp)
    }

/** A two-column grid of tiles (the mockup's .kpis). */
@Composable fun Grid2(content: @Composable () -> Unit) =
    Layout(content, Modifier.fillMaxWidth().padding(top = 6.dp)) { ms, cs ->
        val g = 8.dp.roundToPx()
        val w = (cs.maxWidth - g) / 2
        val ps = ms.map { it.measure(Constraints(minWidth = w, maxWidth = w)) }
        val rows = ps.chunked(2)
        val hs = rows.map { r -> r.maxOf { it.height } }
        layout(cs.maxWidth, hs.sum() + g * (rows.size - 1).coerceAtLeast(0)) {
            var y = 0
            rows.forEachIndexed { i, r -> r.forEachIndexed { j, p -> p.placeRelative(j * (w + g), y) }; y += hs[i] + g }
        }
    }

@Composable fun Kpi(label: String, value: String, num: Boolean = false, valueColor: Color = Color.Unspecified, extra: (@Composable () -> Unit)? = null) =
    Column(Modifier.clip(RoundedCornerShape(14.dp)).background(colors.bg).padding(12.dp)) {
        Text(label, color = colors.hint, fontSize = 12.sp)
        Text(value, style = if (num) mono.copy(fontSize = 18.sp) else TextStyle(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold), color = if (valueColor == Color.Unspecified) colors.fg else valueColor, maxLines = 1)
        extra?.invoke()
    }

/** Progress through a multi-step form. */
@Composable fun Steps(total: Int, step: Int) = EqualRow(Modifier.padding(bottom = 12.dp), 4.dp) {
    repeat(total) { i -> Box(Modifier.height(4.dp).clip(RoundedCornerShape(2.dp)).background(if (i <= step) colors.accent else colors.line)) }
}

/** A thin bar filled to [fraction]. */
@Composable fun Bar(fraction: Float, color: Color = colors.accent) =
    Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(colors.line)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().background(color))
    }

/** Renders [r]: spinner while loading, an error box with retry, or [content]. */
@Composable fun <T> Show(r: Remote<T>, content: @Composable (T) -> Unit) = when (r) {
    is Remote.Loading -> Spinner()
    is Remote.Fail -> ErrorBox(r.error)
    is Remote.Ok -> content(r.data)
}

// ---------- form fields ----------

@Composable fun FieldLabel(text: String) = Text(text, Modifier.padding(start = 2.dp, top = 4.dp), color = colors.hint, fontSize = 12.sp)

@Composable private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = colors.bg, unfocusedContainerColor = colors.bg, disabledContainerColor = colors.bg2,
    unfocusedBorderColor = colors.line, focusedBorderColor = colors.accent,
)

@Composable fun TextField(
    value: String, placeholder: String = "", keyboard: KeyboardType = KeyboardType.Text, search: Boolean = false, num: Boolean = false,
    readOnly: Boolean = false, isError: Boolean = false, modifier: Modifier = Modifier, onChange: (String) -> Unit,
) = OutlinedTextField(
    value, onChange, modifier.fillMaxWidth(),
    readOnly = readOnly, singleLine = true, isError = isError,
    textStyle = if (num) mono.copy(fontSize = 15.sp) else MaterialTheme.typography.bodyLarge,
    placeholder = { Text(placeholder, color = colors.hint, maxLines = 1) },
    leadingIcon = if (search) ({ Icon(Icons.search, null, Modifier.size(18.dp), tint = colors.hint) }) else null,
    keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    shape = RoundedCornerShape(12.dp), colors = fieldColors(),
)

@Composable fun NumberField(value: Int?, placeholder: String = "", onChange: (Int?) -> Unit) =
    TextField(value?.toString() ?: "", placeholder, KeyboardType.Number, num = true) { s -> onChange(s.filter { it.isDigit() }.take(9).toIntOrNull()) }

@Composable fun Area(value: String, placeholder: String, onChange: (String) -> Unit) = OutlinedTextField(
    value, onChange, Modifier.fillMaxWidth().heightIn(min = 90.dp),
    placeholder = { Text(placeholder, color = colors.hint) }, shape = RoundedCornerShape(12.dp), colors = fieldColors(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun <V> Dropdown(options: List<Pair<V, String>>, value: V?, enabled: Boolean = true, modifier: Modifier = Modifier, onPick: (V) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(open, { if (enabled) open = it }, modifier) {
        OutlinedTextField(
            options.find { it.first == value }?.second ?: "", {}, Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled).fillMaxWidth(),
            readOnly = true, enabled = enabled, singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) },
            shape = RoundedCornerShape(12.dp), colors = fieldColors(),
        )
        ExposedDropdownMenu(open, { open = false }) {
            options.forEach { (v, t) -> DropdownMenuItem({ Text(t) }, { onPick(v); open = false }) }
        }
    }
}

/** An ISO date (`2026-10-24`) chosen with the date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun DateField(value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    PickerField(dayMonth(value) + " " + value.substring(0, 4)) { open = true }
    if (open) {
        val state = rememberDatePickerState(LocalDate.parse(value).toEpochDays() * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton({ state.selectedDateMillis?.let { onChange(LocalDate.fromEpochDays(it / 86_400_000L).toString()) }; open = false }) { Text("Готово") } },
            dismissButton = { TextButton({ open = false }) { Text("Отмена") } },
        ) { DatePicker(state, showModeToggle = false) }
    }
}

/** A time (`16:00`) chosen with the time input. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun TimeField(value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    PickerField(value.ifEmpty { "—" }) { open = true }
    if (open) {
        val parts = value.split(":")
        val state = rememberTimePickerState(parts.getOrNull(0)?.toIntOrNull() ?: 12, parts.getOrNull(1)?.toIntOrNull() ?: 0, is24Hour = true)
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton({ onChange("${state.hour.toString().padStart(2, '0')}:${state.minute.toString().padStart(2, '0')}"); open = false }) { Text("Готово") } },
            dismissButton = { TextButton({ open = false }) { Text("Отмена") } },
            text = { TimeInput(state) },
        )
    }
}

@Composable private fun PickerField(text: String, onClick: () -> Unit) =
    Box(Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(12.dp)).background(colors.bg).border(1.dp, colors.line, RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
        Text(text, style = mono.copy(fontSize = 15.sp))
    }

@Composable fun Toggle(title: String, meta: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) =
    ListRow(onClick = if (enabled) ({ onChange(!checked) }) else null, dim = !enabled) {
        Main { Name(title); meta?.let { MetaText(it) } }
        Switch(checked, { onChange(it) }, enabled = enabled)
    }

/** A Telegram user's name that opens their profile; it doesn't trigger the row it sits in. */
@Composable fun UserLink(id: Long, text: String) =
    Text(text, Modifier.clickable { platform.openUrl("tg://user?id=$id") }, color = colors.accent, fontSize = 12.sp)

// ---------- photos ----------

/** A stored photo loaded through the API, cached for the session. */
@Composable fun rememberPhoto(kind: String, id: String): ImageBitmap? {
    val r = remote(kind, id, "photo") { PhotoCache.get(kind, id) }
    return (r as? Remote.Ok)?.data
}

object PhotoCache {
    private val cache = mutableMapOf<String, ImageBitmap>()
    suspend fun get(kind: String, id: String): ImageBitmap =
        cache.getOrPut("$kind/$id") { Api.photo(kind, id).decodeToImageBitmap() }
}

@Composable fun Thumb(kind: String, id: String, caption: String) {
    val img = rememberPhoto(kind, id)
    Box(Modifier.padding(top = 10.dp).size(96.dp).clip(RoundedCornerShape(10.dp)).background(colors.line).clickable { viewImage = (kind to id) to caption }, contentAlignment = Alignment.Center) {
        if (img != null) Image(img, caption, Modifier.size(96.dp), contentScale = ContentScale.Crop)
        else CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
    }
}

/** Where a picked photo goes: shows it once picked, opens the picker on tap. */
@Composable fun PhotoBox(photo: Photo?, empty: String, onPick: (Photo) -> Unit) {
    val img = remember(photo) { photo?.let { runCatching { it.bytes.decodeToImageBitmap() }.getOrNull() } }
    val c = colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (photo != null) c.okBg else c.bg)
            .border(1.5.dp, if (photo != null) c.ok else c.line, RoundedCornerShape(14.dp))
            .clickable { platform.pickPhoto(onPick) { Toasts.show("Не удалось открыть фото") } }.padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (img != null) Image(img, "фото", Modifier.size(140.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop)
        Text(if (photo != null) "Фото приложено · нажмите, чтобы заменить" else empty, color = if (photo != null) c.ok else c.hint, textAlign = TextAlign.Center)
    }
}

@Composable fun Gap(h: Dp) = Spacer(Modifier.height(h))

@Composable fun Ico(v: ImageVector, size: Dp = 22.dp, tint: Color = Color.Unspecified) = Icon(v, null, Modifier.size(size), tint = if (tint == Color.Unspecified) androidx.compose.material3.LocalContentColor.current else tint)
