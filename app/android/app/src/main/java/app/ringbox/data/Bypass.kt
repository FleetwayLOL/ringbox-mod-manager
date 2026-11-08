package app.ringbox.data

import okhttp3.Dns
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Доступ к GameBanana из РФ.
 *
 * Что реально работает, а что нет (проверено на опыте РФ-пользователей, не из песочницы):
 *  - Cloudflare (DoH 1.1.1.1, ECH, corsproxy.io, allorigins) — режется/душится, ставку делать нельзя;
 *  - DoH к НЕ-cloudflare резолверам (AdGuard, Google, Quad9) — помогает, если блок только по DNS;
 *  - свой прокси/VPN (локальный порт) — работает всегда, это основной канал;
 *  - своё зеркало (reverse-proxy на своём домене) — работает, если домен не в реестре;
 *  - импорт мода из ZIP-файла — работает вообще без сети, поэтому он и есть страховка.
 */
enum class BypassMode(val label: String) {
    DIRECT("Напрямую"),
    DOH("DoH"),
    MIRROR("Своё зеркало"),
    PROXY("Прокси"),
}

/** Один провайдер DoH: имя, endpoint и IP для bootstrap (без системного DNS). */
private data class DohProvider(val name: String, val url: String, val ips: List<String>)

data class ChannelResult(val label: String, val ok: Boolean, val ms: Long, val note: String)

data class ProbeResult(
    val ok: Boolean,
    val label: String,
    val ms: Long,
    val note: String,
    val trail: List<ChannelResult> = emptyList(),
)

object Bypass {

    private const val PROBE_URL = "https://gamebanana.com/apiv11/Game/6045/ProfilePage"
    const val OFFICIAL = "https://gamebanana.com"

    /** Публичные прокси — держим последним шансом, большинство живёт за Cloudflare. */
    private val publicProxies = listOf(
        "https://api.codetabs.com/v1/proxy?quest=",
    )

    /**
     * DoH-резолверы. Cloudflare намеренно последний: в РФ он душится по ECH.
     * Порядок = порядок перебора.
     */
    private val dohProviders = listOf(
        DohProvider("AdGuard", "https://dns.adguard-dns.com/dns-query", listOf("94.140.14.14", "94.140.15.15")),
        DohProvider("Google", "https://dns.google/dns-query", listOf("8.8.8.8", "8.8.4.4")),
        DohProvider("Quad9", "https://dns.quad9.net/dns-query", listOf("9.9.9.9", "149.112.112.112")),
        DohProvider("Cloudflare", "https://cloudflare-dns.com/dns-query", listOf("1.1.1.1", "1.0.0.1")),
    )

    private val dohCache = HashMap<String, Dns>()

    private fun buildDoh(p: DohProvider): Dns = dohCache.getOrPut(p.name) {
        val bootstrap = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .callTimeout(7, TimeUnit.SECONDS)
            .build()
        val ips = p.ips.mapNotNull { runCatching { InetAddress.getByName(it) }.getOrNull() }
        val doh = DnsOverHttps.Builder()
            .client(bootstrap)
            .url(p.url.toHttpUrl())
            .apply { if (ips.isNotEmpty()) bootstrapDnsHosts(*ips.toTypedArray()) }
            .includeIPv6(false)
            .build()
        object : Dns {
            override fun lookup(hostname: String): List<InetAddress> =
                try {
                    doh.lookup(hostname).ifEmpty { Dns.SYSTEM.lookup(hostname) }
                } catch (e: Exception) {
                    Dns.SYSTEM.lookup(hostname)
                }
        }
    }

