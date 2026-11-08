package app.ringbox

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import app.ringbox.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ringbox.data.Bypass
import app.ringbox.data.CatalogRepository
import app.ringbox.data.ModMatcher
import app.ringbox.data.BypassMode
import app.ringbox.data.ModDownloader
import app.ringbox.data.ModInstaller
import app.ringbox.data.ModNetworkException
import app.ringbox.ui.Lang
import app.ringbox.ui.MineScreen
import app.ringbox.ui.ModEntry
import app.ringbox.ui.ModSheet
import app.ringbox.data.GameStorage
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import android.widget.Toast
import app.ringbox.ui.CatalogScreen
import app.ringbox.ui.SettingsScreen
import app.ringbox.ui.theme.Accents
import app.ringbox.ui.theme.LocalAccent
import app.ringbox.ui.theme.LocalRadius
import app.ringbox.ui.strings
import app.ringbox.ui.theme.Rb
import app.ringbox.ui.theme.card
import app.ringbox.ui.theme.inset
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RingBoxApp() }
    }
}

private enum class Tab { CATALOG, MINE, FOLDER }

@Composable
fun RingBoxApp() {
    var tab by remember { mutableStateOf(Tab.CATALOG) }

    // ---- обход блокировки GameBanana (в РФ сайт режется провайдером) ----
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val sp = remember { ctx.getSharedPreferences("ringbox", Context.MODE_PRIVATE) }
    var bypassOn by remember { mutableStateOf(sp.getBoolean("bypass_on", true)) }
    var bypassMode by remember {
        mutableStateOf(
            runCatching { BypassMode.valueOf(sp.getString("bypass_mode", "DIRECT")!!) }
                .getOrDefault(BypassMode.DIRECT)
        )
    }
    var bypassProxy by remember { mutableStateOf(sp.getString("bypass_proxy", "") ?: "") }
    var bypassMirror by remember { mutableStateOf(sp.getString("bypass_mirror", "") ?: "") }
    var bypassStatus by remember { mutableStateOf(sp.getString("bypass_status", "") ?: "") }
    var bypassBusy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // карточка мода: скачать через канал / поставить из файла
    var openMod by remember { mutableStateOf<ModEntry?>(null) }
    var sheetStatus by remember { mutableStateOf("") }
    var sheetBusy by remember { mutableStateOf(false) }
    var sheetProgress by remember { mutableFloatStateOf(0f) }

    fun saveBypass() {
        sp.edit()
            .putBoolean("bypass_on", bypassOn)
            .putString("bypass_mode", bypassMode.name)
            .putString("bypass_proxy", bypassProxy)
            .putString("bypass_mirror", bypassMirror)
            .putString("bypass_status", bypassStatus)
            .commit()
    }
    var matches by remember { mutableStateOf<Map<String, ModEntry>>(emptyMap()) }
    var misses by remember { mutableStateOf<Set<String>>(emptySet()) }

    var catalog by remember { mutableStateOf(app.ringbox.ui.BUNDLED_MODS) }
    var catalogSource by remember { mutableStateOf("") }
    var catalogBusy by remember { mutableStateOf(false) }
    var catalogPage by remember { mutableIntStateOf(1) }
    var catalogHasMore by remember { mutableStateOf(false) }
    var catalogLoadingMore by remember { mutableStateOf(false) }
    var catalogPageError by remember { mutableStateOf("") }

    val storage = remember { GameStorage(ctx) }
    var st by remember {
        mutableStateOf(GameStorage.State(access = false, location = "", hasGameData = false, mods = emptyList()))
    }
    var foundPath by remember { mutableStateOf<String?>(null) }
    var showOnboarding by remember { mutableStateOf(!st.access) }
    var autoFindChecked by remember { mutableStateOf(false) }
    var autoFindRunning by remember { mutableStateOf(false) }
    val gameOk = st.hasGameData
    val mods = st.mods

    var settingsOpen by remember { mutableStateOf(false) }

    // персонализация
    var lang by remember { mutableStateOf(Lang.fromCode(sp.getString("lang", "auto"))) }
    val s = strings(lang)
    var accentIndex by remember { mutableStateOf(sp.getInt("accent", 0)) }
    var radius by remember { mutableStateOf(sp.getInt("radius", 20)) }

    // Сканирование папки игры/списка mods/ делаем на IO, а не на UI-потоке.
    fun refresh() {
        scope.launch {
            st = withContext(kotlinx.coroutines.Dispatchers.IO) { storage.state() }
        }
    }

    fun loadCatalog() {
        if (catalogBusy) return
        catalogBusy = true
        scope.launch {
            val cached = withContext(kotlinx.coroutines.Dispatchers.IO) {
                runCatching { CatalogRepository.cacheLoad(ctx) }.getOrNull()
            }
            if (cached != null) {
                catalog = cached.map {
                    ModEntry(it.id, it.title, it.author, it.likes, it.category, it.tag, it.imgUrl)
                }
                catalogPage = 1
                catalogHasMore = cached.size >= CatalogRepository.PAGE_SIZE
                catalogSource = "кэш · ${cached.size} модов"
            }
            try {
                val (list, channel) = CatalogRepository.fetch(
                    1,
                    if (bypassOn) bypassMode else BypassMode.DIRECT,
                    bypassProxy,
                    bypassMirror
                )
                withContext(kotlinx.coroutines.Dispatchers.IO) {
                    runCatching { CatalogRepository.cacheSave(ctx, list) }
                }
                catalog = list.map {
                    ModEntry(it.id, it.title, it.author, it.likes, it.category, it.tag, it.imgUrl)
                }
                catalogPage = 1
                catalogHasMore = list.size >= CatalogRepository.PAGE_SIZE
                catalogPageError = ""
                catalogSource = "онлайн · ${list.size} модов · $channel"
            } catch (e: Exception) {
                if (cached == null) {
                    catalogSource = "офлайн-снимок · ${catalog.size} модов"
                    catalogHasMore = false // не перескакиваем сразу на страницу 2 при отсутствии кэша
                }
            }
            catalogBusy = false
        }
    }

    fun remoteToEntry(r: CatalogRepository.RemoteMod) =
        ModEntry(r.id, r.title, r.author, r.likes, r.category, r.tag, r.imgUrl)

    /** Следующая страница GameBanana добавляется к текущему списку, дубликаты убираем. */
    fun loadMoreCatalog() {
        if (catalogLoadingMore || catalogBusy || !catalogHasMore) return
        catalogLoadingMore = true
        catalogPageError = ""
        val nextPage = catalogPage + 1
        scope.launch {
            try {
                val (list, channel) = CatalogRepository.fetch(
                    nextPage,
                    if (bypassOn) bypassMode else BypassMode.DIRECT,
                    bypassProxy,
                    bypassMirror
                )
                if (list.isEmpty()) {
                    catalogHasMore = false
                } else {
                    catalog = (catalog + list.map(::remoteToEntry)).distinctBy { it.id }
                    catalogPage = nextPage
                    catalogHasMore = list.size >= CatalogRepository.PAGE_SIZE
                    catalogSource = "онлайн · ${catalog.size} модов · стр. $catalogPage · $channel"
                }
            } catch (e: Exception) {
                catalogPageError = "Страница не загрузилась: ${e.message?.take(80) ?: "проверь сеть"}"
            } finally {
                catalogLoadingMore = false
            }
        }
    }

    /** Ищет всю GameBanana по запросу, но показывает только моды для Sonic Mania. */
    suspend fun searchCatalog(query: String): List<ModEntry>? = try {
        val (found, _) = CatalogRepository.search(
            query,
            if (bypassOn) bypassMode else BypassMode.DIRECT,
            bypassProxy,
            bypassMirror
        )
        found.map(::remoteToEntry)
    } catch (e: kotlinx.coroutines.CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    /** Ищем в mods/ моды, которых нет в каталоге: ставились руками/из файла. */
    fun locateInstalled() {
        val names = st.mods
        if (names.isEmpty()) return
        scope.launch {
            val (found, miss) = ModMatcher.locateAll(
                ctx = ctx,
                names = names,
                mode = if (bypassOn) bypassMode else BypassMode.DIRECT,
                proxy = bypassProxy,
                mirror = bypassMirror,
                onFound = { partial ->
                    matches = matches + partial.mapValues { (_, r) -> remoteToEntry(r) }
                }
            )
            matches = matches + found.mapValues { (_, r) -> remoteToEntry(r) }
            misses = miss
        }
    }

    fun autoFindAndSave(force: Boolean = false) {
        if (autoFindRunning || (autoFindChecked && !force)) return
        autoFindChecked = true
        autoFindRunning = true
        scope.launch {
            try {
                val dir = withContext(kotlinx.coroutines.Dispatchers.IO) { storage.autoFind() }
                if (dir != null) {
                    withContext(kotlinx.coroutines.Dispatchers.IO) { storage.setPath(dir) }
                    foundPath = dir.absolutePath
                    st = withContext(kotlinx.coroutines.Dispatchers.IO) { storage.state() }
                } else {
                    foundPath = null
                }
            } finally {
                autoFindRunning = false
            }
        }
    }

    LaunchedEffect(Unit) {
        // Даже первое сканирование состояния диска — не на главном потоке.
        st = withContext(kotlinx.coroutines.Dispatchers.IO) { storage.state() }
        loadCatalog()
        // при наличии доступа сами находим папку — юзеру не надо ничего выбирать
        if (storage.hasAllFilesAccess() && storage.savedPath() == null) autoFindAndSave()
        if (!st.hasGameData) tab = Tab.FOLDER
    }

    LaunchedEffect(mods) {
        if (mods.isNotEmpty()) locateInstalled()
    }

    // после возврата из системных настроек доступа — проверяем и ищем сами
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refresh()
                if (storage.hasAllFilesAccess() && storage.savedPath() == null) autoFindAndSave()
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    val zipPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val r = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    ModInstaller.installFromZip(ctx, storage, uri)
                }
                Toast.makeText(ctx, r.message, Toast.LENGTH_LONG).show()
                if (r.ok) { tab = Tab.MINE; refresh() }
            }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        if (uri != null) {
            storage.setTree(uri)
            refresh()
            showOnboarding = false
        }
    }

    CompositionLocalProvider(
        LocalAccent provides Accents[accentIndex],
        LocalRadius provides radius
    ) {
        val accent = Accents[accentIndex]
        // Сплошной фон дешевле для GPU на слабых устройствах, чем большой радиальный градиент.
        Box(Modifier.fillMaxSize().background(Rb.bg)) {
            if (settingsOpen) {
                SettingsScreen(
                    folderOk = gameOk,
                    folderPath = st.location.ifEmpty { "RSDK/v5" },
                    accentIndex = accentIndex,
                    lightTheme = false,
                    radius = radius,
                    onBack = { settingsOpen = false },
                    onOpenFolder = { settingsOpen = false; tab = Tab.FOLDER },
                    onAccent = { accentIndex = it; sp.edit().putInt("accent", accentIndex).commit() },
                    onTheme = { /* светлая схема — следующий шаг */ },
                    onRadius = { radius = it; sp.edit().putInt("radius", radius).commit() },
                    lang = lang,
                    onLang = { lang = it; sp.edit().putString("lang", it.code).commit() },
                    bypassOn = bypassOn,
                    bypassMode = bypassMode,
                    bypassProxy = bypassProxy,
                    bypassMirror = bypassMirror,
                    bypassStatus = bypassStatus,
                    bypassBusy = bypassBusy,
                    onBypassOn = { on -> bypassOn = on; saveBypass(); if (on) { bypassBusy = true; bypassStatus = "Проверяем…" ; scope.launch {
                        val r = Bypass.auto(bypassProxy, bypassMirror)
                        bypassBusy = false
                        bypassStatus = if (r.ok) "Работает: ${r.label} · ${r.ms} мс" else "Нет канала: ${r.note}"
                        saveBypass()
                    } } },
                    onBypassMode = { m -> bypassMode = m; saveBypass() },
                    onBypassProxy = { v -> bypassProxy = v; saveBypass() },
                    onBypassMirror = { v -> bypassMirror = v; saveBypass() },
                    onBypassCheck = {
                        if (!bypassBusy) {
                            bypassBusy = true; bypassStatus = "Проверяем…"
                            scope.launch {
                                val r = if (bypassMode == BypassMode.DIRECT) Bypass.auto(bypassProxy, bypassMirror)
                                        else Bypass.probe(bypassMode, bypassProxy, bypassMirror)
                                bypassBusy = false
                                bypassStatus = if (r.ok) "Работает: ${r.label} · ${r.ms} мс"
                                               else "Не отвечает · ${r.note}"
                                saveBypass()
                            }
                        }
                    }
                )
            } else {
                Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 20.dp)) {

                    Row(
                        Modifier.fillMaxWidth().padding(top = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RingBoxMark(30)
                        Spacer(Modifier.width(10.dp))
                        Text("RingBox", color = Rb.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        GearButton { settingsOpen = true }
                    }

                    Spacer(Modifier.height(24.dp))

                    Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (tab) {
                        Tab.FOLDER -> FolderPage(
                            title = s.folder,
                            gameOk = gameOk,
                            mods = mods,
                            onGrant = { runCatching { ctx.startActivity(storage.allFilesIntent()) } },
                            onPickManual = { picker.launch(null) },
                            onPick = { picker.launch(null) },
                            onForget = { storage.forget(); foundPath = null; refresh() },
                            onImport = { zipPicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                            path = st.location,
                            onFind = { autoFindAndSave(force = true) }
                        )
                        Tab.CATALOG -> CatalogScreen(
                            mods = catalog,
                            source = catalogSource,
                            offlineHint = !bypassOn,
                            searchLabel = s.search,
                            onSearch = { query -> searchCatalog(query) },
                            page = catalogPage,
                            hasMore = catalogHasMore,
                            loadingMore = catalogLoadingMore || catalogBusy,
                            pageError = catalogPageError,
                            onLoadMore = { loadMoreCatalog() },
                            onOpen = { m ->
                                openMod = m
                                sheetStatus = ""
                                sheetBusy = false
                                sheetProgress = 0f
                            }
                        )
                        Tab.MINE -> MineScreen(
                            mods = mods,
                            gameOk = gameOk,
                            onImport = { zipPicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                            onOpenFolder = { tab = Tab.FOLDER },
                            matches = matches,
                            misses = misses,
                            searchLabel = s.search,
                            onOpen = { m ->
                                openMod = m
                                sheetStatus = ""
                                sheetBusy = false
                                sheetProgress = 0f
                            }
                        )
                    }

                    }
                    TabBar(tab, listOf(s.catalog, s.mine, s.folder)) { tab = it }
                    Spacer(Modifier.height(12.dp))

                    openMod?.let { m ->
                        ModSheet(
                            mod = m,
                            status = sheetStatus,
                            busy = sheetBusy,
                            progress = sheetProgress,
                            onDownload = {
                                if (!sheetBusy) {
                                    sheetBusy = true
                                    sheetProgress = 0f
                                    sheetStatus = "Ищем файл мода на GameBanana…"
                                    scope.launch {
                                        try {
                                            val ch = if (bypassOn) bypassMode else BypassMode.DIRECT
                                            val files = ModDownloader.files(m.id, ch, bypassProxy, bypassMirror)
                                            val f = files.firstOrNull()
                                                ?: throw ModNetworkException("у мода нет файлов")
                                            sheetStatus = "Качаем ${f.name}…"
                                            val bytes = ModDownloader.download(
                                                f, ch, bypassProxy, bypassMirror
                                            ) { sheetProgress = it }
                                            sheetStatus = "Распаковываем в mods/…"
                                            val r = withContext(kotlinx.coroutines.Dispatchers.IO) {
                                                ModInstaller.installFromBytes(ctx, storage, bytes, f.name)
                                            }
                                            sheetBusy = false
                                            sheetStatus = r.message
                                            if (r.ok) { refresh(); openMod = null; tab = Tab.MINE }
                                        } catch (e: ModNetworkException) {
                                            sheetBusy = false
                                            sheetStatus = e.message + " — поставь из файла"
                                        } catch (e: Exception) {
                                            sheetBusy = false
                                            sheetStatus = "Ошибка: ${e.javaClass.simpleName} — поставь из файла"
                                        }
                                    }
                                }
                            },
                            onFromFile = {
                                openMod = null
                                zipPicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                            },
                            onClose = { openMod = null }
                        )
                    }
                }
            }
        }
    }
}

