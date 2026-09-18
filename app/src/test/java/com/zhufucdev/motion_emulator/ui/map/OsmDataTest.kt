package com.zhufucdev.motion_emulator.ui.map

import com.zhufucdev.me.stub.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.junit.Assert.*
import org.junit.Test

class OsmDataTest {
    @Test fun coordinatesUseLatitudeLongitudeAndWgs84() {
        val point = parseCoordinateQuery("31.2, 121.5")!!
        assertEquals(31.2, point.latitude, 0.0)
        assertEquals(121.5, point.longitude, 0.0)
        assertEquals(CoordinateSystem.WGS84, point.coordinateSystem)
        assertNotNull(parseCoordinateQuery("-90 180"))
    }
    @Test fun invalidCoordinatesAreRejected() {
        listOf("NaN,120", "30,Infinity", "91,120", "30,181", "park", "1,2,3").forEach {
            assertNull(it, parseCoordinateQuery(it))
        }
    }
    @Test fun undoRestoresStrokeAndClearWithoutChangingSavedSnapshot() {
        val history = TraceStroke()
        history.markBegin()
        history.add(Point(31.0,121.0))
        val saved = history.points
        history.markBegin()
        history.add(Point(31.1,121.1))
        history.clear()
        assertTrue(history.points.isEmpty())
        history.undo()
        assertEquals(2, history.points.size)
        history.undo()
        assertEquals(1, history.points.size)
        history.undo()
        assertTrue(history.points.isEmpty())
        assertEquals(1, saved.size)
    }
    @Test fun oldGcjRouteLoadsAndDisplayConversionDoesNotModifyIt() {
        val json = """{"id":"legacy","name":"Example","coordSys":"GCJ02","points":[{"latitude":31.2,"longitude":121.5},{"latitude":31.21,"longitude":121.51}]}"""
        val trace = Json.decodeFromString<Trace>(json)
        val before = Json.encodeToString(trace)
        val displayed = trace.points.map { it.offsetFixed() }
        assertEquals(CoordinateSystem.WGS84, displayed.first().coordinateSystem)
        assertNotEquals(trace.points.first().longitude, displayed.first().longitude, 0.0001)
        assertEquals(before, Json.encodeToString(trace))
    }
    @Test fun newWgsRouteRoundTripsForPlugin() {
        val original = Trace("osm", "Example", listOf(Point(31.0,121.0)), CoordinateSystem.WGS84)
        val decoded = Json.decodeFromString<Trace>(Json.encodeToString(original))
        assertEquals(CoordinateSystem.WGS84, decoded.coordinateSystem)
        assertEquals(CoordinateSystem.WGS84, decoded.points.first().coordinateSystem)
        assertEquals(121.0, decoded.points.first().offsetFixed().longitude, 0.0)
    }
}
