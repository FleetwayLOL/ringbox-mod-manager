package app.ringbox.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ringbox.R
import app.ringbox.data.CatalogRepository
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import app.ringbox.ui.theme.LocalAccent
import app.ringbox.ui.theme.Rb
import app.ringbox.ui.theme.card
import app.ringbox.ui.theme.cardShape

/** Мод из GameBanana. Картинки зашиты в APK — рантайм-запросов к API нет. */
data class ModEntry(
    val id: Int,
    val title: String,
    val author: String,
    val likes: Int,
    val category: String,
    val tag: String,
    val imgUrl: String? = null,
)

val BUNDLED_MODS = listOf(
    ModEntry(609081, "Sonic Mania Randomizer", "CodenameGamma", 58, "Разное", "Разное"),
    ModEntry(729864, "Marble Zone: Reheated", "ZachmanAwesomenessII", 3, "Зоны", "Зоны"),
    ModEntry(727317, "Sonic 1/2/3 Signpost Leg", "atgee", 1, "Скины", "Скины"),
    ModEntry(726603, "Signpost Inspired Life Icons (Mania)", "atgee", 1, "Интерфейс", "Интерфейс"),
    ModEntry(468639, "Green Hill Zone - Complete", "Sanic17_DJ", 44, "Зоны", "Зоны"),
    ModEntry(641101, "Eggmania", "JoaoWictor2024", 10, "Разное", "Разное"),
    ModEntry(717853, "Abilities Mighty And Ray", "ahmed1g3", 1, "Персонажи", "Персонажи"),
    ModEntry(709851, "Movie Robotnik - EggTV Patch", "CreepBlaziken", 2, "Зоны", "Зоны"),
    ModEntry(705600, "X1Got Moveset For Sonic in Sonic Mania", "X1Got", 3, "Декомпиляция", "Декомпиляция"),
    ModEntry(697929, "Harder ERZ", "meowstudio", 0, "Зоны", "Зоны"),
    ModEntry(697068, "Eggjanken Restored", "meowstudio", 0, "Зоны", "Зоны"),
    ModEntry(696418, "love tester in sonic mania(decomp)", "meowstudio", 1, "Зоны", "Зоны")
)

