package com.yuyue.app.llm

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalRouterTest {
    @Test
    fun obviousRecordRecallMutateStayLocal() {
        assertEquals(RouteKind.Record, LocalRouter.classify("记下今天下午喝了咖啡"))
        assertEquals(RouteKind.Recall, LocalRouter.classify("上周喝咖啡那次是哪天"))
        assertEquals(RouteKind.Mutate, LocalRouter.classify("把昨天那条删掉"))
    }

    @Test
    fun recallBeatsRecordHintWhenBothPresent() {
        assertEquals(RouteKind.Recall, LocalRouter.classify("记下上周咖啡是哪天"))
    }

    @Test
    fun mutateHintsCoverRescheduleAndMove() {
        assertEquals(RouteKind.Mutate, LocalRouter.classify("改到三点"))
        assertEquals(RouteKind.Mutate, LocalRouter.classify("挪到工作"))
        assertEquals(RouteKind.Mutate, LocalRouter.classify("完成了那件事"))
    }

    @Test
    fun ambiguousGoesCloud() {
        assertEquals(RouteKind.Cloud, LocalRouter.classify("那个事后来怎么样了"))
    }
}
