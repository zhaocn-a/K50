package com.dynamicoasis.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * SharedPreferences管理器
 * 存储应用设置和状态
 * 适配红米K50（居中挖孔屏）
 */
class PreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "dynamic_oasis_prefs_k50"
        
        // 功能开关
        private const val KEY_ENABLED = "enabled"
        private const val KEY_SHOW_CHARGING = "show_charging"
        private const val KEY_SHOW_LOW_BATTERY = "show_low_battery"
        private const val KEY_SHOW_NETWORK = "show_network"
        private const val KEY_SHOW_NOTIFICATIONS = "show_notifications"
        private const val KEY_SHOW_ALARM = "show_alarm"
        private const val KEY_SHOW_TORCH = "show_torch"
        private const val KEY_SHOW_BLUETOOTH = "show_bluetooth"
        private const val KEY_SHOW_UNLOCK = "show_unlock"
        private const val KEY_SHOW_TURBO_CHARGING = "show_turbo_charging"  // K50新增
        
        // 显示设置
        private const val KEY_ANIMATION_STYLE = "animation_style"
        private const val KEY_COLOR_PRIMARY = "color_primary"
        private const val KEY_COLOR_SECONDARY = "color_secondary"
        private const val KEY_OPACITY = "opacity"
        private const val KEY_SIZE = "size"
        private const val KEY_POSITION_Y = "position_y"
        
        // K50居中定位设置
        private const val KEY_ISLAND_POSITION_MODE = "island_position_mode"
        
        // 高级设置
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_BATTERY_OPTIMIZATION = "battery_optimization"
        private const val KEY_NOTIFICATION_FILTER = "notification_filter"
        private const val KEY_HIDE_AOSP = "hide_aosp"
        
        // K50充电设置
        private const val KEY_CHARGING_ANIMATION = "charging_animation"
        private const val KEY_TURBO_CHARGING_ENABLED = "turbo_charging_enabled"
        
        // 状态
        private const val KEY_FIRST_RUN = "first_run"
        private const val KEY_PERMISSION_GRANTED = "permission_granted"
        private const val KEY_SERVICE_RUNNING = "service_running"
        private const val KEY_DEVICE_MODEL = "device_model"
        
        @Volatile
        private var instance: PreferencesManager? = null
        
        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    // 功能开关
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }
    
    var showCharging: Boolean
        get() = prefs.getBoolean(KEY_SHOW_CHARGING, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_CHARGING, value) }
    
    var showLowBattery: Boolean
        get() = prefs.getBoolean(KEY_SHOW_LOW_BATTERY, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_LOW_BATTERY, value) }
    
    var showNetwork: Boolean
        get() = prefs.getBoolean(KEY_SHOW_NETWORK, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_NETWORK, value) }
    
    var showNotifications: Boolean
        get() = prefs.getBoolean(KEY_SHOW_NOTIFICATIONS, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_NOTIFICATIONS, value) }
    
    var showAlarm: Boolean
        get() = prefs.getBoolean(KEY_SHOW_ALARM, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_ALARM, value) }
    
    var showTorch: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TORCH, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_TORCH, value) }
    
    var showBluetooth: Boolean
        get() = prefs.getBoolean(KEY_SHOW_BLUETOOTH, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_BLUETOOTH, value) }
    
    var showUnlock: Boolean
        get() = prefs.getBoolean(KEY_SHOW_UNLOCK, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_UNLOCK, value) }
    
    // K50新增：涡轮快充提示
    var showTurboCharging: Boolean
        get() = prefs.getBoolean(KEY_SHOW_TURBO_CHARGING, true)
        set(value) = prefs.edit { putBoolean(KEY_SHOW_TURBO_CHARGING, value) }
    
    // 显示设置
    var animationStyle: Int
        get() = prefs.getInt(KEY_ANIMATION_STYLE, 0)
        set(value) = prefs.edit { putInt(KEY_ANIMATION_STYLE, value) }
    
    var colorPrimary: Int
        get() = prefs.getInt(KEY_COLOR_PRIMARY, 0xFF1A1A1A.toInt())
        set(value) = prefs.edit { putInt(KEY_COLOR_PRIMARY, value) }
    
    var colorSecondary: Int
        get() = prefs.getInt(KEY_COLOR_SECONDARY, 0xFFFFFFFF.toInt())
        set(value) = prefs.edit { putInt(KEY_COLOR_SECONDARY, value) }
    
    var opacity: Float
        get() = prefs.getFloat(KEY_OPACITY, 0.9f)
        set(value) = prefs.edit { putFloat(KEY_OPACITY, value) }
    
    var size: Float
        get() = prefs.getFloat(KEY_SIZE, 1.0f)
        set(value) = prefs.edit { putFloat(KEY_SIZE, value) }
    
    var positionY: Int
        get() = prefs.getInt(KEY_POSITION_Y, 30)
        set(value) = prefs.edit { putInt(KEY_POSITION_Y, value) }
    
    // K50居中定位模式
    var islandPositionMode: String
        get() = prefs.getString(KEY_ISLAND_POSITION_MODE, "CENTER") ?: "CENTER"
        set(value) = prefs.edit { putString(KEY_ISLAND_POSITION_MODE, value) }
    
    // 高级设置
    var autoStartEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, false)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_START, value) }
    
    var batteryOptimizationEnabled: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_OPTIMIZATION, false)
        set(value) = prefs.edit { putBoolean(KEY_BATTERY_OPTIMIZATION, value) }
    
    var notificationFilter: String
        get() = prefs.getString(KEY_NOTIFICATION_FILTER, "") ?: ""
        set(value) = prefs.edit { putString(KEY_NOTIFICATION_FILTER, value) }
    
    var hideAosp: Boolean
        get() = prefs.getBoolean(KEY_HIDE_AOSP, false)
        set(value) = prefs.edit { putBoolean(KEY_HIDE_AOSP, value) }
    
    // K50充电动画设置
    var chargingAnimation: Int
        get() = prefs.getInt(KEY_CHARGING_ANIMATION, 1)  // 默认启用脉冲动画
        set(value) = prefs.edit { putInt(KEY_CHARGING_ANIMATION, value) }
    
    var turboChargingEnabled: Boolean
        get() = prefs.getBoolean(KEY_TURBO_CHARGING_ENABLED, true)  // K50默认启用涡轮快充提示
        set(value) = prefs.edit { putBoolean(KEY_TURBO_CHARGING_ENABLED, value) }
    
    // 状态
    var isFirstRun: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN, true)
        set(value) = prefs.edit { putBoolean(KEY_FIRST_RUN, value) }
    
    var permissionGranted: Boolean
        get() = prefs.getBoolean(KEY_PERMISSION_GRANTED, false)
        set(value) = prefs.edit { putBoolean(KEY_PERMISSION_GRANTED, value) }
    
    var serviceRunning: Boolean
        get() = prefs.getBoolean(KEY_SERVICE_RUNNING, false)
        set(value) = prefs.edit { putBoolean(KEY_SERVICE_RUNNING, value) }
    
    // K50设备标识
    var deviceModel: String
        get() = prefs.getString(KEY_DEVICE_MODEL, "Redmi K50") ?: "Redmi K50"
        set(value) = prefs.edit { putString(KEY_DEVICE_MODEL, value) }
    
    fun resetToDefaults() {
        prefs.edit { clear() }
        isFirstRun = true
    }
}

/**
 * K50设备特定配置
 */
object K50Preferences {
    // 默认Y轴位置（状态栏下方）
    const val DEFAULT_POSITION_Y = 30
    
    // 灵动岛默认居中
    const val DEFAULT_POSITION_MODE = "CENTER"
    
    // K50电池容量（用于充电时间估算）
    const val BATTERY_CAPACITY_MAH = 5500
    
    // K50快充功率
    const val FAST_CHARGING_WATT = 67
    
    // 默认动画风格
    const val DEFAULT_ANIMATION_STYLE = 1  // 脉冲动画
}