@Composable
fun CatalogScreen(
    mods: List<ModEntry> = BUNDLED_MODS,
    offlineHint: Boolean = false,
    onOpen: (ModEntry) -> Unit = {},
    source: String = "",
    searchLabel: String = "Поиск модов",
    onSearch: suspend (String) -> List<ModEntry>? = { null },
    page: Int = 1,
    hasMore: Boolean = false,
    loadingMore: Boolean = false,
    pageError: String = "",
    onLoadMore: () -> Unit = {},
) {
    val accent = LocalAccent.current
    var query by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf("") }
    var remoteQuery by remember { mutableStateOf("") }
    var remoteResults by remember { mutableStateOf<List<ModEntry>>(emptyList()) }
    var failedQuery by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    val currentSearch by rememberUpdatedState(onSearch)

    // Локальные совпадения показываем сразу, а онлайн-запрос отправляем после короткой паузы.
    LaunchedEffect(query) {
        val q = query.trim()
        remoteQuery = ""
        remoteResults = emptyList()
        failedQuery = ""
        searching = false
        if (q.length < 2) return@LaunchedEffect
        delay(450)
        searching = true
        val result = try {
            currentSearch(q)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (result == null) {
            failedQuery = q
        } else {
            remoteQuery = q
            remoteResults = result
        }
        searching = false
    }

    val q = query.trim()
    val localMatches = if (q.isBlank()) mods else mods.filter { m ->
        listOf(m.title, m.author, m.tag, m.category).any { it.contains(q, ignoreCase = true) }
    }
    val searchMatches = if (q.isBlank()) mods else {
        val remote = if (remoteQuery == q) remoteResults else emptyList()
        (remote + localMatches).distinctBy { it.id }
    }
    val categories = searchMatches
        .flatMap { listOf(it.category, it.tag) }
        .map { it.trim() }
        .filter { it.isNotBlank() && !it.equals("Все", ignoreCase = true) }
        .distinct()
        .sortedBy { it.lowercase() }
    val visibleMods = if (selectedFilter.isBlank()) searchMatches else searchMatches.filter { m ->
        m.category.equals(selectedFilter, ignoreCase = true) || m.tag.equals(selectedFilter, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "catalog-header") {
            Text("Каталог", color = Rb.text, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp)
            Spacer(Modifier.height(4.dp))
            Text(
                source.ifEmpty { "офлайн-снимок каталога · ${mods.size} модов" },
                color = Rb.text2, fontSize = 13.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (q.isBlank()) {
                Spacer(Modifier.height(5.dp))
                PageCounter(page, mods.size, hasMore)
            }
            Spacer(Modifier.height(12.dp))
            ModSearchField(query, { query = it }, searchLabel)
            if (q.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                val status = when {
                    q.length < 2 -> "Добавь ещё символ для поиска на GameBanana"
                    searching -> "Ищем на GameBanana…"
                    failedQuery == q -> "Сеть недоступна — показаны совпадения из загруженного списка"
                    remoteQuery == q && remoteResults.isEmpty() -> "GameBanana · совпадений нет"
                    remoteQuery == q -> "GameBanana · ${remoteResults.size} результатов"
                    else -> "Совпадения в загруженном списке: ${localMatches.size}"
                }
                Text(status, color = Rb.text3, fontSize = 11.5.sp)
            }
            Spacer(Modifier.height(8.dp))
            Text("ФИЛЬТРЫ", color = Rb.text3, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp)
            Spacer(Modifier.height(6.dp))
            CategoryFilters(
                categories = categories,
                selected = selectedFilter,
                onSelect = { selectedFilter = it },
            )
            if (selectedFilter.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Показано ${visibleMods.size} из ${searchMatches.size}",
                    color = Rb.text3, fontSize = 11.sp)
            }
            Spacer(Modifier.height(4.dp))
        }
        if (offlineHint && q.isBlank()) {
            item(key = "offline-hint") {
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.soft)
                        .padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Каталог — офлайн-снимок", color = Rb.text, fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold)
                        Text("GameBanana в РФ режется провайдером. Включи обход в настройках — каталог будет тянуться свежим.",
                            color = Rb.text2, fontSize = 11.5.sp, lineHeight = 15.sp)
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
        if (visibleMods.isEmpty()) {
            item(key = "no-search-results") {
                Column(
                    Modifier.fillMaxWidth().card().padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (searching) "Ищем моды…" else "Ничего не найдено",
                        color = Rb.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Попробуй другое название или проверь подключение к сети.",
                        color = Rb.text2, fontSize = 12.sp)
                }
            }
        } else {
            items(visibleMods, key = { "mod-${it.id}" }) { m ->
                ModRow(m) { onOpen(m) }
            }
        }

        if (q.isBlank()) {
            item(key = "catalog-pagination") {
                Column(
                    Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PageCounter(page, mods.size, hasMore)
                    if (pageError.isNotBlank()) {
                        Spacer(Modifier.height(5.dp))
                        Text(pageError, color = Rb.red, fontSize = 11.5.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    when {
                        loadingMore -> CircularProgressIndicator(
                            modifier = Modifier.size(22.dp), color = accent.main, strokeWidth = 2.dp
                        )
                        hasMore -> Button(
                            onClick = onLoadMore,
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Rb.surface2, contentColor = Rb.text
                            ),
                        ) { Text("Загрузить ещё · страница ${page + 1}", fontWeight = FontWeight.SemiBold) }
                        else -> Text("Новых страниц больше нет", color = Rb.text3, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PageCounter(page: Int, loaded: Int, hasMore: Boolean) {
    val accent = LocalAccent.current
    Row(
        Modifier.fillMaxWidth().heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Страница $page", color = accent.main, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(
            "$loaded модов загружено${if (hasMore) " · есть ещё" else ""}",
            color = Rb.text3, fontSize = 11.sp, maxLines = 1,
        )
    }
}

@Composable
private fun CategoryFilters(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        item(key = "filter-all") {
            CategoryChip("Все", selected.isBlank()) { onSelect("") }
        }
        items(categories, key = { "filter-$it" }) { category ->
            CategoryChip(category, selected.equals(category, ignoreCase = true)) {
                onSelect(category)
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(99.dp)
    Box(
        Modifier.clip(shape)
            .background(if (selected) accent.soft else Rb.surface2)
            .border(1.dp, if (selected) accent.main.copy(alpha = 0.65f) else Rb.line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) accent.main else Rb.text2,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
        )
    }
}

@Composable
private fun ModRow(m: ModEntry, onClick: () -> Unit) {
    val accent = LocalAccent.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .card()
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(m, 54)
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(m.title, color = Rb.text, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
            Spacer(Modifier.height(3.dp))
            Row {
                Text(m.author, color = Rb.text2, fontSize = 12.5.sp, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Text("  ·  ★ ${m.likes}", color = Rb.text2, fontSize = 12.5.sp)
            }
            Spacer(Modifier.height(7.dp))
            Text(
                m.tag,
                color = accent.main,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(accent.soft)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(accent.main)
                .clickable { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", color = Rb.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Кэш обложек в памяти, чтобы не мигали при скролле. */
private object CoverCache {
    // Кэш по реальному размеру: максимум 4 МБ, а не десятки полноразмерных картинок.
    val map = object : android.util.LruCache<String, ImageBitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap): Int =
            (value.width.toLong() * value.height.toLong() * 4L)
                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt().coerceAtLeast(1)
    }
}

private const val MAX_COVER_EDGE = 256

/** Декодируем большой исходник сразу как маленькую обложку для строки каталога. */
private fun decodeCover(bytes: ByteArray): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val edge = maxOf(bounds.outWidth, bounds.outHeight)
    if (edge <= 0) return null
    var sample = 1
    while (edge / sample > MAX_COVER_EDGE) sample *= 2
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        inDither = true
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
}

@Composable
fun Cover(m: ModEntry, size: Int) {
    val url = m.imgUrl
    if (url == null) {
        Image(
            painter = painterResource(coverRes(m.id)),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size.dp).clip(RoundedCornerShape(15.dp)),
        )
        return
    }
    var bmp by remember(url) { mutableStateOf(CoverCache.map.get(url)) }
    val ctx = LocalContext.current
    LaunchedEffect(url) {
        if (bmp == null) {
            bmp = withContext(Dispatchers.IO) {
                CatalogRepository.loadCover(ctx, url)?.let(::decodeCover)?.also {
                    CoverCache.map.put(url, it)
                }
            }
        }
    }
    val current = bmp
    if (current != null) {
        Image(
            bitmap = current, contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size.dp).clip(RoundedCornerShape(15.dp)),
        )
    } else {
        Box(
            Modifier.size(size.dp).clip(RoundedCornerShape(15.dp)).background(Rb.surface2)
        )
    }
}

internal fun coverRes(id: Int): Int = when (id) {
    609081 -> R.drawable.m_609081
    729864 -> R.drawable.m_729864
    727317 -> R.drawable.m_727317
    726603 -> R.drawable.m_726603
    468639 -> R.drawable.m_468639
    641101 -> R.drawable.m_641101
    717853 -> R.drawable.m_717853
    709851 -> R.drawable.m_709851
    705600 -> R.drawable.m_705600
    697929 -> R.drawable.m_697929
    697068 -> R.drawable.m_697068
    696418 -> R.drawable.m_696418
    else -> R.drawable.m_609081
}
