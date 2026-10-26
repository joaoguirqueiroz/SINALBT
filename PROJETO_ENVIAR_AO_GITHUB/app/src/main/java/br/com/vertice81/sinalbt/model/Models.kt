package br.com.vertice81.sinalbt.model

enum class ScanMode(val label: String) {
    BOTH("Todos"), BLE("BLE"), CLASSIC("Clássico")
}

enum class Source(val label: String) {
    BLE("BLE"), CLASSIC("Clássico"), PAIRED("Pareado")
}

enum class LinkState(val label: String) {
    IDLE("Não conectado"), CONNECTING("Conectando"), DISCOVERING("Lendo serviços"),
    CONNECTED("GATT conectado"), DISCONNECTED("Desconectado"), ERROR("Falha")
}

data class DeviceEntry(
    val address: String,
    val name: String,
    val source: Source,
    val rssi: Int? = null,
    val txPower: Int? = null,
    val connectable: Boolean? = null,
    val isFastPair: Boolean = false,
    val modelId: String? = null,
    val serviceUuids: List<String> = emptyList(),
    val manufacturerIds: List<Int> = emptyList(),
    val classicServiceUuids: List<String> = emptyList(),
    val bondState: Int = 10,
    val lastSeen: Long = System.currentTimeMillis(),
    val seenBle: Boolean = source == Source.BLE,
    val seenClassic: Boolean = source == Source.CLASSIC,
) {
    val key: String get() = address.uppercase(java.util.Locale.ROOT)
    val discovered: Boolean get() = seenBle || seenClassic
    val supportsGatt: Boolean get() = seenBle
    val transportLabel: String get() = when {
        seenBle && seenClassic -> "BLE + Classic"
        seenBle -> "BLE"
        seenClassic -> "Classic"
        else -> "Pareado"
    }
    val distanceDescription: String get() = BluetoothDiagnostics.signalLabel(rssi)
}

data class LinkDetail(
    val state: LinkState = LinkState.IDLE,
    val message: String = "Nenhuma conexão iniciada",
    val services: List<String> = emptyList(),
    val characteristics: Int = 0,
    val hasFastPairService: Boolean = false,
    val hasKeyBasedPairingCharacteristic: Boolean = false,
    val since: Long? = null,
    val remoteRssi: Int? = null,
    val batteryPercent: Int? = null,
)

data class EventRecord(
    val time: Long,
    val type: String,
    val device: String,
    val address: String,
    val message: String,
)


data class AudioProfileInfo(
    val a2dpConnected: Boolean? = null,
    val headsetConnected: Boolean? = null,
    val message: String = "Não consultado"
) {
    val anyConnected: Boolean get() = a2dpConnected == true || headsetConnected == true
}

/** Valores anunciados, nunca estimativas de distância física ou de propriedade do dispositivo. */
object BluetoothDiagnostics {
    fun validRssi(value: Int?): Int? = value?.takeIf { it in -127..20 }

    fun fastPairModelId(serviceData: ByteArray?): String? = serviceData
        ?.takeIf { it.size == 3 }
        ?.joinToString("") { "%02X".format(it.toInt() and 0xff) }

    fun signalLabel(rssi: Int?): String {
        val value = validRssi(rssi) ?: return "Sem medição"
        return when {
            value >= -55 -> "Sinal forte"
            value >= -72 -> "Sinal médio"
            else -> "Sinal fraco"
        }
    }

    fun ageLabel(timestamp: Long, now: Long = System.currentTimeMillis()): String {
        val elapsed = (now - timestamp).coerceAtLeast(0)
        return when {
            elapsed < 10_000L -> "Agora"
            elapsed < 60_000L -> "Há ${elapsed / 1_000L}s"
            elapsed < 3_600_000L -> "Há ${elapsed / 60_000L}min"
            else -> "Há ${elapsed / 3_600_000L}h"
        }
    }

    /** IDs de empresas são dados anunciados, NÃO prova da marca física. */
    fun companyName(id: Int): String? = when (id) {
        0x004C -> "Apple"
        0x0006 -> "Microsoft"
        0x0075 -> "Samsung Electronics"
        0x00E0 -> "Google"
        0x000F -> "Broadcom"
        0x000D -> "Texas Instruments"
        0x0131 -> "Xiaomi"
        0x0059 -> "Nordic Semiconductor"
        else -> null
    }
}