/** Знак: монитор с золотым кольцом, нарисованный кодом. */
@Composable
private fun RingBoxMark(size: Int) {
    Image(
        painter = painterResource(R.drawable.ic_ringbox_logo),
        contentDescription = "RingBox",
        modifier = Modifier.size(size.dp)
    )
}

@Composable
private fun GearButton(onClick: () -> Unit) {
    val accent = LocalAccent.current
    Box(
        Modifier.size(38.dp)
            .background(Rb.surface, RoundedCornerShape(13.dp))
            .border(1.dp, Rb.line, RoundedCornerShape(13.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(18.dp)) {
            val c = Offset(size.width / 2, size.height / 2)
            val r = size.width * 0.22f
            repeat(8) { i ->
                val a = (Math.PI / 4 * i).toFloat()
                drawLine(
                    Rb.text2,
                    Offset(c.x + cos(a) * r * 1.3f, c.y + sin(a) * r * 1.3f),
                    Offset(c.x + cos(a) * r * 1.9f, c.y + sin(a) * r * 1.9f),
                    strokeWidth = size.width * 0.09f
                )
            }
            drawCircle(Rb.text2, radius = r, center = c, style = androidx.compose.ui.graphics.drawscope.Stroke(width = size.width * 0.08f))
            drawCircle(accent.main, radius = r * 0.34f, center = c)
        }
    }
}

@Composable
private fun FolderPage(
    title: String = "Папка игры",
    gameOk: Boolean,
    mods: List<String>,
    onPick: () -> Unit,
    onForget: () -> Unit,
    onImport: () -> Unit = {},
    path: String = "",
    onFind: () -> Unit = {},
    onGrant: () -> Unit = {},
    onPickManual: () -> Unit = {},
) {
    val accent = LocalAccent.current
    Column(Modifier.fillMaxWidth()) {
        Text(title, color = Rb.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Откуда RingBox берёт данные и куда ставит моды", color = Rb.text2, fontSize = 13.sp)
        Spacer(Modifier.height(20.dp))

        if (!gameOk) {
            Column(Modifier.fillMaxWidth().card().padding(18.dp)) {
                Text("Кхм. Извини, но мне нужен доступ к файлам",
                    color = Rb.text, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Иначе я не найду RSDK/v5 и не смогу класть моды в mods/. Доступ только к файлам игры.",
                    color = Rb.text2, fontSize = 12.5.sp, lineHeight = 17.sp
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onGrant,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent.main, contentColor = Rb.ink)
                ) { Text("Дать доступ к файлам", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onFind,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Rb.surface2, contentColor = Rb.text)
                ) { Text("Найти папку игры", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onPickManual, modifier = Modifier.fillMaxWidth()) {
                    Text("Выбрать вручную", color = Rb.text3, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        Column(Modifier.fillMaxWidth().card().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).background(accent.soft, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(16.dp).background(accent.main, CircleShape))
                }
                Spacer(Modifier.width(13.dp))
                Column {
                    Text(
                        if (gameOk) "Папка подключена" else "Папка не выбрана",
                        color = Rb.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (gameOk) "Data.rsdk найден · доступ есть" else "Нажми «Изменить папку»",
                        color = if (gameOk) Rb.text2 else Rb.red, fontSize = 12.5.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                path.ifEmpty { "/storage/emulated/0/RSDK/v5" },
                color = Rb.text2, fontSize = 12.5.sp,
                modifier = Modifier.fillMaxWidth().inset().padding(12.dp)
            )

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onPick,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent.main, contentColor = Rb.ink)
            ) { Text("Изменить папку", fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onFind,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Rb.surface2, contentColor = Rb.text
                )
            ) { Text("Найти папку автоматически", fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = onImport,
                enabled = gameOk,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Rb.surface2,
                    contentColor = Rb.text,
                    disabledContainerColor = Rb.surface2.copy(alpha = 0.5f),
                    disabledContentColor = Rb.text3
                )
            ) { Text("Установить мод из файла (.zip)", fontWeight = FontWeight.SemiBold) }
            Text(
                "Без сети: качаешь мод где угодно — приложение раскидает его в mods/",
                color = Rb.text3, fontSize = 11.5.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp, start = 2.dp)
            )

            if (gameOk) {
                TextButton(onClick = onForget, modifier = Modifier.fillMaxWidth()) {
                    Text("Забыть папку", color = Rb.text3, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Установлено: ${mods.size}", color = Rb.text2, fontSize = 13.sp)
        mods.take(8).forEach {
            Text("· $it", color = Rb.text3, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun Placeholder(title: String, sub: String) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, color = Rb.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(sub, color = Rb.text3, fontSize = 13.sp)
    }
}

@Composable
private fun TabBar(current: Tab, labels: List<String>, onSelect: (Tab) -> Unit) {
    val accent = LocalAccent.current
    Row(
        Modifier.fillMaxWidth().height(64.dp)
            .background(Rb.surface.copy(alpha = .92f), RoundedCornerShape(22.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Tab.entries.forEach { t ->
            val on = t == current
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .clickable { onSelect(t) }
                    .background(if (on) accent.soft else Color.Transparent, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    labels.getOrElse(t.ordinal) { t.name },
                    color = if (on) accent.main else Rb.text3,
                    fontSize = 12.5.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
