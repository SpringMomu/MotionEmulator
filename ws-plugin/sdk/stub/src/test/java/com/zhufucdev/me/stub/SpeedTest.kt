package com.zhufucdev.me.stub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SpeedTest {
    private val origin = Point(30.0, 120.0, CoordinateSystem.GCJ02)

    @Test fun stationaryPointHasFiniteZeroSpeed() {
        assertEquals(0.0, estimateSpeed(origin to 2000L, origin to 1000L), 0.0)
    }

    @Test fun zeroAndNegativeIntervalsAreRejected() {
        for (now in listOf(1000L, 999L)) {
            assertThrows(IllegalArgumentException::class.java) {
                estimateSpeed(origin to now, origin to 1000L)
            }
        }
    }

    @Test fun timestampOverflowIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            estimateSpeed(origin to Long.MAX_VALUE, origin to -1L)
        }
    }

    @Test fun invalidCoordinatesCannotBecomeANaNSpeed() {
        assertThrows(IllegalArgumentException::class.java) {
            estimateSpeed(Point(Double.NaN, 120.0, CoordinateSystem.GCJ02) to 2000L,
                origin to 1000L)
        }
    }
}
