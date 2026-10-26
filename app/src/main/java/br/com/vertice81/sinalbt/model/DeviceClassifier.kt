package br.com.vertice81.sinalbt.model

/**
 * Identificação assistida pela descrição anunciada e pelos UUIDs publicados.
 * É uma CATEGORIA SUGERIDA, não uma validação de fabricante/marca/modelo.
 */
object DeviceClassifier {
    enum class Kind(val label: String) {
        SPEAKER("Caixa de som"),
        EARBUDS("Fone TWS"),
        HEADSET("Fone / headset"),
        WATCH("Relógio"),
        COMPUTER("Computador"),
        PHONE("Celular"),
        VEHICLE("Áudio automotivo"),
        SENSOR("Sensor BLE"),
        UNKNOWN("Dispositivo Bluetooth")
    }

    fun identify(name: String, services: List<String> = emptyList()): Kind {
        val n = name.lowercase(java.util.Locale.ROOT)
        val u = services.joinToString(",").lowercase(java.util.Locale.ROOT)
        return when {
            listOf("airpods", "earbuds", "earbud", "tws", "buds", "freebuds", "earphone", "qcy t").any(n::contains) -> Kind.EARBUDS
            listOf("headset", "headphone", "wh-1000", "fone de ouvido", "fone bt", "headphones").any(n::contains) -> Kind.HEADSET
            listOf("speaker", "jbl", "soundbar", "boombox", "flip ", "charge ", "caixa de som", "caixinha", "bt sound", "partybox").any(n::contains) -> Kind.SPEAKER
            listOf("watch", "band ", "amazfit", "smartband", "smart watch", "relógio").any(n::contains) -> Kind.WATCH
            listOf("macbook", "notebook", "laptop", "windows", "thinkpad", "chromebook").any(n::contains) -> Kind.COMPUTER
            listOf("iphone", "samsung galaxy", "pixel phone", "redmi note", "smartphone").any(n::contains) -> Kind.PHONE
            listOf("car audio", "carplay", "android auto", "car kit", "automotivo", "pioneer", "multimidia").any(n::contains) -> Kind.VEHICLE
            u.contains("0000181a-") || u.contains("00001809-") || u.contains("0000181d-") -> Kind.SENSOR
            else -> Kind.UNKNOWN
        }
    }

    fun isAudioCandidate(name: String, services: List<String> = emptyList()): Boolean {
        val kind = identify(name, services)
        if (kind in setOf(Kind.SPEAKER, Kind.EARBUDS, Kind.HEADSET, Kind.VEHICLE)) return true
        val s = services.joinToString(",").lowercase(java.util.Locale.ROOT)
        return listOf("0000110b-", "0000110a-", "0000110c-", "0000110e-", "0000111e-").any(s::contains)
    }
}
