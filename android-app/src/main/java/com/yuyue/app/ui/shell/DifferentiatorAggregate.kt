package com.yuyue.app.ui.shell

import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.data.local.EntryMediaEntity
import com.yuyue.app.util.TimeUtils
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.LinkedHashMap
import java.util.Locale

/** Axis for facet timelines grounded in legacy entry fields (+ optional domain names). */
enum class FacetAxis {
    Person,
    Place,
    Topic,
}

data class FacetBucket(
    val key: String,
    val label: String,
    val entries: List<EntryEntity>,
) {
    val count: Int get() = entries.size
}

data class PeriodDayBucket(
    val day: LocalDate,
    val entries: List<EntryEntity>,
)

data class PeriodReview(
    val label: String,
    val from: LocalDate,
    val toInclusive: LocalDate,
    val entryCount: Int,
    val personCount: Int,
    val placeCount: Int,
    val topicCount: Int,
    val photoCount: Int,
    val days: List<PeriodDayBucket>,
    val topPeople: List<Pair<String, Int>>,
    val topPlaces: List<Pair<String, Int>>,
    val topTopics: List<Pair<String, Int>>,
)

data class PhotoWallItem(
    val mediaId: String,
    val entryId: String,
    val entryTitle: String,
    val path: String,
    val thumbPath: String,
    val mime: String,
    val createdAtMillis: Long,
    val startAtMillis: Long,
)

data class DomainNameLink(
    /** Canonical display name from domain Person/Place/Topic. */
    val canonicalName: String,
    /** Event/entry ids already linked in domain join tables. */
    val linkedEntryIds: Set<String>,
)

/**
 * Local-only grouping / aggregation for Task 8 differentiators.
 * Pure functions — no Room, no network.
 */
object DifferentiatorAggregate {
    private val peopleSplit = Regex("[,，、;/｜|]+")

