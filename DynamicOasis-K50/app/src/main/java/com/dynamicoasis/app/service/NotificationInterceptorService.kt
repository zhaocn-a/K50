package com.dynamicoasis.app.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.dynamicoasis.app.manager.IslandStateManager
import com.dynamicoasis.app.utils.PreferencesManager
import android.content.Context

/**
 * 通知拦截服务
 * 适配澎湃OS的通知系统
 */
class NotificationInterceptorService : NotificationListenerService() {

    private lateinit var prefs: PreferencesManager
    private var lastNotificationTime = 0L
    private val notificationCooldown = 2000L // 通知冷却时间
    
    companion object {
        var isNotificationAccessGranted = false
            private set
        
        fun updateAccessStatus(granted: Boolean) {
            isNotificationAccessGranted = granted
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        prefs = PreferencesManager.getInstance(this)
        isNotificationAccessGranted = true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isNotificationAccessGranted = false
    }
    
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        
        if (!prefs.isEnabled || !prefs.showNotifications) return
        
        // 检查通知冷却
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastNotificationTime < notificationCooldown) return
        lastNotificationTime = currentTime
        
        // 过滤系统通知
        val packageName = sbn.packageName
        if (isSystemPackage(packageName)) return
        
        // 检查通知过滤列表
        val filterList = prefs.notificationFilter.split(",").map { it.trim().lowercase() }
        if (filterList.any { packageName.lowercase().contains(it) && it.isNotEmpty() }) return
        
        // 获取通知信息
        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val content = extras.getCharSequence("android.text")?.toString() ?: ""
        
        // 获取应用名称
        val appName = getAppName(packageName)
        
        // 显示通知
        if (prefs.showNotifications) {
            IslandStateManager.showNotification(title, content, appName)
        }
        
        // 发送本地广播
        sendNotificationBroadcast(packageName, title, content)
    }
    
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // 通知被移除时的处理
    }
    
    private fun isSystemPackage(packageName: String): Boolean {
        val systemPackages = listOf(
            "com.android.systemui",
            "com.android.launcher",
            "com.miui.home",
            "com.miui.securitycenter",
            "com.lbe.security.miui",
            "com.miui.powerkeeper",
            "com.android.packageinstaller",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.android.vending",
            "com.dynamicoasis.app" // 排除自身
        )
        return systemPackages.any { packageName.startsWith(it) }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val packageManager = packageContext.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".")
        }
    }
    
    private fun sendNotificationBroadcast(packageName: String, title: String, content: String) {
        val intent = android.content.Intent("com.dynamicoasis.NOTIFICATION_POSTED").apply {
            putExtra("package", packageName)
            putExtra("title", title)
            putExtra("content", content)
            setPackage(packageContext.packageName)
        }
        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this)
            .sendBroadcast(intent)
    }
}
