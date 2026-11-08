package app.ringbox.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ringbox.R
import app.ringbox.ui.theme.LocalAccent
import app.ringbox.ui.theme.Rb
import app.ringbox.ui.theme.card

/**
 * Первый запуск. Без доступа к файлам приложение бесполезно: оно должно
 * найти RSDK/v5 и положить мод в mods/. Лучше попросить сразу и честно.
 */
@Composable
fun OnboardingScreen(
    hasAccess: Boolean,
    foundPath: String?,
    onRequestAccess: () -> Unit,
    onAutoFind: () -> Unit,
    onPickManually: () -> Unit,
    onContinue: () -> Unit,
) {
    val accent = LocalAccent.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 46.dp, bottom = 30.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_ringbox_logo),
            contentDescription = null,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(18.dp))
        Text("RingBox", color = Rb.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text(
            "Моды для декомпиляции Sonic Mania (RSDKv5)",
            color = Rb.text2, fontSize = 13.sp
        )

        Spacer(Modifier.height(24.dp))

        Column(Modifier.fillMaxWidth().card().padding(18.dp)) {
            Text(
                if (hasAccess) "Доступ к файлам есть" else "Кхм. Извини, но мне нужен доступ к файлам",
                color = Rb.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (hasAccess)
                    "Теперь я могу сам найти папку игры и писать в mods/. Нажми «Найти папку игры»."
                else
                    "Без него я не найду /storage/emulated/0/RSDK/v5 и не смогу положить мод в mods/. " +
                        "Это доступ только к файлам игры — никуда больше я не лезу.",
                color = Rb.text2, fontSize = 12.5.sp, lineHeight = 18.sp
            )

            Spacer(Modifier.height(16.dp))

            if (!hasAccess) {
                Button(
                    onClick = onRequestAccess,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent.main, contentColor = Rb.ink)
                ) { Text("Дать доступ к файлам", fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(10.dp))
            }

            Button(
                onClick = onAutoFind,
                enabled = hasAccess,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (hasAccess) accent.main else Rb.surface2,
                    contentColor = if (hasAccess) Rb.ink else Rb.text3
                )
            ) { Text("Найти папку игры", fontWeight = FontWeight.SemiBold) }

            if (foundPath != null) {
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.fillMaxWidth()
                        .background(Rb.surface2, RoundedCornerShape(11.dp))
                        .border(1.dp, Rb.line, RoundedCornerShape(11.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("Нашёл", color = Rb.text3, fontSize = 11.sp)
                        Spacer(Modifier.height(3.dp))
                        Text(foundPath, color = Rb.text, fontSize = 12.5.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent.main, contentColor = Rb.ink)
                ) { Text("Продолжить", fontWeight = FontWeight.SemiBold) }
            }

            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onPickManually, modifier = Modifier.fillMaxWidth()) {
                Text("Выбрать папку вручную", color = Rb.text3, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "Моды качаются с GameBanana, но в РФ он режется — поэтому есть обход " +
                "и установка из ZIP-файла без сети.",
            color = Rb.text3, fontSize = 11.5.sp, textAlign = TextAlign.Start
        )
    }
}
