package br.com.vertice81.sinalbt.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ModelsTest {
    @Test fun officialDeveloperInstagramLink() {
        assertEquals("@rootscope.br", DeveloperInfo.INSTAGRAM_HANDLE)
        assertEquals("https://www.instagram.com/rootscope.br/", DeveloperInfo.INSTAGRAM_URL)
        assertEquals("RootScope", DeveloperInfo.LAB)
    }
    @Test fun signalCategories() {
        assertEquals("Sinal forte", DeviceEntry("A", "Teste", Source.BLE, rssi = -48).distanceDescription)
        assertEquals("Sinal médio", DeviceEntry("A", "Teste", Source.BLE, rssi = -69).distanceDescription)
        assertEquals("Sinal fraco", DeviceEntry("A", "Teste", Source.BLE, rssi = -91).distanceDescription)
        assertEquals("Sem medição", DeviceEntry("A", "Teste", Source.CLASSIC).distanceDescription)
    }

    @Test fun stableDeviceIdentityAcrossTransports() {
        assertEquals("AA:BB", DeviceEntry("aa:bb", "Caixa", Source.BLE).key)
        assertEquals("AA:BB", DeviceEntry("AA:BB", "Caixa", Source.CLASSIC).key)
    }

    @Test fun mergedDeviceTransportText() {
        val entry = DeviceEntry("AA", "Teste", Source.BLE, seenClassic = true)
        assertEquals("BLE + Classic", entry.transportLabel)
        assertEquals(true, entry.discovered)
    }

    @Test fun pairedDeviceNotListedAsFreshDiscovery() {
        val entry = DeviceEntry("AA", "Teste", Source.PAIRED)
        assertEquals(false, entry.discovered)
        assertEquals("Pareado", entry.transportLabel)
    }

    @Test fun invalidRssiNeverBecomesVisible() {
        assertNull(BluetoothDiagnostics.validRssi(-32768))
        assertNull(BluetoothDiagnostics.validRssi(200))
        assertEquals(-61, BluetoothDiagnostics.validRssi(-61))
        assertEquals("Sem medição", BluetoothDiagnostics.signalLabel(-32768))
    }

    @Test fun exactThreeByteFastPairModelIdOnly() {
        assertEquals("ABCD01", BluetoothDiagnostics.fastPairModelId(
            byteArrayOf(0xAB.toByte(), 0xCD.toByte(), 0x01)))
        assertNull(BluetoothDiagnostics.fastPairModelId(byteArrayOf(1, 2, 3, 4)))
    }

    @Test fun chronologicalLabels() {
        assertEquals("Agora", BluetoothDiagnostics.ageLabel(1000, 1000))
        assertEquals("Há 10s", BluetoothDiagnostics.ageLabel(1000, 11000))
        assertEquals("Há 2min", BluetoothDiagnostics.ageLabel(1000, 121000))
    }

    @Test fun noGattForClassicOnlyDevices() {
        assertEquals(false, DeviceEntry("AA", "Caixa", Source.CLASSIC).supportsGatt)
        assertEquals(true, DeviceEntry("AA", "Fone", Source.BLE).supportsGatt)
    }
}
