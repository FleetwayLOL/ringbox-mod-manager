package app.ringbox.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Скачивание мода с GameBanana через выбранный канал обхода.
 *
 * Ссылки на файлы берём не из каталога, а из DownloadPage конкретного мода —
 * так они всегда актуальные. Если канал не работает, кидаем ModNetworkException
 * и UI предлагает установить мод из файла.
 */
class ModNetworkException(message: String) : Exception(message)

object ModDownloader {

    data class ModFile(val name: String, val url: String, val size: Long)

    private fun client(mode: BypassMode, proxy: String, mirror: String): OkHttpClient =
        Bypass.client(mode, proxy, mirror)

    suspend fun files(
        modId: Int,
        mode: BypassMode,
        proxy: String = "",
        mirror: String = "",
    ): List<ModFile> = withContext(Dispatchers.IO) {
        val api = "https://gamebanana.com/apiv11/Mod/$modId/DownloadPage"
        var lastErr = "нет попыток"
        for (a in Bypass.attempts(api, mode, proxy, mirror)) {
            try {
                val req = Request.Builder().url(a.url).header("User-Agent", Bypass.UA).build()
                a.client.newCall(req).execute().use { r ->
                    val body = r.body?.string().orEmpty()
                    if (r.isSuccessful && body.isNotBlank()) {
                        val parsed = runCatching { parseFiles(body) }.getOrDefault(emptyList())
                        if (parsed.isNotEmpty()) return@withContext parsed
                        lastErr = "у мода нет файлов"
                    } else {
                        lastErr = "${a.label}: HTTP ${r.code}"
                    }
                }
            } catch (e: Exception) {
                lastErr = "${a.label}: ${e.javaClass.simpleName}"
            }
        }
        throw ModNetworkException("Не достучались до GameBanana ($lastErr)")
    }

    fun parseFiles(json: String): List<ModFile> = try {
        val o = JSONObject(json)
        val arr = o.optJSONArray("_aFiles") ?: throw ModNetworkException("в ответе нет файлов")
        (0 until arr.length()).mapNotNull { i ->
            val f = arr.optJSONObject(i) ?: return@mapNotNull null
            val u = f.optString("_sDownloadUrl").ifEmpty { return@mapNotNull null }
            ModFile(
                name = f.optString("_sFile", "mod.zip"),
                url = u,
                size = f.optLong("_nFilesize", 0L),
            )
        }
    } catch (e: ModNetworkException) {
        throw e
    } catch (e: Exception) {
        throw ModNetworkException("Не разобрал ответ GameBanana")
    }

    suspend fun download(
        file: ModFile,
        mode: BypassMode,
        proxy: String = "",
        mirror: String = "",
        onProgress: (Float) -> Unit = {},
    ): ByteArray = withContext(Dispatchers.IO) {
        var lastErr = "нет попыток"
        for (a in Bypass.attempts(file.url, mode, proxy, mirror)) {
        val c = a.client
        try {
            val req = Request.Builder().url(a.url).header("User-Agent", Bypass.UA).build()
            c.newCall(req).execute().use { r ->
                if (!r.isSuccessful) { lastErr = "${a.label}: HTTP ${r.code}"; return@use }
                val body = r.body ?: run { lastErr = "пустой файл"; return@use }
                val total = body.contentLength()
                val out = java.io.ByteArrayOutputStream(
                    if (total > 0) total.toInt() else 1 shl 20
                )
                body.byteStream().use { ins ->
                    val buf = ByteArray(64 * 1024)
                    var read: Int
                    var done = 0L
                    while (ins.read(buf).also { read = it } > 0) {
                        out.write(buf, 0, read)
                        done += read
                        if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
                onProgress(1f)
                return@withContext out.toByteArray()
            }
        } catch (e: Exception) {
            lastErr = "${a.label}: ${e.javaClass.simpleName}"
        }
        }
        throw ModNetworkException("Файл не скачался ($lastErr)")
    }
}
