package com.zhufucdev.motion_emulator.ui.map

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.amap.api.services.core.LatLonPoint
import com.amap.api.services.geocoder.*
import com.zhufucdev.motion_emulator.extension.ensureAmapCoordinate
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import com.amap.api.services.core.PoiItemV2
import com.amap.api.services.poisearch.PoiResultV2
import com.amap.api.services.poisearch.PoiSearchV2
import com.zhufucdev.me.stub.Point
import com.zhufucdev.motion_emulator.extension.toPoint
import kotlin.coroutines.suspendCoroutine

interface PoiSearchEngine {
    suspend fun search(point: Point): Poi?
    suspend fun search(text: String, limit: Int): List<Poi>
}

data class Poi(val city: String, val province: String, val name: String, val location: Point)

class AMapPoiEngine(private val context: Context) : PoiSearchEngine {
    override suspend fun search(point: Point): Poi? {
        val gcj = point.ensureAmapCoordinate(context)
        return withTimeoutOrNull(8000) {
            suspendCancellableCoroutine { continuation ->
                val search = GeocodeSearch(context)
                search.setOnGeocodeSearchListener(object : GeocodeSearch.OnGeocodeSearchListener {
                    override fun onRegeocodeSearched(result: RegeocodeResult?, code: Int) {
                        if (!continuation.isActive) return
                        val address = result?.regeocodeAddress?.takeIf { code == 1000 }
                        continuation.resume(address?.let {
                            Poi(it.city.orEmpty(), it.province.orEmpty(), it.formatAddress.orEmpty(), gcj)
                        })
                    }
                    override fun onGeocodeSearched(result: GeocodeResult?, code: Int) = Unit
                })
                search.getFromLocationAsyn(RegeocodeQuery(LatLonPoint(gcj.latitude, gcj.longitude), 200F, GeocodeSearch.AMAP))
            }
        }
    }

    override suspend fun search(text: String, limit: Int): List<Poi> = suspendCoroutine { res ->
        val query = PoiSearchV2.Query(text, null)
        query.pageSize = limit
        val search = PoiSearchV2(context, query)
        search.setOnPoiSearchListener(object : PoiSearchV2.OnPoiSearchListener {
            override fun onPoiSearched(p0: PoiResultV2?, p1: Int) {
                if (p0 == null) {
                    res.resumeWith(Result.failure(NullPointerException("PoiResultV2")))
                    return
                }
                res.resumeWith(Result.success(p0.pois.map {
                    Poi(it.cityName, it.provinceName, it.title, it.latLonPoint.toPoint())
                }))
            }

            override fun onPoiItemSearched(p0: PoiItemV2?, p1: Int) {}
        })
        search.searchPOIAsyn()
    }
}

class GooglePoiEngine(private val context: Context) : PoiSearchEngine {
    override suspend fun search(point: Point) = suspendCoroutine { res ->
        if (!Geocoder.isPresent()) res.resumeWith(Result.failure(IllegalStateException("Geocoder not present")))
        val coder = Geocoder(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            coder.getFromLocation(point.latitude, point.longitude, 1) {
                res.resumeWith(Result.success(it.getOrNull(0)?.poi()))
            }
        } else {
            @Suppress("DEPRECATION")
            val result = coder.getFromLocation(point.latitude, point.longitude, 1)?.get(0)
            if (result == null) {
                res.resumeWith(Result.success(null))
                return@suspendCoroutine
            }

            res.resumeWith(Result.success(result.poi()))
        }
    }

    override suspend fun search(text: String, limit: Int) = suspendCoroutine { res ->
        if (!Geocoder.isPresent()) res.resumeWith(Result.failure(IllegalStateException("Geocoder not present")))
        val coder = Geocoder(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            coder.getFromLocationName(text, limit) { addr ->
                res.resumeWith(Result.success(addr.map { it.poi() }))
            }
        } else {
            @Suppress("DEPRECATION")
            val results = coder.getFromLocationName(text, limit) ?: emptyList()
            res.resumeWith(Result.success(results.map { it.poi() }))
        }
    }

    private fun Address.poi() = Poi(
        city = subAdminArea ?: getAddressLine(0) ?: "",
        province = adminArea ?: "",
        name = subThoroughfare ?: featureName ?: "",
        location = Point(latitude, longitude)
    )
}
