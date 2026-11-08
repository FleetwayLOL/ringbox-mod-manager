package app.ringbox.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ringbox.ui.theme.LocalAccent
import app.ringbox.ui.theme.Rb

/** Компактная строка поиска без тяжёлых икон-пакетов. */
@Composable
fun ModSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
) {
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(Rb.surface)
            .border(1.dp, Rb.line2, shape)
            .padding(horizontal = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(18.dp)) {
            val stroke = 1.8.dp.toPx()
            val c = androidx.compose.ui.geometry.Offset(size.width * 0.42f, size.height * 0.42f)
            drawCircle(
                color = Rb.text2,
                radius = size.minDimension * 0.27f,
                center = c,
                style = Stroke(width = stroke),
            )
            drawLine(
                color = Rb.text2,
                start = androidx.compose.ui.geometry.Offset(size.width * 0.62f, size.height * 0.62f),
                end = androidx.compose.ui.geometry.Offset(size.width * 0.88f, size.height * 0.88f),
                strokeWidth = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
        Spacer(Modifier.width(11.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                androidx.compose.material3.Text(hint, color = Rb.text3, fontSize = 14.sp, maxLines = 1)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(color = Rb.text, fontSize = 14.sp),
                cursorBrush = SolidColor(accent.main),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            )
        }
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(28.dp).clip(CircleShape)
                    .background(Rb.surface2)
                    .clickable { onValueChange("") },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.material3.Text(
                    "×", color = Rb.text2, fontSize = 18.sp, fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}
