package br.com.vertice81.sinalbt.ui

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SettingsBluetooth
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.vertice81.sinalbt.bluetooth.BluetoothController
import br.com.vertice81.sinalbt.bluetooth.EventStore
import br.com.vertice81.sinalbt.model.AudioProfileInfo
import br.com.vertice81.sinalbt.model.BluetoothDiagnostics
import br.com.vertice81.sinalbt.model.DeviceEntry
import br.com.vertice81.sinalbt.model.DeveloperInfo
import br.com.vertice81.sinalbt.model.EventRecord
import br.com.vertice81.sinalbt.model.LinkDetail
import br.com.vertice81.sinalbt.model.LinkState
import br.com.vertice81.sinalbt.model.ScanMode
import br.com.vertice81.sinalbt.model.Source
import br.com.vertice81.sinalbt.BuildConfig
import br.com.vertice81.sinalbt.updates.GithubUpdateManager
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class Tab(val label: String, val icon: ImageVector) {
    SCAN("Buscar", Icons.Outlined.BluetoothSearching),
    SESSIONS("Meus", Icons.Outlined.Devices),
    HISTORY("Registros", Icons.Outlined.History),
    ABOUT("Sobre", Icons.Outlined.Info)
}

@Composable
fun SinalApp(
    controller: BluetoothController,
    updater: GithubUpdateManager,
    events: EventStore,
    exportMessage: String?,
    dismissExportMessage: () -> Unit,
    startScan: (ScanMode) -> Unit,
    requestConnect: (DeviceEntry) -> Unit,
    requestPair: (DeviceEntry) -> Unit,
    requestRefresh: () -> Unit,
    requestClassicServices: (DeviceEntry) -> Unit,
    requestAudioStatus: (DeviceEntry) -> Unit,
    requestRssi: (String) -> Unit,
    openBluetoothSettings: () -> Unit,
    exportHistory: () -> Unit
) {
    var tab by remember { mutableStateOf(Tab.SCAN) }
    var selectedKey by remember { mutableStateOf<String?>(null) }
    var mode by remember { mutableStateOf(ScanMode.BOTH) }
    val current = selectedKey?.let { key -> controller.devices.firstOrNull { it.key == key } }
    LaunchedEffect(selectedKey, current) {
        if (selectedKey != null && current == null) selectedKey = null
    }
    BackHandler(enabled = current != null) { selectedKey = null }

    if (exportMessage != null) {
        AlertDialog(
            onDismissRequest = dismissExportMessage,
            title = { Text("Exportação") },
            text = { Text(exportMessage) },
            confirmButton = { TextButton(onClick = dismissExportMessage) { Text("Entendi") } },
            containerColor = Elevated
        )
    }

    Scaffold(
        containerColor = Background,
        bottomBar = {
            if (current == null) {
                NavigationBar(containerColor = Surface, tonalElevation = 0.dp) {
                    Tab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(22.dp)) },
                            label = { Text(item.label, maxLines = 1, fontSize = 11.sp, overflow = TextOverflow.Ellipsis) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent, selectedTextColor = TextStrong,
                                indicatorColor = Elevated, unselectedIconColor = TextDim,
                                unselectedTextColor = TextDim
                            )
                        )
                    }
                }
            }
        }
    ) { inner ->
        when {
            current != null -> DeviceDetail(
                entry = current,
                link = controller.links[current.address],
                audio = controller.audioProfiles[current.address],
                systemNotice = controller.notice,
                onBack = { selectedKey = null },
                onConnect = { requestConnect(current) },
                onDisconnect = { controller.disconnectAddress(current.address) },
                onPair = { requestPair(current) },
                onClassicServices = { requestClassicServices(current) },
                onAudioStatus = { requestAudioStatus(current) },
                onRssi = { requestRssi(current.address) },
                onSettings = openBluetoothSettings,
                modifier = Modifier.padding(inner)
            )
            tab == Tab.SCAN -> ExplorerScreen(
                devices = controller.devices.filter { it.discovered }
                    .sortedWith(compareByDescending<DeviceEntry> { it.rssi != null }.thenByDescending { it.rssi ?: -999 }),
                scanning = controller.scanning,
                notice = controller.notice,
                mode = mode,
                onModeChange = { mode = it },
                onScan = { startScan(mode) },
                onStop = { controller.stopScan() },
                onClear = { controller.clearScanResults() },
                onSelect = { selectedKey = it.key },
                modifier = Modifier.padding(inner)
            )
            tab == Tab.SESSIONS -> SessionsScreen(
                devices = controller.devices.filter {
                    it.bondState == BluetoothDevice.BOND_BONDED || controller.links[it.address]?.state in
                        listOf(LinkState.CONNECTED, LinkState.DISCOVERING, LinkState.CONNECTING)
                }.distinctBy { it.address },
                links = controller.links,
                onRefresh = requestRefresh,
                onSelect = { selectedKey = it.key },
                onSettings = openBluetoothSettings,
                modifier = Modifier.padding(inner)
            )
            tab == Tab.HISTORY -> HistoryScreen(
                records = events.records,
                onClear = events::clear,
                onExport = exportHistory,
                modifier = Modifier.padding(inner)
            )
            else -> AboutScreen(updater = updater, modifier = Modifier.padding(inner))
        }
    }
}

