package com.dynamicoasis.app.manager

/**
 * 灵动岛状态管理器
 * 管理灵动岛的各种显示状态
 * 适配红米K50（居中挖孔屏）
 */
object IslandStateManager {
    
    // 当前状态
    @Volatile
    var currentState: IslandState = IslandState.IDLE
        private set
    
    // 状态数据
    @Volatile
    var stateData: StateData? = null
        private set
    
    // 状态监听器
    private val stateListeners = mutableListOf<StateListener>()
    
    enum class IslandState {
        IDLE,                    // 空闲/常驻
        CHARGING,                // 充电中
        FAST_CHARGING,           // 快速充电
        TURBO_CHARGING,          // 涡轮快充 (K50 67W)
        FULL_BATTERY,            // 充满电
        LOW_BATTERY,             // 低电量
        NO_NETWORK,              // 无网络
        NETWORK_CONNECTED,       // 网络已连接
        NOTIFICATION,            // 通知
        ALARM,                   // 闹钟提醒
        TORCH_ON,                // 手电筒开启
        TORCH_OFF,               // 手电筒关闭
        BLUETOOTH_CONNECTED,     // 蓝牙已连接
        BLUETOOTH_DISCONNECTED,  // 蓝牙已断开
        UNLOCK,                  // 解锁提示
        MUSIC_PLAYING,           // 音乐播放
        MUSIC_PAUSED,            // 音乐暂停
        CUSTOM                   // 自定义
    }
    
    data class StateData(
        val title: String = "",
        val subtitle: String = "",
        val icon: Int = 0,
        val extra: Map<String, Any> = emptyMap(),
        val timestamp: Long = System.currentTimeMillis()
    )
    
    interface StateListener {
        fun onStateChanged(state: IslandState, data: StateData?)
    }
    
    fun addListener(listener: StateListener) {
        if (!stateListeners.contains(listener)) {
            stateListeners.add(listener)
        }
    }
    
    fun removeListener(listener: StateListener) {
        stateListeners.remove(listener)
    }
    
    fun setState(state: IslandState, data: StateData? = null) {
        if (currentState != state || stateData != data) {
            currentState = state
            stateData = data
            notifyStateChange()
        }
    }
    
    fun resetToIdle() {
        setState(IslandState.IDLE)
    }
    
