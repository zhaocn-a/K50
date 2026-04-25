package com.dynamicoasis.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager

/**
 * DynamicOasis Application类
 * 负责应用初始化和全局配置
 * 适配红米K50（居中挖孔屏）
 */
class DynamicOasisApp : Application(), Configuration.Provider {

    companion object {
        const val NOTIFICATION_CHANNEL_SERVICE = "dynamic_island_service_k50"
        const val NOTIFICATION_CHANNEL_ALERT = "dynamic_island_alerts"
        const val NOTIFICATION_CHANNEL_NOTIFICATIONS = "dynamic_island_notifications"
        
        // K50设备标识
        const val DEVICE_MODEL = " Redmi K50"
        const val DEVICE_CODENAME = "matisse"
        
        lateinit var instance: DynamicOasisApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
        initWorkManager()
        detectDeviceModel()
    }
    
    /**
     * 检测设备型号
     * 用于K50/K40等不同设备的适配
     */
    private fun detectDeviceModel() {
        val deviceModel = Build.MODEL
        val deviceBrand = Build.BRAND
        
        // 记录设备信息用于调试
        android.util.Log.d("DynamicOasis", "Device: $deviceBrand $deviceModel")
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        
        // 前台服务通知通道 - K50适配版
        val serviceChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_SERVICE,
            "灵动岛服务 (K50)",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "DynamicOasis K50 灵动岛功能运行的前台服务"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        
        // 警报通知通道
        val alertChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_ALERT,
            "灵动岛提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "灵动岛状态变化提醒"
            enableVibration(true)
            setShowBadge(true)
        }
        
        // 通知消息通道
        val notificationsChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_NOTIFICATIONS,
            "通知消息",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "应用通知消息"
            setShowBadge(true)
        }
        
        notificationManager.createNotificationChannels(
            listOf(serviceChannel, alertChannel, notificationsChannel)
        )
    }

    private fun initWorkManager() {
        // 初始化WorkManager（使用自定义配置）
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}

/**
 * K50设备特定常量
 */
object K50Constants {
    // 设备信息
    const val MANUFACTURER = "Xiaomi"
    const val BRAND = "Redmi"
    const val MODEL = "K50"
    
    // 屏幕配置
    const val SCREEN_WIDTH = 3200
    const val SCREEN_HEIGHT = 1440
    const val SCREEN_PPI = 526
    
    // 电池配置
    const val BATTERY_CAPACITY = 5500  // mAh
    const val FAST_CHARGING_POWER = 67  // W
    
    // 充电阈值
    const val TURBO_CHARGING_MIN_POWER = 45  // W
    const val FAST_CHARGING_MIN_POWER = 15   // W
    const val LOW_BATTERY_THRESHOLD = 20      // %
    
    // 充电时间估算（分钟）
    const val FULL_CHARGE_TIME_MINUTES = 48  // 约48分钟充满5500mAh
}
