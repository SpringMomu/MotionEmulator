package com.zhufucdev.motion_emulator.ui.map

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.zhufucdev.motion_emulator.BuildConfig
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.CustomZoomButtonsController
import java.io.File

class OsmMapFragment : Fragment() {
    var onReady: ((MapController) -> Unit)? = null
    private var map: MapView? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val context = requireContext()
        Configuration.getInstance().apply {
            userAgentValue = "MotionEmulator/SpringMomu-${BuildConfig.VERSION_NAME} (+https://github.com/SpringMomu/MotionEmulator)"
            osmdroidBasePath = File(context.filesDir, "osm")
            osmdroidTileCache = File(context.cacheDir, "osm-tiles")
            // HTTP expiry is honored; osmdroid's fallback is seven days. No prefetching.
        }
        val mapView = MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            minZoomLevel = 2.0
            maxZoomLevel = 19.0
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(3.0)
            controller.setCenter(GeoPoint(0.0, 0.0))
        }
        map = mapView
        val attribution = TextView(context).apply {
            text = "\u00a9 OpenStreetMap contributors"
            textSize = 12F
            setTextColor(Color.BLACK)
            setBackgroundColor(0xDDFFFFFF.toInt())
            val pad = (4 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            setOnClickListener {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/copyright")))
            }
        }
        return FrameLayout(context).apply {
            addView(mapView, FrameLayout.LayoutParams(-1, -1))
            addView(attribution, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START))
            onReady?.invoke(OsmMapController(context, mapView))
        }
    }

    override fun onResume() { super.onResume(); map?.onResume() }
    override fun onPause() { map?.onPause(); super.onPause() }
    override fun onDestroyView() {
        map?.onDetach()
        map = null
        super.onDestroyView()
    }
}
