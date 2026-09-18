package com.zhufucdev.me.xposed

import com.zhufucdev.me.stub.CoordinateSystem
import com.zhufucdev.me.stub.Point
import org.junit.Assert.*
import org.junit.Test

class AMapCoordinatesTest {
    private val gps = Point(39.9, 116.4, CoordinateSystem.WGS84)
    private val amap = Point(39.901403529849404, 116.40624278491117, CoordinateSystem.GCJ02)

    @Test fun gpsIsConvertedToDomesticAMapCoordinates() {
        val result = gps.amapCoordinates()
        assertEquals(CoordinateSystem.GCJ02, result.coordinateSystem)
        assertEquals(amap.latitude, result.latitude, 1e-8)
        assertEquals(amap.longitude, result.longitude, 1e-8)
    }

    @Test fun pointsDrawnOnAMapArePreservedExactly() {
        assertSame(amap, amap.amapCoordinates())
    }

    @Test fun disablingOffsetPreservesGpsCoordinates() {
        val result = gps.amapCoordinates(false)
        assertEquals(CoordinateSystem.WGS84, result.coordinateSystem)
        assertEquals(gps.latitude, result.latitude, 0.0)
        assertEquals(gps.longitude, result.longitude, 0.0)
    }

    @Test fun disablingOffsetConvertsAnAMapRouteToGps() {
        val result = amap.amapCoordinates(false)
        assertEquals(CoordinateSystem.WGS84, result.coordinateSystem)
        // Existing SDK inverse projection is approximate (within metres).
        assertEquals(gps.latitude, result.latitude, 0.00005)
        assertEquals(gps.longitude, result.longitude, 0.00005)
    }

    @Test fun overseasCoordinatesStayWgs84() {
        for (system in CoordinateSystem.values()) {
            val point = Point(51.5074, -0.1278, system)
            val result = point.amapCoordinates()
            assertEquals(CoordinateSystem.WGS84, result.coordinateSystem)
            assertEquals(point.latitude, result.latitude, 0.0)
            assertEquals(point.longitude, result.longitude, 0.0)
        }
    }
}
