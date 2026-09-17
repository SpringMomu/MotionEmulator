package com.zhufucdev.me.stub

import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.SystemClock
import java.math.RoundingMode
import java.text.DateFormat
import java.text.DecimalFormat
import java.util.*
import kotlin.random.Random

fun DateFormat.dateString(time: Long = System.currentTimeMillis()): String =
    format(Date(time))

const val SERIALIZATION_ID = "com.zhufucdev.motion_emulator"

fun Point.offsetFixed(): Point =
    with(if (coordinateSystem == CoordinateSystem.GCJ02) MapProjector else BypassProjector) { toIdeal() }
        .toPoint(CoordinateSystem.WGS84)

fun Point.android(
    provider: String = LocationManager.GPS_PROVIDER,
    speed: Float? = null,
): Location {
    val validSpeed = speed?.takeIf { it.isFinite() && it >= 0F }
    val result = Location(provider).apply {
        // fake some data
        time = System.currentTimeMillis()
        elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            elapsedRealtimeUncertaintyNanos = 5000.0 + (Random.nextDouble() - 0.5) * 1000
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            verticalAccuracyMeters = Random.nextFloat() * 10
        }
        accuracy = 1F
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            verticalAccuracyMeters = 0.1F
            if (validSpeed != null) {
                speedAccuracyMetersPerSecond = 0.01F
            }
        }
    }


    val fixed = offsetFixed()
    result.latitude = fixed.latitude
    result.longitude = fixed.longitude
    // Leave hasSpeed false for an unavailable estimate. Never publish NaN/Infinity.
    if (validSpeed != null) result.speed = validSpeed

    return result
}

/**
 * Generate a salted trace, where all points are
 * involved with random factors
 *
 * @param mapProjector Requires projection in case
 * the trace's coordination system is not WGS84,
 * in which points will be project to WGS84, then
 * back to GCJ02
 */
fun Trace.generateSaltedTrace(): List<Point> {
    val salt = this.salt
    return if (salt != null) {
        val runtime = salt.runtime()
        val projector =
            if (coordinateSystem == CoordinateSystem.GCJ02) MapProjector else BypassProjector
        points.map {
            runtime.apply(
                point = it,
                projector = projector,
                parent = this
            ).toPoint(this.coordinateSystem)
        }
    } else {
        points
    }
}

fun estimateDistance(current: Point, last: Point) =
    if (current.coordinateSystem == CoordinateSystem.WGS84 && last.coordinateSystem == CoordinateSystem.WGS84) {
        with(MapProjector) {
            current.distanceIdeal(last)
        }
    } else if (current.coordinateSystem == CoordinateSystem.GCJ02 && last.coordinateSystem == CoordinateSystem.GCJ02) {
        with(MapProjector) {
            current.distance(last)
        }
    } else {
        throw IllegalArgumentException("current comes with a different coordination " +
                "system (${current.coordinateSystem.name}) than last")
    }

/** Estimates metres/second from millisecond timestamps; invalid samples are rejected. */
fun estimateSpeed(current: Pair<Point, Long>, last: Pair<Point, Long>): Double {
    require(current.second > last.second) { "Speed requires a positive time interval" }
    val elapsed = current.second - last.second
    require(elapsed > 0) { "Time interval overflow" }
    val result = estimateDistance(current.first, last.first) / elapsed * 1000
    require(result.isFinite() && result >= 0) { "Speed must be finite and non-negative" }
    return result
}

/**
 * Length of a trace, including the closing part
 */
fun Trace.length(): Double {
    var sum = 0.0
    for (i in 1 until points.size) {
        sum += estimateDistance(points[i], points[i - 1])
    }
    sum += estimateDistance(points.first(), points.last())
    return sum
}

fun Double.toFixed(n: Int): String {
    val df = DecimalFormat(buildString {
        append("#.")
        repeat(n) {
            append("#")
        }
    })
    df.roundingMode = RoundingMode.HALF_UP
    return df.format(this)
}
