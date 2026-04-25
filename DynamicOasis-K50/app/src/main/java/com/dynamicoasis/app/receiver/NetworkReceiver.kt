package com.dynamicoasis.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import com.dynamicoasis.app.manager.IslandStateManager
import com.dynamicoasis.app.service.DynamicIslandService
import com.dynamicoasis.app.utils.PreferencesManager

/**
 * 网络状态广播接收器
 * 监听网络连接状态变化
 * 适配澎湃OS网络系统
 */
class NetworkReceiver : BroadcastReceiver() {
    
    private lateinit var prefs: PreferencesManager
    private var lastNetworkType = ""
    
    companion object {
        private var isConnected = false
        private var currentNetworkType = ""
        
        fun isNetworkConnected(): Boolean = isConnected
        fun getCurrentNetworkType(): String = currentNetworkType
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (!::prefs.isInitialized) {
            prefs = PreferencesManager.getInstance(context)
        }
        
        if (!prefs.isEnabled || !prefs.showNetwork) return
        
        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION,
            "android.net.conn.CONNECTIVITY_CHANGE" -> {
                checkNetworkStatus(context)
            }
            "android.net.wifi.STATE_CHANGE" -> {
                handleWifiStateChange(context, intent)
            }
        }
    }
    
    private fun checkNetworkStatus(context: Context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        
        if (network != null && capabilities != null) {
            val type = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "移动数据"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "以太网"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "蓝牙"
                else -> "未知"
            }
            
            isConnected = true
            currentNetworkType = type
            
            if (type != lastNetworkType) {
                lastNetworkType = type
                IslandStateManager.showNetworkConnected(type)
            }
        } else {
            isConnected = false
            if (lastNetworkType.isNotEmpty()) {
                lastNetworkType = ""
                IslandStateManager.showNoNetwork()
            }
        }
    }
    
    private fun handleWifiStateChange(context: Context, intent: Intent) {
        val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
        
        when (wifiState) {
            WifiManager.WIFI_STATE_ENABLED -> {
                checkNetworkStatus(context)
            }
            WifiManager.WIFI_STATE_DISABLED -> {
                // WiFi已断开，检查是否有其他网络
                checkNetworkStatus(context)
            }
        }
    }
}

/**
 * 屏幕状态广播接收器
 * 监听屏幕解锁、亮灭屏
 */
class ScreenReceiver : BroadcastReceiver() {
    
    private lateinit var prefs: PreferencesManager
    private var lastScreenState = ""
    
    override fun onReceive(context: Context, intent: Intent) {
        if (!::prefs.isInitialized) {
            prefs = PreferencesManager.getInstance(context)
        }
        
        if (!prefs.isEnabled) return
        
        when (intent.action) {
            Intent.ACTION_USER_PRESENT -> handleUserPresent(context)
            Intent.ACTION_SCREEN_ON -> handleScreenOn(context)
            Intent.ACTION_SCREEN_OFF -> handleScreenOff(context)
            Intent.ACTION_CONFIGURATION_CHANGED -> handleConfigurationChange(context)
        }
    }
    
    private fun handleUserPresent(context: Context) {
        // 用户解锁屏幕
        if (prefs.showUnlock) {
            val intent = Intent(context, DynamicIslandService::class.java).apply {
                action = DynamicIslandService.ACTION_SHOW
                putExtra("state", "UNLOCK")
                putExtra("title", "已解锁")
                putExtra("subtitle", "欢迎回来")
            }
            context.startService(intent)
        }
    }
    
    private fun handleScreenOn(context: Context) {
        lastScreenState = "ON"
    }
    
    private fun handleScreenOff(context: Context) {
        lastScreenState = "OFF"
    }
    
    private fun handleConfigurationChange(context: Context) {
        // 系统配置变化（如旋转屏幕）
    }
}
