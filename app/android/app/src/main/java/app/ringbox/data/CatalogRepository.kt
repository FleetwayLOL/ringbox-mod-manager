package app.ringbox.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Каталог модов: тянем свежий список с GameBanana через каналы обхода,
 * кэшируем JSON и обложки на диск — тогда при отсутствии сети показываем кэш,
 * а не 12 зашитых модов.
 */
object CatalogRepository {

    private const val GAME_ID = 6045
    const val PAGE_SIZE = 30
    private const val PER_PAGE = PAGE_SIZE
    private const val CACHE_JSON = "catalog.json"

    data class RemoteMod(
        val id: Int,
        val title: String,
        val author: String,
        val likes: Int,
        val category: String,
        val tag: String,
        val imgUrl: String?,
    )

    data class Loaded(val mods: List<RemoteMod>, val channel: String, val fromCache: Boolean)

    private fun url(page: Int) =
        "https://gamebanana.com/apiv11/Mod/Index?_aFilters%5BGeneric_Game%5D=$GAME_ID" +
            "&_nPerpage=$PER_PAGE&_nPage=$page"

    /** Свежий список. Пробуем каналы по очереди; что сработало — тем и качаем обложки. */
    suspend fun fetch(
        page: Int = 1,
        mode: BypassMode,
        proxy: String = "",
        mirror: String = "",
    ): Pair<List<RemoteMod>, String> = withContext(Dispatchers.IO) {
        var lastErr = "нет попыток"
        for (a in Bypass.attempts(url(page), mode, proxy, mirror)) {
            try {
                val req = Request.Builder().url(a.url).header("User-Agent", Bypass.UA).build()
                a.client.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) { lastErr = "${a.label}: HTTP ${r.code}"; return@use }
                    val body = r.body?.string().orEmpty()
                    val parsed = runCatching { parse(body) }.getOrDefault(emptyList())
                    if (parsed.isNotEmpty()) return@withContext parsed to a.label
                    lastErr = "${a.label}: пусто"
                }
            } catch (e: Exception) {
                lastErr = "${a.label}: ${e.javaClass.simpleName}"
            }
        }
        throw ModNetworkException(lastErr)
    }

    fun parse(json: String): List<RemoteMod> = parseRecords(json, null)

    /** Поиск GameBanana может вернуть моды других игр — оставляем только Sonic Mania. */
    fun parseSearch(json: String): List<RemoteMod> = parseRecords(json, GAME_ID)

    private fun parseRecords(json: String, requiredGameId: Int?): List<RemoteMod> {
        val root = JSONObject(json)
        val recs = root.optJSONArray("_aRecords") ?: return emptyList()
        val out = ArrayList<RemoteMod>(recs.length())
        for (i in 0 until recs.length()) {
            val r = recs.optJSONObject(i) ?: continue
            if (requiredGameId != null && r.optJSONObject("_aGame")?.optInt("_idRow") != requiredGameId) continue
            val id = r.optInt("_idRow")
            val title = r.optString("_sName")
            if (title.isBlank() || id == 0) continue
            val author = r.optJSONObject("_aSubmitter")?.optString("_sName").orEmpty()
            val likes = r.optInt("_nLikeCount", 0)
            val cat = r.optJSONObject("_aRootCategory")?.optString("_sName").orEmpty()
            val tags = r.optJSONArray("_aTags")
            var tag = cat
            if (tags != null && tags.length() > 0) {
                tag = tags.optJSONObject(0)?.optString("_sName").orEmpty().ifBlank { cat }
            }
            val img = r.optJSONObject("_aPreviewMedia")
                ?.optJSONArray("_aImages")
                ?.optJSONObject(0)?.let { im ->
                    val base = im.optString("_sBaseUrl")
                    val file = im.optString("_sFile100").ifBlank { im.optString("_sFile") }
                    if (base.isBlank() || file.isBlank()) null else "$base/$file"
                }
            out += RemoteMod(id, title, author, likes, cat, tag.ifBlank { "Мод" }, img)
        }
        return out
    }

    /** Онлайн-поиск модов по всей GameBanana, но в выдаче только игра 6045. */
    suspend fun search(
        query: String,
        mode: BypassMode,
        proxy: String = "",
        mirror: String = "",
    ): Pair<List<RemoteMod>, String> = withContext(Dispatchers.IO) {
        val encoded = java.net.URLEncoder.encode(query.trim(), "UTF-8")
        val target = "https://gamebanana.com/apiv11/Util/Search/Results" +
            "?_sModelName=Mod&_idGameRow=$GAME_ID&_nPerpage=$PER_PAGE&_nPage=1&_sSearchString=$encoded"
        var lastErr = "нет попыток"
        for (a in Bypass.attempts(target, mode, proxy, mirror)) {
            try {
                val req = Request.Builder().url(a.url).header("User-Agent", Bypass.UA).build()
                a.client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) {
                        lastErr = "${a.label}: HTTP ${response.code}"
                        return@use
                    }
                    val body = response.body?.string().orEmpty()
                    if (body.isBlank()) {
                        lastErr = "${a.label}: пустой ответ"
                        return@use
                    }
                    val root = JSONObject(body)
                    if (!root.has("_aRecords")) {
                        lastErr = "${a.label}: нет результатов поиска"
                        return@use
                    }
                    return@withContext parseSearch(body) to a.label
                }
            } catch (e: Exception) {
                lastErr = "${a.label}: ${e.javaClass.simpleName}"
            }
        }
        throw ModNetworkException(lastErr)
    }

    // ---------- кэш ----------

    fun cacheSave(ctx: Context, mods: List<RemoteMod>) {
        runCatching {
            val arr = org.json.JSONArray()
            mods.forEach { m ->
                arr.put(JSONObject().apply {
                    put("id", m.id); put("title", m.title); put("author", m.author)
                    put("likes", m.likes); put("category", m.category); put("tag", m.tag)
                    put("img", m.imgUrl ?: "")
                })
            }
            java.io.File(ctx.filesDir, CACHE_JSON).writeText(arr.toString())
        }
    }

    fun cacheLoad(ctx: Context): List<RemoteMod>? = runCatching {
        val f = java.io.File(ctx.filesDir, CACHE_JSON)
        if (!f.exists()) return null
        val arr = org.json.JSONArray(f.readText())
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            RemoteMod(
                o.optInt("id"), o.optString("title"), o.optString("author"),
                o.optInt("likes"), o.optString("category"), o.optString("tag"),
                o.optString("img").ifBlank { null }
            )
        }.takeIf { it.isNotEmpty() }
    }.getOrNull()

    // ---------- обложки ----------

    private val imgClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
            .callTimeout(8, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    // Не даём прокрутке запускать десятки одновременных загрузок обложек на слабом телефоне.
    private val coverSlots = java.util.concurrent.Semaphore(3, true)

    /** Обложка в кэш на диск, чтобы не дёргать сеть повторно. */
    fun coverFile(ctx: Context, url: String): java.io.File =
        java.io.File(ctx.cacheDir, "cover_" + url.hashCode().toUInt().toString(16) + ".img")

    fun loadCover(ctx: Context, url: String): ByteArray? {
        val f = coverFile(ctx, url)
        if (f.exists() && f.length() > 0) return runCatching { f.readBytes() }.getOrNull()
        var acquired = false
        return try {
            coverSlots.acquire()
            acquired = true
            // Другая видимая карточка могла успеть загрузить эту же обложку.
            if (f.exists() && f.length() > 0) return f.readBytes()
            val req = Request.Builder().url(url).header("User-Agent", Bypass.UA).build()
            imgClient.newCall(req).execute().use { r ->
                if (!r.isSuccessful) return null
                val b = r.body?.bytes() ?: return null
                runCatching { f.writeBytes(b) }
                b
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (e: Exception) {
            null
        } finally {
            if (acquired) coverSlots.release()
        }
    }
}
