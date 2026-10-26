package br.com.vertice81.sinalbt

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import br.com.vertice81.sinalbt.bluetooth.BluetoothController
import br.com.vertice81.sinalbt.bluetooth.EventStore
import br.com.vertice81.sinalbt.ui.SinalApp
import br.com.vertice81.sinalbt.ui.SinalTheme
import br.com.vertice81.sinalbt.updates.GithubUpdateManager

class MainActivity : ComponentActivity() {
    private lateinit var events: EventStore
    private lateinit var controller: BluetoothController
    private lateinit var updater: GithubUpdateManager
    private var exportMessage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        events = EventStore(this)
        controller = BluetoothController(this, events)
        updater = GithubUpdateManager(this)

        setContent {
            SinalTheme {
                // Toda ação que usa Bluetooth passa pelo mesmo controle de permissões.
                // Não executamos operações de conexão sem consentimento do Android.
                var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
                var pendingNeedsScan by remember { mutableStateOf(false) }
                val enableBluetooth = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) {
                    if (controller.enabled()) {
                        val action = pendingAction
                        pendingAction = null
                        action?.invoke()
                    } else {
                        pendingAction = null
                        controller.showNotice("Bluetooth permanece desligado")
                    }
                }

                val permissions = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { _ ->
                    if (if (pendingNeedsScan) !controller.hasScanPermission() else !controller.hasConnectPermission()) {
                        pendingAction = null
                        controller.showNotice("Permissão Bluetooth negada. Autorize dispositivos próximos nas configurações.")
                    } else if (controller.enabled()) {
                        val action = pendingAction
                        pendingAction = null
                        action?.invoke()
                    } else {
                        enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    }
                }

                fun withBluetoothPermission(needsScan: Boolean = false, action: () -> Unit) {
                    pendingAction = action
                    pendingNeedsScan = needsScan
                    when {
                        !controller.available() -> {
                            pendingAction = null
                            controller.showNotice("Este aparelho não possui Bluetooth")
                        }
                        (if (needsScan) !controller.hasScanPermission() else !controller.hasConnectPermission()) -> {
                            val required = if (Build.VERSION.SDK_INT >= 31) {
                                if (needsScan) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                                else arrayOf(Manifest.permission.BLUETOOTH_CONNECT)
                            } else {
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                            permissions.launch(required)
                        }
                        !controller.enabled() -> {
                            enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                        }
                        else -> {
                            pendingAction = null
                            action()
                        }
                    }
                }

                val exportFile = rememberLauncherForActivityResult(
                    ActivityResultContracts.CreateDocument("application/json")
                ) { uri ->
                    if (uri != null) {
                        exportMessage = runCatching {
                            contentResolver.openOutputStream(uri)?.use { output ->
                                output.write(events.toJson().toByteArray(Charsets.UTF_8))
                            } ?: error("Não foi possível abrir o arquivo")
                            "Relatório salvo com sucesso"
                        }.getOrElse { "Falha ao exportar: ${it.message ?: "erro desconhecido"}" }
                    }
                }

                SinalApp(
                    controller = controller,
                    updater = updater,
                    events = events,
                    exportMessage = exportMessage,
                    dismissExportMessage = { exportMessage = null },
                    startScan = { mode -> withBluetoothPermission(needsScan = true) { controller.startScan(mode) } },
                    requestConnect = { device -> withBluetoothPermission { controller.connectGatt(device) } },
                    requestPair = { device -> withBluetoothPermission { controller.pair(device) } },
                    requestRefresh = { withBluetoothPermission { controller.refreshBonded() } },
                    requestClassicServices = { device -> withBluetoothPermission { controller.inspectClassicServices(device) } },
                    requestAudioStatus = { device -> withBluetoothPermission { controller.inspectLocalAudio(device) } },
                    requestRssi = { address -> withBluetoothPermission { controller.refreshConnectedRssi(address) } },
                    openBluetoothSettings = { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                    exportHistory = { exportFile.launch("sinal-bt-relatorio.json") }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::controller.isInitialized) controller.refreshBonded()
        if (::updater.isInitialized) updater.checkIfDue()
    }

    override fun onDestroy() {
        if (::controller.isInitialized) controller.close()
        super.onDestroy()
    }
}
