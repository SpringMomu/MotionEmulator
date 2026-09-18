package com.zhufucdev.motion_emulator.ui.map

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.zhufucdev.me.stub.Point
import com.zhufucdev.me.stub.offsetFixed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/** Uses the system geocoder; never sends autocomplete to public Nominatim. */
class OsmPoiEngine(private val context: Context) : PoiSearchEngine {
    override suspend fun search(point: Point): Poi? {
        val wgs = point.offsetFixed()
        return addresses(point = wgs).firstOrNull()?.poi()
    }

    override suspend fun search(text: String, limit: Int): List<Poi> {
        if (text.isBlank() || limit <= 0) return emptyList()
        parseCoordinateQuery(text)?.let {
            return listOf(Poi("", "", String.format(Locale.ROOT, "%.6f, %.6f", it.latitude, it.longitude), it))
        }
        return addresses(text = text, limit = limit.coerceAtMost(20)).map { it.poi() }
    }

    private suspend fun addresses(point: Point? = null, text: String = "", limit: Int = 1): List<Address> {
        if (!Geocoder.isPresent()) return emptyList()
        return try {
            withTimeoutOrNull(8000) {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { continuation ->
                        val listener = object : Geocoder.GeocodeListener {
                            override fun onGeocode(addresses: MutableList<Address>) {
                                if (continuation.isActive) continuation.resume(addresses)
                            }
                            override fun onError(errorMessage: String?) {
                                if (continuation.isActive) continuation.resume(emptyList())
                            }
                        }
                        if (point != null) geocoder.getFromLocation(point.latitude, point.longitude, limit, listener)
                        else geocoder.getFromLocationName(text, limit, listener)
                    }
                } else withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    (if (point != null) geocoder.getFromLocation(point.latitude, point.longitude, limit)
                    else geocoder.getFromLocationName(text, limit)) ?: emptyList()
                }
            } ?: emptyList()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { emptyList() }
    }

    private fun Address.poi() = Poi(
        city = locality ?: subAdminArea ?: "",
        province = adminArea ?: "",
        name = getAddressLine(0) ?: featureName ?: "",
        location = Point(latitude, longitude)
    )
}
