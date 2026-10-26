package br.com.vertice81.sinalbt.ui

import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BluetoothAudio
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.vertice81.sinalbt.media.MediaControlManager

@Composable
fun MediaScreen(media: MediaControlManager, openBluetoothSettings: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(Unit) { media.refresh() }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { 
            SinalMediaHeader()
            Spacer(Modifier.height(6.dp))
            Text("Mídia e Bluetooth", color = TextStrong, fontSize = 26.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold)
            Text("Teste a rota de áudio, ajuste o volume e envie comandos aos players deste celular.",
                color = TextMuted, fontSize = 13.sp, lineHeight = 20.sp)
        }
        item { MediaControlPanel(media = media, openBluetoothSettings = openBluetoothSettings) }
        item {
            Text("O SINAL BT não transmite som por GATT. O áudio é reproduzido pela saída de mídia que o Android selecionou. Pareamento e saída de áudio devem ser autorizados no sistema.",
                color = TextMuted, fontSize = 12.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun SinalMediaHeader() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SinalMark(Modifier.size(42.dp))
        Column {
            Text("SINAL BT", color = TextStrong, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Text("CONTROLE DE ÁUDIO LOCAL", color = Accent, fontSize = 10.sp, letterSpacing = 1.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun MediaControlPanel(
    media: MediaControlManager,
    openBluetoothSettings: () -> Unit
) {
    val state = media.status
    val outer = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier.fillMaxWidth().background(DarkPanel, outer)
            .border(1.dp, Color(0xFF254557), outer).padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.BluetoothAudio, contentDescription = null, tint = BrightCyan, modifier = Modifier.size(23.dp))
            Column(Modifier.weight(1f)) {
                Text("Saída de mídia", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    if (state.hasBluetoothOutput) "Bluetooth detectado neste celular" else "Sem saída Bluetooth detectada",
                    color = Color(0xFFC1D9E5), fontSize = 11.sp
                )
            }
            IconButton(onClick = media::refresh) {
                Icon(Icons.Outlined.Refresh, "Atualizar saídas", tint = BrightCyan)
            }
        }

        Box(Modifier.fillMaxWidth().background(DarkPanelElevated, RoundedCornerShape(12.dp)).padding(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                val outputs = state.outputNames
                if (outputs.isEmpty()) {
                    Text("Áudio do aparelho / saída não selecionada", color = Color.White, fontSize = 12.sp)
                } else {
                    outputs.take(3).forEach { name ->
                        Text("• $name", color = Color.White, fontSize = 12.sp, maxLines = 2)
                    }
                }
                Text("Lista de saídas disponíveis, não confirmação do destino ativo.",
                    color = Color(0xFF9BB6C6), fontSize = 10.sp, lineHeight = 15.sp)
            }
        }

        Text("CONTROLE DO PLAYER DO CELULAR", color = BrightCyan,
            fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly) {
            MediaKey(Icons.Outlined.SkipPrevious, "Faixa anterior", onClick = {
                media.mediaCommand(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            })
            MediaKey(Icons.Outlined.PlayArrow, "Reproduzir / pausar", featured = true, onClick = {
                media.mediaCommand(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            })
            MediaKey(Icons.Outlined.SkipNext, "Próxima faixa", onClick = {
                media.mediaCommand(KeyEvent.KEYCODE_MEDIA_NEXT)
            })
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.VolumeUp, null, tint = BrightCyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Volume", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text("${state.volumePercent}%", color = Color.White, fontSize = 12.sp)
        }
        Slider(
            value = state.volumePercent.toFloat(),
            onValueChange = { media.setVolumePercent(it.toInt()) },
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = BrightCyan, activeTrackColor = BrightCyan,
                inactiveTrackColor = Color(0xFF345365)
            )
        )
        Button(
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            onClick = media::playTestTone, enabled = !state.testing,
            colors = ButtonDefaults.buttonColors(containerColor = BrightCyan, contentColor = DarkPanel),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Outlined.MusicNote, null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (state.testing) "Reproduzindo som…" else "Reproduzir som de teste",
                fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Text(state.message, color = Color(0xFFC1D9E5), fontSize = 11.sp, lineHeight = 17.sp)
        state.routeAfterTest?.let {
            Text("Última rota informada: $it", color = BrightCyan, fontSize = 11.sp)
        }
        OutlinedButton(
            onClick = openBluetoothSettings,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF567385))
        ) {
            Icon(Icons.Outlined.BluetoothConnected, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Escolher saída nas configurações", fontSize = 12.sp, lineHeight = 17.sp)
        }
        Text("O som de teste usa a rota de mídia do Android. Os botões anteriores enviam comandos para aplicativos de música do próprio telefone e não controlam diretamente um acessório remoto.",
            color = Color(0xFFA6C3D2), fontSize = 10.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun MediaKey(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String,
    featured: Boolean = false, onClick: () -> Unit) {
    val shape = CircleShape
    Box(modifier = Modifier.size(if (featured) 66.dp else 50.dp)
        .background(if (featured) BrightCyan else DarkPanelElevated, shape)
        .border(1.dp, if (featured) BrightCyan else Color(0xFF3A5E70), shape),
        contentAlignment = Alignment.Center) {
        IconButton(onClick = onClick, modifier = Modifier.size(if (featured) 62.dp else 48.dp)) {
            Icon(icon, contentDescription = label,
                tint = if (featured) DarkPanel else Color.White,
                modifier = Modifier.size(if (featured) 32.dp else 24.dp))
        }
    }
}
