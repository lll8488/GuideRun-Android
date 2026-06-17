package com.blindrunner.app.util

import android.content.Context
import android.content.pm.PackageManager

/**
 * 统一读取高德地图API Key，避免硬编码。
 * Key 仅在 AndroidManifest.xml 的 meta-data 中声明一处。
 */
object MapConfig {
    private var cachedKey: String? = null

    fun getAmapKey(context: Context): String {
        cachedKey?.let { return it }
        return try {
            val appInfo = context.packageManager.getApplicationInfo(
                context.packageName, PackageManager.GET_META_DATA
            )
            val key = appInfo.metaData.getString("com.amap.api.v2.apikey") ?: ""
            cachedKey = key
            key
        } catch (e: Exception) {
            ""
        }
    }
}
