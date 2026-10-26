package br.com.vertice81.sinalbt.updates

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import br.com.vertice81.sinalbt.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import kotlin.concurrent.thread

/**
 * Atualizações oficiais pelo GitHub Releases de um repositório PÚBLICO separado.
 * Sem tokens embutidos, instalação invisível, serviços persistentes ou permissões privilegiadas.
 * A assinatura do APK é verificada pelo sistema Android durante a atualização.
 */
class GithubUpdateManager(private val activity: Activity) {
    enum class Phase { IDLE, CHECKING, CURRENT, AVAILABLE, DOWNLOADING, VERIFYING, READY, ERROR }

    data class State(
        val phase: Phase = Phase.IDLE,
        val message: String = "",
        val availableVersion: String? = null,
        val progress: Int? = null
    )

    var state by mutableStateOf(State())
        private set

    private data class Release(
        val version: String,
        val url: String,
        val expectedSha256: String?
    )

    private val ui = Handler(Looper.getMainLooper())
    private val prefs = activity.getSharedPreferences("sinal_updates", Context.MODE_PRIVATE)
    private val manager = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val repo = BuildConfig.UPDATES_REPO
    private var available: Release? = null
    private var validationStarted = false

    fun checkIfDue() {
        refreshDownload()
        if (state.phase in listOf(Phase.DOWNLOADING, Phase.VERIFYING, Phase.READY, Phase.AVAILABLE)) return
        if (System.currentTimeMillis() - prefs.getLong("last_check", 0L) > 12 * 60 * 60 * 1000L) {
            checkNow()
        }
    }

