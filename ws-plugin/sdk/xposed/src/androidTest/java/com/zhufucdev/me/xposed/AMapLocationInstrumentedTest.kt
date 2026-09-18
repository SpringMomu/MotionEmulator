package com.zhufucdev.me.xposed

import android.os.Parcel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClientOption
import com.zhufucdev.me.stub.CoordinateSystem
import com.zhufucdev.me.stub.Point
import com.zhufucdev.me.stub.android
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AMapLocationInstrumentedTest {
    private val gps = Point(39.9, 116.4, CoordinateSystem.WGS84)
    private val gcj = Point(39.901403529849404, 116.40624278491117, CoordinateSystem.GCJ02)
    private val factory = AMapLocationFactory(AMapLocation::class.java)
    private fun create(point: Point = gcj, speed: Float? = 3f, offset: Boolean = true) =
        factory.create(LocationSample(point, 1L, speed), offset, 12) as AMapLocation

    @Test fun gpsAndAMapCallbacksRepresentTheSamePlaceInDifferentSystems() {
        val android = gcj.android(speed = 3f)
        val amap = create()
        assertEquals(gps.latitude, android.latitude, 0.00005)
        assertEquals(gps.longitude, android.longitude, 0.00005)
        assertEquals(gcj.latitude, amap.latitude, 0.0)
        assertEquals(gcj.longitude, amap.longitude, 0.0)
        assertEquals(AMapLocation.COORD_TYPE_GCJ02, amap.coordType)
        assertTrue(amap.isOffset)
        assertEquals(AMapLocation.LOCATION_TYPE_GPS, amap.locationType)
        assertEquals(12, amap.satellites)
        assertEquals(3f, amap.speed, 0f)
        assertTrue(amap.hasSpeed())
        assertTrue(amap.elapsedRealtimeNanos > 0)
    }

    @Test fun wgs84RouteReceivesTheDomesticOffsetOnce() {
        val amap = create(gps)
        assertEquals(gcj.latitude, amap.latitude, 1e-8)
        assertEquals(gcj.longitude, amap.longitude, 1e-8)
        assertEquals(AMapLocation.COORD_TYPE_GCJ02, amap.coordType)
    }

    @Test fun realClientOptionCanDisableOffset() {
        val options = AMapClientOptions()
        val client = Any()
        options.set(client, AMapLocationClientOption().setOffset(false))
        val amap = create(gps, offset = options.offset(client))
        assertEquals(gps.latitude, amap.latitude, 0.0)
        assertEquals(gps.longitude, amap.longitude, 0.0)
        assertEquals(AMapLocation.COORD_TYPE_WGS84, amap.coordType)
        assertFalse(amap.isOffset)
    }

    @Test fun overseasCoordinatesAndMetadataStayWgs84() {
        val point = Point(51.5074, -0.1278, CoordinateSystem.WGS84)
        val amap = create(point)
        assertEquals(point.latitude, amap.latitude, 0.0)
        assertEquals(point.longitude, amap.longitude, 0.0)
        assertEquals(AMapLocation.COORD_TYPE_WGS84, amap.coordType)
        assertFalse(amap.isOffset)
    }

    @Test fun missingAndInvalidSpeedsRemainUnavailable() {
        for (speed in listOf(null, Float.NaN, Float.POSITIVE_INFINITY, -1f)) {
            val amap = create(speed = speed)
            assertFalse("speed=$speed", amap.hasSpeed())
            assertTrue(amap.speed.isFinite())
        }
        assertTrue(create(speed = 0f).hasSpeed())
        assertEquals(0f, create(speed = 0f).speed, 0f)
    }

    @Test fun laterFramesCannotMoveAnEarlierLocationOrItsCopies() {
        val first = create()
        create(Point(30.0, 120.0, CoordinateSystem.GCJ02))
        val parcel = Parcel.obtain()
        try {
            first.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val copy = AMapLocation.CREATOR.createFromParcel(parcel)
            for (location in listOf(first, first.clone(), copy)) {
                assertEquals(gcj.latitude, location.latitude, 0.0)
                assertEquals(gcj.longitude, location.longitude, 0.0)
                assertEquals(AMapLocation.COORD_TYPE_GCJ02, location.coordType)
                assertEquals(3f, location.speed, 0f)
            }
        } finally { parcel.recycle() }
    }
}
