package app.ringbox.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ringbox.ui.theme.LocalAccent
import app.ringbox.ui.theme.Rb
import app.ringbox.ui.theme.card

/**
 * «Мои» — реальный список того, что лежит в mods/.
 * Порядок важен: движок читает mods/ снизу вверх, нижний мод перекрывает верхние.
 */
@Composable
fun MineScreen(
    mods: List<String>,
    gameOk: Boolean,
    onImport: () -> Unit,
    onOpenFolder: () -> Unit,
    matches: Map<String, ModEntry> = emptyMap(),
    misses: Set<String> = emptySet(),
    onOpen: (ModEntry) -> Unit = {},
    searchLabel: String = "Поиск модов",
) {
    val accent = LocalAccent.current
    val enabled = remember(mods) { mods.associateWith { mutableStateOf(true) } }
    var query by rememberSaveable { mutableStateOf("") }
    val q = query.trim()
    val visibleMods = if (q.isBlank()) mods else mods.filter { name ->
        val m = matches[name]
        listOf(name, m?.title.orEmpty(), m?.author.orEmpty(), m?.tag.orEmpty(), m?.category.orEmpty())
            .any { it.contains(q, ignoreCase = true) }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Мои моды", color = Rb.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                when {
                    mods.isEmpty() -> "Пока ничего не установлено"
                    q.isNotBlank() -> "Найдено ${visibleMods.size} из ${mods.size}"
                    else -> "${mods.size} установлено · порядок загрузки снизу вверх"
                },
                color = Rb.text2, fontSize = 13.sp
            )
            Spacer(Modifier.height(12.dp))
            ModSearchField(query, { query = it }, searchLabel)
            Spacer(Modifier.height(4.dp))
        }

        if (mods.isEmpty()) {
            item {
                Column(
                    Modifier.fillMaxWidth().card().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        Modifier.size(46.dp).clip(CircleShape).background(accent.soft),
                        contentAlignment = Alignment.Center
                    ) { Text("+", color = accent.main, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                    Spacer(Modifier.height(12.dp))
                    Text("Пусто", color = Rb.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (gameOk) "Скачай мод в каталоге или поставь готовый .zip из файла"
                        else "Сначала выбери папку игры на вкладке «Папка»",
                        color = Rb.text2, fontSize = 12.5.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = if (gameOk) onImport else onOpenFolder,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent.main, contentColor = Rb.ink)
                    ) {
                        Text(
                            if (gameOk) "Установить из файла (.zip)" else "Выбрать папку игры",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            if (visibleMods.isEmpty()) {
                item(key = "mine-no-results") {
                    Column(
                        Modifier.fillMaxWidth().card().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("Ничего не найдено", color = Rb.text, fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("Проверь название мода или очисти поиск.", color = Rb.text2, fontSize = 12.sp)
                    }
                }
            }
            items(visibleMods, key = { it }) { name ->
                val on = enabled[name]?.value ?: true
                val match = matches[name]
                Row(
                    Modifier.fillMaxWidth().card()
                        .clickABLE(match, onOpen)
                        .padding(13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (match != null) {
                        Cover(match, 44)
                    } else {
                        Box(
                            Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                                .background(if (on) accent.soft else Rb.surface2),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                name.take(1).uppercase(),
                                color = if (on) accent.main else Rb.text3,
                                fontSize = 17.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            match?.title ?: name, color = Rb.text, fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        if (match != null) {
                            Text(
                                "${match.author} · ★ ${match.likes}",
                                color = Rb.text2, fontSize = 12.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(5.dp))
                            Row {
                                Text(
                                    match.tag,
                                    color = accent.main, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clip(RoundedCornerShape(99.dp))
                                        .background(accent.soft)
                                        .padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    "найден на GameBanana",
                                    color = Rb.text3, fontSize = 10.sp
                                )
                            }
                        } else {
                            Text(
                                if (name in misses) "на GameBanana не нашёлся" else "ищем на GameBanana…",
                                color = Rb.text3, fontSize = 11.5.sp
                            )
                        }
                    }
                    Box(
                        Modifier.size(width = 44.dp, height = 26.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (on) accent.main else Rb.surface2)
                            .border(1.dp, if (on) accent.dark else Rb.line, RoundedCornerShape(99.dp))
                            .clickable { enabled[name]?.value = !on },
                        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(
                            Modifier.padding(horizontal = 3.dp).size(20.dp)
                                .clip(CircleShape)
                                .background(if (on) Rb.ink else Rb.text3)
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Чем ниже мод в списке, тем позже он грузится и тем выше его приоритет.",
                    color = Rb.text3, fontSize = 11.5.sp
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onImport,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Rb.surface2, contentColor = Rb.text)
                ) { Text("Установить из файла (.zip)", fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

/** Клик по строке — только если мод распознан на GameBanana (иначе некуда вести). */
private fun Modifier.clickABLE(match: ModEntry?, onOpen: (ModEntry) -> Unit): Modifier =
    if (match != null) this.clickable { onOpen(match) } else this
