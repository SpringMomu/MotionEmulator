package com.zhufucdev.me.xposed

import com.zhufucdev.me.stub.CoordinateSystem
import com.zhufucdev.me.stub.Point
import org.junit.Assert.*
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs

class LocationDispatcherTest {
    // Synthetic distance in metres isolates dispatch/timing from Android map SDKs.
    private fun point(x: Double) = Point(x, 0.0)
    private class Clock(var nanos: Long = 0) {
        var reads = 0
        fun read(): Long { reads++; return nanos }
    }
    private fun dispatcher(clock: Clock) = LocationDispatcher(clock::read) { a, b ->
        abs(a.latitude - b.latitude)
    }

    @Test fun everyListenerGetsTheSameEstimateForSeventeenFrames() {
        val clock = Clock()
        var measurements = 0
        val dispatcher = LocationDispatcher(clock::read) { a, b ->
            measurements++
            abs(a.latitude - b.latitude)
        }
        val received = List(3) { mutableListOf<LocationSample>() }
        received.forEachIndexed { key, list -> dispatcher.register(key) { list.add(it) } }
        dispatcher.raise(point(0.0))
        for (frame in 1..17) {
            clock.nanos = frame * 1_000_000_000L
            dispatcher.raise(point(frame * 3.0))
        }
        assertEquals(18, clock.reads)
        assertEquals(17, measurements)
        for (listener in received) {
            assertNull(listener.first().speed)
            assertEquals(List(17) { 3f }, listener.drop(1).map { it.speed })
        }
        for (frame in 0..17) {
            assertSame(received[0][frame], received[1][frame])
            assertSame(received[0][frame], received[2][frame])
        }
    }

    @Test fun callbacksDoNotAffectTheSamplingTime() {
        val clock = Clock()
        val dispatcher = dispatcher(clock)
        dispatcher.raise(point(0.0))
        val received = mutableListOf<Float?>()
        dispatcher.register(1) { received.add(it.speed); clock.nanos += 900_000_000 }
        dispatcher.register(2) { received.add(it.speed) }
        clock.nanos = 1_000_000_000
        dispatcher.raise(point(3.0))
        assertEquals(listOf(3f, 3f), received)
        assertEquals(2, clock.reads)
        assertEquals(1_000_000_000L, dispatcher.current!!.elapsedNanos)
    }

    @Test fun subMillisecondIntervalsUseNanoseconds() {
        val clock = Clock()
        val dispatcher = dispatcher(clock)
        dispatcher.raise(point(0.0))
        clock.nanos = 500_000
        dispatcher.raise(point(0.0015))
        assertEquals(3f, dispatcher.current!!.speed!!, 0.0001f)
    }

    @Test fun nonIncreasingTimestampsDoNotReplaceTheBaseline() {
        val clock = Clock(1_000_000_000)
        val dispatcher = dispatcher(clock)
        dispatcher.raise(point(0.0))
        val baseline = dispatcher.current
        dispatcher.raise(point(30.0))
        clock.nanos = 999_999_999
        dispatcher.raise(point(60.0))
        assertSame(baseline, dispatcher.current)
        clock.nanos = 2_000_000_000
        dispatcher.raise(point(3.0))
        assertEquals(3f, dispatcher.current!!.speed!!, 0f)
    }

    @Test fun stationaryPositionHasMeasuredZeroSpeed() {
        val clock = Clock()
        val dispatcher = dispatcher(clock)
        dispatcher.raise(point(3.0))
        clock.nanos = 1_000_000_000
        dispatcher.raise(point(3.0))
        assertEquals(0f, dispatcher.current!!.speed!!, 0f)
    }

    @Test fun invalidDistanceAndFloatOverflowAreUnavailable() {
        for (distance in listOf(Double.NaN, Double.POSITIVE_INFINITY,
            Double.NEGATIVE_INFINITY, -1.0, Double.MAX_VALUE)) {
            val clock = Clock()
            val dispatcher = LocationDispatcher(clock::read) { _, _ -> distance }
            dispatcher.raise(point(0.0))
            clock.nanos = 1_000_000_000
            dispatcher.raise(point(3.0))
            assertNull("distance=$distance", dispatcher.current!!.speed)
        }
    }

    @Test fun invalidCoordinatesAreNotDispatchedAndResetTheBaseline() {
        for (invalid in listOf(Point(Double.NaN, 0.0), Point(0.0, Double.POSITIVE_INFINITY),
            Point(91.0, 0.0), Point(0.0, -181.0))) {
            val clock = Clock()
            val dispatcher = dispatcher(clock)
            val received = mutableListOf<LocationSample>()
            dispatcher.register(1) { received.add(it) }
            dispatcher.raise(point(0.0))
            clock.nanos = 1_000_000_000
            dispatcher.raise(invalid)
            assertEquals(1, received.size)
            clock.nanos = 2_000_000_000
            dispatcher.raise(point(3.0))
            assertNull(received.last().speed)
        }
    }

