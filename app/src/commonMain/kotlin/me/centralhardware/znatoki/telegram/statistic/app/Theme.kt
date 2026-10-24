package me.centralhardware.znatoki.telegram.statistic.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import me.centralhardware.znatoki.telegram.statistic.app.res.Res
import me.centralhardware.znatoki.telegram.statistic.app.res.manrope
import me.centralhardware.znatoki.telegram.statistic.app.res.plex_mono_medium
import me.centralhardware.znatoki.telegram.statistic.app.res.plex_mono_regular
import org.jetbrains.compose.resources.Font

/** The mockup's palette (formerly app.css custom properties). */
@Immutable
data class Palette(
    val ground: Color, val bg: Color, val bg2: Color, val fg: Color, val hint: Color, val line: Color,
    val accent: Color, val accentFg: Color, val ok: Color, val warn: Color, val bad: Color,
    val okBg: Color, val warnBg: Color, val badBg: Color, val accentBg: Color,
)

val LightPalette = Palette(
    ground = Color(0xFFE9EDF1), bg = Color(0xFFFFFFFF), bg2 = Color(0xFFF1F4F7), fg = Color(0xFF15202B), hint = Color(0xFF6B7A89), line = Color(0xFFDFE5EB),
    accent = Color(0xFF2A8BD8), accentFg = Color(0xFFFFFFFF), ok = Color(0xFF2F9E62), warn = Color(0xFFC98A12), bad = Color(0xFFD64545),
    okBg = Color(0xFFE5F5EC), warnBg = Color(0xFFFBF1DC), badBg = Color(0xFFFBE7E7), accentBg = Color(0xFFE3F0FB),
)

val DarkPalette = Palette(
    ground = Color(0xFF0B1117), bg = Color(0xFF17212B), bg2 = Color(0xFF0F1820), fg = Color(0xFFE8EEF4), hint = Color(0xFF8696A6), line = Color(0xFF26323E),
    accent = Color(0xFF5EAEE8), accentFg = Color(0xFF0B1117), ok = Color(0xFF5CC58A), warn = Color(0xFFE2AD4A), bad = Color(0xFFEF7A7A),
    okBg = Color(0xFF16301F), warnBg = Color(0xFF33280F), badBg = Color(0xFF3A1B1B), accentBg = Color(0xFF162C40),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }
val LocalMono = staticCompositionLocalOf<FontFamily> { FontFamily.Monospace }

val colors: Palette @Composable get() = LocalPalette.current

/** Tabular figures for sums and times. */
val mono: TextStyle @Composable get() = TextStyle(fontFamily = LocalMono.current, fontWeight = FontWeight.Medium)

@Composable
fun ClassLogTheme(content: @Composable () -> Unit) {
    // Inside Telegram the client's theme wins; elsewhere the system's.
    val dark = tg?.dark ?: isSystemInDarkTheme()
    val p = if (dark) DarkPalette else LightPalette
    // Manrope is a variable font: each weight is the same file at its own `wght`.
    val body = FontFamily(listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold).map {
        Font(Res.font.manrope, it, variationSettings = FontVariation.Settings(FontVariation.weight(it.weight)))
    })
    val monoFamily = FontFamily(Font(Res.font.plex_mono_regular, FontWeight.Normal), Font(Res.font.plex_mono_medium, FontWeight.Medium))
    // No colour here: text takes the content colour, so buttons and chips can set their own.
    val base = TextStyle(fontFamily = body, fontSize = 14.sp)
    val scheme = (if (dark) darkColorScheme() else lightColorScheme()).copy(
        primary = p.accent, onPrimary = p.accentFg, primaryContainer = p.accentBg, onPrimaryContainer = p.fg,
        background = p.bg2, onBackground = p.fg, surface = p.bg, onSurface = p.fg, surfaceVariant = p.bg2, onSurfaceVariant = p.hint,
        surfaceContainerLow = p.bg, surfaceContainer = p.bg, surfaceContainerHigh = p.bg, surfaceContainerHighest = p.bg2,
        outline = p.line, outlineVariant = p.line, error = p.bad, secondaryContainer = p.accentBg, onSecondaryContainer = p.accent,
    )
    val typography = Typography(
        bodyLarge = base.copy(fontSize = 15.sp), bodyMedium = base, bodySmall = base.copy(fontSize = 12.sp),
        labelLarge = base.copy(fontWeight = FontWeight.Bold), labelMedium = base.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
        labelSmall = base.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = base.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold), titleMedium = base.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
        titleSmall = base.copy(fontWeight = FontWeight.Bold),
    )
    CompositionLocalProvider(LocalPalette provides p, LocalMono provides monoFamily) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
