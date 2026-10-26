package br.com.vertice81.sinalbt.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** Identidade RootScope: off-white, ciano, azul profundo e grafite. Sem neon ou gradiente. */
val Background = Color(0xFFF4F8FB)
val Surface = Color(0xFFFFFFFF)
val Elevated = Color(0xFFEAF4FA)
val Outline = Color(0xFFD8E7EF)
val TextStrong = Color(0xFF112233)
val TextMuted = Color(0xFF506779)
val TextDim = Color(0xFF6D8190)
val Accent = Color(0xFF007EA9)
val AccentInk = Color(0xFFFFFFFF)
val Ok = Color(0xFF087D63)
val Warning = Color(0xFF99621F)
val Error = Color(0xFFB12A45)
val DarkPanel = Color(0xFF0B1A27)
val DarkPanelElevated = Color(0xFF142838)
val BrightCyan = Color(0xFF45D7F5)

private val Palette = lightColorScheme(
    primary = Accent,
    onPrimary = AccentInk,
    background = Background,
    onBackground = TextStrong,
    surface = Surface,
    onSurface = TextStrong,
    surfaceVariant = Elevated,
    onSurfaceVariant = TextMuted,
    outline = Outline,
    secondary = Ok,
    error = Error
)

@Composable
fun SinalTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Palette, content = content)
}

/** Monograma geométrico RS/ligação, desenhado em vetor e independente de fontes ou imagens. */
@Composable
fun SinalMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Surface, RoundedCornerShape(13.dp))
            .border(1.dp, Outline, RoundedCornerShape(13.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(34.dp)) {
            val w = size.width
            val h = size.height
            val r = Path().apply {
                moveTo(w * .13f, h * .85f)
                lineTo(w * .13f, h * .18f)
                lineTo(w * .56f, h * .18f)
                quadraticBezierTo(w * .75f, h * .18f, w * .75f, h * .36f)
                quadraticBezierTo(w * .75f, h * .52f, w * .57f, h * .52f)
                lineTo(w * .34f, h * .52f)
                lineTo(w * .69f, h * .84f)
            }
            val s = Path().apply {
                moveTo(w * .9f, h * .18f)
                lineTo(w * .8f, h * .18f)
                moveTo(w * .89f, h * .65f)
                quadraticBezierTo(w * .89f, h * .84f, w * .73f, h * .84f)
                lineTo(w * .54f, h * .84f)
            }
            drawPath(r, color = TextStrong, style = Stroke(width = 5.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(r, color = Accent, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(s, color = Accent, style = Stroke(width = 3.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
