package com.androidvisualqa.annotation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StrokeModelTest {

    @Test
    fun `empty stroke has no points`() {
        val stroke = Stroke()
        assert(stroke.points.isEmpty())
    }

    @Test
    fun `stroke with points returns them in order`() {
        val stroke = Stroke(
            points = listOf(
                OffsetF(0f, 0f),
                OffsetF(100f, 0f),
                OffsetF(100f, 100f),
            ),
        )
        assertEquals(3, stroke.points.size)
        assertEquals(OffsetF(100f, 100f), stroke.points.last())
    }

    @Test
    fun `offsetF xy companion creates instance`() {
        val o = OffsetF.xy(42f, 99f)
        assertEquals(42f, o.x)
        assertEquals(99f, o.y)
    }
}
