import br.com.vertice81.sinalbt.model.BluetoothDiagnostics
import br.com.vertice81.sinalbt.model.DeviceEntry
import br.com.vertice81.sinalbt.model.Source
import br.com.vertice81.sinalbt.model.DeveloperInfo

fun main() {
    check(BluetoothDiagnostics.validRssi(-32768) == null)
    check(BluetoothDiagnostics.validRssi(-55) == -55)
    check(BluetoothDiagnostics.validRssi(200) == null)
    check(BluetoothDiagnostics.fastPairModelId(byteArrayOf(0xAB.toByte(), 0xCD.toByte(), 0x01)) == "ABCD01")
    check(BluetoothDiagnostics.fastPairModelId(byteArrayOf(0x01, 0x02, 0x03, 0x04)) == null)
    check(BluetoothDiagnostics.fastPairModelId(null) == null)
    check(BluetoothDiagnostics.signalLabel(null) == "Sem medição")
    check(BluetoothDiagnostics.signalLabel(-90) == "Sinal fraco")
    check(BluetoothDiagnostics.ageLabel(1_000L, 11_000L) == "Há 10s")
    check(DeviceEntry("aB:cD", "Fone", Source.BLE).key == DeviceEntry("AB:CD", "Fone", Source.CLASSIC).key)
    check(DeviceEntry("AB", "Fone", Source.PAIRED).discovered.not())
    check(DeviceEntry("AB", "Fone", Source.BLE).supportsGatt)
    check(BluetoothDiagnostics.companyName(0x004C) == "Apple")
    check(DeveloperInfo.INSTAGRAM_HANDLE == "@rootscope.br")
    check(DeveloperInfo.INSTAGRAM_URL == "https://www.instagram.com/rootscope.br/")
    println("OK — 15 verificações locais do modelo, da telemetria e do desenvolvedor")
}
