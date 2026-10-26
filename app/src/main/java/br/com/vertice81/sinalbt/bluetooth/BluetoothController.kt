package br.com.vertice81.sinalbt.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.Build
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.com.vertice81.sinalbt.model.AudioProfileInfo
import br.com.vertice81.sinalbt.model.BluetoothDiagnostics
import br.com.vertice81.sinalbt.model.DeviceEntry
import br.com.vertice81.sinalbt.model.LinkDetail
import br.com.vertice81.sinalbt.model.LinkState
import br.com.vertice81.sinalbt.model.ScanMode
import br.com.vertice81.sinalbt.model.Source
import java.util.UUID

/**
 * Scanner inspirado na organização do ZWhisper Android (zr0n/zwhisper-android,
 * Apache-2.0). Esta implementação não contém exploração, escrita de chaves,
 * captura de áudio nem chamadas a APIs privilegiadas do Android.
 */
class BluetoothController(private val context: Context, private val events: EventStore) {
    companion object {
        private val FAST_PAIR_PARCEL = ParcelUuid.fromString("0000fe2c-0000-1000-8000-00805f9b34fb")
        private val FAST_PAIR_UUID = UUID.fromString("0000fe2c-0000-1000-8000-00805f9b34fb")
        private val KEY_PAIR_UUID = UUID.fromString("fe2c1234-8366-4814-8eb0-01de32100bea")
        private const val CONNECT_TIMEOUT = 16000L
        private const val SERVICE_TIMEOUT = 12000L
        private val BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        private val BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
    }

    private val handler = Handler(Looper.getMainLooper())
    private val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? = manager?.adapter
    val devices = mutableStateListOf<DeviceEntry>()
    val links = mutableStateMapOf<String, LinkDetail>()
    val audioProfiles = mutableStateMapOf<String, AudioProfileInfo>()
    var scanning by mutableStateOf(false)
        private set
    var scanMode by mutableStateOf(ScanMode.BOTH)
        private set
    var notice by mutableStateOf("Pronto para procurar dispositivos")
        private set

