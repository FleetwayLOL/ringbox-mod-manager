package app.ringbox.data

import android.content.Context
import android.net.Uri
import java.util.zip.ZipInputStream

/**
 * Установка мода из ZIP — работает без сети.
 *
 * В РФ GameBanana режется, поэтому основной путь такой: скачал мод где угодно
 * (браузер с VPN, с другого устройства) и скормил файл приложению.
 *
 * Поддерживаемые раскладки:
 *   mod.ini                       → один мод, имя = имя файла
 *   MyMod/mod.ini                 → один мод, имя = папка
 *   MyMod/mod.ini + Other/mod.ini → несколько модов из одного архива
 *   __MACOSX/ и .DS_Store игнорируются
 */
object ModInstaller {

    private const val MAX_BYTES = 768L * 1024 * 1024
    private const val MOD_INI = "mod.ini"

    data class Result(val ok: Boolean, val mods: List<String>, val message: String)

    fun installFromZip(ctx: Context, storage: GameStorage, zipUri: Uri): Result {
        val bytes = runCatching { ctx.contentResolver.openInputStream(zipUri)?.use { it.readBytes() } }
            .getOrNull() ?: return Result(false, emptyList(), "Не удалось открыть файл")
        return installFromBytes(ctx, storage, bytes, zipName(ctx, zipUri))
    }

    fun installFromBytes(ctx: Context, storage: GameStorage, bytes: ByteArray, zipName: String): Result {
        if (bytes.isEmpty()) return Result(false, emptyList(), "Пустой файл")
        if (bytes.size > MAX_BYTES) return Result(false, emptyList(), "Файл больше 768 МБ")

        val files = LinkedHashMap<String, ByteArray>()
        try {
            ZipInputStream(bytes.inputStream().buffered()).use { z ->
                while (true) {
                    val e = z.nextEntry ?: break
                    val path = e.name.replace('\\', '/')
                    if (e.isDirectory || isJunk(path)) { z.closeEntry(); continue }
                    files[path] = z.readBytes()
                    z.closeEntry()
                }
            }
        } catch (e: Exception) {
            return Result(false, emptyList(), "Битый архив: ${e.javaClass.simpleName}")
        }
        if (files.isEmpty()) return Result(false, emptyList(), "В архиве нет файлов")

        val base = zipName.substringBeforeLast('.').ifBlank { "Мод" }
        val groups = group(files, base)
        val installed = mutableListOf<String>()
        for ((name, content) in groups) {
            val n = storage.writeMod(name, content)
            if (n > 0) installed += name
        }
        if (installed.isEmpty()) {
            return Result(false, emptyList(), "Не получилось записать в mods/ — проверь доступ к папке")
        }
        val msg = if (installed.size == 1) "Установлен: ${installed.first()}"
        else "Установлено модов: ${installed.size}"
        return Result(true, installed, msg)
    }

    /** Разбивает содержимое архива на группы «мод → файлы». */
    fun group(files: Map<String, ByteArray>, base: String): Map<String, Map<String, ByteArray>> {
        val inis = files.keys.filter { it == MOD_INI || it.endsWith("/$MOD_INI") }
        if (inis.isEmpty()) return mapOf(base to files)

        if (inis.size == 1) {
            val ini = inis.first()
            if (ini == MOD_INI) return mapOf(base to files)
            val prefix = ini.substringBeforeLast('/') + "/"
            val name = prefix.trimEnd('/').substringAfterLast('/').ifBlank { base }
            val content = files.filterKeys { it.startsWith(prefix) }
                .mapKeys { it.key.removePrefix(prefix) }
                .filterKeys { it.isNotEmpty() }
            return mapOf(name to content)
        }

        // несколько модов в одном архиве
        return inis.associate { ini ->
            val dir = ini.substringBeforeLast('/', "")
            val name = if (dir.isEmpty()) base else dir.substringAfterLast('/')
            val prefix = if (dir.isEmpty()) "" else "$dir/"
            name to files.filterKeys { it.startsWith(prefix) }
                .mapKeys { it.key.removePrefix(prefix) }
                .filterKeys { it.isNotEmpty() }
        }
    }

    private fun isJunk(path: String): Boolean {
        val p = path.lowercase()
        return p.startsWith("__macosx/") || p.contains("/__macosx/") ||
            p.endsWith(".ds_store") || p.substringAfterLast('/') == "thumbs.db" ||
            p.substringAfterLast('/').startsWith("._")
    }

    private fun zipName(ctx: Context, uri: Uri): String = runCatching {
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val i = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (i >= 0 && c.moveToFirst()) c.getString(i) else null
        }
    }.getOrNull() ?: "mod.zip"
}
