package com.yuyue.app.ui.shell

import com.yuyue.app.data.local.EntryEntity
import com.yuyue.app.data.local.EntryMediaEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class DifferentiatorAggregateTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val today = LocalDate.of(2026, 9, 20)

    private fun entry(
        id: String,
        day: LocalDate = today,
        hour: Int = 12,
        people: String? = null,
        place: String? = null,
        diaryType: String? = null,
        title: String = id,
    ) = EntryEntity(
        id = id,
        notebookId = "nb",
        entryKind = "voice",
        status = "active",
        subjectKey = null,
        polarity = null,
        title = title,
        body = title,
        rawTranscript = null,
        startAtMillis = day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli(),
        endAtMillis = null,
        statedAtMillis = 0L,
        updatedAtMillis = 0L,
        people = people,
        place = place,
        diaryType = diaryType,
    )

    private fun media(
        id: String,
        entryId: String,
        mime: String,
        created: Long = 1L,
    ) = EntryMediaEntity(
        id = id,
        entryId = entryId,
        requestId = "r",
        mime = mime,
        localPath = "/tmp/$id",
        thumbPath = "/tmp/${id}_t",
        width = 1,
        height = 1,
        sortOrder = 0,
        createdAtMillis = created,
    )

    @Test
    fun splitPeople_handlesChineseAndCommaDelimiters() {
        assertEquals(listOf("阿哲", "小雨"), DifferentiatorAggregate.splitPeople("阿哲、小雨"))
        assertEquals(listOf("A", "B"), DifferentiatorAggregate.splitPeople("A, B"))
        assertEquals(emptyList<String>(), DifferentiatorAggregate.splitPeople("  "))
    }

    @Test
    fun groupByPeople_mergesDuplicatesAndDomainLinks() {
        val entries = listOf(
            entry("e1", people = "阿哲、小雨", hour = 20),
            entry("e2", people = "阿哲", hour = 10),
            entry("e3", people = null),
            entry("e4", day = today.minusDays(1), people = "阿哲"),
        )
        val buckets = DifferentiatorAggregate.groupByPeople(
            entries,
            domainLinks = listOf(
                DomainNameLink("阿哲", setOf("e3")),
            ),
        )
        assertEquals(2, buckets.size)
        val zhe = buckets.find { it.label == "阿哲" }!!
        assertEquals(listOf("e1", "e3", "e2", "e4"), zhe.entries.map { it.id })
        assertEquals(1, buckets.find { it.label == "小雨" }!!.count)
    }

    @Test
    fun groupByPlace_andTopic() {
        val entries = listOf(
            entry("a", place = "咖啡馆", diaryType = "life"),
            entry("b", place = "咖啡馆", diaryType = "food"),
            entry("c", place = "家", diaryType = null),
            entry("d", place = null, diaryType = "life"),
        )
        val places = DifferentiatorAggregate.groupByPlace(entries)
        assertEquals(listOf("咖啡馆", "家"), places.map { it.label })
        assertEquals(2, places.first().count)

        val topics = DifferentiatorAggregate.groupByTopic(entries)
        assertEquals(3, topics.size)
        assertEquals(2, topics.find { it.label == "life" }!!.count)
        assertTrue(topics.any { it.label == "未分类" })
    }

    @Test
    fun filterFacet_isCaseInsensitive() {
        val entries = listOf(entry("1", people = "阿哲"), entry("2", people = "小雨"))
        val hit = DifferentiatorAggregate.filterFacet(FacetAxis.Person, entries, "阿哲")
        assertEquals(listOf("1"), hit.map { it.id })
    }

    @Test
    fun weekReview_aggregatesLocalBounds() {
        val mon = LocalDate.of(2026, 9, 14) // Monday
        val entries = listOf(
            entry("in", mon.plusDays(2), people = "阿哲", place = "家", diaryType = "life"),
            entry("out", mon.minusDays(1), people = "阿哲"),
            entry("sun", mon.plusDays(6), place = "公园", diaryType = "life"),
        )
        val media = listOf(
            media("m1", "in", "image/jpeg"),
            media("m2", "out", "image/png"),
            media("a1", "in", "audio/m4a"),
        )
        val review = DifferentiatorAggregate.weekReview(entries, media, mon.plusDays(3), zone)
        assertEquals(mon, review.from)
        assertEquals(mon.plusDays(6), review.toInclusive)
        assertEquals(2, review.entryCount)
        assertEquals(1, review.photoCount)
        assertEquals(1, review.personCount)
        assertEquals(2, review.placeCount)
        assertEquals(listOf("阿哲" to 1), review.topPeople)
        assertEquals(2, review.days.size)
    }

    @Test
    fun monthReview_countsDaysInMonth() {
        val month = YearMonth.of(2026, 9)
        val entries = listOf(
            entry("a", LocalDate.of(2026, 9, 1)),
            entry("b", LocalDate.of(2026, 9, 30)),
            entry("c", LocalDate.of(2026, 8, 31)),
        )
        val review = DifferentiatorAggregate.monthReview(entries, emptyList(), month, zone)
        assertEquals(2, review.entryCount)
        assertEquals("2026年9月", review.label)
    }

    @Test
    fun photoWall_skipsNonImagesAndDeleted() {
        val live = entry("e1")
        val deleted = entry("e2").copy(deletedAtMillis = 9L)
        val wall = DifferentiatorAggregate.photoWall(
            media = listOf(
                media("i1", "e1", "image/jpeg"),
                media("a1", "e1", "audio/mp4"),
                media("i2", "e2", "image/png"),
            ),
            entriesById = mapOf("e1" to live, "e2" to deleted),
        )
        assertEquals(listOf("i1"), wall.map { it.mediaId })
        assertFalse(DifferentiatorAggregate.isAudioMime("image/jpeg"))
        assertTrue(DifferentiatorAggregate.isAudioMime("audio/m4a"))
    }

    @Test
    fun axisRoute_roundTrips() {
        for (axis in FacetAxis.entries) {
            assertEquals(axis, DifferentiatorAggregate.parseAxis(DifferentiatorAggregate.axisRoute(axis)))
        }
    }
}
