package br.com.vertice81.sinalbt.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.sin

/**
 * Controle somente da sessão de mídia deste telefone.
 * A saída Bluetooth é escolhida pelo Android/usuário; nenhuma conexão é forçada.
 * O som de teste é gerado localmente e não usa microfone.
 */
class MediaControlManager(context: Context) {
    data class MediaStatus(
        val outputNames: List<String> = emptyList(),
        val hasBluetoothOutput: Boolean = false,
        val volumePercent: Int = 0,
        val testing: Boolean = false,
        val routeAfterTest: String? = null,
        val message: String = "Atualize as saídas para verificar o áudio deste celular."
    )

    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var testing = false
    var status by mutableStateOf(MediaStatus())
        private set

    init { refresh() }

    fun refresh() {
        val outputs = runCatching {
            audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                .filter { it.isBluetoothAudio() }
                .map { it.productName?.toString()?.takeIf(String::isNotBlank) ?: "Áudio Bluetooth" }
                .distinct()
        }.getOrDefault(emptyList())
        val level = runCatching {
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            (audio.getStreamVolume(AudioManager.STREAM_MUSIC) * 100 / max).coerceIn(0,100)
        }.getOrDefault(0)
        status = status.copy(
            outputNames = outputs,
            hasBluetoothOutput = outputs.isNotEmpty(),
            volumePercent = level,
            message = when {
                outputs.isEmpty() -> "Nenhuma saída de áudio Bluetooth disponível neste celular. Conecte-a nas configurações."
                else -> "Saída Bluetooth disponível. O som será enviado ao dispositivo selecionado como saída de mídia no Android."
            }
        )
    }

    fun mediaCommand(keyCode: Int) {
        if (keyCode !in setOf(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS)) return
        runCatching {
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
            status = status.copy(message = "Comando enviado à sessão de mídia deste celular. O player pode ignorá-lo.")
        }.onFailure {
            status = status.copy(message = "Não foi possível enviar o comando: ${it.message ?: "indisponível"}")
        }
    }

    fun setVolumePercent(percent: Int) {
        val safe = percent.coerceIn(0, 100)
        runCatching {
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, (max * safe / 100f).toInt(), 0)
            refresh()
        }.onFailure {
            status = status.copy(message = "Não foi possível ajustar o volume de mídia.")
        }
    }

    fun playTestTone() {
        if (testing) return
        testing = true
        status = status.copy(testing = true, message = "Reproduzindo som de teste pelo caminho de mídia do Android…")
        thread(name = "sinal-bt-tone") {
            var track: AudioTrack? = null
            try {
                val rate = 44100
                val length = rate * 3 / 4
                val samples = ShortArray(length) { i ->
                    val ramp = minOf(1.0, i / 1800.0, (length - i) / 1800.0).coerceAtLeast(0.0)
                    (sin(2 * PI * 660.0 * i / rate) * ramp * 0.12 * Short.MAX_VALUE).toInt().toShort()
                }
                track = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(samples.size * 2)
                    .build()
                if (track.state != AudioTrack.STATE_INITIALIZED) error("Saída de áudio não inicializada")
                val written = track.write(samples, 0, samples.size)
                if (written <= 0) error("O Android não aceitou os dados de áudio")
                track.play()
                Thread.sleep(160L)
                val route = track.routedDevice?.productName?.toString()?.ifBlank { "Saída de mídia" }
                Thread.sleep(670L)
                main.post {
                    status = status.copy(
                        testing = false,
                        routeAfterTest = route,
                        message = if (route == null) "Teste reproduzido. O Android não informou a rota utilizada."
                                  else "Som de teste enviado pela rota informada pelo Android: $route"
                    )
                    refresh()
                }
            } catch (e: Exception) {
                main.post { status = status.copy(testing = false, message = "Teste de áudio indisponível: ${e.message ?: "erro"}") }
            } finally {
                runCatching { track?.stop() }
                runCatching { track?.release() }
                testing = false
            }
        }
    }

    private fun AudioDeviceInfo.isBluetoothAudio(): Boolean =
        type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
        type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
        (Build.VERSION.SDK_INT >= 31 &&
            (type == AudioDeviceInfo.TYPE_BLE_HEADSET || type == AudioDeviceInfo.TYPE_BLE_SPEAKER))
}
