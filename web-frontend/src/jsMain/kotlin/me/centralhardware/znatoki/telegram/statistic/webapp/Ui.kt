package me.centralhardware.znatoki.telegram.statistic.webapp

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.AttrsScope
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.placeholder
import org.jetbrains.compose.web.attributes.selected
import org.jetbrains.compose.web.dom.B
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Select
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.TextArea
import org.w3c.dom.HTMLElement

// Small building blocks over the mockup's CSS classes.

fun AttrsScope<*>.style(css: String) = attr("style", css)

@Composable fun Box(cls: String, css: String? = null, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) =
    Div({ classes(*cls.split(" ").filter { it.isNotEmpty() }.toTypedArray()); css?.let { style(it) }; onClick?.let { f -> onClick { f() }; attr("role", "button"); attr("tabindex", "0") } }) { content() }

@Composable fun T(text: String, cls: String? = null, css: String? = null) =
    if (cls == null && css == null) Text(text) else Span({ cls?.let { classes(*it.split(" ").toTypedArray()) }; css?.let { style(it) } }) { Text(text) }

@Composable fun Title(text: String) = H2 { Text(text) }
@Composable fun Sec(text: String) = Box("sec") { Text(text) }
@Composable fun Lock(text: String) = Box("lock") { Text(text) }
@Composable fun Empty(text: String) = Box("empty") { Text(text) }
@Composable fun Chip(text: String, kind: String = "") = Span({ classes(*("chip $kind").trim().split(" ").toTypedArray()) }) { Text(text) }
@Composable fun Spinner() = Box("spin") {}

@Composable fun ErrorBox(e: Throwable) = Box("err") {
    Text(e.message ?: "Ошибка"); Text(" ")
    Btn("Повторить", "mini ghost") { App.refresh() }
}

@Composable fun Btn(label: String, cls: String = "secondary", enabled: Boolean = true, css: String? = null, onClick: () -> Unit) =
    Button({ classes(*cls.split(" ").toTypedArray()); css?.let { style(it) }; if (!enabled) disabled(); onClick { onClick() } }) { Text(label) }

@Composable fun Actions(content: @Composable () -> Unit) = Box("actions") { content() }

/** Segmented control, like the mockup's .seg. */
@Composable fun <V> Seg(options: List<Pair<V, String>>, value: V, css: String? = null, onPick: (V) -> Unit) =
    Box("seg", css) { options.forEach { (v, t) -> Button({ attr("aria-pressed", (v == value).toString()); onClick { onPick(v) } }) { Text(t) } } }

@Composable fun Opt(label: String, pressed: Boolean, enabled: Boolean = true, css: String? = null, onClick: () -> Unit) =
    Button({ classes("opt"); attr("aria-pressed", pressed.toString()); css?.let { style(it) }; if (!enabled) disabled(); onClick { onClick() } }) { Text(label) }

@Composable fun FieldLabel(text: String) = Label(attrs = { style("color:var(--hint);font-size:12px") }) { Text(text) }

@Composable fun TextField(value: String, placeholder: String = "", type: InputType<String> = InputType.Text, cls: String = "field", css: String? = null, readOnly: Boolean = false, onChange: (String) -> Unit) =
    Input(type) {
        classes(*cls.split(" ").toTypedArray()); value(value); placeholder(placeholder); attr("autocomplete", "off")
        css?.let { style(it) }; if (readOnly) attr("readonly", "")
        onInput { onChange(it.value) }
    }

@Composable fun NumberField(value: Int?, placeholder: String = "", onChange: (Int?) -> Unit) =
    Input(InputType.Number) {
        classes("field", "num"); value(value?.toString() ?: ""); placeholder(placeholder); attr("inputmode", "numeric"); attr("min", "1")
        onInput { onChange(it.value?.toInt()) }
    }

@Composable fun Area(value: String, placeholder: String, onChange: (String) -> Unit) =
    TextArea(value) { classes("field"); placeholder(placeholder); onInput { onChange(it.value) } }

@Composable fun <V> Dropdown(options: List<Pair<V, String>>, value: V?, enabled: Boolean = true, css: String? = null, cls: String = "field", onPick: (V) -> Unit) =
    Select({
        classes(*cls.split(" ").toTypedArray()); css?.let { style(it) }; if (!enabled) disabled()
        onChange { e -> e.value?.toIntOrNull()?.let { onPick(options[it].first) } }
    }) { options.forEachIndexed { i, (v, t) -> Option(i.toString(), { if (v == value) selected() }) { Text(t) } } }

@Composable fun Toggle(title: String, meta: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) =
    Label(attrs = { classes("row", "tg", "chk"); if (!enabled) classes("off") }) {
        Box("main") { Box("name") { Text(title) }; meta?.let { Box("meta") { Text(it) } } }
        Input(InputType.Checkbox) { checked(checked); if (!enabled) disabled(); onChange { onChange(it.value) } }
    }

@Composable fun SummaryRow(label: String, value: String, num: Boolean = false) =
    Box("row") { Box("main") { Text(label) }; Div({ if (num) classes("num") }) { Text(value) } }

@Composable fun Avatar(id: Int, fio: String) {
    val p = fio.split(" ")
    Box("av", "background:${PALETTE[id % PALETTE.size]}") { Text((p.getOrNull(0)?.take(1) ?: "?") + (p.getOrNull(1)?.take(1) ?: "")) }
}

@Composable fun Thumb(src: String, caption: String) =
    Img(src, caption) { classes("thumb"); attr("loading", "lazy"); onClick { viewImage = src to caption } }

@Composable fun StatCard(value: String, label: String, numCls: Boolean = false) =
    Box("stat") { B({ if (numCls) classes("num") }) { Text(value) }; Span { Text(label) } }

@Composable fun Steps(total: Int, step: Int) = Box("steps") { repeat(total) { i -> org.jetbrains.compose.web.dom.I({ if (i <= step) classes("on") }) {} } }

/** Renders [r]: spinner while loading, an error box with retry, or [content]. */
@Composable fun <T> Show(r: Remote<T>, content: @Composable (T) -> Unit) = when (r) {
    is Remote.Loading -> Spinner()
    is Remote.Fail -> ErrorBox(r.error)
    is Remote.Ok -> content(r.data)
}

fun focus(el: HTMLElement) { el.focus() }
