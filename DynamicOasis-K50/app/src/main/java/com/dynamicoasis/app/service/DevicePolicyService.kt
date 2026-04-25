package com.dynamicoasis.app.service

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

/**
 * 设备策略服务 - 用于检测屏幕解锁状态
 * 适配澎湃OS设备管理器
 */
class DevicePolicyService : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Toast.makeText(context, "灵动岛已获得设备管理器权限", Toast.LENGTH_SHORT).show()
    }

    override fun onDisabled(context: Context, intent: Intent) {
        super.onDisabled(context, intent)
        Toast.makeText(context, "灵动岛已失去设备管理器权限", Toast.LENGTH_SHORT).show()
    }

    override fun onPasswordFailed(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordFailed(context, intent, userHandle)
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent, userHandle: android.os.UserHandle) {
        super.onPasswordSucceeded(context, intent, userHandle)
        // 密码成功后发送解锁广播
        val unlockIntent = Intent("com.dynamicoasis.SCREEN_UNLOCKED").apply {
            setPackage(context.packageName)
        }
        context.sendBroadcast(unlockIntent)
    }
}
