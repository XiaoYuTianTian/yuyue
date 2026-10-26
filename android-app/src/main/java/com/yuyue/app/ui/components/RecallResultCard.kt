package com.yuyue.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yuyue.app.data.WelcomeFeed
import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.data.recall.RecallGrounding
import com.yuyue.app.util.TimeUtils
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun RecallResultCard(
    hits: List<EntryEntity>,
    onOpen: (EntryEntity) -> Unit,
    onDismiss: () -> Unit,
    headline: String? = null,
    query: String = "",
) {
    val zone = ZoneId.of("Asia/Shanghai")
    val today = remember { LocalDate.now(zone) }
    val blurb = headline ?: remember(hits, query) {
        RecallGrounding.fromHits(hits, query, zone).text
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                blurb,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(10.dp))
            if (hits.isNotEmpty()) {
                Column(
                    modifier = Modifier.heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    hits.forEach { e ->
                        val day = TimeUtils.millisToLocalDate(e.startAtMillis, zone)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(e) }
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column {
                                Text(
                                    WelcomeFeed.shortDayLabel(day, today),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    TimeUtils.formatTime(e.startAtMillis, zone),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                e.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("关闭")
            }
        }
    }
}
