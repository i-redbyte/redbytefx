package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SwizzleMachineTest {

    @Test
    fun symbolsChooseAnAlphabetAndAComponent() {
        val x = swizzleMove(SwizzleState.Start, 'x')
        val red = swizzleMove(SwizzleState.Start, 'r')

        assertEquals(SwizzleState.InSet(SwizzleAlphabet.Xyzw, 1), x.state)
        assertEquals(0, x.component)
        assertNull(x.code)
        assertEquals(SwizzleState.InSet(SwizzleAlphabet.Rgba, 1), red.state)
        assertEquals(0, red.component)
        assertEquals(3, swizzleMove(SwizzleState.Start, 'a').component)
        assertEquals(2, swizzleMove(x.state, 'z').component)
    }

    @Test
    fun mixedAlphabetsAndOverlongMasksFail() {
        val xyzw = SwizzleState.InSet(SwizzleAlphabet.Xyzw, 1)
        val full = SwizzleState.InSet(SwizzleAlphabet.Rgba, 4)

        assertEquals(SwizzleCode.MixedSets, swizzleMove(xyzw, 'g').code)
        assertEquals(SwizzleState.Error, swizzleMove(xyzw, 'g').state)
        assertEquals(SwizzleCode.TooLong, swizzleMove(full, 'r').code)
        assertEquals(SwizzleCode.UnknownSymbol, swizzleMove(SwizzleState.Start, 'q').code)
    }

    @Test
    fun errorIsAbsorbing() {
        val move = swizzleMove(SwizzleState.Error, 'x')
        assertEquals(SwizzleState.Error, move.state)
        assertNull(move.component)
        assertNull(move.code)
    }

    @Test
    fun masksFoldThroughTheSameTransition() {
        val xy = readSwizzle("xy")
        val rgba = readSwizzle("rgba")

        assertEquals(listOf(0, 1), (xy as SwizzleOutcome.Accepted).components)
        assertEquals(SwizzleAlphabet.Xyzw, xy.alphabet)
        assertEquals(listOf(0, 1, 2, 3), (rgba as SwizzleOutcome.Accepted).components)
        assertEquals(SwizzleAlphabet.Rgba, rgba.alphabet)
        assertEquals(SwizzleCode.Empty, (readSwizzle("") as SwizzleOutcome.Rejected).code)
        assertEquals(SwizzleCode.MixedSets, (readSwizzle("xg") as SwizzleOutcome.Rejected).code)
        assertEquals(SwizzleCode.TooLong, (readSwizzle("xxxxx") as SwizzleOutcome.Rejected).code)
        assertTrue(readSwizzle("w") is SwizzleOutcome.Accepted)
    }
}
