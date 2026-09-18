package com.zhufucdev.motion_emulator.extension

import android.content.Context
import com.amap.api.maps.MapsInitializer
import com.amap.api.maps.model.LatLng
import com.amap.api.services.core.LatLonPoint
import com.zhufucdev.motion_emulator.data.AMapProjector
import com.zhufucdev.me.stub.*

fun Vector2D.toAmapLatLng(): LatLng = LatLng(x, y)

fun LatLng.toPoint(): Point = Point(latitude, longitude, CoordinateSystem.GCJ02)

fun LatLonPoint.toPoint(): Point = Point(latitude, longitude, CoordinateSystem.GCJ02)

fun skipAmapFuckingLicense(context: Context) {
    MapsInitializer.updatePrivacyShow(context, true, true)
    MapsInitializer.updatePrivacyAgree(context, true)
    com.amap.api.services.core.ServiceSettings.updatePrivacyShow(context, true, true)
    com.amap.api.services.core.ServiceSettings.updatePrivacyAgree(context, true)
}

/**
 * Do minus operation, treating
 * the two [LatLng]s as 2D vectors
 */
operator fun LatLng.minus(other: LatLng) =
    LatLng(latitude - other.latitude, longitude - other.longitude)

/** Reverse geocoding uses the same Android SDK key as the map. */
suspend fun getAddressWithAmap(location: LatLng, context: Context): String? =
    com.zhufucdev.motion_emulator.ui.map.AMapPoiEngine(context).search(location.toPoint())?.name

fun Point.ensureAmapCoordinate(context: Context): Point =
    if (coordinateSystem == CoordinateSystem.WGS84) with(AMapProjector(context)) { toTarget() }.toPoint(CoordinateSystem.GCJ02)
    else this
