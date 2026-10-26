package br.com.vertice81.sinalbt.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

// Cores chapadas de interface utilitária. Sem gradientes, brilho neon ou efeitos 3D.
val Background = Color(0xFF10151B)
val Surface = Color(0xFF192127)
val Elevated = Color(0xFF243039)
val Outline = Color(0xFF394853)
val TextStrong = Color(0xFFF3F5F2)
val TextMuted = Color(0xFFC0CBCF)
val TextDim = Color(0xFF9CAAB3)
val Accent = Color(0xFFE6BA7B)
val AccentInk = Color(0xFF282015)
val Ok = Color(0xFF8DD5B4)
val Warning = Color(0xFFE8C68F)
val Error = Color(0xFFE5A5A1)

private val Palette = darkColorScheme(
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

@Composable
fun SinalMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Elevated, RoundedCornerShape(12.dp))
            .border(1.dp, Outline, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(30.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 3.dp.toPx()
            drawLine(Accent, Offset(w * 0.08f, h * 0.16f), Offset(w * 0.29f, h * 0.16f), stroke, StrokeCap.Square)
            drawLine(Accent, Offset(w * 0.29f, h * 0.16f), Offset(w * 0.5f, h * 0.82f), stroke, StrokeCap.Square)
            drawLine(Accent, Offset(w * 0.5f, h * 0.82f), Offset(w * 0.71f, h * 0.16f), stroke, StrokeCap.Square)
            drawLine(Accent, Offset(w * 0.71f, h * 0.16f), Offset(w * 0.92f, h * 0.16f), stroke, StrokeCap.Square)
            drawLine(TextStrong, Offset(w * 0.13f, h * 0.89f), Offset(w * 0.33f, h * 0.89f), stroke * 0.65f, StrokeCap.Square)
            drawLine(TextStrong, Offset(w * 0.67f, h * 0.89f), Offset(w * 0.87f, h * 0.89f), stroke * 0.65f, StrokeCap.Square)
        }
    }
}
