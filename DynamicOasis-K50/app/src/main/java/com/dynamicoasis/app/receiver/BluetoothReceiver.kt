package com.dynamicoasis.app.receiver

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.dynamicoasis.app.manager.IslandStateManager
import com.dynamicoasis.app.service.DynamicIslandService
import com.dynamicoasis.app.utils.PreferencesManager

/**
 * 蓝牙状态广播接收器
 * 监听蓝牙连接状态
 * 适配澎湃OS蓝牙系统
 */
class BluetoothReceiver : BroadcastReceiver() {
    
    private lateinit var prefs: PreferencesManager
    
    companion object {
        private var isBluetoothEnabled = false
        private var connectedDeviceName = ""
        
        fun isBluetoothOn(): Boolean = isBluetoothEnabled
        fun getConnectedDevice(): String = connectedDeviceName
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (!::prefs.isInitialized) {
            prefs = PreferencesManager.getInstance(context)
        }
        
        if (!prefs.isEnabled || !prefs.showBluetooth) return
        
        when (intent.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                handleBluetoothStateChange(context, intent)
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                handleDeviceConnected(context, intent)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                handleDeviceDisconnected(context, intent)
            }
        }
    }
    
    private fun handleBluetoothStateChange(context: Context, intent: Intent) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        
        when (state) {
            BluetoothAdapter.STATE_ON -> {
                isBluetoothEnabled = true
            }
            BluetoothAdapter.STATE_OFF -> {
                isBluetoothEnabled = false
                connectedDeviceName = ""
                if (prefs.showBluetooth) {
                    IslandStateManager.showBluetooth(false)
                }
            }
            BluetoothAdapter.STATE_TURNING_ON -> {
                // 正在开启蓝牙
            }
            BluetoothAdapter.STATE_TURNING_OFF -> {
                // 正在关闭蓝牙
            }
        }
    }
    
    private fun handleDeviceConnected(context: Context, intent: Intent) {
        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        
        device?.let {
            val deviceName = it.name ?: it.address
            connectedDeviceName = deviceName
            
            if (prefs.showBluetooth) {
                val showIntent = Intent(context, DynamicIslandService::class.java).apply {
                    action = DynamicIslandService.ACTION_SHOW
                    putExtra("state", "BLUETOOTH_CONNECTED")
                    putExtra("title", "蓝牙已连接")
                    putExtra("subtitle", deviceName)
                }
                context.startService(showIntent)
            }
        }
    }
    
    private fun handleDeviceDisconnected(context: Context, intent: Intent) {
        val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        
        device?.let {
            // 如果断开的是当前连接的设备，清除状态
            val deviceName = it.name ?: it.address
            if (deviceName == connectedDeviceName) {
                connectedDeviceName = ""
                
                if (prefs.showBluetooth) {
                    val showIntent = Intent(context, DynamicIslandService::class.java).apply {
                        action = DynamicIslandService.ACTION_SHOW
                        putExtra("state", "BLUETOOTH_DISCONNECTED")
                        putExtra("title", "蓝牙已断开")
                        putExtra("subtitle", deviceName)
                    }
                    context.startService(showIntent)
                }
            }
        }
    }
}

/**
 * 闹钟广播接收器
 * 监听闹钟提醒
 * 适配澎湃OS闹钟系统
 */
class AlarmReceiver : BroadcastReceiver() {
    
    private lateinit var prefs: PreferencesManager
    
    override fun onReceive(context: Context, intent: Intent) {
        if (!::prefs.isInitialized) {
            prefs = PreferencesManager.getInstance(context)
        }
        
        if (!prefs.isEnabled || !prefs.showAlarm) return
        
        when (intent.action) {
            Intent.ACTION_ALARM_CLOCK -> {
                handleAlarmClock(context, intent)
            }
            "com.dynamicoasis.ALARM_REMINDER" -> {
                handleCustomAlarm(context, intent)
            }
        }
    }
    
    private fun handleAlarmClock(context: Context, intent: Intent) {
        val alarmTime = intent.getStringExtra(android.app.AlarmManager.EXTRA_ALARM_TIME)
        
        val showIntent = Intent(context, DynamicIslandService::class.java).apply {
            action = DynamicIslandService.ACTION_SHOW
            putExtra("state", "ALARM")
            putExtra("title", "⏰ 闹钟")
            putExtra("subtitle", alarmTime ?: "时间到")
        }
        context.startService(showIntent)
    }
    
    private fun handleCustomAlarm(context: Context, intent: Intent) {
        val time = intent.getStringExtra("time") ?: ""
        val label = intent.getStringExtra("label") ?: ""
        
        val showIntent = Intent(context, DynamicIslandService::class.java).apply {
            action = DynamicIslandService.ACTION_SHOW
            putExtra("state", "ALARM")
            putExtra("title", "⏰ $label")
            putExtra("subtitle", time)
        }
        context.startService(showIntent)
    }
}

/**
 * 手电筒状态接收器
 * 监听手电筒开关状态
 */
class TorchReceiver : BroadcastReceiver() {
    
    private lateinit var prefs: PreferencesManager
    
    companion object {
        private var isTorchOn = false
        
        fun isTorchEnabled(): Boolean = isTorchOn
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (!::prefs.isInitialized) {
            prefs = PreferencesManager.getInstance(context)
        }
        
        if (!prefs.isEnabled || !prefs.showTorch) return
        
        // 检测手电筒状态变化
        // 注意: Android没有直接的手电筒状态广播，需要通过ContentObserver监听
        // 这里主要用于接收手动触发的手电筒通知
    }
}
