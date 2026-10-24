@file:OptIn(ExperimentalWasmJsInterop::class)

package me.centralhardware.znatoki.telegram.statistic.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlin.js.ExperimentalWasmJsInterop

/** index.html shows a spinner while the bundle loads; Compose draws over the page from here. */
private fun hideBoot(): Unit = js("{ var b = document.getElementById('boot'); if (b) b.remove(); }")

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    hideBoot()
    ComposeViewport { ClassLogApp() }
}
