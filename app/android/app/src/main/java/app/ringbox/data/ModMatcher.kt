package app.ringbox.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject

/**
 * Распознавание «чужих» модов: тех, что лежат в mods/, но ставились не через RingBox
 * (руками, из ZIP другого менеджера, с ПК).
 *
 * Ищем каждый по имени папки на GameBanana, фильтруем по игре Sonic Mania (6045),
 * выбираем наиболее похожее название. Результат кэшируем, чтобы не дёргать API повторно.
 */
object ModMatcher {

    private const val GAME_ID = 6045
    private const val CACHE = "modmatch.json"

    data class Match(val remote: CatalogRepository.RemoteMod, val score: Double)

    /** Ищем один мод по имени. null — если на банане такого нет. */
    suspend fun find(
        name: String,
        mode: BypassMode,
        proxy: String = "",
        mirror: String = "",
        ctx2: Context? = null,
        onProgress: (String) -> Unit = {},
    ): Match? = withContext(Dispatchers.IO) {
        val q = cleanQuery(name)
        if (q.isBlank()) return@withContext null
        val url = "https://gamebanana.com/apiv11/Util/Search/Results" +
            "?_sModelName=Mod&_nPerpage=15&_sSearchString=" +
            java.net.URLEncoder.encode(q, "UTF-8")

        var json: String? = null
        for (a in Bypass.attempts(url, mode, proxy, mirror)) {
            try {
                val req = Request.Builder().url(a.url).header("User-Agent", Bypass.UA).build()
                a.client.newCall(req).execute().use { r ->
                    if (r.isSuccessful) {
                        json = r.body?.string()
                        if (!json.isNullOrBlank()) return@use
                    }
                }
                if (!json.isNullOrBlank()) break
            } catch (_: Exception) {
            }
        }
        json?.let { body ->
            parseBest(body, name)?.let { if (it.score >= 0.6) return@withContext it }
        }
        // поиск не справился — ищем по локальному индексу каталога
        val index = ensureIndex(ctx2, mode, proxy, mirror, onProgress)
        bestIn(index, name)
    }

    /** Разбор ответа поиска: только Sonic Mania + лучшее совпадение по названию. */
    fun parseBest(json: String, wanted: String): Match? {
        val arr = runCatching { JSONObject(json).optJSONArray("_aRecords") }.getOrNull() ?: return null
        val want = norm(wanted)
        var best: Match? = null
        for (i in 0 until arr.length()) {
            val r = arr.optJSONObject(i) ?: continue
            val game = r.optJSONObject("_aGame")?.optInt("_idRow") ?: 0
            if (game != GAME_ID) continue
            val title = r.optString("_sName")
            if (title.isBlank()) continue
            val score = similarity(want, norm(title))
            if (score < 0.55) continue
            val img = r.optJSONObject("_aPreviewMedia")
                ?.optJSONArray("_aImages")
                ?.optJSONObject(0)?.let { im ->
                    val base = im.optString("_sBaseUrl")
                    val file = im.optString("_sFile100").ifBlank { im.optString("_sFile") }
                    if (base.isBlank() || file.isBlank()) null else "$base/$file"
                }
            val author = r.optJSONObject("_aSubmitter")?.optString("_sName").orEmpty()
            val cat = r.optJSONObject("_aRootCategory")?.optString("_sName").orEmpty()
            val m = CatalogRepository.RemoteMod(
                id = r.optInt("_idRow"),
                title = title,
                author = author,
                likes = r.optInt("_nLikeCount", 0),
                category = cat,
                tag = cat.ifBlank { "Мод" },
                imgUrl = img,
            )
            if (best == null || score > best!!.score) best = Match(m, score)
        }
        return best
    }

    /** Папки вида "HYPERMANIA_1.2" или "Sonic Mania Randomizer (v3)" → чистое имя. */
    fun cleanQuery(name: String): String {
        var s = name.replace('_', ' ').replace('.', ' ')
        s = s.replace(Regex("""\s*[\(\[]?v?\d+(\.\d+)*[\)\]]?\s*$"""), "")
        s = s.replace(Regex("""\s{2,}"""), " ").trim()
        return s
    }

    private fun norm(s: String): String =
        s.lowercase().replace(Regex("""[^a-zа-яё0-9 ]"""), " ").replace(Regex("""\s+"""), " ").trim()

    /**
     * Сходство названий: насколько полно слова запроса присутствуют в названии мода,
     * плюс небольшая точность. Длинные названия штрафуются — иначе «HYPERMANIA»
     * прилипает к «save file 100% (HYPERMANIA mod save file)».
     */
    fun similarity(query: String, candidate: String): Double {
        if (query.isBlank() || candidate.isBlank()) return 0.0
        val a = norm(query); val b = norm(candidate)
        if (a == b) return 1.0
        val ta = tokens(a); val tb = tokens(b)
        if (ta.isEmpty() || tb.isEmpty()) return 0.0
        val inter = ta.intersect(tb).size.toDouble()
        val coverage = inter / ta.size
        val precision = inter / tb.size
        var sc = coverage * 0.75 + precision * 0.25
        if (tb.size > ta.size * 2 + 2) sc *= 0.6
        return sc
    }

    private fun tokens(s: String) = s.split(' ').filter { it.length > 1 }.toSet()

    // ---------- локальный индекс каталога ----------

    private const val INDEX_FILE = "gb_index.json"
    private const val INDEX_TTL = 24L * 60 * 60 * 1000
    private const val INDEX_PAGES = 10 // 10 × 50 = 500 свежих модов
    @Volatile private var index: List<CatalogRepository.RemoteMod>? = null

