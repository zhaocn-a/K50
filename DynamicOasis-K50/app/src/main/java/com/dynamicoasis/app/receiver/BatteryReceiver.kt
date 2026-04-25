package com.dynamicoasis.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.os.BatteryManager
import com.dynamicoasis.app.manager.IslandStateManager
import com.dynamicoasis.app.manager.K50Constants
import com.dynamicoasis.app.service.DynamicIslandService
import com.dynamicoasis.app.utils.PreferencesManager

/**
 * 电池状态广播接收器
 * 监听充电状态、低电量等
 * 适配红米K50（5500mAh + 67W快充）
 */
class BatteryReceiver : BroadcastReceiver() {
    
    private lateinit var prefs: PreferencesManager
    
    companion object {
        private var isCharging = false
        private var lastPercentage = 100
        private var lastChargingLevel = 0
        
        fun isCurrentlyCharging(): Boolean = isCharging
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        if (!::prefs.isInitialized) {
            prefs = PreferencesManager.getInstance(context)
        }
        
        if (!prefs.isEnabled) return
        
        when (intent.action) {
            Intent.ACTION_BATTERY_CHANGED -> handleBatteryChanged(intent, context)
            Intent.ACTION_BATTERY_LOW -> handleBatteryLow(context)
            Intent.ACTION_BATTERY_OKAY -> handleBatteryOkay(context)
            Intent.ACTION_POWER_CONNECTED -> handlePowerConnected(context, intent)
            Intent.ACTION_POWER_DISCONNECTED -> handlePowerDisconnected(context)
        }
    }
    
    /**
     * 处理电池状态变化
     * K50适配：支持67W涡轮快充检测
     */
    private fun handleBatteryChanged(intent: Intent, context: Context) {
        val level = intent.getIntExtra("level", 0)
        val scale = intent.getIntExtra("scale", 100)
        val percentage = (level * 100) / scale
        
        val status = intent.getIntExtra("status", 0)
        val isChargingNow = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                           status == BatteryManager.BATTERY_STATUS_FULL
        
        // K50快充检测
        val plugged = intent.getIntExtra("plugged", 0)
        val voltage = intent.getIntExtra("voltage", 0)
        val current = intent.getIntExtra(BatteryManager.EXTRA_CURRENT, 0)
        
        // 计算充电功率等级
        val chargingLevel = calculateChargingLevel(plugged, voltage, current)
        
        // 判断是否为涡轮快充（K50 67W）
        val isTurboCharging = chargingLevel >= 2
        
        // 判断是否为快速充电
        val isFastCharging = chargingLevel >= 1 && !isTurboCharging
        
        isCharging = isChargingNow
        
        when {
            status == BatteryManager.BATTERY_STATUS_FULL && isChargingNow -> {
                if (prefs.showCharging) {
                    IslandStateManager.showFullBattery()
                }
            }
            isChargingNow -> {
                if (prefs.showCharging) {
                    // K50适配：支持涡轮快充状态
                    IslandStateManager.showCharging(percentage, isFastCharging, isTurboCharging)
                }
            }
            percentage <= 20 -> {
                if (prefs.showLowBattery) {
                    IslandStateManager.showLowBattery(percentage)
                }
            }
            else -> {
                if (percentage != lastPercentage) {
                    lastPercentage = percentage
                }
            }
        }
        
        lastChargingLevel = chargingLevel
    }
    
    /**
     * 计算充电功率等级
     * K50适配：支持67W涡轮快充检测
     * 
     * @param plugged 充电类型 (USB/AC/Wireless)
     * @param voltage 电压 (mV)
     * @param current 电流 (mA)
     * @return 0:普通充电, 1:快速充电, 2:涡轮快充
     */
    private fun calculateChargingLevel(plugged: Int, voltage: Int, current: Int): Int {
        // 如果没有充电电流，使用plugged类型判断
        if (current <= 0) {
            return when (plugged) {
                BatteryManager.BATTERY_PLUGGED_USB -> 1  // USB快充
                BatteryManager.BATTERY_PLUGGED_AC -> 2   // AC 67W
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> 1  // 无线快充
                else -> 0
            }
        }
        
        // 估算功率 (P = V * I / 1000)
        val powerMw = voltage * current
        val powerW = powerMw / 1000.0
        
        return when {
            powerW >= K50Constants.TURBO_CHARGING_MIN_POWER -> 2  // 涡轮快充 (45W+)
            powerW >= K50Constants.FAST_CHARGING_MIN_POWER -> 1   // 快速充电 (15W+)
            else -> 0  // 普通充电
        }
    }
    
    /**
     * 获取充电状态描述
     * K50适配：显示67W涡轮快充标识
     */
    fun getChargingStatusDescription(level: Int): String {
        return when (level) {
            2 -> "涡轮快充 67W"
            1 -> "快速充电"
            else -> "充电中"
        }
    }
    
    private fun handleBatteryLow(context: Context) {
        if (prefs.showLowBattery) {
            val intent = Intent(context, DynamicIslandService::class.java).apply {
                action = DynamicIslandService.ACTION_SHOW
                putExtra("state", "LOW_BATTERY")
                putExtra("title", "⚠ 低电量警告")
                putExtra("subtitle", "请及时充电")
            }
            context.startService(intent)
        }
    }
    
    private fun handleBatteryOkay(context: Context) {
        // 电池恢复正常
    }
    
    private fun handlePowerConnected(context: Context, intent: Intent) {
        if (!prefs.showCharging) return
        
        val plugged = intent.getIntExtra("plugged", 0)
        val description = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_USB -> "USB充电"
            BatteryManager.BATTERY_PLUGGED_AC -> "AC充电 (67W)"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "无线充电"
            else -> "充电中"
        }
        
        // 获取当前电量
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra("level", 0) ?: 0
        val scale = batteryIntent?.getIntExtra("scale", 100) ?: 100
        val percentage = (level * 100) / scale
        
        val serviceIntent = Intent(context, DynamicIslandService::class.java).apply {
            action = DynamicIslandService.ACTION_SHOW
            putExtra("state", "CHARGING")
            putExtra("title", "⚡ $percentage%")
            putExtra("subtitle", description)
        }
        context.startService(serviceIntent)
    }
    
    private fun handlePowerDisconnected(context: Context) {
        // 断开充电
    }
    
    /**
     * 估算充满电时间
     * K50: 5500mAh + 67W 快充，约48分钟
     */
    fun estimateFullChargeTime(currentPercentage: Int): Int {
        if (currentPercentage >= 100) return 0
        
        val remainingCapacity = K50Constants.BATTERY_CAPACITY * (100 - currentPercentage) / 100
        val chargingPower = 67.0  // W
        val voltage = 9.0  // V (典型快充电压)
        val current = chargingPower / voltage  // A
        
        // 估算时间（分钟）
        val timeMinutes = (remainingCapacity / (current * 1000) * 60).toInt()
        
        return timeMinutes.coerceAtLeast(1)
    }
}
