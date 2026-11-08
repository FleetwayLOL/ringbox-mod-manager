package app.ringbox.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/**
 * Работа с папкой игры через SAF.
 *
 * Данные декомпиляции RSDKv5 на Android лежат в /storage/emulated/0/RSDK/v5/:
 *   Data.rsdk      — сами данные игры
 *   mods/<Имя>/    — мод + mod.ini (TargetVersion=5)
 *   *.sav          — сейвы
 *
 * Мы НЕ просим MANAGE_EXTERNAL_STORAGE: только дерево, которое выбрал юзер.
 */
class SafeStorage(private val context: Context) {

    companion object {
        const val RSDK_FILE = "Data.rsdk"
        const val MODS_DIR = "mods"
        private const val PREFS = "ringbox_prefs"
        private const val KEY_TREE = "tree_uri"
    }

    fun savedTree(): Uri? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_TREE, null)?.let(Uri::parse)

    fun saveTree(uri: Uri) {
        // Берём persistable-права, иначе после перезапуска папка «отвалится».
        // На части прошивок и в части файловых провайдеров это кидает SecurityException —
        // поэтому глушим: путь всё равно сохраняем, иначе приложение падало бы при выборе папки.
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }.onFailure {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_TREE, uri.toString()).commit()
    }

    /** Права на папку ещё живы (проверка после перезапуска). */
    fun hasPersistedAccess(): Boolean {
        val uri = savedTree() ?: return false
        return context.contentResolver.persistedUriPermissions.any { it.uri == uri && it.isReadPermission }
    }

    fun hasGameDataSafe(): Boolean = runCatching { hasGameData() }.getOrDefault(false)

    fun forgetTree() {
        savedTree()?.let { uri ->
            runCatching {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_TREE).apply()
    }

    /** Корень выбранного дерева, если права ещё живы. */
    fun root(): DocumentFile? = savedTree()?.let { DocumentFile.fromTreeUri(context, it) }

    fun hasGameData(): Boolean = root()?.findFile(RSDK_FILE) != null

    /** Все папки внутри mods/ — по одной на мод. */
    fun installedMods(): List<DocumentFile> {
        val mods = root()?.findFile(MODS_DIR)?.takeIf { it.isDirectory } ?: return emptyList()
        return mods.listFiles()
            .filter { it.isDirectory }
            .sortedBy { it.name?.lowercase() ?: "" }
    }

    /** Папка конкретного мода (создаём, если нет). */
    fun modDir(name: String, create: Boolean = false): DocumentFile? {
        val mods = root()?.findFile(MODS_DIR)?.takeIf { it.isDirectory } ?: return null
        mods.findFile(name)?.let { return it }
        if (!create) return null
        return mods.createDirectory(name)
    }

    /** Порядок загрузки: движок читает mods/ снизу вверх (позже = ниже = важнее). */
    fun loadOrder(): List<String> = installedMods().mapNotNull { it.name }

    fun writable(): Boolean = root()?.canWrite() == true
}
