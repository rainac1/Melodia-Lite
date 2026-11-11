package com.lin0721.linmusic.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// 通用整数滑杆。
@Composable
fun CapsuleSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 16f..32f,
    modifier: Modifier = Modifier,
    startLabel: String = "",
    endLabel: String = "",
    onValueChangeFinished: (() -> Unit)? = null
) {
    Column(modifier.fillMaxWidth()) {
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = valueRange,
            onValueChangeFinished = onValueChangeFinished
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text(startLabel, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Text(
                text = endLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}
