package com.zhufucdev.me.stub

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationSpeedInstrumentedTest {
    private val point = Point(30.0, 120.0)

    @Test fun unknownOrInvalidSpeedIsAbsentFromAndroidLocation() {
        for (speed in listOf(null, Float.NaN, Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY, -1f)) {
            val location = point.android(speed = speed)
            assertFalse("speed=$speed", location.hasSpeed())
            assertTrue(location.speed.isFinite())
            if (Build.VERSION.SDK_INT >= 26) assertFalse(location.hasSpeedAccuracy())
        }
    }

    @Test fun stationarySpeedIsPresentAndZero() {
        val location = point.android(speed = 0f)
        assertTrue(location.hasSpeed())
        assertEquals(0f, location.speed, 0f)
        if (Build.VERSION.SDK_INT >= 26) assertTrue(location.hasSpeedAccuracy())
    }

    @Test fun validSpeedSurvivesTheLocationCopyUsedByConsumers() {
        val original = point.android(speed = 3f)
        val copy = android.location.Location(original)
        assertTrue(copy.hasSpeed())
        assertEquals(3f, copy.speed, 0f)
        assertEquals(original.latitude, copy.latitude, 0.0)
        assertEquals(original.longitude, copy.longitude, 0.0)
        assertTrue(copy.elapsedRealtimeNanos > 0)
    }
}
