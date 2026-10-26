package com.yuyue.app.ui.shell.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.yuyue.app.ui.components.LocalFileImage
import com.yuyue.app.ui.paper.EventCapsule
import com.yuyue.app.ui.paper.PaperEmptyState
import com.yuyue.app.ui.shell.DifferentiatorAggregate
import com.yuyue.app.ui.shell.DifferentiatorViewModel
import com.yuyue.app.ui.shell.FacetAxis
import com.yuyue.app.ui.shell.FacetBucket
import com.yuyue.app.ui.shell.PeriodReview
import com.yuyue.app.ui.shell.PhotoWallItem
import com.yuyue.app.ui.theme.yuyueTopBarColors
import com.yuyue.app.util.FuzzyTimeLabel
import com.yuyue.app.util.TimeUtils
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacetBrowseScreen(
    axis: FacetAxis,
    vm: DifferentiatorViewModel,
    onBack: () -> Unit,
    onOpenBucket: (FacetBucket) -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    LaunchedEffect(Unit) { vm.refresh() }
    val entries by vm.entries.collectAsState()
    val buckets = remember(entries, axis) {
        DifferentiatorAggregate.groupFor(axis, entries)
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("${DifferentiatorAggregate.axisLabel(axis)}时间线") },
                colors = yuyueTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        if (buckets.isEmpty()) {
            Column(Modifier.padding(padding).padding(20.dp)) {
                PaperEmptyState(
                    title = "还没有${DifferentiatorAggregate.axisLabel(axis)}",
                    body = when (axis) {
                        FacetAxis.Person -> "条目里的「人物」字段或领域 Person 链接会出现在这里。"
                        FacetAxis.Place -> "条目里的「地点」字段或领域 Place 链接会出现在这里。"
                        FacetAxis.Topic -> "diaryType / Topic 会出现在这里。"
                    },
                )
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item {
                    Text(
                        "本机聚合 · ${buckets.size} 组",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                items(buckets, key = { it.key }) { bucket ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenBucket(bucket) }
                            .padding(vertical = 12.dp),
                    ) {
                        Text(bucket.label, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${bucket.count} 条",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        bucket.entries.take(2).forEach { e ->
                            Text(
                                "· ${e.title.ifBlank { "（无标题）" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { onOpenEntry(e.id) }
                                    .padding(top = 2.dp),
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacetTimelineScreen(
    axis: FacetAxis,
    key: String,
    vm: DifferentiatorViewModel,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    LaunchedEffect(Unit) { vm.refresh() }
    val entries by vm.entries.collectAsState()
    val zone = remember { ZoneId.of("Asia/Shanghai") }
    val filtered = remember(entries, axis, key) {
        DifferentiatorAggregate.filterFacet(axis, entries, key)
    }
    val title = remember(entries, axis, key) {
        DifferentiatorAggregate.groupFor(axis, entries)
            .find { it.key.equals(key, ignoreCase = true) || it.label.equals(key, ignoreCase = true) }
            ?.label
            ?: key
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(title) },
                colors = yuyueTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        if (filtered.isEmpty()) {
            Column(Modifier.padding(padding).padding(20.dp)) {
                PaperEmptyState(title = "没有条目", body = "该${DifferentiatorAggregate.axisLabel(axis)}下暂无记忆。")
            }
        } else {
            LazyColumn(
                Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            ) {
                item {
                    Text(
                        "${DifferentiatorAggregate.axisLabel(axis)} · ${filtered.size} 条",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                items(filtered, key = { it.id }) { entry ->
                    val time = FuzzyTimeLabel.forEntry(entry.startAtMillis, entry.rawTranscript, zone)
                    val day = TimeUtils.millisToLocalDate(entry.startAtMillis, zone)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenEntry(entry.id) },
                    ) {
                        EventCapsule(
                            timeLabel = time,
                            title = entry.title.ifBlank { "（无标题）" },
                            support = day.toString(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodReviewScreen(
    kind: String,
    vm: DifferentiatorViewModel,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenPerson: (String) -> Unit = {},
    onOpenPlace: (String) -> Unit = {},
    onOpenTopic: (String) -> Unit = {},
) {
    LaunchedEffect(Unit) { vm.refresh() }
    val entries by vm.entries.collectAsState()
    val media by vm.media.collectAsState()
    val zone = remember { ZoneId.of("Asia/Shanghai") }
    val today = remember { LocalDate.now(zone) }
    val review: PeriodReview = remember(entries, media, kind, today) {
        if (kind == "month") {
            DifferentiatorAggregate.monthReview(entries, media, YearMonth.from(today), zone)
        } else {
            DifferentiatorAggregate.weekReview(entries, media, today, zone)
        }
    }
    val dayFmt = remember { DateTimeFormatter.ofPattern("M月d日 EEE") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (kind == "month") "本月回顾" else "本周回顾") },
                colors = yuyueTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(review.label, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "${review.entryCount} 条 · ${review.personCount} 人 · ${review.placeCount} 地 · ${review.topicCount} 题 · ${review.photoCount} 图",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (review.entryCount == 0) {
                Spacer(Modifier.height(24.dp))
                PaperEmptyState(title = "这段时间还空着", body = "说几句，落轴后再来看回顾。")
                return@Column
            }
            if (review.topPeople.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("常出现的人", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    review.topPeople.forEach { (name, n) ->
                        FilterChip(
                            selected = false,
                            onClick = { onOpenPerson(name) },
                            label = { Text("$name · $n") },
                        )
                    }
                }
            }
            if (review.topPlaces.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("常去的地方", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    review.topPlaces.forEach { (name, n) ->
                        FilterChip(
                            selected = false,
                            onClick = { onOpenPlace(name) },
                            label = { Text("$name · $n") },
                        )
                    }
                }
            }
            if (review.topTopics.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("主题", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    review.topTopics.forEach { (name, n) ->
                        FilterChip(
                            selected = false,
                            onClick = { onOpenTopic(name) },
                            label = { Text("$name · $n") },
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("按日", style = MaterialTheme.typography.titleMedium)
            review.days.forEach { dayBucket ->
                Spacer(Modifier.height(12.dp))
                Text(
                    dayBucket.day.format(dayFmt),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                dayBucket.entries.forEach { entry ->
                    val time = FuzzyTimeLabel.forEntry(entry.startAtMillis, entry.rawTranscript, zone)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onOpenEntry(entry.id) },
                    ) {
                        EventCapsule(
                            timeLabel = time,
                            title = entry.title.ifBlank { "（无标题）" },
                            support = listOfNotNull(entry.place, entry.people).joinToString(" · ").ifBlank { null },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoWallScreen(
    vm: DifferentiatorViewModel,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    LaunchedEffect(Unit) { vm.refresh() }
    val entries by vm.entries.collectAsState()
    val media by vm.media.collectAsState()
    val wall = remember(entries, media) {
        DifferentiatorAggregate.photoWall(media, entries.associateBy { it.id })
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("照片墙") },
                colors = yuyueTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        if (wall.isEmpty()) {
            Column(Modifier.padding(padding).padding(20.dp)) {
                PaperEmptyState(title = "还没有照片", body = "采集时附带图片，会按条目时间排在这里。")
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(wall, key = { it.mediaId }) { item: PhotoWallItem ->
                    LocalFileImage(
                        path = item.thumbPath.ifBlank { item.path },
                        contentDescription = item.entryTitle,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenEntry(item.entryId) },
                        contentScale = ContentScale.Crop,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalExtrasScreen(
    vm: DifferentiatorViewModel,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
) {
    LaunchedEffect(Unit) { vm.refresh() }
    val entries by vm.entries.collectAsState()
    val media by vm.media.collectAsState()
    val audio = remember(entries, media) {
        DifferentiatorAggregate.audioClips(media, entries.associateBy { it.id })
    }
    val placesWithText = remember(entries) {
        DifferentiatorAggregate.groupByPlace(entries).take(8)
    }
    var playingId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("地图 · 天气 · 原声") },
                colors = yuyueTopBarColors(),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "本机占位 · 不请求地图或天气 API",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("地点（文本）", style = MaterialTheme.typography.titleMedium)
            if (placesWithText.isEmpty()) {
                Text("尚无地点字段。", style = MaterialTheme.typography.bodySmall)
            } else {
                placesWithText.forEach { b ->
                    Text(
                        "${b.label} · ${b.count} 条（无经纬度，不画地图）",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
            Text("天气", style = MaterialTheme.typography.titleMedium)
            Text(
                "条目未存天气快照；离线不做网络拉取。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text("原声音频", style = MaterialTheme.typography.titleMedium)
            if (audio.isEmpty()) {
                Text(
                    "entry_media 中暂无 audio/*。有文件时可在本页点选回放（本地路径）。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                audio.forEach { clip ->
                    val selected = playingId == clip.mediaId
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                playingId = if (selected) null else clip.mediaId
                                onOpenEntry(clip.entryId)
                            }
                            .padding(vertical = 8.dp),
                    ) {
                        Text(clip.entryTitle, style = MaterialTheme.typography.titleSmall)
                        Text(
                            if (selected) "已选 · ${clip.path}" else clip.mime,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider()
                }
                Text(
                    "完整 MediaPlayer 接线留给真机；此处只列出本地音频媒体并跳转条目。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Compact link row used from Vault / Time / Recall. */
@Composable
fun DifferentiatorLinks(
    onPeople: () -> Unit,
    onPlaces: () -> Unit,
    onTopics: () -> Unit,
    onWeek: () -> Unit,
    onMonth: () -> Unit,
    onPhotos: () -> Unit,
    onExtras: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = false, onClick = onPeople, label = { Text("人物") })
            FilterChip(selected = false, onClick = onPlaces, label = { Text("地点") })
            FilterChip(selected = false, onClick = onTopics, label = { Text("主题") })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = false, onClick = onWeek, label = { Text("本周") })
            FilterChip(selected = false, onClick = onMonth, label = { Text("本月") })
            FilterChip(selected = false, onClick = onPhotos, label = { Text("照片墙") })
            FilterChip(selected = false, onClick = onExtras, label = { Text("地图·原声") })
        }
    }
}
