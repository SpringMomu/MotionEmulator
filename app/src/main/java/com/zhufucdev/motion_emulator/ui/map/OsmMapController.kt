package com.zhufucdev.motion_emulator.ui.map

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.location.Location
import com.zhufucdev.me.stub.Point
import com.zhufucdev.me.stub.Trace
import com.zhufucdev.me.stub.offsetFixed
import com.zhufucdev.motion_emulator.extension.getAttrColor
import com.zhufucdev.motion_emulator.extension.isDarkModeEnabled
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay

internal fun Point.osmPoint(): GeoPoint = offsetFixed().let { GeoPoint(it.latitude, it.longitude) }

@SuppressLint("ClickableViewAccessibility")
class OsmMapController(context: Context, private val map: MapView) : MapController(context) {
    private val lineColor get() = getAttrColor(com.google.android.material.R.attr.colorTertiary, context)
    private val density get() = context.resources.displayMetrics.density

    override var displayStyle = MapStyle.NORMAL
        set(value) {
            field = value
            map.overlayManager.tilesOverlay.setColorFilter(if (value == MapStyle.NIGHT) TilesOverlay.INVERT_COLORS else null)
            map.invalidate()
        }
    override var displayType = MapDisplayType.INTERACTIVE
        set(value) {
            field = value
            map.setMultiTouchControls(value == MapDisplayType.INTERACTIVE)
        }

    init {
        map.setOnTouchListener { _, _ -> displayType == MapDisplayType.STILL }
        if (isDarkModeEnabled(context.resources)) displayStyle = MapStyle.NIGHT
    }

    override fun moveCamera(location: Point, focus: Boolean, animate: Boolean) {
        val target = location.osmPoint()
        val zoom = if (focus) 18.0 else 14.0
        if (animate) map.controller.animateTo(target, zoom, 400L)
        else { map.controller.setZoom(zoom); map.controller.setCenter(target) }
    }

    override fun boundCamera(bounds: TraceBounds, animate: Boolean) {
        val ne = bounds.northeast.osmPoint()
        val sw = bounds.southwest.osmPoint()
        map.post {
            if (ne.distanceToAsDouble(sw) < 1.0) moveCamera(bounds.northeast, focus = true, animate = animate)
            else map.zoomToBoundingBox(BoundingBox(ne.latitude, ne.longitude, sw.latitude, sw.longitude), animate, (40 * density).toInt())
        }
    }

    override fun project(x: Int, y: Int): Point = map.projection.fromPixels(x, y).let { Point(it.latitude, it.longitude) }
    override fun cameraCenter(): Point = map.mapCenter.let { Point(it.latitude, it.longitude) }
    override suspend fun getAddress(point: Point): String? = OsmPoiEngine(context).search(point)?.name

    private fun line() = Polyline(map).apply {
        outlinePaint.color = lineColor
        outlinePaint.strokeWidth = 4 * density
        setOnClickListener { _, _, _ -> false }
        map.overlays.add(this)
    }

    override fun usePen(): MapScrawl = object : MapScrawl {
        private val stroke = TraceStroke()
        private val polyline = line()
        private var last: GeoPoint? = null
        override val points get() = stroke.points
        private fun redraw() {
            val path = points.map { it.osmPoint() }
            last = path.lastOrNull()
            polyline.setPoints(path)
            map.invalidate()
        }
        override fun addPoint(point: Point) {
            val wgs = point.offsetFixed()
            val next = wgs.osmPoint()
            if (last?.distanceToAsDouble(next)?.let { it < mapCaptureAccuracy } == true) return
            stroke.add(wgs)
            redraw()
        }
        override fun markBegin() = stroke.markBegin()
        override fun undo() { stroke.undo(); redraw() }
        override fun clear() { stroke.clear(); redraw() }
    }

    override fun drawTrace(trace: Trace): MapTraceCallback {
        val polyline = line()
        val points = trace.points.map { it.osmPoint() }
        polyline.setPoints(if (points.isEmpty()) points else points + points.first())
        map.invalidate()
        return object : MapTraceCallback {
            override fun remove() { map.overlays.remove(polyline); map.invalidate() }
        }
    }

    private var location: GeoPoint? = null
    private val locationOverlay = object : Overlay() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
            if (shadow) return
            val position = location ?: return
            val pixel = mapView.projection.toPixels(position, null)
            paint.color = Color.WHITE
            canvas.drawCircle(pixel.x.toFloat(), pixel.y.toFloat(), 8 * density, paint)
            paint.color = Color.rgb(35, 120, 230)
            canvas.drawCircle(pixel.x.toFloat(), pixel.y.toFloat(), 6 * density, paint)
        }
    }
    override fun updateLocationIndicator(location: Location) {
        this.location = GeoPoint(location.latitude, location.longitude)
        if (!map.overlays.contains(locationOverlay)) map.overlays.add(locationOverlay)
        map.invalidate()
    }
}