    private fun notifyStateChange() {
        stateListeners.forEach { listener ->
            try {
                listener.onStateChanged(currentState, stateData)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // 便捷方法
    fun showCharging(percentage: Int, isFast: Boolean = false, isTurbo: Boolean = false) {
        val state = when {
            isTurbo -> IslandState.TURBO_CHARGING
            isFast -> IslandState.FAST_CHARGING
            else -> IslandState.CHARGING
        }
        setState(state, StateData(
            title = when (state) {
                IslandState.TURBO_CHARGING -> "⚡⚡⚡ 涡轮快充"
                IslandState.FAST_CHARGING -> "⚡⚡ 快速充电"
                else -> "⚡ 充电中"
            },
            subtitle = "$percentage%",
            extra = mapOf("percentage" to percentage, "isFast" to isFast, "isTurbo" to isTurbo)
        ))
    }
    
    fun showFullBattery() {
        setState(IslandState.FULL_BATTERY, StateData(
            title = "✓ 已充满",
            subtitle = "100% (5500mAh)"
        ))
    }
    
    fun showLowBattery(percentage: Int) {
        setState(IslandState.LOW_BATTERY, StateData(
            title = "⚠ 低电量",
            subtitle = "$percentage%",
            extra = mapOf("percentage" to percentage)
        ))
    }
    
    fun showNoNetwork() {
        setState(IslandState.NO_NETWORK, StateData(
            title = "✕ 无网络"
        ))
    }
    
    fun showNetworkConnected(type: String) {
        setState(IslandState.NETWORK_CONNECTED, StateData(
            title = "✓ 已连接",
            subtitle = type
        ))
    }
    
    fun showNotification(title: String, content: String, appName: String) {
        setState(IslandState.NOTIFICATION, StateData(
            title = appName,
            subtitle = "$title: $content",
            extra = mapOf("appName" to appName)
        ))
    }
    
    fun showAlarm(time: String) {
        setState(IslandState.ALARM, StateData(
            title = "⏰ 闹钟",
            subtitle = time
        ))
    }
    
    fun showTorch(on: Boolean) {
        val state = if (on) IslandState.TORCH_ON else IslandState.TORCH_OFF
        setState(state, StateData(
            title = if (on) "🔦 手电筒已开启" else "○ 手电筒已关闭"
        ))
    }
    
    fun showBluetooth(connected: Boolean, deviceName: String = "") {
        val state = if (connected) IslandState.BLUETOOTH_CONNECTED else IslandState.BLUETOOTH_DISCONNECTED
        setState(state, StateData(
            title = if (connected) "📶 蓝牙已连接" else "○ 蓝牙已断开",
            subtitle = if (connected && deviceName.isNotEmpty()) deviceName else ""
        ))
    }
    
    fun showUnlock() {
        setState(IslandState.UNLOCK, StateData(
            title = "✓ 已解锁"
        ))
    }
    
    fun showMusic(title: String, artist: String, isPlaying: Boolean) {
        val state = if (isPlaying) IslandState.MUSIC_PLAYING else IslandState.MUSIC_PAUSED
        setState(state, StateData(
            title = if (isPlaying) "♪ $title" else "❚❚ $title",
            subtitle = artist,
            extra = mapOf("isPlaying" to isPlaying)
        ))
    }
}

/**
 * 红米K50设备配置
 */
object K50DeviceConfig {
    // 设备标识
    const val DEVICE_MODEL = " Redmi K50"
    const val DEVICE_BRAND = "Redmi"
    const val DEVICE_CODENAME = "matisse"
    
    // 屏幕配置 - K50居中挖孔
    const val SCREEN_WIDTH = 3200  // 2K分辨率
    const val SCREEN_HEIGHT = 1440
    const val SCREEN_DENSITY = 526 // PPI
    const val HOLE_POSITION = HolePosition.CENTER  // 居中挖孔
    
    enum class HolePosition {
        LEFT,      // K40左侧挖孔
        CENTER,    // K50居中挖孔
        RIGHT
    }
    
    // 电池配置 - K50 5500mAh
    const val BATTERY_CAPACITY = 5500  // mAh
    const val FAST_CHARGING_POWER = 67  // Watt
    
    // 灵动岛位置配置 - K50居中
    const val ISLAND_GRAVITY = "CENTER_HORIZONTAL"  // 居中
    const val ISLAND_X_OFFSET = 0  // 无需X轴偏移
    
    // 摄像头位置 - K50居中
    const val FRONT_CAMERA_POSITION = "CENTER"
    const val FRONT_CAMERA_SENSOR = "IMX596"
    const val FRONT_CAMERA_PIXELS = 2000  // 万像素
}

/**
 * 充电配置
 */
object ChargingConfig {
    // K50 67W快充参数
    const val K50_MAX_POWER = 67  // Watt
    const val K50_BATTERY_CAPACITY = 5500  // mAh
    
    // 充电速度等级
    const val LEVEL_NORMAL = 0
    const val LEVEL_FAST = 1
    const val LEVEL_TURBO = 2
    
    // 充电阶段阈值
    const val TURBO_THRESHOLD = 45  // >=45W为涡轮快充
    const val FAST_THRESHOLD = 15   // >=15W为快充
    
    /**
     * 根据功率判断充电等级
     */
    fun getChargingLevel(voltageMv: Int, currentMa: Int): Int {
        val powerMw = voltageMv * currentMa
        val powerW = powerMw / 1000.0
        
        return when {
            powerW >= TURBO_THRESHOLD -> LEVEL_TURBO
            powerW >= FAST_THRESHOLD -> LEVEL_FAST
            else -> LEVEL_NORMAL
        }
    }
}
