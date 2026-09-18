package com.zhufucdev.me.xposed

import com.zhufucdev.me.stub.CoordinateSystem
import com.zhufucdev.me.stub.MapProjector
import com.zhufucdev.me.stub.Point
import com.zhufucdev.me.stub.offsetFixed
import com.zhufucdev.me.stub.toPoint

/** AMap's domestic default is GCJ-02; Android Location uses WGS-84. */
internal fun Point.amapCoordinates(offset: Boolean = true): Point {
    if (!offset || MapProjector.outOfChina(latitude, longitude)) return offsetFixed()
    // Preserve points drawn on AMap exactly: converting back and forth adds error.
    if (coordinateSystem == CoordinateSystem.GCJ02) return this
    return with(MapProjector) { toTarget() }.toPoint(CoordinateSystem.GCJ02)
}
