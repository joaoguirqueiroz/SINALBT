package br.com.vertice81.sinalbt.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceClassifierTest {
    @Test fun classifySpeakersAndGenericNames() {
        assertEquals(DeviceClassifier.Kind.SPEAKER, DeviceClassifier.identify("BR Speaker"))
        assertEquals(DeviceClassifier.Kind.SPEAKER, DeviceClassifier.identify("JBL Flip 6"))
        assertEquals(DeviceClassifier.Kind.UNKNOWN, DeviceClassifier.identify("BT 0002"))
    }

    @Test fun identifyWearablesAndEarbuds() {
        assertEquals(DeviceClassifier.Kind.EARBUDS, DeviceClassifier.identify("AirPods Pro"))
        assertEquals(DeviceClassifier.Kind.EARBUDS, DeviceClassifier.identify("TWS F9"))
        assertEquals(DeviceClassifier.Kind.WATCH, DeviceClassifier.identify("Smart Watch"))
        assertEquals(DeviceClassifier.Kind.COMPUTER, DeviceClassifier.identify("MacBook Air"))
    }

    @Test fun audioServiceDoesNotProveManufacturer() {
        val unknown = DeviceClassifier.identify("Device 1829", listOf("0000110b-0000-1000-8000-00805f9b34fb"))
        assertEquals(DeviceClassifier.Kind.UNKNOWN, unknown)
        assertTrue(DeviceClassifier.isAudioCandidate("Device 1829", listOf("0000110b-0000-1000-8000-00805f9b34fb")))
        assertFalse(DeviceClassifier.isAudioCandidate("Beacon 4"))
    }
}