    fun splitPeople(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split(peopleSplit)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    fun normalizePlace(raw: String?): String? =
        raw?.trim()?.takeIf { it.isNotEmpty() }

    fun normalizeTopic(raw: String?): String {
        val t = raw?.trim().orEmpty()
        return if (t.isEmpty()) "未分类" else t
    }

    fun groupByPeople(
        entries: List<EntryEntity>,
        domainLinks: List<DomainNameLink> = emptyList(),
    ): List<FacetBucket> {
        val map = LinkedHashMap<String, MutableList<EntryEntity>>()
        val labels = LinkedHashMap<String, String>()
        fun keyOf(name: String) = name.lowercase(Locale.ROOT)

        for (e in entries.sortedByDescending { it.startAtMillis }) {
            for (n in splitPeople(e.people)) {
                val k = keyOf(n)
                labels.putIfAbsent(k, n)
                val list = map.getOrPut(k) { mutableListOf() }
                if (list.none { row -> row.id == e.id }) list += e
            }
        }
        for (link in domainLinks) {
            val k = keyOf(link.canonicalName)
            labels[k] = link.canonicalName
            val list = map.getOrPut(k) { mutableListOf() }
            for (id in link.linkedEntryIds) {
                val e = entries.find { it.id == id } ?: continue
                if (list.none { row -> row.id == e.id }) list += e
            }
            list.sortByDescending { row -> row.startAtMillis }
        }

        return map.map { (k, list) ->
            FacetBucket(
                key = k,
                label = labels[k] ?: k,
                entries = list.sortedByDescending { row -> row.startAtMillis },
            )
        }.sortedWith(compareByDescending<FacetBucket> { it.count }.thenBy { it.label })
    }

    fun groupByPlace(
        entries: List<EntryEntity>,
        domainLinks: List<DomainNameLink> = emptyList(),
    ): List<FacetBucket> {
        val map = LinkedHashMap<String, MutableList<EntryEntity>>()
        val labels = LinkedHashMap<String, String>()
        fun keyOf(name: String) = name.lowercase(Locale.ROOT)

        for (e in entries.sortedByDescending { it.startAtMillis }) {
            val place = normalizePlace(e.place) ?: continue
            val k = keyOf(place)
            labels.putIfAbsent(k, place)
            val list = map.getOrPut(k) { mutableListOf() }
            if (list.none { row -> row.id == e.id }) list += e
        }
        for (link in domainLinks) {
            val k = keyOf(link.canonicalName)
            labels[k] = link.canonicalName
            val list = map.getOrPut(k) { mutableListOf() }
            for (id in link.linkedEntryIds) {
                val e = entries.find { it.id == id } ?: continue
                if (list.none { row -> row.id == e.id }) list += e
            }
            list.sortByDescending { row -> row.startAtMillis }
        }
        return map.map { (k, list) ->
            FacetBucket(
                key = k,
                label = labels[k] ?: k,
                entries = list.sortedByDescending { row -> row.startAtMillis },
            )
        }.sortedWith(compareByDescending<FacetBucket> { it.count }.thenBy { it.label })
    }

    fun groupByTopic(
        entries: List<EntryEntity>,
        domainLinks: List<DomainNameLink> = emptyList(),
    ): List<FacetBucket> {
        val map = LinkedHashMap<String, MutableList<EntryEntity>>()
        val labels = LinkedHashMap<String, String>()
        fun keyOf(name: String) = name.lowercase(Locale.ROOT)

        for (e in entries.sortedByDescending { it.startAtMillis }) {
            val topic = normalizeTopic(e.diaryType)
            val k = keyOf(topic)
            labels.putIfAbsent(k, topic)
            val list = map.getOrPut(k) { mutableListOf() }
            if (list.none { row -> row.id == e.id }) list += e
        }
        for (link in domainLinks) {
            val k = keyOf(link.canonicalName)
            labels[k] = link.canonicalName
            val list = map.getOrPut(k) { mutableListOf() }
            for (id in link.linkedEntryIds) {
                val e = entries.find { it.id == id } ?: continue
                if (list.none { row -> row.id == e.id }) list += e
            }
            list.sortByDescending { row -> row.startAtMillis }
        }
        return map.map { (k, list) ->
            FacetBucket(
                key = k,
                label = labels[k] ?: k,
                entries = list.sortedByDescending { row -> row.startAtMillis },
            )
        }.sortedWith(compareByDescending<FacetBucket> { it.count }.thenBy { it.label })
    }

    fun groupFor(axis: FacetAxis, entries: List<EntryEntity>, domainLinks: List<DomainNameLink> = emptyList()) =
        when (axis) {
            FacetAxis.Person -> groupByPeople(entries, domainLinks)
            FacetAxis.Place -> groupByPlace(entries, domainLinks)
            FacetAxis.Topic -> groupByTopic(entries, domainLinks)
        }

    fun filterFacet(
        axis: FacetAxis,
        entries: List<EntryEntity>,
        key: String,
        domainLinks: List<DomainNameLink> = emptyList(),
    ): List<EntryEntity> {
        val bucket = groupFor(axis, entries, domainLinks).find { it.key == key.lowercase(Locale.ROOT) }
            ?: groupFor(axis, entries, domainLinks).find { it.label.equals(key, ignoreCase = true) }
        return bucket?.entries.orEmpty()
    }

    fun weekBounds(anchor: LocalDate): Pair<LocalDate, LocalDate> {
        val start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = start.plusDays(6)
        return start to end
    }

    fun weekReview(
        entries: List<EntryEntity>,
        media: List<EntryMediaEntity> = emptyList(),
        anchor: LocalDate,
        zone: ZoneId,
    ): PeriodReview {
        val (from, to) = weekBounds(anchor)
        return periodReview(
            entries = entries,
            media = media,
            from = from,
            toInclusive = to,
            label = "本周 · ${from.monthValue}/${from.dayOfMonth}–${to.monthValue}/${to.dayOfMonth}",
            zone = zone,
        )
    }

    fun monthReview(
        entries: List<EntryEntity>,
        media: List<EntryMediaEntity> = emptyList(),
        month: YearMonth,
        zone: ZoneId,
    ): PeriodReview {
        val from = month.atDay(1)
        val to = month.atEndOfMonth()
        return periodReview(
            entries = entries,
            media = media,
            from = from,
            toInclusive = to,
            label = "${month.year}年${month.monthValue}月",
            zone = zone,
        )
    }

    fun periodReview(
        entries: List<EntryEntity>,
        media: List<EntryMediaEntity>,
        from: LocalDate,
        toInclusive: LocalDate,
        label: String,
        zone: ZoneId,
    ): PeriodReview {
        val inRange = entries
            .filter {
                val d = TimeUtils.millisToLocalDate(it.startAtMillis, zone)
                !d.isBefore(from) && !d.isAfter(toInclusive)
            }
            .sortedByDescending { it.startAtMillis }
        val ids = inRange.map { it.id }.toSet()
        val photos = media.count { it.entryId in ids && isImageMime(it.mime) }
        val people = groupByPeople(inRange)
        val places = groupByPlace(inRange)
        val topics = groupByTopic(inRange)
        val byDay = inRange
            .groupBy { TimeUtils.millisToLocalDate(it.startAtMillis, zone) }
            .toSortedMap(compareByDescending { it })
            .map { (day, rows) ->
                PeriodDayBucket(day, rows.sortedByDescending { it.startAtMillis })
            }
        return PeriodReview(
            label = label,
            from = from,
            toInclusive = toInclusive,
            entryCount = inRange.size,
            personCount = people.size,
            placeCount = places.size,
            topicCount = topics.size,
            photoCount = photos,
            days = byDay,
            topPeople = people.take(5).map { it.label to it.count },
            topPlaces = places.take(5).map { it.label to it.count },
            topTopics = topics.take(5).map { it.label to it.count },
        )
    }

    fun isImageMime(mime: String?): Boolean {
        val m = mime?.lowercase(Locale.ROOT).orEmpty()
        return m.startsWith("image/") || m in setOf("image", "jpg", "jpeg", "png", "webp", "heic")
    }

    fun isAudioMime(mime: String?): Boolean {
        val m = mime?.lowercase(Locale.ROOT).orEmpty()
        return m.startsWith("audio/") || m in setOf("audio", "m4a", "aac", "mp3", "wav", "ogg")
    }

    fun photoWall(
        media: List<EntryMediaEntity>,
        entriesById: Map<String, EntryEntity>,
    ): List<PhotoWallItem> {
        return media
            .filter { isImageMime(it.mime) }
            .mapNotNull { row ->
                val entry = entriesById[row.entryId] ?: return@mapNotNull null
                if (entry.deletedAtMillis != null) return@mapNotNull null
                PhotoWallItem(
                    mediaId = row.id,
                    entryId = row.entryId,
                    entryTitle = entry.title.ifBlank { "（无标题）" },
                    path = row.localPath,
                    thumbPath = row.thumbPath.ifBlank { row.localPath },
                    mime = row.mime,
                    createdAtMillis = row.createdAtMillis,
                    startAtMillis = entry.startAtMillis,
                )
            }
            .sortedByDescending { it.startAtMillis }
    }

    fun audioClips(
        media: List<EntryMediaEntity>,
        entriesById: Map<String, EntryEntity>,
    ): List<PhotoWallItem> {
        return media
            .filter { isAudioMime(it.mime) }
            .mapNotNull { row ->
                val entry = entriesById[row.entryId] ?: return@mapNotNull null
                if (entry.deletedAtMillis != null) return@mapNotNull null
                PhotoWallItem(
                    mediaId = row.id,
                    entryId = row.entryId,
                    entryTitle = entry.title.ifBlank { "（无标题）" },
                    path = row.localPath,
                    thumbPath = row.thumbPath,
                    mime = row.mime,
                    createdAtMillis = row.createdAtMillis,
                    startAtMillis = entry.startAtMillis,
                )
            }
            .sortedByDescending { it.startAtMillis }
    }

    fun axisLabel(axis: FacetAxis): String = when (axis) {
        FacetAxis.Person -> "人物"
        FacetAxis.Place -> "地点"
        FacetAxis.Topic -> "主题"
    }

    fun parseAxis(raw: String): FacetAxis = when (raw.lowercase(Locale.ROOT)) {
        "person", "people", "人物" -> FacetAxis.Person
        "place", "地点" -> FacetAxis.Place
        "topic", "theme", "主题" -> FacetAxis.Topic
        else -> FacetAxis.Topic
    }

    fun axisRoute(axis: FacetAxis): String = when (axis) {
        FacetAxis.Person -> "person"
        FacetAxis.Place -> "place"
        FacetAxis.Topic -> "topic"
    }
}
