package com.nothopeless.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ModelsTest {

    @Test
    fun `SceneType keys match design spec`() {
        assertEquals("commute", SceneType.COMMUTE.key)
        assertEquals("shop", SceneType.SHOP.key)
        assertEquals("workplace", SceneType.WORKPLACE.key)
        assertEquals("public", SceneType.PUBLIC.key)
    }

    @Test
    fun `KindnessType keys match design spec`() {
        assertEquals("care", KindnessType.CARE.key)
        assertEquals("help", KindnessType.HELP.key)
        assertEquals("integrity", KindnessType.INTEGRITY.key)
        assertEquals("courage", KindnessType.COURAGE.key)
        assertEquals("pro", KindnessType.PRO.key)
    }

    @Test
    fun `EffectType has 6 entries`() {
        assertEquals(6, EffectType.values().size)
    }

    @Test
    fun `ReactionType keys match backend`() {
        val keys = ReactionType.values().map { it.key }
        assertEquals(listOf("notHopeless", "moved", "doToo"), keys)
    }

    @Test
    fun `Post default reactionCounts has all 3 keys`() {
        val post = Post()
        assertEquals(3, post.reactionCounts.size)
        assertEquals(0L, post.reactionCounts["notHopeless"])
        assertEquals(0L, post.reactionCounts["moved"])
        assertEquals(0L, post.reactionCounts["doToo"])
    }
}
