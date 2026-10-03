package ru.redbyte.redbytefx.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GlDrawPlanTest {
    @Test
    fun planDrawSelectsTheCallFromTheBuffersThatWerePassed() {
        assertEquals(DrawKind.None, planDraw(0, null, null))
        assertEquals(DrawKind.None, planDraw(3, null, 0))
        assertEquals(DrawKind.Arrays, planDraw(3, null, null))
        assertEquals(DrawKind.Elements, planDraw(3, 6, null))
        assertEquals(DrawKind.ArraysInstanced, planDraw(3, null, 1))
        assertEquals(DrawKind.ElementsInstanced, planDraw(8, 36, 1))
        assertThrows(IllegalArgumentException::class.java) { planDraw(3, 0, null) }
    }

    @Test
    fun indexWidthSwitchesAboveTheUnsignedShortLimit() {
        assertEquals(IndexElementKind.UnsignedShort, indexElementKind(intArrayOf(0, INDEX_SHORT_LIMIT)))
        assertEquals(IndexElementKind.UnsignedInt, indexElementKind(intArrayOf(0, INDEX_SHORT_LIMIT + 1)))
        assertThrows(IllegalArgumentException::class.java) { indexElementKind(intArrayOf()) }
        assertThrows(IllegalArgumentException::class.java) {
            indexElementKind(intArrayOf(INDEX_SHORT_LIMIT + 1, -1))
        }
    }

    @Test
    fun aMatchingFloatCountUsesSubData() {
        assertEquals(BufferUploadKind.Full, planBufferUpload(0, 8))
        assertEquals(BufferUploadKind.Sub, planBufferUpload(8, 8))
        assertEquals(BufferUploadKind.Full, planBufferUpload(8, 16))
    }
}
