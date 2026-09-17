package com.zhufucdev.me.xposed

import com.zhufucdev.me.stub.Point

/** One position and its speed belong to the same immutable sampling instant. */
internal data class LocationSample(
    val point: Point,
    val elapsedNanos: Long,
    val speed: Float?,
)

/**
 * Estimates once per incoming position, independently of listener count/order.
 * The clock must be monotonic nanoseconds (SystemClock.elapsedRealtimeNanos on Android).
 * A null speed means unavailable; zero means a measured stationary position.
 */
internal class LocationDispatcher(
    private val clock: () -> Long,
    private val distance: (Point, Point) -> Double,
) {
    private val listeners = linkedMapOf<Any, (LocationSample) -> Unit>()
    private var previous: LocationSample? = null

    @Volatile
    var current: LocationSample? = null
        private set

    @Synchronized
    fun register(key: Any, listener: (LocationSample) -> Unit) {
        listeners[key] = listener
    }

    @Synchronized
    fun remove(key: Any): Boolean = listeners.remove(key) != null

    @Synchronized
    fun resetSpeed() {
        previous = null
        current = current?.copy(speed = null)
    }

    @Synchronized
    fun raise(point: Point) {
        if (!point.latitude.isFinite() || point.latitude !in -90.0..90.0 ||
            !point.longitude.isFinite() || point.longitude !in -180.0..180.0
        ) {
            resetSpeed()
            return
        }

        // Capture once, before invoking any callbacks. Do not advance the baseline
        // for duplicate or out-of-order timestamps.
        val now = clock()
        val last = previous
        if (last != null && now <= last.elapsedNanos) return

        val speed = if (last == null || point.coordinateSystem != last.point.coordinateSystem) {
            null
        } else {
            runCatching {
                val metres = distance(point, last.point)
                val seconds = (now - last.elapsedNanos).toDouble() / 1_000_000_000.0
                val value = metres / seconds
                value.takeIf { metres.isFinite() && metres >= 0 && it.isFinite() && it >= 0 }
                    ?.toFloat()?.takeIf { it.isFinite() }
            }.getOrNull()
        }

        val sample = LocationSample(point, now, speed)
        previous = sample
        current = sample
        // A callback can register/remove a listener without changing this frame's recipients.
        // Serializing raise calls keeps concurrent frames from interleaving their fan-out.
        listeners.values.toList().forEach { it(sample) }
    }
}