    private fun base(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private fun parseProxy(addr: String): Proxy? {
        val a = addr.trim().removePrefix("http://").removePrefix("socks5://").removePrefix("socks://")
        if (a.isEmpty()) return null
        val host = a.substringBefore(':')
        val port = a.substringAfter(':', "1080").toIntOrNull() ?: return null
        if (host.isEmpty()) return null
        val type = if (addr.startsWith("socks")) Proxy.Type.SOCKS else Proxy.Type.HTTP
        return Proxy(type, InetSocketAddress(host, port))
    }

    fun client(
        mode: BypassMode,
        proxyAddr: String = "",
        mirror: String = "",
        dohName: String = "AdGuard",
    ): OkHttpClient {
        val b = base().newBuilder()
        when (mode) {
            BypassMode.DOH -> {
                val p = dohProviders.firstOrNull { it.name == dohName } ?: dohProviders.first()
                b.dns(buildDoh(p))
            }
            BypassMode.PROXY -> parseProxy(proxyAddr)?.let { b.proxy(it) }
            else -> {}
        }
        return b.build()
    }

    /** Своё зеркало: https://gb.example.ru → https://gb.example.ru/... вместо gamebanana.com */
    fun viaMirror(url: String, mirror: String): String {
        val m = mirror.trim().trimEnd('/')
        if (m.isEmpty()) return url
        return url.replaceFirst(OFFICIAL, m)
    }

    /** Адрес запроса с учётом канала. */
    fun resolve(url: String, mode: BypassMode, mirror: String = ""): String = when (mode) {
        BypassMode.MIRROR -> viaMirror(url, mirror)
        else -> url
    }

    private fun call(url: String, client: OkHttpClient): Pair<Boolean, String> =
        try {
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", UA)
                .header("Accept", "*/*")
                .build()
            client.newCall(req).execute().use { r ->
                if (r.code in 200..399) true to "HTTP ${r.code}" else false to "HTTP ${r.code}"
            }
        } catch (e: Exception) {
            false to (e.javaClass.simpleName + (e.message?.let { ": " + it.take(50) } ?: ""))
        }

    /** Одна попытка запроса: канал + готовый клиент + итоговый URL. */
    data class Attempt(val label: String, val client: OkHttpClient, val url: String)

    /**
     * Очередь попыток для реального запроса (не проверки).
     * «Авто» = напрямую → DoH по резолверам → прокси → зеркало → публичные прокси.
     */
    fun attempts(url: String, mode: BypassMode, proxy: String = "", mirror: String = ""): List<Attempt> {
        val out = mutableListOf<Attempt>()
        fun add(label: String, c: OkHttpClient, u: String = url) { out += Attempt(label, c, u) }
        when (mode) {
            BypassMode.DIRECT -> {
                add("Напрямую", base())
                dohProviders.forEach { p -> add("DoH ${p.name}", client(BypassMode.DOH, dohName = p.name)) }
                if (parseProxy(proxy) != null) add("Прокси", client(BypassMode.PROXY, proxy))
                if (mirror.isNotBlank()) add("Своё зеркало", base(), viaMirror(url, mirror))
                publicProxies.forEach { add("Публичное зеркало", base(), it + enc(url)) }
            }
            BypassMode.DOH ->
                dohProviders.forEach { p -> add("DoH ${p.name}", client(BypassMode.DOH, dohName = p.name)) }
            BypassMode.PROXY -> add("Прокси", client(BypassMode.PROXY, proxy))
            BypassMode.MIRROR -> {
                if (mirror.isNotBlank()) add("Своё зеркало", base(), viaMirror(url, mirror))
                publicProxies.forEach { add("Публичное зеркало", base(), it + enc(url)) }
            }
        }
        return out
    }

    /** Проверка одного канала. */
    suspend fun probe(
        mode: BypassMode,
        proxyAddr: String = "",
        mirror: String = "",
        dohName: String = "AdGuard",
    ): ProbeResult = withContext(Dispatchers.IO) {
        val targets: List<String> = when (mode) {
            BypassMode.MIRROR -> if (mirror.isBlank()) emptyList()
                else listOf(viaMirror(PROBE_URL, mirror)) + publicProxies.map { it + enc(PROBE_URL) }
            else -> listOf(PROBE_URL)
        }
        if (targets.isEmpty()) {
            return@withContext ProbeResult(false, mode.label, 0, "не задано зеркало")
        }
        val client = if (mode == BypassMode.MIRROR) base() else client(mode, proxyAddr, mirror, dohName)
        var note = ""
        for (t in targets) {
            val t0 = System.currentTimeMillis()
            val (ok, msg) = call(t, client)
            if (ok) {
                val host = runCatching { t.substringAfter("://").substringBefore('/') }.getOrDefault(t)
                return@withContext ProbeResult(true, mode.label, System.currentTimeMillis() - t0, host)
            }
            note = msg
        }
        ProbeResult(false, mode.label, 0, note.ifEmpty { "нет ответа" })
    }

    /** DoH: перебираем резолверы, отдаём первый живой. */
    suspend fun autoDoh(): ProbeResult = withContext(Dispatchers.IO) {
        val trail = mutableListOf<ChannelResult>()
        for (p in dohProviders) {
            val t0 = System.currentTimeMillis()
            val (ok, msg) = call(PROBE_URL, client(BypassMode.DOH, dohName = p.name))
            val ms = System.currentTimeMillis() - t0
            trail += ChannelResult(p.name, ok, ms, msg)
            if (ok) return@withContext ProbeResult(true, "DoH · ${p.name}", ms, msg, trail)
        }
        ProbeResult(false, "DoH", 0, "все резолверы молчат", trail)
    }

    /** Авто: напрямую → DoH по резолверам → прокси → своё зеркало. */
    suspend fun auto(
        proxyAddr: String = "",
        mirror: String = "",
        onStep: ((ChannelResult) -> Unit)? = null,
    ): ProbeResult = withContext(Dispatchers.IO) {
        val trail = mutableListOf<ChannelResult>()

        fun step(r: ChannelResult): Boolean {
            trail += r
            onStep?.invoke(r)
            return r.ok
        }

        val t0 = System.currentTimeMillis()
        val (dOk, dMsg) = call(PROBE_URL, base())
        if (step(ChannelResult("Напрямую", dOk, System.currentTimeMillis() - t0, dMsg))) {
            return@withContext ProbeResult(true, "Напрямую", trail.last().ms, dMsg, trail)
        }

        val doh = autoDoh()
        trail += doh.trail; trail.lastOrNull()?.let { onStep?.invoke(it) }
        if (doh.ok) return@withContext doh.copy(trail = trail)

        if (parseProxy(proxyAddr) != null) {
            val t = System.currentTimeMillis()
            val (ok, msg) = call(PROBE_URL, client(BypassMode.PROXY, proxyAddr))
            if (step(ChannelResult("Прокси", ok, System.currentTimeMillis() - t, msg))) {
                return@withContext ProbeResult(true, "Прокси", trail.last().ms, msg, trail)
            }
        }

        if (mirror.isNotBlank()) {
            val t = System.currentTimeMillis()
            val (ok, msg) = call(viaMirror(PROBE_URL, mirror), base())
            if (step(ChannelResult("Своё зеркало", ok, System.currentTimeMillis() - t, msg))) {
                return@withContext ProbeResult(true, "Своё зеркало", trail.last().ms, msg, trail)
            }
        }

        ProbeResult(false, "нет канала", 0, trail.lastOrNull()?.note ?: "нет ответа", trail)
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    const val UA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"
}
