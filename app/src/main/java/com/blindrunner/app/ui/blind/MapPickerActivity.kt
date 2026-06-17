package com.blindrunner.app.ui.blind

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.blindrunner.app.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MapPickerActivity : AppCompatActivity(), AMap.OnCameraChangeListener {

    private lateinit var mapView: MapView
    private var aMap: AMap? = null
    private var locationClient: AMapLocationClient? = null

    private val handler = Handler(Looper.getMainLooper())
    private var currentAddress = "拖动地图选择位置"
    private var currentLat = 39.9829
    private var currentLng = 116.3978
    private var reverseGeocodeJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map_picker)

        mapView = findViewById(R.id.map_view)
        mapView.onCreate(savedInstanceState)

        initMap()

        findViewById<Button>(R.id.btn_confirm).setOnClickListener {
            intent.putExtra("selected_address", currentAddress)
            intent.putExtra("selected_lat", currentLat)
            intent.putExtra("selected_lng", currentLng)
            setResult(RESULT_OK, intent)
            Toast.makeText(this, "已确认位置：$currentAddress", Toast.LENGTH_SHORT).show()
            finish()
        }

        findViewById<Button>(R.id.btn_cancel).setOnClickListener { finish() }

        findViewById<Button>(R.id.btn_location_me).setOnClickListener {
            findViewById<TextView>(R.id.tv_address).text = "正在定位..."
            locateToMe()
        }

        // 有权限就直接定位，没权限先申请
        requestLocation()

        val searchQuery = intent.getStringExtra("search_query")
        if (!searchQuery.isNullOrEmpty()) {
            searchAddress(searchQuery)
        }
    }

    private fun searchAddress(query: String) {
        lifecycleScope.launch {
            try {
                val addresses = withContext(Dispatchers.IO) {
                    Geocoder(this@MapPickerActivity, Locale.CHINESE)
                        .getFromLocationName(query, 3)
                }
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    currentLat = addr.latitude
                    currentLng = addr.longitude
                    currentAddress = buildString {
                        append(addr.adminArea ?: "")
                        append(addr.locality ?: "")
                        append(addr.subLocality ?: "")
                        append(addr.featureName ?: "")
                        append(addr.thoroughfare ?: "")
                        if (isNotEmpty()) append("附近") else append(query)
                    }
                    val latLng = LatLng(currentLat, currentLng)
                    aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f))
                    findViewById<TextView>(R.id.tv_address).text = currentAddress
                }
            } catch (e: Exception) {
                android.util.Log.e("MapPicker", "searchAddress failed", e)
            }
        }
    }

    private fun initMap() {
        try {
            aMap = mapView.map
            aMap?.apply {
                uiSettings.apply {
                    isZoomControlsEnabled = false
                    isMyLocationButtonEnabled = false
                }
                setOnCameraChangeListener(this@MapPickerActivity)
                setMapType(AMap.MAP_TYPE_NORMAL)
            }
        } catch (e: Exception) {
            // PRD 4.6: 地图SDK降级方案 — 地图加载失败时使用文字输入坐标
            aMap = null
            mapView.visibility = android.view.View.GONE
            findViewById<TextView>(R.id.tv_address).text =
                "地图加载失败，请返回使用文字输入地址\n当前坐标: %.4f, %.4f".format(currentLng, currentLat)
            com.blindrunner.app.util.TtsHelper.speak(
                "地图服务暂不可用，请使用返回按钮并通过文字输入选择跑步地点", true
            )
        }
    }

    // ========== Map drag → reverse geocode on background thread ==========

    override fun onCameraChange(position: CameraPosition) {}

    override fun onCameraChangeFinish(position: CameraPosition) {
        val target = position.target
        currentLat = target.latitude
        currentLng = target.longitude
        findViewById<TextView>(R.id.tv_address).text = "正在获取地址..."
        reverseGeocode(currentLat, currentLng)
    }

    private fun reverseGeocode(lat: Double, lng: Double) {
        reverseGeocodeJob?.cancel()
        reverseGeocodeJob = lifecycleScope.launch {
            try {
                val addr = withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(this@MapPickerActivity, Locale.CHINESE)
                    val addresses: List<Address>? =
                        geocoder.getFromLocation(lat, lng, 1)
                    addresses?.firstOrNull()?.let {
                        buildString {
                            append(it.adminArea ?: "")
                            append(it.locality ?: "")
                            append(it.subLocality ?: "")
                            append(it.featureName ?: "")
                            append(it.thoroughfare ?: "")
                            if (isNotEmpty()) append("附近")
                        }
                    } ?: "%.4f, %.4f".format(lng, lat)
                }
                currentAddress = addr
                findViewById<TextView>(R.id.tv_address).text = addr
            } catch (_: Exception) {
                val coords = "%.4f, %.4f".format(lng, lat)
                currentAddress = coords
                findViewById<TextView>(R.id.tv_address).text = coords
            }
        }
    }

    // ========== Locate to current position ==========

    private fun requestLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            // PRD 3.2: 权限申请前语音说明
            com.blindrunner.app.util.TtsHelper.speak(
                "为了帮您确定当前位置以便选择跑步地点，需要获取位置权限。" +
                "位置信息仅在地图选点时使用，您可以在系统设置中随时关闭。",
                true
            )
            // 延迟弹出权限请求，确保语音播报先被听到
            handler.postDelayed({
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 100)
            }, 3000L)
            return
        }
        locateToMe()
    }

    private fun locateToMe() {
        if (aMap == null) {
            Toast.makeText(this, "地图已降级，请使用文字搜索选择地点", Toast.LENGTH_LONG).show()
            return
        }
        try {
            aMap?.isMyLocationEnabled = true
            AMapLocationClient.setApiKey(com.blindrunner.app.util.MapConfig.getAmapKey(this))
            AMapLocationClient.updatePrivacyShow(this, true, true)
            AMapLocationClient.updatePrivacyAgree(this, true)

            // 每次都确保 client 存在且重新配置
            locationClient?.onDestroy()
            locationClient = AMapLocationClient(applicationContext)
            val option = AMapLocationClientOption().apply {
                // 网络优先，出结果快
                locationMode = AMapLocationClientOption.AMapLocationMode.Battery_Saving
                isOnceLocation = false       // 持续定位直到拿到结果
                isNeedAddress = true
                httpTimeOut = 8000
                interval = 1500
            }
            locationClient?.setLocationOption(option)
            locationClient?.setLocationListener { location ->
                if (location != null && location.errorCode == 0) {
                    currentLat = location.latitude
                    currentLng = location.longitude
                    val latLng = LatLng(currentLat, currentLng)
                    aMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                    currentAddress = location.address ?: "已定位"
                    findViewById<TextView>(R.id.tv_address).text = currentAddress
                    // 拿到结果后停止定位
                    locationClient?.stopLocation()
                }
            }
            locationClient?.startLocation()

            // 超时兜底：8秒后还没结果就停掉
            handler.postDelayed({
                if (currentLat == 39.9829 && currentLng == 116.3978) {
                    locationClient?.stopLocation()
                    Toast.makeText(this, "定位超时，请检查网络或手动拖动地图", Toast.LENGTH_LONG).show()
                }
            }, 8000L)
        } catch (e: Exception) {
            Toast.makeText(this, "定位异常: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            locateToMe()
        }
    }

    // ========== Lifecycle ==========

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        reverseGeocodeJob?.cancel()
        mapView.onDestroy()
        locationClient?.stopLocation()
        locationClient?.onDestroy()
        super.onDestroy()
    }
}
