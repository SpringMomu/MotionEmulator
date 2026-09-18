package com.zhufucdev.me.xposed

import android.location.Location
import com.zhufucdev.me.stub.CoordinateSystem
import com.zhufucdev.me.stub.android

/** Uses the host's AMap SDK without bundling a second copy into the plugin. */
internal class AMapLocationFactory(locationClass: Class<*>) {
    private val constructor = locationClass.getConstructor(Location::class.java)
    private val setLocationType = locationClass.getMethod("setLocationType", Int::class.javaPrimitiveType)
    private val setSatellites = locationClass.getMethod("setSatellites", Int::class.javaPrimitiveType)
    // These metadata APIs are absent from some older host SDK versions.
    private val setCoordType = try {
        locationClass.getMethod("setCoordType", String::class.java)
    } catch (_: NoSuchMethodException) { null }
    private val setOffset = try {
        locationClass.getMethod("setOffset", Boolean::class.javaPrimitiveType)
    } catch (_: NoSuchMethodException) { null }

    fun create(sample: LocationSample, offset: Boolean = true, satellites: Int = 0): Location {
        val coordinates = sample.point.amapCoordinates(offset)
        // The copy constructor preserves speed/timing, but does NOT convert coordinates.
        val location = constructor.newInstance(sample.point.android(speed = sample.speed)) as Location
        location.latitude = coordinates.latitude
        location.longitude = coordinates.longitude
        location.accuracy = 5F
        setLocationType.invoke(location, 1) // AMap LOCATION_TYPE_GPS
        setSatellites.invoke(location, satellites)
        val gcj02 = coordinates.coordinateSystem == CoordinateSystem.GCJ02
        setCoordType?.invoke(location, if (gcj02) "GCJ02" else "WGS84")
        setOffset?.invoke(location, gcj02)
        return location
    }
}
