package com.yuyue.app.ui.components

import android.widget.NumberPicker
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun YuyueWheelTimeDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    var hour by remember { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }

    YuyueAlertDialog(
        onDismissRequest = onDismiss,
        title = "选择时间",
        text = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AndroidView(
                    modifier = Modifier.weight(1f),
                    factory = { context ->
                        NumberPicker(context).apply {
                            minValue = 0
                            maxValue = 23
                            value = hour
                            setFormatter { "%02d".format(it) }
                            wrapSelectorWheel = true
                            setOnValueChangedListener { _, _, newVal -> hour = newVal }
                        }
                    },
                )
                Text(":", style = MaterialTheme.typography.headlineMedium)
                AndroidView(
                    modifier = Modifier.weight(1f),
                    factory = { context ->
                        NumberPicker(context).apply {
                            minValue = 0
                            maxValue = 59
                            value = minute
                            setFormatter { "%02d".format(it) }
                            wrapSelectorWheel = true
                            setOnValueChangedListener { _, _, newVal -> minute = newVal }
                        }
                    },
                )
            }
        },
        buttons = {
            TextButton(onClick = onDismiss) { Text("取消") }
            TextButton(onClick = { onConfirm(hour, minute) }) { Text("确定") }
        },
    )
}