@Composable
private fun BrandHeader(subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 17.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SinalMark(Modifier.size(48.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("SINAL BT", color = TextStrong, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp)
            Text(subtitle, color = TextMuted, fontSize = 11.sp,
                lineHeight = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Outlined.Security, null, tint = Accent, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Eyebrow(text: String) {
    Text(text.uppercase(), color = TextDim, fontSize = 11.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
}

@Composable
private fun Headline(text: String, detail: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text, fontSize = 24.sp, lineHeight = 29.sp, fontWeight = FontWeight.Bold, color = TextStrong)
        if (detail != null) Text(detail, fontSize = 13.sp, lineHeight = 19.sp, color = TextMuted)
    }
}

@Composable
private fun SectionBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier.fillMaxWidth()
            .background(Surface, RoundedCornerShape(16.dp))
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) { content() }
}

@Composable
private fun Tag(text: String, color: Color = TextMuted, surface: Color = Elevated) {
    Box(
        modifier = Modifier.background(surface, RoundedCornerShape(6.dp))
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun InformationRow(label: String, value: String, mono: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(label, modifier = Modifier.weight(0.9f), fontSize = 12.sp, color = TextMuted)
        Text(value, modifier = Modifier.weight(1.6f), color = TextStrong, fontSize = 12.sp,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            textAlign = TextAlign.End, lineHeight = 18.sp)
    }
}

@Composable
private fun PrimaryAction(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(11.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = AccentInk)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2,
            lineHeight = 17.sp, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SecondaryAction(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 50.dp),
        shape = RoundedCornerShape(11.dp),
        contentPadding = PaddingValues(horizontal = 11.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextStrong)
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(7.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            maxLines = 2, lineHeight = 16.sp, textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Em telas estreitas ou com fonte do sistema ampliada, empilha as ações.
 * Isso evita botões cortados mesmo em Android com fonte > 125%.
 */
@Composable
private fun ResponsiveActionPair(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    val scale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 360.dp || scale > 1.2f) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ExplorerScreen(
    devices: List<DeviceEntry>,
    scanning: Boolean,
    notice: String,
    mode: ScanMode,
    onModeChange: (ScanMode) -> Unit,
    onScan: () -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
    onSelect: (DeviceEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var nameSort by remember { mutableStateOf(false) }
    val visible = devices.filter { entry ->
        query.isBlank() || entry.name.contains(query.trim(), ignoreCase = true) ||
            entry.address.contains(query.trim(), ignoreCase = true) ||
            (entry.modelId?.contains(query.trim(), ignoreCase = true) == true)
    }.let { list ->
        if (nameSort) list.sortedBy { it.name.lowercase(Locale.ROOT) }
        else list.sortedWith(compareByDescending<DeviceEntry> { it.rssi != null }.thenByDescending { it.rssi ?: -999 })
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        item { BrandHeader("Laboratório de segurança Bluetooth") }
        item {
            Headline("Explore o sinal.", "Localize caixas, fones e periféricos. Veja o que cada conexão realmente permite.")
        }
        item {
            SectionBox {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(10.dp).background(if (scanning) Ok else TextDim, CircleShape)
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(if (scanning) "BUSCA ATIVA" else "SCANNER PRONTO", fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, color = if (scanning) Ok else TextMuted,
                        letterSpacing = 0.8.sp)
                    Spacer(Modifier.weight(1f))
                    Tag("${devices.size} encontrados", TextStrong)
                }
                Spacer(Modifier.height(13.dp))
                Text(notice, color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                if (scanning) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Accent,
                        trackColor = Elevated)
                }
                Spacer(Modifier.height(16.dp))
                Eyebrow("Modo de descoberta")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    ScanMode.entries.forEach { candidate ->
                        val chosen = candidate == mode
                        Box(
                            modifier = Modifier.weight(1f)
                                .background(if (chosen) Accent else Elevated, RoundedCornerShape(8.dp))
                                .clickable(enabled = !scanning) { onModeChange(candidate) }
                                .padding(horizontal = 3.dp, vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(candidate.label, fontSize = 12.sp, maxLines = 1,
                                color = if (chosen) AccentInk else TextMuted,
                                fontWeight = if (chosen) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                }
                Spacer(Modifier.height(13.dp))
                if (scanning) {
                    SecondaryAction("Parar busca", Icons.Outlined.Stop, onStop)
                } else {
                    PrimaryAction("Iniciar busca Bluetooth", Icons.Outlined.BluetoothSearching, onScan)
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 11.dp, bottom = 3.dp)) {
                Column(Modifier.weight(1f)) {
                    Eyebrow("Descoberta")
                    Spacer(Modifier.height(4.dp))
                    Text("Dispositivos próximos", fontWeight = FontWeight.SemiBold,
                        color = TextStrong, fontSize = 17.sp)
                }
                if (devices.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Outlined.DeleteOutline, "Limpar busca", tint = TextMuted)
                    }
                }
            }
        }
        item {
            SectionBox {
                Eyebrow("Filtrar resultados")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it.take(80) },
                    placeholder = { Text("Nome, endereço ou Model ID", fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.BluetoothSearching, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent, unfocusedBorderColor = Outline,
                        focusedTextColor = TextStrong, unfocusedTextColor = TextStrong
                    )
                )
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ordenar por nome", color = TextMuted, fontSize = 12.sp,
                        modifier = Modifier.weight(1f))
                    Switch(checked = nameSort, onCheckedChange = { nameSort = it })
                }
                Text("${visible.size} exibidos de ${devices.size} encontrados", color = TextDim, fontSize = 11.sp)
            }
        }
        if (visible.isEmpty()) {
            item {
                SectionBox {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.BluetoothSearching, null, tint = Accent, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(15.dp))
                        Text("Nenhum dispositivo listado", fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold, color = TextStrong)
                        Spacer(Modifier.height(5.dp))
                        Text("Inicie uma busca. Aparelhos que não anunciam sinal podem não aparecer.",
                            fontSize = 12.sp, color = TextMuted, lineHeight = 18.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            items(visible, key = { it.key }) { device -> DeviceTile(device, onClick = { onSelect(device) }) }
        }
        item {
            Text("RSSI não mede distância exata. Um dispositivo conectado a outro celular pode não estar detectável. O app não identifica o dono da conexão.",
                fontSize = 11.sp, lineHeight = 17.sp, color = TextDim,
                modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun DeviceTile(entry: DeviceEntry, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(Surface, RoundedCornerShape(14.dp))
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(43.dp).background(Elevated, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(if (entry.seenClassic && !entry.seenBle) Icons.Outlined.Headphones
                     else Icons.Outlined.Bluetooth,
                    null, tint = Accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.name, color = TextStrong, fontSize = 14.sp,
                    lineHeight = 19.sp, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(entry.address, color = TextMuted, fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Outlined.ChevronRight, "Detalhes", tint = TextDim,
                modifier = Modifier.size(19.dp))
        }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val compact = maxWidth < 320.dp || LocalDensity.current.fontScale > 1.25f
            if (compact) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Tag(entry.transportLabel, TextMuted)
                        if (entry.isFastPair) Tag("Fast Pair", Accent)
                    }
                    Text(if (entry.rssi != null) "RSSI: ${entry.rssi} dBm · ${BluetoothDiagnostics.ageLabel(entry.lastSeen)}" else "RSSI não disponível",
                        color = TextMuted, fontSize = 11.sp)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(entry.transportLabel, TextMuted)
                    if (entry.isFastPair) Tag("Fast Pair", Accent)
                    Spacer(Modifier.weight(1f))
                    Text(if (entry.rssi != null) "${entry.rssi} dBm" else "RSSI —",
                        color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun DeviceDetail(
    entry: DeviceEntry,
    link: LinkDetail?,
    audio: AudioProfileInfo?,
    systemNotice: String,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onPair: () -> Unit,
    onClassicServices: () -> Unit,
    onAudioStatus: () -> Unit,
    onRssi: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val state = link?.state ?: LinkState.IDLE
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Voltar", tint = TextStrong) }
                Text("DISPOSITIVO", color = TextMuted, fontSize = 11.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SinalMark(Modifier.size(56.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.name, fontSize = 21.sp, lineHeight = 26.sp, color = TextStrong, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text(entry.address, fontSize = 11.sp, lineHeight = 16.sp,
                        color = TextMuted, fontFamily = FontFamily.Monospace)
                }
            }
        }
        item {
            SectionBox {
                Eyebrow("Situação da conexão")
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (state == LinkState.CONNECTED) Icons.Outlined.CheckCircle else Icons.Outlined.Link,
                        null, tint = if (state == LinkState.CONNECTED) Ok else Accent,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(state.label, color = TextStrong, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(7.dp))
                Text(link?.message ?: "Sem conexão BLE/GATT nesta sessão", fontSize = 12.sp,
                    color = TextMuted, lineHeight = 18.sp)
                if (systemNotice.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Android: $systemNotice", fontSize = 11.sp, color = TextDim,
                        lineHeight = 16.sp)
                }
                Spacer(Modifier.height(13.dp))
                if (entry.supportsGatt) {
                    if (state == LinkState.CONNECTED || state == LinkState.DISCOVERING) {
                        SecondaryAction("Desconectar GATT", Icons.Outlined.Stop, onDisconnect)
                    } else {
                        PrimaryAction("Conectar por BLE/GATT", Icons.Outlined.BluetoothConnected,
                            onConnect, enabled = state != LinkState.CONNECTING)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("O Android confirmou a sessão GATT apenas quando aparece ‘GATT conectado’. Isso não estabelece áudio Classic.",
                        color = TextDim, fontSize = 11.sp, lineHeight = 17.sp)
                } else {
                    Text("Esta entrada foi descoberta por Bluetooth Classic. Para áudio e perfis A2DP/HFP, use o pareamento e a conexão nas configurações do Android.",
                        color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                }
                Spacer(Modifier.height(13.dp))
                ResponsiveActionPair(
                    first = { m -> SecondaryAction("Parear pelo Android", Icons.Outlined.Link, onPair, m) },
                    second = { m -> SecondaryAction("Configurações", Icons.Outlined.SettingsBluetooth, onSettings, m) }
                )
            }
        }
        item {
            SectionBox {
                Eyebrow("Telemetria")
                InformationRow("Transporte detectado", entry.transportLabel)
                ThinRule()
                InformationRow("RSSI no anúncio", entry.rssi?.let { "$it dBm" } ?: "Não disponível")
                ThinRule()
                InformationRow("Última detecção", if (entry.discovered) BluetoothDiagnostics.ageLabel(entry.lastSeen) else "Pareamento salvo")
                ThinRule()
                InformationRow("Sinal (indicativo)", entry.distanceDescription)
                ThinRule()
                InformationRow("TX Power (anunciado)", entry.txPower?.let { "$it dBm" } ?: "Não informado")
                ThinRule()
                InformationRow("Conectável via anúncio BLE", when (entry.connectable) {
                    true -> "Anunciado como conectável"
                    false -> "Não anunciado como conectável"
                    null -> "Não informado"
                })
                ThinRule()
                InformationRow("Pareamento Android", when (entry.bondState) {
                    BluetoothDevice.BOND_BONDED -> "Pareado"
                    BluetoothDevice.BOND_BONDING -> "Em andamento"
                    else -> "Não pareado / desconhecido"
                })
                ThinRule()
                InformationRow("Fast Pair (anúncio)", if (entry.isFastPair) "Detectado" else "Não identificado")
                entry.modelId?.let { ThinRule(); InformationRow("Fast Pair Model ID", it, true) }
                if (entry.manufacturerIds.isNotEmpty()) {
                    ThinRule()
                    InformationRow("Fabricantes (IDs BLE)", entry.manufacturerIds.joinToString(", ") {
                        "0x${it.toString(16).uppercase().padStart(4, '0')}" +
                            (BluetoothDiagnostics.companyName(it)?.let { company -> " · $company" } ?: "")
                    })
                    Spacer(Modifier.height(5.dp))
                    Text("O ID do fabricante vem do anúncio e não comprova a marca do aparelho.",
                        fontSize = 11.sp, color = TextDim)
                }
            }
        }
        item {
            SectionBox {
                Eyebrow("Áudio neste celular")
                Spacer(Modifier.height(8.dp))
                Text("O Android só informa perfis de áudio conectados a ESTE celular, não ao telefone de outra pessoa.",
                    fontSize = 12.sp, lineHeight = 18.sp, color = TextMuted)
                Spacer(Modifier.height(8.dp))
                InformationRow("A2DP · música", when (audio?.a2dpConnected) {
                    true -> "Conectado aqui"
                    false -> "Não conectado aqui"
                    null -> "Não consultado"
                })
                ThinRule()
                InformationRow("HFP · chamadas", when (audio?.headsetConnected) {
                    true -> "Conectado aqui"
                    false -> "Não conectado aqui"
                    null -> "Não consultado"
                })
                audio?.let {
                    Spacer(Modifier.height(6.dp))
                    Text(it.message, color = TextMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(12.dp))
                SecondaryAction("Verificar conexões de áudio", Icons.Outlined.Headphones, onAudioStatus)
                Spacer(Modifier.height(7.dp))
                Text("Para conectar áudio Classic, use o botão Configurações acima.",
                    color = TextDim, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        if (entry.seenClassic || entry.bondState == BluetoothDevice.BOND_BONDED) {
            item {
                SectionBox {
                    Eyebrow("Serviços Bluetooth Classic")
                    Spacer(Modifier.height(8.dp))
                    Text("Consulta SDP usando a API oficial do Android. Alguns aparelhos não respondem.",
                        fontSize = 12.sp, lineHeight = 18.sp, color = TextMuted)
                    Spacer(Modifier.height(10.dp))
                    SecondaryAction("Consultar serviços Classic", Icons.Outlined.Router, onClassicServices)
                    if (entry.classicServiceUuids.isNotEmpty()) {
                        Spacer(Modifier.height(9.dp))
                        entry.classicServiceUuids.take(20).forEachIndexed { index, uuid ->
                            if (index > 0) ThinRule()
                            InformationRow("Perfil ${index + 1}", readableUuid(uuid), mono = true)
                        }
                    }
                }
            }
        }
        item {
            SectionBox {
                Eyebrow("Serviços BLE/GATT")
                Spacer(Modifier.height(8.dp))
                Text("Serviços anunciados: ${entry.serviceUuids.size} · Serviços encontrados por conexão: ${link?.services?.size ?: 0}",
                    color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(12.dp))
                val services = link?.services?.takeIf { it.isNotEmpty() } ?: entry.serviceUuids
                if (services.isEmpty()) {
                    Text("Nenhum UUID disponível. Conecte por GATT para tentar listar os serviços.",
                        color = TextDim, fontSize = 12.sp, lineHeight = 18.sp)
                } else {
                    services.forEachIndexed { index, uuid ->
                        if (index > 0) ThinRule()
                        InformationRow("Serviço ${index + 1}", readableUuid(uuid), true)
                    }
                }
                if (link?.state == LinkState.CONNECTED) {
                    ThinRule()
                    InformationRow("Características GATT", "${link.characteristics}")
                    ThinRule()
                    InformationRow("RSSI da sessão GATT", link.remoteRssi?.let { "$it dBm" } ?: "Não solicitado")
                    ThinRule()
                    InformationRow("Bateria (serviço 0x180F)", link.batteryPercent?.let { "$it%" } ?: "Indisponível")
                    Spacer(Modifier.height(10.dp))
                    SecondaryAction("Atualizar RSSI do link", Icons.Outlined.SignalCellularAlt, onRssi)
                    Text("A leitura de bateria depende do serviço BLE padronizado 0x180F e da permissão do dispositivo.",
                        fontSize = 11.sp, color = TextDim, lineHeight = 17.sp)
                }
            }
        }
        item { FastPairDiagnostics(entry, link) }
        item {
            SectionBox {
                Eyebrow("Teste controlado")
                Spacer(Modifier.height(9.dp))
                Text("1. Conecte a caixa/fone ao seu primeiro celular.\n2. Busque o aparelho aqui no segundo celular.\n3. Tente conectar por BLE ou parear pelo sistema.\n4. Registre o comportamento no histórico.",
                    color = TextMuted, fontSize = 12.sp, lineHeight = 21.sp)
                Spacer(Modifier.height(10.dp))
                Text("Conexão GATT, multiponto e pareamento Fast Pair são mecanismos diferentes. O sucesso de um não confirma vulnerabilidade.",
                    color = Warning, fontSize = 11.sp, lineHeight = 17.sp)
                Spacer(Modifier.height(12.dp))
                SecondaryAction("Compartilhar resumo", Icons.Outlined.Description, onClick = {
                    val report = buildString {
                        appendLine("SINAL BT — RESUMO DO DISPOSITIVO")
                        appendLine("Nome: ${entry.name}")
                        appendLine("Endereço: ${entry.address}")
                        appendLine("Transporte: ${entry.transportLabel}")
                        appendLine("RSSI: ${entry.rssi ?: "não disponível"}")
                        appendLine("Fast Pair no anúncio: ${entry.isFastPair}")
                        appendLine("Estado GATT: ${link?.state?.label ?: "Não testado"}")
                        appendLine("Serviços GATT: ${link?.services?.size ?: 0}")
                        appendLine("CVE-2025-36911: não confirmada por este diagnóstico")
                    }
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, report)
                    }
                    ctx.startActivity(Intent.createChooser(share, "Compartilhar análise"))
                })
            }
        }
    }
}

@Composable
private fun FastPairDiagnostics(entry: DeviceEntry, link: LinkDetail?) {
    SectionBox {
        Eyebrow("Diagnóstico de segurança")
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Security, null, tint = Accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("CVE-2025-36911", fontSize = 17.sp, color = TextStrong, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(9.dp))
        Text("WhisperPair — falha de autenticação em implementações de Key-Based Pairing do Google Fast Pair.",
            fontSize = 12.sp, color = TextMuted, lineHeight = 19.sp)
        Spacer(Modifier.height(15.dp))
        ThinRule()
        InformationRow("Anúncio Fast Pair", if (entry.isFastPair) "Encontrado" else "Não observado")
        ThinRule()
        InformationRow("Serviço GATT 0xFE2C", when {
            link?.state != LinkState.CONNECTED -> "Não verificado via GATT"
            link.hasFastPairService -> "Encontrado"
            else -> "Não encontrado"
        })
        ThinRule()
        InformationRow("Chave de pareamento (UUID)", when {
            link?.state != LinkState.CONNECTED -> "Não inspecionado"
            link.hasKeyBasedPairingCharacteristic -> "Característica presente"
            else -> "Não localizada"
        })
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().background(Elevated, RoundedCornerShape(9.dp)).padding(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Info, null, tint = Warning, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(9.dp))
                Column {
                    Text("Vulnerabilidade: NÃO DETERMINADA", color = Warning,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(5.dp))
                    Text("Este app não envia requisições de exploração nem tenta contornar a autenticação. Detecção de UUID ou conexão GATT não prova a CVE nem que o firmware esteja corrigido.",
                        fontSize = 11.sp, color = TextMuted, lineHeight = 17.sp)
                }
            }
        }
    }
}

private fun readableUuid(uuid: String): String {
    val lower = uuid.lowercase(Locale.ROOT)
    return when {
        lower.startsWith("0000fe2c-") -> "Fast Pair · 0xFE2C"
        lower.startsWith("0000180a-") -> "Device Information · 0x180A"
        lower.startsWith("00001800-") -> "Generic Access · 0x1800"
        lower.startsWith("00001801-") -> "Generic Attribute · 0x1801"
        lower.startsWith("0000180f-") -> "Battery Service · 0x180F"
        lower.startsWith("0000110b-") -> "A2DP Audio Sink · 0x110B"
        lower.startsWith("0000110a-") -> "A2DP Audio Source · 0x110A"
        lower.startsWith("0000110e-") -> "AVRCP Controller · 0x110E"
        lower.startsWith("0000110c-") -> "AVRCP Target · 0x110C"
        lower.startsWith("0000111e-") -> "Handsfree · 0x111E"
        lower.startsWith("00001108-") -> "Headset · 0x1108"
        lower.startsWith("00001101-") -> "Serial Port · 0x1101"
        else -> uuid
    }
}

@Composable
private fun ThinRule() { HorizontalDivider(color = Outline, thickness = 1.dp) }

@Composable
private fun SessionsScreen(
    devices: List<DeviceEntry>,
    links: Map<String, LinkDetail>,
    onRefresh: () -> Unit,
    onSelect: (DeviceEntry) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BrandHeader("Pareamento e sessões") }
        item { Headline("Seus dispositivos.", "Pareamentos conhecidos pelo Android e conexões BLE iniciadas neste aplicativo.") }
        item {
            SectionBox {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null, tint = Accent)
                    Spacer(Modifier.width(10.dp))
                    Text("Pareamentos autorizados", fontWeight = FontWeight.SemiBold, color = TextStrong)
                }
                Spacer(Modifier.height(11.dp))
                Text("Uma caixa pode estar emparelhada sem estar reproduzindo áudio. Para ativar A2DP/HFP, use o controle de Bluetooth do Android.",
                    fontSize = 12.sp, lineHeight = 18.sp, color = TextMuted)
                Spacer(Modifier.height(13.dp))
                ResponsiveActionPair(
                    first = { m -> SecondaryAction("Atualizar", Icons.Outlined.Refresh, onRefresh, m) },
                    second = { m -> SecondaryAction("Abrir Bluetooth", Icons.Outlined.SettingsBluetooth, onSettings, m) }
                )
            }
        }
        item { Spacer(Modifier.height(6.dp)); Eyebrow("Dispositivos vinculados · ${devices.size}") }
        if (devices.isEmpty()) {
            item {
                SectionBox {
                    Text("Nenhum dispositivo pareado ou sessão ativa encontrada.",
                        color = TextMuted, fontSize = 13.sp)
                }
            }
        }
        items(devices, key = { it.key }) { entry ->
            Column {
                DeviceTile(entry) { onSelect(entry) }
                if (links[entry.address]?.state == LinkState.CONNECTED) {
                    Text("Conexão BLE ativa", color = Ok, fontSize = 11.sp,
                        modifier = Modifier.padding(start = 10.dp, top = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    records: List<EventRecord>,
    onClear: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }
    if (showClearDialog) AlertDialog(
        onDismissRequest = { showClearDialog = false },
        title = { Text("Apagar histórico?") },
        text = { Text("Os registros locais serão excluídos deste aparelho.") },
        confirmButton = { TextButton(onClick = { onClear(); showClearDialog = false }) { Text("Apagar", color = Error) } },
        dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("Cancelar") } },
        containerColor = Elevated
    )

    LazyColumn(modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { BrandHeader("Relatórios locais") }
        item { Headline("Histórico técnico.", "Registros de conexões, pareamentos e serviços descobertos.") }
        item {
            SectionBox {
                Text("${records.size} eventos armazenados", color = TextStrong, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Os dados permanecem no aparelho até que você os exporte ou apague. Endereços Bluetooth podem aparecer no relatório.",
                    color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(14.dp))
                PrimaryAction("Exportar JSON", Icons.Outlined.SaveAlt, onExport)
                Spacer(Modifier.height(8.dp))
                SecondaryAction("Limpar histórico", Icons.Outlined.DeleteOutline,
                    { showClearDialog = true }, enabled = records.isNotEmpty())
            }
        }
        item { Spacer(Modifier.height(5.dp)); Eyebrow("Linha do tempo") }
        if (records.isEmpty()) {
            item { SectionBox { Text("Sem eventos ainda. Comece um teste de conexão na aba Explorar.", color = TextMuted, fontSize = 13.sp) } }
        }
        items(records) { record ->
            SectionBox {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Tag(record.type, Accent)
                    Text(formatTime(record.time), color = TextDim, fontSize = 11.sp)
                }
                Spacer(Modifier.height(11.dp))
                Text(record.device, fontWeight = FontWeight.SemiBold, color = TextStrong,
                    fontSize = 14.sp, maxLines = 2)
                Text(record.address, fontFamily = FontFamily.Monospace, color = TextDim, fontSize = 11.sp)
                Spacer(Modifier.height(7.dp))
                Text(record.message, color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

private fun formatTime(epoch: Long): String = SimpleDateFormat("dd/MM HH:mm", Locale("pt", "BR")).format(Date(epoch))

@Composable
private fun AboutScreen(updater: GithubUpdateManager, modifier: Modifier = Modifier) {
    val updateState = updater.state
    // Enquanto esta tela está aberta, acompanha downloads reais do DownloadManager.
    LaunchedEffect(updateState.phase) {
        if (updateState.phase == GithubUpdateManager.Phase.DOWNLOADING) {
            while (true) {
                delay(1800)
                updater.refreshDownload()
                if (updater.state.phase != GithubUpdateManager.Phase.DOWNLOADING) break
            }
        }
    }
    val ctx = LocalContext.current
    LazyColumn(modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BrandHeader("Documentação e limites") }
        item { Headline("Análise sem atalhos.", "Uma ferramenta pessoal para entender o comportamento de dispositivos Bluetooth.") }
        item {
            SectionBox {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SinalMark(Modifier.size(52.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("SINAL BT", color = TextStrong, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text("Versão ${BuildConfig.VERSION_NAME} · Android 8+", color = TextMuted, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Identidade visual do SINAL BT e pesquisa RootScope: grafite, detalhes âmbar, tipografia clara e interface sem gradientes.",
                    color = TextMuted, fontSize = 12.sp, lineHeight = 19.sp)
            }
        }
        item {
            SectionBox {
                Eyebrow("Desenvolvimento e pesquisa")
                Spacer(Modifier.height(12.dp))
                Text(DeveloperInfo.LAB, color = TextStrong, fontWeight = FontWeight.Bold,
                    fontSize = 18.sp)
                Spacer(Modifier.height(4.dp))
                Text("Desenvolvido por ${DeveloperInfo.AUTHOR}", color = TextMuted,
                    fontSize = 13.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(5.dp))
                Text("Estudante e pesquisador de Cibersegurança. Ferramenta para estudos e diagnósticos autorizados.",
                    color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(13.dp))
                Text("Instagram oficial: ${DeveloperInfo.INSTAGRAM_HANDLE}",
                    color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                ResponsiveActionPair(
                    first = { m -> SecondaryAction("Abrir Instagram", Icons.Outlined.OpenInNew, {
                        openUrl(ctx, DeveloperInfo.INSTAGRAM_URL)
                    }, m) },
                    second = { m -> SecondaryAction("Copiar @rootscope.br", Icons.Outlined.Description, {
                        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("RootScope Instagram", DeveloperInfo.INSTAGRAM_HANDLE))
                        Toast.makeText(ctx, "Instagram copiado", Toast.LENGTH_SHORT).show()
                    }, m) }
                )
                Spacer(Modifier.height(9.dp))
                Text("O perfil é aberto no Instagram ou navegador do dispositivo. O SINAL BT não envia dados de varredura para esse perfil.",
                    color = TextDim, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        item {
            SectionBox {
                Eyebrow("Atualizações oficiais pelo GitHub")
                Spacer(Modifier.height(10.dp))
                Text("Versão instalada: ${BuildConfig.VERSION_NAME}", color = TextStrong,
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(7.dp))
                Text("O SINAL BT consulta versões oficiais publicadas no GitHub e pode baixar a atualização sem exigir que você procure um arquivo APK manualmente. O Android confirmará a instalação.",
                    color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(10.dp))
                Text(updateState.message.ifBlank { "Clique para verificar se existe uma versão nova." },
                    color = if (updateState.phase == GithubUpdateManager.Phase.ERROR) Warning else TextMuted,
                    fontSize = 12.sp, lineHeight = 18.sp)
                updateState.progress?.let { pct ->
                    Spacer(Modifier.height(6.dp))
                    Text("$pct% transferido", color = Accent, fontSize = 12.sp)
                    LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(13.dp))
                when (updateState.phase) {
                    GithubUpdateManager.Phase.AVAILABLE -> PrimaryAction("Baixar versão ${updateState.availableVersion}",
                        Icons.Outlined.Refresh, updater::download)
                    GithubUpdateManager.Phase.READY -> PrimaryAction("Instalar atualização",
                        Icons.Outlined.Refresh, updater::installDownloaded)
                    GithubUpdateManager.Phase.DOWNLOADING -> SecondaryAction("Atualizar progresso",
                        Icons.Outlined.Refresh, updater::refreshDownload)
                    GithubUpdateManager.Phase.CHECKING, GithubUpdateManager.Phase.VERIFYING -> {
                        Text("Processando…", color = Accent, fontSize = 12.sp)
                    }
                    else -> PrimaryAction("Procurar atualizações", Icons.Outlined.Refresh, updater::checkNow)
                }
                Spacer(Modifier.height(8.dp))
                Text("É necessário um repositório público de distribuição e APKs assinados sempre com a mesma chave. O GitHub privado do portal RootScope permanece separado.",
                    color = TextDim, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        item {
            SectionBox {
                Eyebrow("O que o aplicativo faz")
                Spacer(Modifier.height(10.dp))
                FeatureLine(Icons.Outlined.BluetoothSearching, "Descoberta BLE e Classic", "Encontra anúncios de fones, caixas e outros periféricos próximos.")
                ThinRule()
                FeatureLine(Icons.Outlined.SignalCellularAlt, "Telemetria", "Mostra RSSI, identificação anunciada, serviço e IDs de fabricante.")
                ThinRule()
                FeatureLine(Icons.Outlined.Link, "Conexões reais", "Abre sessão BLE/GATT e mostra resultado real; solicita pareamento pelo Android.")
                ThinRule()
                FeatureLine(Icons.Outlined.Security, "Fast Pair / CVE", "Identifica indícios do protocolo e fornece contexto técnico sem exploração.")
                ThinRule()
                FeatureLine(Icons.Outlined.Description, "Relatórios locais", "Registra eventos e exporta um arquivo JSON quando solicitado.")
            }
        }
        item {
            SectionBox {
                Eyebrow("CVE em análise")
                Spacer(Modifier.height(8.dp))
                Text("CVE-2025-36911 / WhisperPair", color = TextStrong, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text("Uma falha em implementações de Key-Based Pairing pode permitir pareamento não autorizado de dispositivos compatíveis com Google Fast Pair. O impacto depende do firmware e modelo.",
                    color = TextMuted, fontSize = 12.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(12.dp))
                Text("O diagnóstico desta versão não dispara o payload da CVE, não classifica um produto como vulnerável/patched e não contorna a autenticação. Confirme atualizações de firmware com o fabricante.",
                    color = Warning, fontSize = 12.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(11.dp))
                SecondaryAction("Abrir ficha da CVE", Icons.Outlined.OpenInNew, {
                    openUrl(ctx, "https://nvd.nist.gov/vuln/detail/CVE-2025-36911")
                })
            }
        }
        item {
            SectionBox {
                Eyebrow("Privacidade e limites")
                Spacer(Modifier.height(9.dp))
                Text("• Sem permissão de microfone, gravação ou acesso ao áudio.\n• Sem acesso à internet no próprio aplicativo.\n• Sem rastreamento por localização ou telemetria remota.\n• Bluetooth Classic e BLE podem usar endereços diferentes.\n• Nem toda caixa oferece GATT; áudio depende do sistema e dos perfis de conexão.\n• Só faça testes nos aparelhos que controla.",
                    color = TextMuted, fontSize = 12.sp, lineHeight = 21.sp)
            }
        }
        item {
            SectionBox {
                Eyebrow("Créditos")
                Spacer(Modifier.height(8.dp))
                Text("Inspirado na estrutura de descoberta Fast Pair do projeto ZWhisper Android, por ziron e colaboradores, sob licença Apache 2.0. Este aplicativo não utiliza os módulos de exploração, HFP ou gravação do projeto.",
                    color = TextMuted, fontSize = 12.sp, lineHeight = 19.sp)
                Spacer(Modifier.height(12.dp))
                SecondaryAction("Projeto de referência", Icons.Outlined.OpenInNew, {
                    openUrl(ctx, "https://github.com/zr0n/zwhisper-android")
                })
            }
        }
    }
}

@Composable
private fun FeatureLine(icon: ImageVector, title: String, detail: String) {
    Row(modifier = Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextStrong)
            Spacer(Modifier.height(3.dp))
            Text(detail, fontSize = 12.sp, lineHeight = 18.sp, color = TextMuted)
        }
    }
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, "Não foi possível abrir o link neste aparelho", Toast.LENGTH_LONG).show()
    }
}
