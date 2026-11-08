package app.ringbox.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import app.ringbox.data.BypassMode
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ringbox.ui.theme.Accents
import app.ringbox.ui.theme.LocalAccent
import app.ringbox.ui.theme.Rb
import app.ringbox.ui.theme.card

@Composable
fun SettingsScreen(
    folderOk: Boolean,
    folderPath: String,
    accentIndex: Int,
    lightTheme: Boolean,
    radius: Int,
    onBack: () -> Unit,
    onOpenFolder: () -> Unit,
    onAccent: (Int) -> Unit,
    onTheme: (Boolean) -> Unit,
    onRadius: (Int) -> Unit,
    bypassOn: Boolean = false,
    bypassMode: BypassMode = BypassMode.DIRECT,
    bypassProxy: String = "",
    bypassMirror: String = "",
    bypassStatus: String = "",
    bypassBusy: Boolean = false,
    onBypassOn: (Boolean) -> Unit = {},
    onBypassMode: (BypassMode) -> Unit = {},
    onBypassProxy: (String) -> Unit = {},
    onBypassMirror: (String) -> Unit = {},
    onBypassCheck: () -> Unit = {},
    lang: Lang = Lang.AUTO,
    onLang: (Lang) -> Unit = {},
) {
    val st = strings(lang)
    val accent = Accents[accentIndex]
    Column(
        Modifier.fillMaxSize().background(Rb.bg).statusBarsPadding()
    ) {
        // шапка
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 20.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack)
            Spacer(Modifier.width(8.dp))
            Text("Настройки", color = Rb.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 32.dp)
        ) {
            SectionLabel(st.language)

            // Автоопределение — на всю ширину
            LanguageTile(
                lang = Lang.AUTO,
                selected = lang == Lang.AUTO,
                title = "Автоопределение",
                sub = "Язык интерфейса системы, без учёта региона",
                wide = true,
                onClick = { onLang(Lang.AUTO) }
            )
            Spacer(Modifier.height(8.dp))
            val langs = Lang.entries.filter { it != Lang.AUTO }
            langs.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { l ->
                        Box(Modifier.weight(1f)) {
                            LanguageTile(
                                lang = l,
                                selected = lang == l,
                                title = l.native,
                                sub = l.english,
                                wide = false,
                                onClick = { onLang(l) }
                            )
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
            }

            SectionLabel(st.settings.let { "Игра" })
            Group {
                SettingRow(
                    icon = { FolderGlyph(accent.main) },
                    title = "Папка игры",
                    sub = if (folderOk) "$folderPath · Data.rsdk найден" else "Папка не выбрана",
                    onClick = onOpenFolder,
                    chevron = true
                )
                Divider()
                SettingRow(
                    icon = { RefreshGlyph(accent.main) },
                    title = "Проверять обновления",
                    sub = "Раз в сутки, по Wi-Fi",
                    trailing = {
                        Switch(
                            checked = true,
                            onCheckedChange = {},
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accent.main,
                                checkedBorderColor = accent.dark
                            )
                        )
                    }
                )
                Divider()
                SettingRow(
                    icon = { CacheGlyph(accent.main) },
                    title = "Кэш загрузок",
                    sub = "128 МБ · очистить",
                    chevron = true
                )
            }

            SectionLabel("Обход блокировки")
            Group {
                SettingRow(
                    icon = { ShieldGlyph(accent.main) },
                    title = "Обход блокировки",
                    sub = if (bypassOn) "Включён · Cloudflare не используем — он в РФ не работает"
                          else "GameBanana в РФ режется провайдером",
                    trailing = {
                        Switch(
                            checked = bypassOn,
                            onCheckedChange = onBypassOn,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accent.main,
                                checkedBorderColor = accent.dark
                            )
                        )
                    }
                )
                if (bypassOn) {
                    Divider()
                    Column(Modifier.padding(14.dp)) {
                        Text("Канал", color = Rb.text, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                        Text(
                            when (bypassMode) {
                                BypassMode.DIRECT -> "Авто — напрямую, потом DoH, прокси и зеркало"
                                BypassMode.DOH -> "DoH: AdGuard, Google, Quad9 (Cloudflare последним — он в РФ душится)"
                                BypassMode.MIRROR -> "Своё зеркало: reverse-proxy на твоём домене"
                                BypassMode.PROXY -> "Свой прокси — локальный порт от VPN или byedpi"
                            },
                            color = Rb.text2, fontSize = 12.sp
                        )
                        Spacer(Modifier.height(11.dp))
                        SegmentedRow(
                            items = listOf("Авто", "DoH", "Прокси", "Зеркало"),
                            selected = when (bypassMode) {
                                BypassMode.DIRECT -> 0
                                BypassMode.DOH -> 1
                                BypassMode.PROXY -> 2
                                BypassMode.MIRROR -> 3
                            },
                            accent = accent.main,
                            onSelect = { i ->
                                onBypassMode(
                                    when (i) {
                                        1 -> BypassMode.DOH
                                        2 -> BypassMode.PROXY
                                        3 -> BypassMode.MIRROR
                                        else -> BypassMode.DIRECT
                                    }
                                )
                            }
                        )
                    }
                    if (bypassMode == BypassMode.PROXY || bypassMode == BypassMode.MIRROR) {
                        Divider()
                        Column(Modifier.padding(14.dp)) {
                            val isMirror = bypassMode == BypassMode.MIRROR
                            Text(
                                if (isMirror) "Адрес зеркала" else "Адрес прокси",
                                color = Rb.text, fontSize = 14.5.sp, fontWeight = FontWeight.Medium
                            )
                            Text(
                                if (isMirror) "https://твой-домен.ru — reverse-proxy на gamebanana.com"
                                else "host:port · socks5://host:port · 127.0.0.1:1080 для VPN/byedpi",
                                color = Rb.text2, fontSize = 12.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            Box(
                                Modifier.fillMaxWidth()
                                    .background(Rb.surface2, RoundedCornerShape(11.dp))
                                    .border(1.dp, Rb.line, RoundedCornerShape(11.dp))
                                    .padding(horizontal = 12.dp, vertical = 11.dp)
                            ) {
                                val value = if (isMirror) bypassMirror else bypassProxy
                                if (value.isEmpty()) {
                                    Text(
                                        if (isMirror) "https://gb.example.ru" else "127.0.0.1:1080",
                                        color = Rb.text3, fontSize = 13.sp
                                    )
                                }
                                BasicTextField(
                                    value = value,
                                    onValueChange = { v -> if (isMirror) onBypassMirror(v) else onBypassProxy(v) },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Rb.text, fontSize = 13.sp),
                                    cursorBrush = SolidColor(accent.main),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Done
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    Divider()
                    SettingRow(
                        icon = { PulseGlyph(accent.main) },
                        title = if (bypassBusy) "Проверяем…" else "Проверить соединение",
                        sub = bypassStatus.ifEmpty { "Пока не проверяли" },
                        onClick = onBypassCheck,
                        chevron = !bypassBusy
                    )
                }
            }

            SectionLabel("Персонализация")
            Group {
                Column(Modifier.padding(14.dp)) {
                    Text("Тема", color = Rb.text, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                    Text("Оформление интерфейса", color = Rb.text2, fontSize = 12.sp)
                    Spacer(Modifier.height(11.dp))
                    val items = listOf("Авто" to null, "Тёмная" to false, "Светлая" to true)
                    SegmentedRow(
                        items = items.map { it.first },
                        selected = if (lightTheme) 2 else 1,
                        accent = accent.main,
                        onSelect = { i -> if (i != 0) onTheme(i == 2) }
                    )
                }
                Divider()
                Column(Modifier.padding(14.dp)) {
                    Text("Акцент", color = Rb.text, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                    Text(accent.name, color = Rb.text2, fontSize = 12.sp)
                    Spacer(Modifier.height(11.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Accents.forEachIndexed { i, a ->
                            Box(
                                Modifier.size(30.dp)
                                    .then(
                                        if (i == accentIndex)
                                            Modifier.border(2.dp, accent.main, CircleShape).padding(4.dp)
                                        else Modifier.padding(2.dp)
                                    )
                                    .background(
                                        Brush.verticalGradient(listOf(a.light, a.dark)),
                                        CircleShape
                                    )
                                    .clickable { onAccent(i) }
                            )
                        }
                    }
                }
                Divider()
                Column(Modifier.padding(14.dp)) {
                    Text("Скругление", color = Rb.text, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
                    Text("Форма карточек", color = Rb.text2, fontSize = 12.sp)
                    Spacer(Modifier.height(11.dp))
                    SegmentedRow(
                        items = listOf("Мягкое", "Обычное", "Строгое"),
                        selected = when (radius) { 24 -> 0; 12 -> 2; else -> 1 },
                        accent = accent.main,
                        onSelect = { i -> onRadius(when (i) { 0 -> 24; 2 -> 12; else -> 20 }) }
                    )
                }
            }

            SectionLabel("Ещё")
            Group {
                SettingRow(
                    icon = { InfoGlyph(accent.main) },
                    title = "О приложении",
                    sub = "RingBox 1.0.0",
                    chevron = true
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                "Менеджер модов для декомпиляции Sonic Mania (RSDKv5)",
                color = Rb.text3, fontSize = 11.5.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            )
        }
    }
}

// ---------- кирпичики ----------


@Composable
private fun ShieldGlyph(c: Color) {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width; val p = Path().apply {
            moveTo(w * 0.5f, size.height * 0.08f)
            lineTo(w * 0.88f, size.height * 0.26f)
            lineTo(w * 0.88f, size.height * 0.55f)
            cubicTo(w * 0.88f, size.height * 0.78f, w * 0.68f, size.height * 0.9f, w * 0.5f, size.height * 0.94f)
            cubicTo(w * 0.32f, size.height * 0.9f, w * 0.12f, size.height * 0.78f, w * 0.12f, size.height * 0.55f)
            lineTo(w * 0.12f, size.height * 0.26f)
            close()
        }
        drawPath(p, c, style = Stroke(width = 1.8.dp.toPx()))
        drawLine(c, Offset(w * 0.34f, size.height * 0.5f), Offset(w * 0.46f, size.height * 0.63f),
            strokeWidth = 1.8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(c, Offset(w * 0.46f, size.height * 0.63f), Offset(w * 0.68f, size.height * 0.38f),
            strokeWidth = 1.8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
    }
}

@Composable
private fun PulseGlyph(c: Color) {
    Canvas(Modifier.size(18.dp)) {
        val w = size.width; val h = size.height
        val p = Path().apply {
            moveTo(0f, h * 0.55f); lineTo(w * 0.26f, h * 0.55f)
            lineTo(w * 0.4f, h * 0.22f); lineTo(w * 0.58f, h * 0.8f)
            lineTo(w * 0.72f, h * 0.55f); lineTo(w, h * 0.55f)
        }
        drawPath(p, c, style = Stroke(width = 1.8.dp.toPx()))
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text, color = Rb.text3, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 2.dp, top = 18.dp, bottom = 9.dp)
    )
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().card().padding(vertical = 2.dp)) { content() }
}

@Composable
private fun Divider() {
    Box(Modifier.padding(start = 62.dp).fillMaxWidth().height(1.dp).background(Rb.line))
}

@Composable
private fun SetRowScope() {}

@Composable
private fun SettingRow(
    icon: @Composable () -> Unit,
    title: String,
    sub: String,
    onClick: (() -> Unit)? = null,
    chevron: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).background(LocalAccent.current.soft, RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center
        ) { icon() }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Rb.text, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
            Text(sub, color = Rb.text2, fontSize = 12.sp, maxLines = 1)
        }
        if (trailing != null) trailing()
        if (chevron) Text("›", color = Rb.text3, fontSize = 20.sp)
    }
}

@Composable
private fun SegmentedRow(items: List<String>, selected: Int, accent: Color, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(Rb.surface2, RoundedCornerShape(12.dp))
            .border(1.dp, Rb.line, RoundedCornerShape(12.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(32.dp)
                    .background(if (on) accent else Color.Transparent, RoundedCornerShape(9.dp))
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (on) Rb.ink else Rb.text2,
                    fontSize = 12.5.sp,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).background(Rb.surface, RoundedCornerShape(13.dp))
            .border(1.dp, Rb.line, RoundedCornerShape(13.dp)).clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("‹", color = Rb.text, fontSize = 22.sp)
    }
}

// ---------- иконки, нарисованные кодом ----------

@Composable private fun FolderGlyph(color: Color) = Canvas(Modifier.size(18.dp)) {
    val w = size.width; val h = size.height
    val body = Path().apply {
        moveTo(w * 0.08f, h * 0.28f)
        lineTo(w * 0.38f, h * 0.28f)
        lineTo(w * 0.47f, h * 0.40f)
        lineTo(w * 0.92f, h * 0.40f)
        lineTo(w * 0.92f, h * 0.82f)
        lineTo(w * 0.08f, h * 0.82f)
        close()
    }
    drawPath(body, color, style = Stroke(width = w * 0.10f))
    drawLine(color, Offset(w * 0.08f, h * 0.52f), Offset(w * 0.92f, h * 0.52f), w * 0.10f)
}

@Composable private fun RefreshGlyph(color: Color) = Canvas(Modifier.size(18.dp)) {
    val w = size.width; val h = size.height
    drawArc(color, startAngle = 40f, sweepAngle = 270f, useCenter = false,
        topLeft = Offset(w * 0.14f, h * 0.14f), size = Size(w * 0.72f, h * 0.72f),
        style = Stroke(width = w * 0.10f))
    val p = Path().apply {
        moveTo(w * 0.78f, h * 0.10f); lineTo(w * 0.90f, h * 0.34f); lineTo(w * 0.64f, h * 0.30f); close()
    }
    drawPath(p, color)
}

@Composable private fun CacheGlyph(color: Color) = Canvas(Modifier.size(18.dp)) {
    val w = size.width; val h = size.height
    drawOval(color, topLeft = Offset(w * 0.14f, h * 0.10f), size = Size(w * 0.72f, h * 0.26f),
        style = Stroke(width = w * 0.09f))
    drawLine(color, Offset(w * 0.14f, h * 0.23f), Offset(w * 0.14f, h * 0.78f), w * 0.09f)
    drawLine(color, Offset(w * 0.86f, h * 0.23f), Offset(w * 0.86f, h * 0.78f), w * 0.09f)
    drawArc(color, startAngle = 0f, sweepAngle = 180f, useCenter = false,
        topLeft = Offset(w * 0.14f, h * 0.65f), size = Size(w * 0.72f, h * 0.26f),
        style = Stroke(width = w * 0.09f))
}

@Composable private fun InfoGlyph(color: Color) = Canvas(Modifier.size(18.dp)) {
    val w = size.width
    drawCircle(color, radius = w * 0.38f, center = Offset(w / 2, w / 2), style = Stroke(width = w * 0.09f))
    drawCircle(color, radius = w * 0.055f, center = Offset(w / 2, w * 0.34f))
    drawLine(color, Offset(w / 2, w * 0.46f), Offset(w / 2, w * 0.66f), w * 0.09f)
}

/** Плитка выбора языка — как в соседней проге: бейдж, название, англ. подпись. */
@Composable
private fun LanguageTile(
    lang: Lang,
    selected: Boolean,
    title: String,
    sub: String,
    wide: Boolean,
    onClick: () -> Unit,
) {
    val accent = LocalAccent.current
    Row(
        Modifier.fillMaxWidth()
            .background(if (selected) accent.soft else Rb.surface, RoundedCornerShape(13.dp))
            .border(
                (if (selected) 1.5.dp else 1.dp),
                if (selected) accent.main else Rb.line,
                RoundedCornerShape(13.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 11.dp, vertical = if (wide) 13.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .then(if (wide) Modifier.size(width = 40.dp, height = 30.dp) else Modifier.size(width = 32.dp, height = 26.dp))
                .background(Rb.surface2, RoundedCornerShape(7.dp))
                .border(1.dp, Rb.line, RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                lang.badge,
                color = if (selected) accent.main else Rb.text2,
                fontSize = if (wide) 10.5.sp else 9.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Rb.text, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(sub, color = Rb.text3, fontSize = 11.sp, maxLines = 1)
        }
        if (selected) {
            Text("✓", color = accent.main, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}
