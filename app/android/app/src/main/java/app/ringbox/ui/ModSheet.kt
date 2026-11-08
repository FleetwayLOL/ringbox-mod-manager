package app.ringbox.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.ringbox.ui.theme.LocalAccent
import app.ringbox.ui.theme.Rb

/** Нижняя карточка мода: обложка, автор, тег и два способа поставить. */
@Composable
fun ModSheet(
    mod: ModEntry,
    status: String,
    busy: Boolean,
    progress: Float,
    onDownload: () -> Unit,
    onFromFile: () -> Unit,
    onClose: () -> Unit,
) {
    val accent = LocalAccent.current
    Dialog(onDismissRequest = onClose) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Rb.surface)
                .border(1.dp, Rb.line, RoundedCornerShape(24.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Cover(mod, 52)
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(mod.title, color = Rb.text, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
                    Text("${mod.author} · ★ ${mod.likes}", color = Rb.text2, fontSize = 12.5.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        mod.tag,
                        color = accent.main, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(99.dp))
                            .background(accent.soft)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (busy) {
                Text(status.ifEmpty { "Работаем…" }, color = Rb.text2, fontSize = 12.5.sp)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().height(6.dp)
                        .clip(RoundedCornerShape(99.dp)).background(Rb.surface2)
                ) {
                    Box(
                        Modifier.fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(99.dp))
                            .background(accent.main)
                    )
                }
            } else if (status.isNotEmpty()) {
                Text(status, color = Rb.text2, fontSize = 12.5.sp)
            }

            Spacer(Modifier.height(14.dp))

            Button(
                onClick = onDownload,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent.main, contentColor = Rb.ink)
            ) { Text("Скачать и установить", fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(9.dp))

            Button(
                onClick = onFromFile,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Rb.surface2,
                    contentColor = Rb.text,
                    disabledContainerColor = Rb.surface2.copy(alpha = .5f),
                    disabledContentColor = Rb.text3
                )
            ) { Text("Установить из файла (.zip)", fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(6.dp))
            Text(
                "«Из файла» работает без сети — если канал обхода не пробивает.",
                color = Rb.text3, fontSize = 11.sp
            )

            TextButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Закрыть", color = Rb.text3, fontSize = 13.sp)
            }
        }
    }
}
