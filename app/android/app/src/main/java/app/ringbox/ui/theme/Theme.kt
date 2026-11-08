package app.ringbox.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Токены дизайна RingBox. Держать в синхроне с prototype-файлом ui-v4. */
object Rb {
    val bg = Color(0xFF0A0B0F)
    val surface = Color(0xFF131519)
    val surface2 = Color(0xFF191C22)
    val line = Color(0x1FFFFFFF)
    val line2 = Color(0x33FFFFFF)
    val inset = Color(0xFF0D0F13)

    val text = Color(0xFFF4F5F7)
    val text2 = Color(0xFF9BA1AE)
    val text3 = Color(0xFF666C79)

    val green = Color(0xFF39D07A)
    val red = Color(0xFFFF5A5F)
    val ink = Color(0xFF241A06) // текст на акцентной плашке

    // светлая тема
    val lightBg = Color(0xFFF3F4F7)
    val lightSurface = Color(0xFFFFFFFF)
    val lightStroke = Color(0x17101420)
}

/** Один акцент — шесть вариантов из настроек. */
data class Accent(
    val name: String,
    val main: Color,
    val dark: Color,
    val light: Color,
    val soft: Color,
)

val Accents = listOf(
    Accent("Золото", Color(0xFFFFC94A), Color(0xFFF0AF2A), Color(0xFFFFD463), Color(0x1FFFC94A)),
    Accent("Изумруд", Color(0xFF34D399), Color(0xFF10A97B), Color(0xFF6EE7B7), Color(0x1F34D399)),
    Accent("Лазурь", Color(0xFF4F9CFF), Color(0xFF2C6FE8), Color(0xFF86BBFF), Color(0x1F4F9CFF)),
    Accent("Малина", Color(0xFFFF5C8A), Color(0xFFE23A6B), Color(0xFFFF92AE), Color(0x1FFF5C8A)),
    Accent("Лиловый", Color(0xFFA78BFA), Color(0xFF7C5CF0), Color(0xFFC4B2FF), Color(0x1FA78BFA)),
    Accent("Графит", Color(0xFFC7CCD6), Color(0xFF9AA3B2), Color(0xFFE4E8EF), Color(0x1FC7CCD6)),
)

val LocalAccent = staticCompositionLocalOf { Accents[0] }
val LocalRadius = staticCompositionLocalOf { 20 }

@Composable
fun cardShape() = RoundedCornerShape(LocalRadius.current.dp)

/** Карточка: тёмная поверхность + волосяная рамка. */
@Composable
fun Modifier.card(): Modifier {
    val shape = cardShape()
    return background(Rb.surface, shape).border(1.dp, Rb.line, shape)
}

/** Вложенный блок (поле, чип, превью). */
@Composable
fun Modifier.inset(shape: RoundedCornerShape = RoundedCornerShape(11.dp)): Modifier =
    background(Rb.inset, shape).border(1.dp, Rb.line, shape)

@Composable
fun Screen(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.Box(
        Modifier.fillMaxSize().background(Rb.bg)
    ) { content() }
}