    fun checkNow() {
        if (state.phase == Phase.CHECKING) return
        state = State(Phase.CHECKING, "Consultando versões publicadas no GitHub…")
        thread(name = "sinal-github-releases") {
            var conn: HttpURLConnection? = null
            try {
                val url = URL("https://api.github.com/repos/$repo/releases/latest")
                conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = 12000
                    readTimeout = 12000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "SINAL-BT-Android/${BuildConfig.VERSION_NAME}")
                }
                val code = conn.responseCode
                if (code == 404) throw IllegalStateException("O repositório público de atualizações ou a primeira versão oficial ainda não existe.")
                if (code != 200) throw IllegalStateException("Resposta do GitHub: HTTP $code")
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val obj = JSONObject(body)
                val tag = obj.optString("tag_name").removePrefix("v")
                val comparison = compareVersions(tag, BuildConfig.VERSION_NAME)
                    ?: throw IllegalStateException("Versão publicada inválida: $tag")
                prefs.edit().putLong("last_check", System.currentTimeMillis()).apply()
                if (comparison <= 0) {
                    available = null
                    ui.post { state = State(Phase.CURRENT, "Você já utiliza a versão mais recente (${BuildConfig.VERSION_NAME}).") }
                    return@thread
                }
                val assets = obj.getJSONArray("assets")
                var found: Release? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.optString("name")
                    val link = asset.optString("browser_download_url")
                    val uri = Uri.parse(link)
                    val path = uri.pathSegments
                    val validLocation = uri.scheme == "https" && uri.host.equals("github.com", true) &&
                        path.size == 6 && path[0].equals("joaoguirqueiroz", true) &&
                        path[1].equals("SINALBT", true) && path[2] == "releases" &&
                        path[3] == "download" && path[4] == obj.optString("tag_name")
                    if (name.startsWith("SINAL-BT-") && name.endsWith(".apk", true) &&
                        validLocation && asset.optLong("size") > 100_000L) {
                        val digest = asset.optString("digest")
                        found = Release(tag, link, digest.removePrefix("sha256:").lowercase(Locale.ROOT)
                            .takeIf { digest.startsWith("sha256:") && it.matches(Regex("[a-f0-9]{64}")) })
                        break
                    }
                }
                if (found == null) throw IllegalStateException("A versão publicada não contém um APK oficial do SINAL BT.")
                available = found
                ui.post { state = State(Phase.AVAILABLE, "Versão ${found.version} disponível para atualização.", found.version) }
            } catch (ex: Exception) {
                ui.post { state = State(Phase.ERROR, ex.message ?: "Falha ao consultar as atualizações.") }
            } finally {
                conn?.disconnect()
            }
        }
    }

    fun download() {
        val release = available ?: return
        if (state.phase != Phase.AVAILABLE) return
        try {
            val request = DownloadManager.Request(Uri.parse(release.url))
                .setTitle("SINAL BT ${release.version}")
                .setDescription("Atualização oficial do aplicativo")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .setDestinationInExternalFilesDir(
                    activity, Environment.DIRECTORY_DOWNLOADS, "sinal-bt-${release.version}-${System.currentTimeMillis()}.apk"
                )
            val id = manager.enqueue(request)
            prefs.edit()
                .putLong("download_id", id)
                .putString("download_version", release.version)
                .putString("download_sha256", release.expectedSha256 ?: "")
                .apply()
            state = State(Phase.DOWNLOADING, "Baixando a versão ${release.version}…", release.version)
        } catch (ex: Exception) {
            state = State(Phase.ERROR, "Não foi possível iniciar o download: ${ex.message}")
        }
    }

    fun refreshDownload() {
        val id = prefs.getLong("download_id", -1L)
        if (id <= 0 || validationStarted) return
        val cursor = try { manager.query(DownloadManager.Query().setFilterById(id)) }
            catch (ex: Exception) { null }
        if (cursor == null) return
        cursor.use {
            if (!it.moveToFirst()) {
                clearDownload()
                return
            }
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val version = prefs.getString("download_version", null)
            when (status) {
                DownloadManager.STATUS_PENDING, DownloadManager.STATUS_PAUSED, DownloadManager.STATUS_RUNNING -> {
                    val bytes = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                    val progress = if (total > 0) ((100L * bytes / total).coerceIn(0, 100)).toInt() else null
                    state = State(Phase.DOWNLOADING, "Download em andamento…", version, progress)
                }
                DownloadManager.STATUS_SUCCESSFUL -> validateDownloaded(id, version)
                DownloadManager.STATUS_FAILED -> {
                    clearDownload()
                    state = State(Phase.ERROR, "O download falhou. Consulte sua conexão e tente novamente.")
                }
            }
        }
    }

    private fun validateDownloaded(id: Long, version: String?) {
        if (validationStarted || state.phase == Phase.READY) return
        validationStarted = true
        state = State(Phase.VERIFYING, "Conferindo integridade do arquivo recebido…", version)
        thread(name = "sinal-verify-apk") {
            try {
                val uri = manager.getUriForDownloadedFile(id) ?: error("Download sem arquivo acessível")
                val bytes = MessageDigest.getInstance("SHA-256")
                activity.contentResolver.openInputStream(uri)?.use { stream ->
                    val buffer = ByteArray(16_384)
                    while (true) {
                        val n = stream.read(buffer)
                        if (n == -1) break
                        bytes.update(buffer, 0, n)
                    }
                } ?: error("Não foi possível ler o arquivo recebido")
                val actual = bytes.digest().joinToString("") { "%02x".format(it) }
                val expected = prefs.getString("download_sha256", "").orEmpty()
                if (expected.isNotEmpty() && !actual.equals(expected, ignoreCase = true)) {
                    manager.remove(id)
                    throw IllegalStateException("Verificação SHA-256 falhou. O arquivo foi descartado.")
                }
                ui.post { state = State(Phase.READY, "Atualização validada e pronta para instalar.", version) }
            } catch (ex: Exception) {
                ui.post {
                    clearDownload()
                    state = State(Phase.ERROR, ex.message ?: "Falha na verificação do arquivo")
                }
            } finally { validationStarted = false }
        }
    }

    fun installDownloaded() {
        if (state.phase != Phase.READY) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !activity.packageManager.canRequestPackageInstalls()) {
                activity.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${activity.packageName}"))
                )
                state = state.copy(message = "Autorize a instalação por este aplicativo nas configurações e toque novamente em Instalar.")
                return
            }
            val id = prefs.getLong("download_id", -1L)
            val apkUri = manager.getUriForDownloadedFile(id) ?: error("Arquivo de atualização indisponível")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(intent)
        } catch (ex: Exception) {
            state = state.copy(phase = Phase.ERROR, message = "Falha ao abrir instalador: ${ex.message}")
        }
    }

    private fun clearDownload() {
        prefs.edit().remove("download_id").remove("download_version").remove("download_sha256").apply()
    }

    companion object {
        /** Somente números de versões estritas vMAJOR.MINOR.PATCH são aceitos. */
        fun compareVersions(remote: String, current: String): Int? {
            val p = Regex("^(?:v)?(\\d+)\\.(\\d+)\\.(\\d+)$")
            val a = p.matchEntire(remote)?.groupValues?.drop(1)?.map { it.toIntOrNull() ?: return null } ?: return null
            val b = p.matchEntire(current)?.groupValues?.drop(1)?.map { it.toIntOrNull() ?: return null } ?: return null
            for (i in 0..2) if (a[i] != b[i]) return a[i].compareTo(b[i])
            return 0
        }
    }
}