    /** Индекс каталога (500 свежих модов игры), с диска или из сети. */
    private suspend fun ensureIndex(
        ctx: Context?,
        mode: BypassMode,
        proxy: String,
        mirror: String,
        onProgress: (String) -> Unit,
    ): List<CatalogRepository.RemoteMod> {
        index?.let { return it }
        if (ctx == null) return emptyList()

        val f = java.io.File(ctx.filesDir, INDEX_FILE)
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < INDEX_TTL) {
            runCatching { parseIndex(f.readText()) }.getOrNull()?.let {
                index = it
                return it
            }
        }

        val acc = LinkedHashMap<Int, CatalogRepository.RemoteMod>()
        for (page in 1..INDEX_PAGES) {
            onProgress("Строим индекс каталога: $page/$INDEX_PAGES")
            val url = "https://gamebanana.com/apiv11/Game/$GAME_ID/Subfeed" +
                "?_nPage=$page&_csvModelInclusions=Mod&_nPerpage=50"
            var pageMods: List<CatalogRepository.RemoteMod> = emptyList()
            for (a in Bypass.attempts(url, mode, proxy, mirror)) {
                try {
                    val req = Request.Builder().url(a.url).header("User-Agent", Bypass.UA).build()
                    a.client.newCall(req).execute().use { r ->
                        if (r.isSuccessful) {
                            val body = r.body?.string().orEmpty()
                            if (body.isNotBlank()) pageMods = CatalogRepository.parse(body)
                        }
                    }
                } catch (_: Exception) {
                }
                if (pageMods.isNotEmpty()) break
            }
            if (pageMods.isEmpty()) break
            pageMods.forEach { acc[it.id] = it }
            delay(200)
        }
        val list = acc.values.toList()
        if (list.isNotEmpty()) {
            index = list
            runCatching { f.writeText(toJson(list)) }
        }
        return list
    }

    /** Лучшее совпадение по локальному индексу. */
    fun bestIn(list: List<CatalogRepository.RemoteMod>, name: String): Match? {
        var best: Match? = null
        for (m in list) {
            val sc = similarity(name, m.title)
            if (sc < 0.72) continue
            if (best == null || sc > best!!.score) best = Match(m, sc)
        }
        return best
    }

    private fun toJson(list: List<CatalogRepository.RemoteMod>): String {
        val arr = org.json.JSONArray()
        list.forEach { m ->
            arr.put(JSONObject().apply {
                put("id", m.id); put("title", m.title); put("author", m.author)
                put("likes", m.likes); put("category", m.category); put("tag", m.tag)
                put("img", m.imgUrl ?: "")
            })
        }
        return arr.toString()
    }

    private fun parseIndex(json: String): List<CatalogRepository.RemoteMod> {
        val arr = org.json.JSONArray(json)
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            CatalogRepository.RemoteMod(
                o.optInt("id"), o.optString("title"), o.optString("author"),
                o.optInt("likes"), o.optString("category"), o.optString("tag"),
                o.optString("img").ifBlank { null }
            )
        }
    }

    // ---------- кэш ----------

    fun loadCache(ctx: Context): MutableMap<String, CatalogRepository.RemoteMod> {
        val map = mutableMapOf<String, CatalogRepository.RemoteMod>()
        runCatching {
            val f = java.io.File(ctx.filesDir, CACHE)
            if (!f.exists()) return map
            val o = JSONObject(f.readText())
            o.keys().forEach { k ->
                val m = o.optJSONObject(k) ?: return@forEach
                if (m.optInt("id", 0) == 0) return@forEach
                map[k] = CatalogRepository.RemoteMod(
                    m.optInt("id"), m.optString("title"), m.optString("author"),
                    m.optInt("likes"), m.optString("category"), m.optString("tag"),
                    m.optString("img").ifBlank { null }
                )
            }
        }
        return map
    }

    fun saveCache(ctx: Context, map: Map<String, CatalogRepository.RemoteMod>, misses: Set<String>) {
        runCatching {
            val o = JSONObject()
            map.forEach { (k, m) ->
                o.put(k, JSONObject().apply {
                    put("id", m.id); put("title", m.title); put("author", m.author)
                    put("likes", m.likes); put("category", m.category); put("tag", m.tag)
                    put("img", m.imgUrl ?: "")
                })
            }
            misses.forEach { o.put(it, JSONObject().put("id", 0)) }
            java.io.File(ctx.filesDir, CACHE).writeText(o.toString())
        }
    }

    /**
     * Ищет все неизвестные имена. Возвращает найденные; не найденные — в misses.
     * Идём по одному с паузой: не хочется ловить бан за шквал запросов.
     */
    suspend fun locateAll(
        ctx: Context,
        names: List<String>,
        mode: BypassMode,
        proxy: String = "",
        mirror: String = "",
        onFound: (Map<String, CatalogRepository.RemoteMod>) -> Unit = {},
        onProgress: (String) -> Unit = {},
    ): Pair<Map<String, CatalogRepository.RemoteMod>, Set<String>> {
        val cache = loadCache(ctx)
        val found = cache.toMutableMap()
        val misses = mutableSetOf<String>()

        // уже известные из кэша — отдаём сразу
        val known = names.filter { cache.containsKey(it) }
        if (known.isNotEmpty()) onFound(found.filterKeys { it in known })

        val unknown = names.filter { !cache.containsKey(it) }
        for (n in unknown) {
            val m = runCatching { find(n, mode, proxy, mirror, ctx, onProgress) }.getOrNull()
            if (m != null) {
                found[n] = m.remote
                onFound(mapOf(n to m.remote))
            } else {
                misses += n
            }
            delay(350)
        }
        saveCache(ctx, found, misses)
        return found to misses
    }
}
