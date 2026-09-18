package com.zhufucdev.motion_emulator.ui.map

import com.zhufucdev.me.stub.Point

/** Explicit latitude, longitude input also works without a device geocoder. */
internal fun parseCoordinateQuery(text: String): Point? {
    val parts = text.trim().split(Regex("[,\uFF0C\\s]+"))
    if (parts.size != 2) return null
    val lat = parts[0].toDoubleOrNull() ?: return null
    val lon = parts[1].toDoubleOrNull() ?: return null
    if (!lat.isFinite() || !lon.isFinite() || lat !in -90.0..90.0 || lon !in -180.0..180.0) return null
    return Point(lat, lon)
}