    private var leScanning = false
    private var classicScanning = false
    private var gatt: BluetoothGatt? = null
    private var gattAddress: String? = null
    private var scanRunnable: Runnable? = null
    private var connectTimeout: Runnable? = null
    private var isRegistered = false
    private var scanGeneration = 0
    private var receiverRegistered = false
    private var adapterReceiverRegistered = false
    private var classicUuidReceiverRegistered = false
    private var profileListener: BluetoothProfile.ServiceListener? = null
    private val activeProxies = mutableMapOf<Int, BluetoothProfile>()
    private var audioInspectionTimeout: Runnable? = null
    private val pendingSdp = mutableMapOf<String, Runnable>()
    private var audioInspectionGeneration = 0
    private var bleScannerCallback: ScanCallback? = null
    private var classicReceiver: BroadcastReceiver? = null
    private val bondReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission") // protegido por hasConnectPermission() e runCatching
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != BluetoothDevice.ACTION_BOND_STATE_CHANGED) return
            val changed = getDeviceFromIntent(intent) ?: return
            if (!hasConnectPermission()) return
            val bond = runCatching { changed.bondState }.getOrNull() ?: return
            handler.post {
                devices.indices.filter { devices[it].address == changed.address }.forEach { index ->
                    devices[index] = devices[index].copy(bondState = bond)
                }
                if (bond != BluetoothDevice.BOND_BONDING) refreshBonded()
                notice = when (bond) {
                    BluetoothDevice.BOND_BONDED -> "Pareamento confirmado pelo Android"
                    BluetoothDevice.BOND_BONDING -> "Pareamento em andamento"
                    else -> "Dispositivo não pareado"
                }
            }
        }
    }

    private val adapterReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != BluetoothAdapter.ACTION_STATE_CHANGED) return
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                    stopScan()
                    disconnectGatt()
                    notice = "Bluetooth desligado. Ative-o para continuar."
                }
                BluetoothAdapter.STATE_ON -> {
                    notice = "Bluetooth ativo. Scanner pronto."
                    refreshBonded()
                }
            }
        }
    }

    private val classicUuidReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != BluetoothDevice.ACTION_UUID) return
            val remote = getDeviceFromIntent(intent) ?: return
            @Suppress("DEPRECATION")
            val parcelUuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)
            val uuids = parcelUuids?.filterIsInstance<ParcelUuid>()?.map { it.uuid.toString() }.orEmpty()
            handler.post {
                pendingSdp.remove(remote.address.uppercase(java.util.Locale.ROOT))?.let { handler.removeCallbacks(it) }
                val index = devices.indexOfFirst { it.address.equals(remote.address, true) }
                if (index >= 0) {
                    devices[index] = devices[index].copy(classicServiceUuids = uuids)
                    notice = "Consulta SDP finalizada: ${uuids.size} serviços Classic"
                    events.add("SDP Classic", devices[index].name, remote.address, "${uuids.size} serviços")
                }
            }
        }
    }

    init {
        // Eventos do sistema: pareamento e desligamento do adaptador.
        runCatching {
            ContextCompat.registerReceiver(context, bondReceiver,
                IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED), ContextCompat.RECEIVER_EXPORTED)
            receiverRegistered = true
        }.onFailure { notice = "Falha ao ativar eventos de pareamento: ${it.javaClass.simpleName}" }
        runCatching {
            ContextCompat.registerReceiver(context, adapterReceiver,
                IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED), ContextCompat.RECEIVER_EXPORTED)
            adapterReceiverRegistered = true
        }
        runCatching {
            ContextCompat.registerReceiver(context, classicUuidReceiver,
                IntentFilter(BluetoothDevice.ACTION_UUID), ContextCompat.RECEIVER_EXPORTED)
            classicUuidReceiverRegistered = true
        }
    }

    fun showNotice(message: String) { notice = message }

    fun available(): Boolean = adapter != null
    @SuppressLint("MissingPermission")
    fun enabled(): Boolean = adapter?.isEnabled == true

    fun hasConnectPermission(): Boolean = Build.VERSION.SDK_INT < 31 ||
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    fun hasScanPermission(): Boolean = if (Build.VERSION.SDK_INT >= 31) {
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            hasConnectPermission()
    } else {
        context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    /** Compatibilidade com verificações antigas; equivale às permissões de busca. */
    fun hasPermissions(): Boolean = hasScanPermission()

    @SuppressLint("MissingPermission")
    fun refreshBonded() {
        if (!hasConnectPermission()) return
        try {
            val bonded = adapter?.bondedDevices.orEmpty()
            val bondedAddresses = bonded.map { it.address.uppercase(java.util.Locale.ROOT) }.toSet()
            // Remover entradas antigas que só vieram da lista de pareados.
            devices.removeAll { it.source == Source.PAIRED && it.key !in bondedAddresses }
            for (i in devices.indices) {
                val item = devices[i]
                if (item.key !in bondedAddresses && item.bondState == BluetoothDevice.BOND_BONDED) {
                    devices[i] = item.copy(bondState = BluetoothDevice.BOND_NONE)
                }
            }
            bonded.forEach { d ->
                addOrUpdate(DeviceEntry(
                    address = d.address,
                    name = d.name?.takeIf { it.isNotBlank() } ?: "Dispositivo pareado",
                    source = Source.PAIRED,
                    bondState = d.bondState
                ))
            }
        } catch (_: SecurityException) {
            notice = "Sem permissão para consultar pareamentos"
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(mode: ScanMode) {
        if (!available()) { notice = "Bluetooth não disponível neste aparelho"; return }
        if (!hasScanPermission()) { notice = "Autorize a busca Bluetooth nas permissões"; return }
        if (!enabled()) { notice = "Ative o Bluetooth do celular"; return }
        if (Build.VERSION.SDK_INT <= 30) {
            val location = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val on = runCatching {
                if (Build.VERSION.SDK_INT >= 28) location?.isLocationEnabled == true
                else location?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                    location?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
            }.getOrDefault(false)
            if (!on) { notice = "Android 8–11: ative Localização nas configurações para buscar BLE"; return }
        }
        stopScan()
        resetDiscoveryCache()
        scanGeneration += 1
        scanMode = mode
        scanning = true
        notice = when (mode) {
            ScanMode.BOTH -> "Buscando BLE, depois dispositivos clássicos"
            ScanMode.BLE -> "Procurando anúncios Bluetooth LE"
            ScanMode.CLASSIC -> "Procurando dispositivos Bluetooth Classic"
        }
        refreshBonded()
        if (mode == ScanMode.CLASSIC) startClassic()
        else startBle()
    }

    @SuppressLint("MissingPermission")
    private fun startBle() {
        val scanner = adapter?.bluetoothLeScanner
        if (scanner == null) {
            if (scanMode == ScanMode.BOTH) startClassic()
            else { scanning = false; notice = "Scanner BLE indisponível" }
            return
        }
        val generation = scanGeneration
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                if (generation == scanGeneration) onBleResult(result, generation)
            }
            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                if (generation == scanGeneration) results.forEach { onBleResult(it, generation) }
            }
            override fun onScanFailed(errorCode: Int) {
                handler.post {
                    if (generation != scanGeneration) return@post
                    stopBle()
                    leScanning = false
                    scanRunnable?.let { handler.removeCallbacks(it) }
                    scanRunnable = null
                    val detail = when (errorCode) {
                        1 -> "busca já iniciada"
                        2 -> "falha no registro do scanner"
                        3 -> "erro interno do Bluetooth"
                        4 -> "recurso não suportado"
                        5 -> "limite de recursos do sistema"
                        else -> "código desconhecido"
                    }
                    notice = "Falha no scanner BLE ($errorCode): $detail"
                    if (scanMode == ScanMode.BOTH) {
                        scanGeneration += 1
                        startClassic()
                    } else scanning = false
                }
            }
        }
        bleScannerCallback = callback
        try {
            scanner.startScan(null, ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setReportDelay(0).build(), callback)
            leScanning = true
            scanRunnable = Runnable {
                stopBle()
                if (scanMode == ScanMode.BOTH) {
                    scanGeneration += 1
                    startClassic()
                } else { scanning = false; notice = "Busca BLE concluída" }
            }.also { handler.postDelayed(it, 9500L) }
        } catch (e: Exception) {
            scanning = false
            notice = "Não foi possível iniciar BLE: ${e.message ?: "erro"}"
        }
    }

    @SuppressLint("MissingPermission")
    private fun onBleResult(result: ScanResult, generation: Int) {
        val record = result.scanRecord
        val uuids = record?.serviceUuids?.map { it.uuid.toString() } ?: emptyList()
        val fastPairData = record?.getServiceData(FAST_PAIR_PARCEL)
        val fastPair = fastPairData != null || uuids.any { it.equals(FAST_PAIR_UUID.toString(), true) }
        val modelId = BluetoothDiagnostics.fastPairModelId(fastPairData)
        val manufacturerIds = record?.manufacturerSpecificData?.let { data ->
            (0 until data.size()).map { data.keyAt(it) }
        } ?: emptyList()
        val name = runCatching {
            record?.deviceName ?: result.device.name
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Dispositivo sem nome"
        val entry = runCatching {
            DeviceEntry(
                address = result.device.address, name = name, source = Source.BLE,
                rssi = BluetoothDiagnostics.validRssi(result.rssi),
                txPower = result.scanRecord?.txPowerLevel?.takeIf { it in -127..20 },
                connectable = result.isConnectable,
                isFastPair = fastPair, modelId = modelId,
                serviceUuids = uuids, manufacturerIds = manufacturerIds,
                bondState = runCatching { result.device.bondState }.getOrDefault(BluetoothDevice.BOND_NONE)
            )
        }.getOrNull() ?: return
        handler.post { if (generation == scanGeneration) addOrUpdate(entry) }
    }

    @SuppressLint("MissingPermission")
    private fun startClassic() {
        if (!hasScanPermission() || !enabled()) { scanning = false; return }
        val generation = scanGeneration
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (generation != scanGeneration) return
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val d = getDeviceFromIntent(intent) ?: return
                        if (!hasScanPermission()) return
                        val rssi = BluetoothDiagnostics.validRssi(
                            if (intent.hasExtra(BluetoothDevice.EXTRA_RSSI))
                                intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                            else null
                        )
                        runCatching {
                            addOrUpdate(DeviceEntry(
                                address = d.address, name = d.name ?: "Dispositivo sem nome",
                                source = Source.CLASSIC, rssi = rssi, bondState = d.bondState
                            ))
                        }.onFailure { notice = "Falha ao identificar dispositivo Classic" }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        classicScanning = false
                        scanning = false
                        scanRunnable?.let { handler.removeCallbacks(it) }
                        scanRunnable = null
                        notice = "Busca concluída — ${devices.count { it.discovered }} dispositivos encontrados"
                        unregisterClassic()
                    }
                }
            }
        }
        classicReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        try {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
            isRegistered = true
            classicScanning = adapter?.startDiscovery() == true
            if (!classicScanning) {
                notice = "Não foi possível iniciar busca Classic"
                scanning = false
                unregisterClassic()
            } else {
                notice = "Buscando caixas e fones Bluetooth Classic"
                scanRunnable = Runnable { stopScan() }.also { handler.postDelayed(it, 18000L) }
            }
        } catch (e: Exception) {
            notice = "Erro na busca Classic: ${e.message}"
            scanning = false
            unregisterClassic()
        }
    }

    @Suppress("DEPRECATION")
    private fun getDeviceFromIntent(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    private fun addOrUpdate(item: DeviceEntry) {
        val index = devices.indexOfFirst { it.key == item.key }
        if (index < 0) { devices.add(item); return }
        val old = devices[index]
        val ble = item.seenBle || old.seenBle
        val classic = item.seenClassic || old.seenClassic
        val preferredSource = when {
            ble -> Source.BLE
            classic -> Source.CLASSIC
            else -> Source.PAIRED
        }
        val placeholder = setOf("Dispositivo sem nome", "Dispositivo pareado")
        devices[index] = item.copy(
            source = preferredSource,
            name = if (item.name in placeholder && old.name !in placeholder) old.name else item.name,
            rssi = item.rssi ?: old.rssi,
            txPower = item.txPower ?: old.txPower,
            connectable = item.connectable ?: old.connectable,
            isFastPair = item.isFastPair || old.isFastPair,
            modelId = item.modelId ?: old.modelId,
            serviceUuids = (item.serviceUuids + old.serviceUuids).distinct(),
            manufacturerIds = (item.manufacturerIds + old.manufacturerIds).distinct(),
            bondState = if (item.source == Source.PAIRED) item.bondState else if (old.bondState == BluetoothDevice.BOND_BONDED) old.bondState else item.bondState,
            seenBle = ble,
            seenClassic = classic,
            lastSeen = if (item.source == Source.PAIRED) old.lastSeen else item.lastSeen
        )
    }

    @SuppressLint("MissingPermission")
    private fun stopBle() {
        if (leScanning) {
            runCatching { bleScannerCallback?.let { adapter?.bluetoothLeScanner?.stopScan(it) } }
        }
        bleScannerCallback = null
        leScanning = false
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanGeneration += 1
        scanRunnable?.let { handler.removeCallbacks(it) }
        scanRunnable = null
        stopBle()
        if (classicScanning) runCatching { adapter?.cancelDiscovery() }
        classicScanning = false
        unregisterClassic()
        scanning = false
        notice = "Busca interrompida"
    }

    private fun unregisterClassic() {
        if (isRegistered) {
            runCatching { classicReceiver?.let { context.unregisterReceiver(it) } }
            isRegistered = false
        }
        classicReceiver = null
    }

    @SuppressLint("MissingPermission")
    fun inspectClassicServices(entry: DeviceEntry) {
        if (!hasConnectPermission() || !enabled()) { notice = "Autorize e ative Bluetooth"; return }
        stopScan()
        val remote = runCatching { adapter?.getRemoteDevice(entry.address) }.getOrNull()
        if (remote == null) { notice = "Endereço Classic inválido"; return }
        if (runCatching { remote.fetchUuidsWithSdp() }.getOrDefault(false)) {
            val key = entry.key
            pendingSdp.remove(key)?.let { handler.removeCallbacks(it) }
            pendingSdp[key] = Runnable {
                pendingSdp.remove(key)
                notice = "O dispositivo não respondeu à consulta SDP em 12 segundos"
                events.add("SDP Classic", entry.name, entry.address, "Sem resposta em 12 s")
            }.also { handler.postDelayed(it, SERVICE_TIMEOUT) }
            notice = "Consulta de serviços Classic solicitada; aguardando resposta do Android"
            events.add("SDP Classic", entry.name, entry.address, "Consulta solicitada")
        } else {
            notice = "Não foi possível iniciar consulta SDP"
        }
    }

    /** Consulta perfis de áudio *deste celular*. Não tenta acessar áudio nem outro telefone. */
    @SuppressLint("MissingPermission")
    fun inspectLocalAudio(entry: DeviceEntry) {
        if (!hasConnectPermission() || !enabled()) { notice = "Autorize Bluetooth primeiro"; return }
        closeAudioInspection()
        val generation = audioInspectionGeneration
        val owner = entry.address
        var a2dp: Boolean? = null
        var headset: Boolean? = null
        var finished = false
        fun publish(message: String) {
            audioProfiles[owner] = AudioProfileInfo(a2dp, headset, message)
            if (a2dp != null && headset != null && !finished) {
                finished = true
                events.add("Áudio local", entry.name, owner,
                    "A2DP=${a2dp == true}; HFP=${headset == true}")
                audioInspectionTimeout?.let { handler.removeCallbacks(it) }
                audioInspectionTimeout = null
            }
        }
        publish("Consultando A2DP e HFP do próprio celular")
        val listener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                handler.post {
                    if (generation != audioInspectionGeneration) {
                        runCatching { adapter?.closeProfileProxy(profile, proxy) }
                        return@post
                    }
                    activeProxies[profile] = proxy
                    val connected = runCatching {
                        proxy.connectedDevices.any { it.address.equals(owner, true) }
                    }.getOrDefault(false)
                    when (profile) {
                        BluetoothProfile.A2DP -> a2dp = connected
                        BluetoothProfile.HEADSET -> headset = connected
                    }
                    publish(if (a2dp != null && headset != null) "Consulta local concluída" else "Consultando perfis restantes")
                    if (a2dp != null && headset != null) closeAudioInspection()
                }
            }
            override fun onServiceDisconnected(profile: Int) {
                handler.post {
                    if (generation != audioInspectionGeneration) return@post
                    if (profile == BluetoothProfile.A2DP) a2dp = false
                    if (profile == BluetoothProfile.HEADSET) headset = false
                    publish("Perfil Bluetooth indisponível")
                }
            }
        }
        profileListener = listener
        val a2dpStarted = adapter?.getProfileProxy(context, listener, BluetoothProfile.A2DP) == true
        val hfpStarted = adapter?.getProfileProxy(context, listener, BluetoothProfile.HEADSET) == true
        if (!a2dpStarted) a2dp = false
        if (!hfpStarted) headset = false
        publish(if (a2dpStarted || hfpStarted) "Aguardando perfis Bluetooth" else "Não foi possível consultar perfis")
        if (a2dp == null || headset == null) {
            audioInspectionTimeout = Runnable {
                publish("Tempo limite na consulta de áudio do celular")
                closeAudioInspection()
            }.also { handler.postDelayed(it, 8000L) }
        } else closeAudioInspection()
    }

    private fun closeAudioInspection() {
        audioInspectionGeneration += 1
        audioInspectionTimeout?.let { handler.removeCallbacks(it) }
        audioInspectionTimeout = null
        for ((profile, proxy) in activeProxies) {
            runCatching { adapter?.closeProfileProxy(profile, proxy) }
        }
        activeProxies.clear()
        profileListener = null
    }

    @SuppressLint("MissingPermission")
    fun refreshConnectedRssi(address: String) {
        if (!hasConnectPermission() || !enabled() || gattAddress != address || gatt == null) {
            notice = "Primeiro estabeleça uma conexão GATT para atualizar o RSSI"
            return
        }
        if (runCatching { gatt?.readRemoteRssi() }.getOrDefault(false) != true) {
            notice = "O Android não conseguiu solicitar o RSSI da sessão"
        }
    }

    @SuppressLint("MissingPermission")
    fun pair(entry: DeviceEntry) {
        if (!hasConnectPermission()) { notice = "Permissão de conexão Bluetooth ausente"; return }
        stopScan()
        try {
            val remote = adapter?.getRemoteDevice(entry.address) ?: return
            if (remote.bondState == BluetoothDevice.BOND_BONDED) {
                notice = "Já pareado. Abra Configurações Bluetooth para ativar áudio."
                events.add("Pareamento", entry.name, entry.address, "Dispositivo já pareado")
                return
            }
            if (remote.createBond()) {
                notice = "Pareamento solicitado. Confirme no Android/dispositivo."
                events.add("Pareamento", entry.name, entry.address, "Solicitado via API oficial")
            } else {
                notice = "O Android não iniciou pareamento"
                events.add("Pareamento", entry.name, entry.address, "Solicitação recusada")
            }
        } catch (e: Exception) {
            notice = "Não foi possível parear: ${e.message}"
            events.add("Erro", entry.name, entry.address, notice)
        }
    }

    @SuppressLint("MissingPermission")
    fun connectGatt(entry: DeviceEntry) {
        if (!entry.supportsGatt) { notice = "Este dispositivo não foi detectado por BLE; use pareamento Classic."; return }
        if (!hasConnectPermission()) { notice = "Permissão Bluetooth ausente"; return }
        if (!enabled()) { notice = "Ative o Bluetooth"; return }
        stopScan()
        disconnectGatt()
        val address = entry.address
        val name = entry.name
        gattAddress = address
        links[address] = LinkDetail(state = LinkState.CONNECTING, message = "Abrindo ligação BLE/GATT")
        notice = "Tentando conexão BLE com $name"

        val callback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(client: BluetoothGatt, status: Int, newState: Int) {
                handler.post {
                    if (gatt !== client) return@post
                    if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                        clearConnectionTimeout()
                        links[address] = LinkDetail(
                            state = LinkState.DISCOVERING,
                            message = "Link GATT ativo; descobrindo serviços",
                            since = System.currentTimeMillis()
                        )
                        events.add("GATT", name, address, "Conexão BLE estabelecida")
                        notice = "Link BLE estabelecido com $name"
                        if (!runCatching { client.discoverServices() }.getOrDefault(false)) {
                            links[address] = LinkDetail(state = LinkState.CONNECTED,
                                message = "Conectado; dispositivo recusou descoberta de serviços",
                                since = System.currentTimeMillis())
                        } else {
                            // Um dispositivo pode conectar e nunca retornar onServicesDiscovered.
                            connectTimeout = Runnable {
                                if (gatt === client && links[address]?.state == LinkState.DISCOVERING) {
                                    links[address] = LinkDetail(state = LinkState.CONNECTED,
                                        message = "GATT conectado; tempo limite na leitura dos serviços",
                                        since = System.currentTimeMillis())
                                    events.add("Serviços GATT", name, address, "Tempo limite de descoberta")
                                }
                            }.also { handler.postDelayed(it, 12000L) }
                        }
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        clearConnectionTimeout()
                        links[address] = LinkDetail(
                            state = if (status == BluetoothGatt.GATT_SUCCESS) LinkState.DISCONNECTED else LinkState.ERROR,
                            message = "Desconectado — código GATT $status"
                        )
                        events.add("GATT", name, address, "Desconectado (status $status)")
                        runCatching { client.close() }
                        if (gatt === client) { gatt = null; gattAddress = null }
                    } else if (status != BluetoothGatt.GATT_SUCCESS) {
                        clearConnectionTimeout()
                        val guidance = if (status == 133) "erro genérico Android; reinicie o Bluetooth e tente novamente"
                            else "verifique sinal, permissão e compatibilidade"
                        links[address] = LinkDetail(state = LinkState.ERROR,
                            message = "Falha GATT ($status): $guidance")
                        events.add("Erro GATT", name, address, "Código $status")
                        runCatching { client.close() }
                        if (gatt === client) { gatt = null; gattAddress = null }
                    }
                }
            }

            override fun onReadRemoteRssi(client: BluetoothGatt, rssi: Int, status: Int) {
                handler.post {
                    if (gatt !== client) return@post
                    val current = links[address] ?: return@post
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        links[address] = current.copy(remoteRssi = BluetoothDiagnostics.validRssi(rssi))
                        notice = "RSSI atualizado nesta conexão GATT"
                    } else notice = "Não foi possível atualizar RSSI (código $status)"
                }
            }

            @Suppress("DEPRECATION")
            override fun onCharacteristicRead(client: BluetoothGatt,
                characteristic: android.bluetooth.BluetoothGattCharacteristic, status: Int) {
                if (characteristic.uuid != BATTERY_LEVEL) return
                handler.post {
                    if (gatt !== client) return@post
                    val value = if (status == BluetoothGatt.GATT_SUCCESS)
                        runCatching { characteristic.getIntValue(android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8, 0) }.getOrNull()
                    else null
                    if (value != null && value in 0..100) {
                        links[address]?.let { links[address] = it.copy(batteryPercent = value) }
                    }
                }
            }

            override fun onCharacteristicRead(client: BluetoothGatt,
                characteristic: android.bluetooth.BluetoothGattCharacteristic,
                value: ByteArray, status: Int) {
                if (characteristic.uuid != BATTERY_LEVEL) return
                handler.post {
                    if (gatt !== client) return@post
                    val level = if (status == BluetoothGatt.GATT_SUCCESS)
                        value.firstOrNull()?.toInt()?.and(0xff) else null
                    if (level != null && level in 0..100) {
                        links[address]?.let { links[address] = it.copy(batteryPercent = level) }
                    }
                }
            }

            override fun onServicesDiscovered(client: BluetoothGatt, status: Int) {
                handler.post {
                    if (gatt !== client) return@post
                    clearConnectionTimeout()
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        runCatching {
                            val services = client.services.orEmpty()
                            val fp = services.firstOrNull { it.uuid == FAST_PAIR_UUID }
                            val count = services.sumOf { it.characteristics.size }
                            links[address] = LinkDetail(
                                state = LinkState.CONNECTED,
                                message = "Conexão BLE ativa — ${services.size} serviços, $count características",
                                services = services.map { it.uuid.toString() },
                                characteristics = count,
                                hasFastPairService = fp != null,
                                hasKeyBasedPairingCharacteristic = fp?.getCharacteristic(KEY_PAIR_UUID) != null,
                                since = System.currentTimeMillis()
                            )
                            events.add("Serviços GATT", name, address,
                                "${services.size} serviços; Fast Pair=${fp != null}; característica Key-Based=${fp?.getCharacteristic(KEY_PAIR_UUID) != null}")
                            // Somente leitura BLE padronizada; nenhuma escrita de característica.
                            val battery = client.getService(BATTERY_SERVICE)?.getCharacteristic(BATTERY_LEVEL)
                            if (battery != null && battery.properties and
                                android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ != 0) {
                                runCatching { client.readCharacteristic(battery) }
                            }
                        }.onFailure { error ->
                            links[address] = LinkDetail(state = LinkState.CONNECTED,
                                message = "GATT conectado; não foi possível ler serviços: ${error.message}",
                                since = System.currentTimeMillis())
                        }
                    } else {
                        links[address] = LinkDetail(state = LinkState.CONNECTED,
                            message = "Conectado; serviços indisponíveis (status $status)",
                            since = System.currentTimeMillis())
                    }
                }
            }
        }
        try {
            val remote = adapter?.getRemoteDevice(address) ?: return
            val client = remote.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
                ?: throw IllegalStateException("O Android não abriu a conexão BLE")
            gatt = client
            connectTimeout = Runnable {
                if (gatt === client && links[address]?.state == LinkState.CONNECTING) {
                    links[address] = LinkDetail(state = LinkState.ERROR, message = "Tempo limite de conexão (16 s)")
                    events.add("Timeout", name, address, "GATT sem resposta em 16 segundos")
                    runCatching { client.disconnect(); client.close() }
                    gatt = null
                    gattAddress = null
                }
            }.also { handler.postDelayed(it, CONNECT_TIMEOUT) }
        } catch (e: Exception) {
            links[address] = LinkDetail(state = LinkState.ERROR, message = "Erro: ${e.message}")
            events.add("Erro GATT", name, address, e.message ?: "Erro de conexão")
            gatt = null
            gattAddress = null
        }
    }

    private fun clearConnectionTimeout() {
        connectTimeout?.let { handler.removeCallbacks(it) }
        connectTimeout = null
    }

    @SuppressLint("MissingPermission")
    fun disconnectGatt() {
        clearConnectionTimeout()
        val active = gatt ?: return
        val address = gattAddress
        gatt = null
        gattAddress = null
        runCatching { active.disconnect(); active.close() }
        if (address != null) {
            links[address] = LinkDetail(state = LinkState.DISCONNECTED, message = "Desconectado pelo usuário")
            val entry = devices.firstOrNull { it.address == address }
            events.add("GATT", entry?.name ?: "Dispositivo", address, "Desconexão manual")
        }
    }

    fun disconnectAddress(address: String) {
        if (gattAddress == address) disconnectGatt()
    }

    private fun resetDiscoveryCache() {
        // Uma nova busca não pode mostrar anúncios antigos como se fossem novos.
        devices.removeAll { it.bondState != BluetoothDevice.BOND_BONDED && it.address != gattAddress }
        for (i in devices.indices) {
            val entry = devices[i]
            if (entry.address != gattAddress) {
                devices[i] = entry.copy(source = Source.PAIRED, seenBle = false,
                    seenClassic = false, rssi = null, txPower = null, connectable = null,
                    serviceUuids = emptyList(), manufacturerIds = emptyList(),
                    modelId = null, isFastPair = false)
            }
        }
    }

    fun clearScanResults() {
        stopScan()
        resetDiscoveryCache()
        notice = "Busca limpa. Pareamentos e sessão ativa preservados."
    }

    fun close() {
        stopScan()
        disconnectGatt()
        closeAudioInspection()
        pendingSdp.values.forEach { handler.removeCallbacks(it) }
        pendingSdp.clear()
        if (classicUuidReceiverRegistered) runCatching { context.unregisterReceiver(classicUuidReceiver) }
        if (receiverRegistered) runCatching { context.unregisterReceiver(bondReceiver) }
        if (adapterReceiverRegistered) runCatching { context.unregisterReceiver(adapterReceiver) }
    }
}
