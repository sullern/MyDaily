package com.example.app_daily.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object LocationHelper {
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Pair<String, String>? = suspendCancellableCoroutine { continuation ->
        // 1. 检查权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // 2. 先尝试获取最后一次已知的位置（速度最快）
        val lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        if (lastKnown != null) {
            continuation.resume(Pair(lastKnown.latitude.toString(), lastKnown.longitude.toString()))
            return@suspendCancellableCoroutine
        }

        // 3. 如果没有缓存，实时请求一次定位
        val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            LocationManager.GPS_PROVIDER
        } else {
            LocationManager.NETWORK_PROVIDER
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                // 拿到位置后，恢复协程并返回
                if (continuation.isActive) {
                    continuation.resume(Pair(location.latitude.toString(), location.longitude.toString()))
                }
                locationManager.removeUpdates(this) // 拿到一次后就停止监听
            }
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        try {
            // 请求单次更新
            locationManager.requestSingleUpdate(provider, listener, null)
        } catch (e: Exception) {
            e.printStackTrace()
            if (continuation.isActive) continuation.resume(null)
        }

        // 协程取消时，移除监听
        continuation.invokeOnCancellation {
            locationManager.removeUpdates(listener)
        }
    }
}