    @Test fun distanceFailureDoesNotPoisonTheNextFrame() {
        val clock = Clock()
        var fail = true
        val dispatcher = LocationDispatcher(clock::read) { a, b ->
            if (fail) throw IllegalArgumentException("distance unavailable")
            abs(a.latitude - b.latitude)
        }
        dispatcher.raise(point(0.0))
        clock.nanos = 1_000_000_000
        dispatcher.raise(point(3.0))
        assertNull(dispatcher.current!!.speed)
        fail = false
        clock.nanos = 2_000_000_000
        dispatcher.raise(point(6.0))
        assertEquals(3f, dispatcher.current!!.speed!!, 0f)
    }

    @Test fun coordinateSystemChangeStartsANewBaseline() {
        val clock = Clock()
        val dispatcher = dispatcher(clock)
        dispatcher.raise(point(0.0))
        clock.nanos = 1_000_000_000
        dispatcher.raise(Point(3.0, 0.0, CoordinateSystem.GCJ02))
        assertNull(dispatcher.current!!.speed)
        clock.nanos = 2_000_000_000
        dispatcher.raise(Point(6.0, 0.0, CoordinateSystem.GCJ02))
        assertEquals(3f, dispatcher.current!!.speed!!, 0f)
    }

    @Test fun restartDoesNotMeasureFromThePreviousTrace() {
        val clock = Clock()
        val dispatcher = dispatcher(clock)
        dispatcher.raise(point(0.0))
        clock.nanos = 1_000_000_000
        dispatcher.raise(point(3.0))
        val old = dispatcher.current!!
        dispatcher.resetSpeed()
        assertNull(dispatcher.current!!.speed)
        assertEquals(3f, old.speed!!, 0f)
        clock.nanos = 2_000_000_000
        dispatcher.raise(point(60.0))
        assertNull(dispatcher.current!!.speed)
        clock.nanos = 3_000_000_000
        dispatcher.raise(point(63.0))
        assertEquals(3f, dispatcher.current!!.speed!!, 0f)
    }

    @Test fun samplingDoesNotDependOnHavingListeners() {
        val clock = Clock()
        val dispatcher = dispatcher(clock)
        dispatcher.raise(point(0.0))
        var speed: Float? = null
        dispatcher.register(1) { speed = it.speed }
        clock.nanos = 1_000_000_000
        dispatcher.raise(point(3.0))
        assertEquals(3f, speed!!, 0f)
    }

    @Test fun listenerChangesApplyToTheNextFrame() {
        val clock = Clock()
        val dispatcher = dispatcher(clock)
        val calls = mutableListOf<String>()
        dispatcher.register(1) {
            calls.add("first")
            dispatcher.remove(2)
            dispatcher.register(3) { calls.add("third") }
        }
        dispatcher.register(2) { calls.add("second") }
        dispatcher.raise(point(0.0))
        clock.nanos = 1_000_000_000
        dispatcher.raise(point(3.0))
        assertEquals(listOf("first", "second", "first", "third"), calls)
        assertFalse(dispatcher.remove(2))
        assertTrue(dispatcher.remove(3))
    }

    @Test fun registeringTheSameKeyReplacesTheListener() {
        val clock = Clock()
        val dispatcher = dispatcher(clock)
        val calls = mutableListOf<String>()
        dispatcher.register(1) { calls.add("old") }
        dispatcher.register(1) { calls.add("new") }
        dispatcher.raise(point(0.0))
        assertEquals(listOf("new"), calls)
    }

    @Test fun reentrantCallbackCannotChangeAnotherListenersFrame() {
        val clock = Clock()
        val dispatcher = dispatcher(clock)
        dispatcher.raise(point(0.0))
        val received = mutableListOf<LocationSample>()
        dispatcher.register(1) {
            if (it.point.latitude == 3.0) {
                clock.nanos = 2_000_000_000
                dispatcher.raise(point(9.0))
            }
        }
        dispatcher.register(2) { received.add(it) }
        clock.nanos = 1_000_000_000
        dispatcher.raise(point(3.0))
        assertEquals(3f, received.single { it.point.latitude == 3.0 }.speed!!, 0f)
        assertEquals(6f, received.single { it.point.latitude == 9.0 }.speed!!, 0f)
    }

    @Test fun concurrentFramesCannotInterleaveTheirListeners() {
        val clock = AtomicLong(0)
        val dispatcher = LocationDispatcher({ clock.getAndAdd(1_000_000_000) }) { a, b ->
            abs(a.latitude - b.latitude)
        }
        dispatcher.raise(point(0.0))
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val calls = Collections.synchronizedList(mutableListOf<LocationSample>())
        dispatcher.register(1) {
            calls.add(it)
            if (it.point.latitude == 3.0) {
                entered.countDown()
                check(release.await(5, TimeUnit.SECONDS))
            }
        }
        dispatcher.register(2) { calls.add(it) }
        val pool = Executors.newFixedThreadPool(2)
        try {
            val first = pool.submit { dispatcher.raise(point(3.0)) }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            val second = pool.submit { dispatcher.raise(point(6.0)) }
            release.countDown()
            first.get(5, TimeUnit.SECONDS)
            second.get(5, TimeUnit.SECONDS)
            assertEquals(listOf(3.0, 3.0, 6.0, 6.0), calls.map { it.point.latitude })
            assertEquals(List(4) { 3f }, calls.map { it.speed })
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
    }
}
