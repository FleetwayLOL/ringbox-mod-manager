package app.ringbox.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import java.io.File

/**
 * Хранилище папки игры: два бэкенда под одним API.
 *
 *  1. File-бэкенд — работает, если выдано «Доступ ко всем файлам» (MANAGE_EXTERNAL_STORAGE).
 *     Тогда мы можем САМИ найти /storage/emulated/0/RSDK/v5 и писать туда напрямую.
 *     Это основной путь: SAF-папка после перезапуска отваливается, а путь — нет.
 *
 *  2. SAF-бэкенд — запасной, если доступ ко всем файлам не дали. Папку выбирает юзер,
 *     права persistable, но некоторые прошивки их теряют.
 */
class GameStorage(private val ctx: Context) {

    companion object {
        const val RSDK_FILE = "Data.rsdk"
        const val MODS_DIR = "mods"
        private const val PREFS = "ringbox_storage"
        private const val KEY_PATH = "game_path"
        private const val KEY_TREE = "tree_uri"

        /** Куда обычно ставят декомпиляцию RSDKv5. */
        fun candidates(): List<File> {
            val ext = Environment.getExternalStorageDirectory() ?: File("/storage/emulated/0")
            val bases = listOf(
                ext, File("/sdcard"), File("/storage/emulated/0"),
                File(ext, "Download"), File(ext, "Documents"), File(ext, "Games"),
                File(ext, "SonicMania"), File(ext, "Android/data"),
            ).distinct()
            val out = LinkedHashSet<File>()
            for (b in bases) {
                out += File(b, "RSDK/v5")
                out += File(b, "RSDKv5")
                out += File(b, "SonicMania/RSDK/v5")
            }
            return out.toList()
        }
    }

    // ---------- доступ ----------

    fun hasAllFilesAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager()
        else true

    fun allFilesIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .setData(Uri.parse("package:${ctx.packageName}"))
        } else {
            Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${ctx.packageName}"))
        }

    // ---------- выбор папки ----------

    fun savedPath(): File? = prefs().getString(KEY_PATH, null)?.let { File(it) }
    fun savedTree(): Uri? = prefs().getString(KEY_TREE, null)?.let(Uri::parse)

    fun setPath(dir: File) {
        prefs().edit().putString(KEY_PATH, dir.absolutePath).commit()
    }

    fun setTree(uri: Uri) {
        runCatching {
            ctx.contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }.onFailure {
            runCatching {
                ctx.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        prefs().edit().putString(KEY_TREE, uri.toString()).commit()
    }

    fun forget() {
        savedTree()?.let { uri ->
            runCatching {
                ctx.contentResolver.releasePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        prefs().edit().remove(KEY_PATH).remove(KEY_TREE).commit()
    }

    /**
     * Автопоиск: сначала сохранённый путь (если он ещё валиден), потом обход кандидатов,
     * потом один уровень вглубь по папкам внешнего хранилища.
     */
    fun autoFind(): File? {
        if (!hasAllFilesAccess()) return null

        savedPath()?.let { if (isGameDir(it)) return it }

        candidates().forEach { if (isGameDir(it)) return it }

        // один уровень вглубь: /storage/emulated/0/<dir>/RSDK/v5 и <dir> сам как папка игры
        val roots = listOf(
            Environment.getExternalStorageDirectory(),
            File("/storage/emulated/0"),
        ).filterNotNull().distinct()
        for (r in roots) {
            val children = runCatching { r.listFiles()?.filter { it.isDirectory } }.getOrNull() ?: continue
            for (c in children) {
                if (c.name.startsWith("Android") || c.name.startsWith(".")) continue
                if (isGameDir(c)) return c
                val deep = File(c, "RSDK/v5")
                if (isGameDir(deep)) return deep
            }
        }
        return null
    }

    fun isGameDir(dir: File): Boolean = File(dir, RSDK_FILE).isFile

    // ---------- описание состояния ----------

    data class State(val access: Boolean, val location: String, val hasGameData: Boolean, val mods: List<String>)

    fun describe(): String? = savedPath()?.absolutePath ?: savedTree()?.lastPathSegment?.replace("%2F", "/")

    fun hasGameData(): Boolean = runCatching {
        savedPath()?.let { return@runCatching isGameDir(it) }
        safRoot()?.findFile(RSDK_FILE) != null
    }.getOrDefault(false)

    fun mods(): List<String> = runCatching {
        savedPath()?.let { p ->
            val d = File(p, MODS_DIR)
            return@runCatching d.listFiles()?.filter { it.isDirectory }?.mapNotNull { it.name }
                ?.sortedBy { it.lowercase() } ?: emptyList()
        }
        val modsDir = safRoot()?.findFile(MODS_DIR)?.takeIf { it.isDirectory } ?: return@runCatching emptyList()
        modsDir.listFiles().filter { it.isDirectory }.mapNotNull { it.name }.sortedBy { it.lowercase() }
    }.getOrDefault(emptyList())

    fun state(): State = State(
        access = hasAllFilesAccess() || hasTreeAccess(),
        location = describe() ?: "",
        hasGameData = hasGameData(),
        mods = mods(),
    )

    fun hasTreeAccess(): Boolean {
        val uri = savedTree() ?: return false
        return ctx.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
    }

    // ---------- запись ----------

    /** Кладёт один мод в mods/<name>/ со всеми вложенными путями. Возвращает число файлов. */
    fun writeMod(name: String, files: Map<String, ByteArray>): Int {
        val safe = name.replace(Regex("""[\\/:*?"<>|]"""), "_").trim().trim('.').take(64).ifBlank { "mod" }
        return savedPath()?.let { p -> writeModFile(File(p, MODS_DIR), safe, files) }
            ?: safRoot()?.let { root ->
                val modsDir = root.findFile(MODS_DIR)?.takeIf { it.isDirectory }
                    ?: root.createDirectory(MODS_DIR) ?: return 0
                writeModSaf(modsDir, safe, files)
            }
            ?: 0
    }

    private fun writeModFile(modsDir: File, name: String, files: Map<String, ByteArray>): Int {
        val dir = File(modsDir, name)
        if (!dir.exists() && !dir.mkdirs()) return 0
        var n = 0
        for ((rel, bytes) in files) {
            val f = File(dir, rel)
            f.parentFile?.mkdirs()
            runCatching { f.writeBytes(bytes); n++ }
        }
        return n
    }

    private fun writeModSaf(modsDir: DocumentFile, name: String, files: Map<String, ByteArray>): Int {
        val dir = modsDir.findFile(name)?.takeIf { it.isDirectory } ?: modsDir.createDirectory(name) ?: return 0
        var n = 0
        for ((rel, bytes) in files) {
            val parts = rel.split('/').filter { it.isNotEmpty() }
            if (parts.isEmpty()) continue
            var cur = dir
            for (i in 0 until parts.size - 1) {
                cur = cur.findFile(parts[i])?.takeIf { it.isDirectory }
                    ?: cur.createDirectory(parts[i]) ?: break
            }
            val fileName = parts.last()
            val f = cur.findFile(fileName)?.takeIf { it.isFile }
                ?: cur.createFile("application/octet-stream", fileName) ?: continue
            runCatching {
                ctx.contentResolver.openOutputStream(f.uri, "wt")?.use { it.write(bytes) }
                n++
            }
        }
        return n
    }

    private fun safRoot(): DocumentFile? = savedTree()?.let { DocumentFile.fromTreeUri(ctx, it) }

    private fun prefs() = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
