package com.dynamicoasis.app.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.dynamicoasis.app.DynamicOasisApp
import com.dynamicoasis.app.R
import com.dynamicoasis.app.manager.IslandStateManager
import com.dynamicoasis.app.manager.K50DeviceConfig
import com.dynamicoasis.app.ui.MainActivity
import com.dynamicoasis.app.utils.PreferencesManager
import kotlin.math.abs

/**
 * 灵动岛服务 - 核心服务
 * 负责在屏幕上显示灵动岛悬浮窗
 * 适配红米K50（居中挖孔屏）
 * 
 * K50特点：
 * - 居中挖孔前置摄像头（2000万IMX596）
 * - 6.67英寸 2K AMOLED屏幕
 * - 5500mAh电池 + 67W快充
 */
class DynamicIslandService : Service(), IslandStateManager.StateListener {

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: PreferencesManager
    private var islandView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isExpanded = false
    
    // 动画相关
    private val hideDelay = 3000L // 3秒后自动隐藏（非重要状态）
    private val notificationDelay = 5000L // 通知显示5秒
    
    companion object {
        const val ACTION_START = "com.dynamicoasis.START_SERVICE"
        const val ACTION_STOP = "com.dynamicoasis.STOP_SERVICE"
        const val ACTION_SHOW = "com.dynamicoasis.SHOW_ISLAND"
        const val ACTION_HIDE = "com.dynamicoasis.HIDE_ISLAND"
        
        private var isRunning = false
        
        fun start(context: Context) {
            val intent = Intent(context, DynamicIslandService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            context.stopService(Intent(context, DynamicIslandService::class.java))
        }
        
        fun isServiceRunning(): Boolean = isRunning
    }
    
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        prefs = PreferencesManager.getInstance(this)
        IslandStateManager.addListener(this)
        isRunning = true
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SHOW -> {
                val state = intent.getStringExtra("state") ?: "IDLE"
                val title = intent.getStringExtra("title") ?: ""
                val subtitle = intent.getStringExtra("subtitle") ?: ""
                showIsland(state, title, subtitle)
            }
            ACTION_HIDE -> {
                hideIsland()
            }
            else -> {
                startForegroundNotification()
                if (canDrawOverlays()) {
                    showIslandView()
                }
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        IslandStateManager.removeListener(this)
        isRunning = false
        hideIsland()
    }
    
    private fun startForegroundNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, DynamicOasisApp.NOTIFICATION_CHANNEL_SERVICE)
            .setContentTitle("DynamicOasis K50 运行中")
            .setContentText("红米K50灵动岛适配版 - 点击打开设置")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()
        
        startForeground(1001, notification)
    }
    
    private fun canDrawOverlays(): Boolean {
        return Settings.canDrawOverlays(this)
    }
    
    private fun showIslandView() {
        if (islandView != null) return
        
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        islandView = inflater.inflate(R.layout.layout_island, null)
        
        val params = createLayoutParams()
        
        try {
            windowManager.addView(islandView, params)
            setupTouchListener()
            updateIslandContent(IslandStateManager.currentState, IslandStateManager.stateData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 创建灵动岛布局参数
     * K50适配：居中挖孔屏，灵动岛默认居中显示
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        
        // K50灵动岛尺寸（适配2K屏幕）
        val width = (280 * density).toInt()
        val height = (50 * density).toInt()
        
        return WindowManager.LayoutParams(
            width,
            height,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            // K50居中挖孔，灵动岛居中定位
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = K50DeviceConfig.ISLAND_X_OFFSET  // K50无需X轴偏移
            
            // Y轴位置：状态栏下方，距顶部至少30dp
            y = (prefs.positionY * density).toInt().coerceAtLeast((30 * density).toInt())
        }
    }
    
    private fun setupTouchListener() {
        islandView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.tag = Pair(event.rawX, event.rawY)
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val oldPair = view.tag as? Pair<Float, Float> ?: return@setOnTouchListener true
                    val dx = event.rawX - oldPair.first
                    val dy = event.rawY - oldPair.second
                    
                    val params = view.layoutParams as WindowManager.LayoutParams
                    params.x += dx.toInt()
                    params.y += dy.toInt()
                    windowManager.updateViewLayout(view, params)
                    
                    view.tag = Pair(event.rawX, event.rawY)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val dx = abs(event.rawX - (view.tag as? Pair<Float, Float>)?.first ?: 0f)
                    val dy = abs(event.rawY - (view.tag as? Pair<Float, Float>)?.second ?: 0f)
                    
                    if (dx < 10 && dy < 10) {
                        // 点击事件
                        onIslandClicked()
                    }
                    true
                }
                else -> false
            }
        }
    }
    
    private fun onIslandClicked() {
        // 点击后展开或跳转到对应应用
        if (!isExpanded) {
            expandIsland()
        } else {
            collapseIsland()
        }
    }
    
    private fun expandIsland() {
        islandView?.let { view ->
            val params = view.layoutParams as WindowManager.LayoutParams
            val density = resources.displayMetrics.density
            
            // K50适配：展开尺寸适配2K屏幕
            params.width = (320 * density).toInt()
            params.height = (120 * density).toInt()
            
            view.animate()
                .scaleX(1.15f)
                .scaleY(1.2f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
            
            windowManager.updateViewLayout(view, params)
            
            view.findViewById<View>(R.id.island_expanded_content)?.visibility = View.VISIBLE
            view.findViewById<View>(R.id.island_collapsed_content)?.visibility = View.GONE
            
            isExpanded = true
            
            // 5秒后自动收起
            handler.postDelayed({
                collapseIsland()
            }, 5000)
        }
    }
    
    private fun collapseIsland() {
        islandView?.let { view ->
            val params = view.layoutParams as WindowManager.LayoutParams
            val density = resources.displayMetrics.density
            params.width = (280 * density).toInt()
            params.height = (50 * density).toInt()
            
            view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
            
            windowManager.updateViewLayout(view, params)
            
            view.findViewById<View>(R.id.island_expanded_content)?.visibility = View.GONE
            view.findViewById<View>(R.id.island_collapsed_content)?.visibility = View.VISIBLE
            
            isExpanded = false
        }
    }
    
    private fun hideIsland() {
        islandView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            islandView = null
        }
        isExpanded = false
    }
    
    override fun onStateChanged(state: IslandStateManager.IslandState, data: IslandStateManager.StateData?) {
        handler.post {
            updateIslandContent(state, data)
            showIslandWithAnimation(state, data)
            
            // 根据状态设置自动隐藏时间
            val delay = when (state) {
                IslandStateManager.IslandState.NOTIFICATION -> notificationDelay
                IslandStateManager.IslandState.MUSIC_PLAYING,
                IslandStateManager.IslandState.MUSIC_PAUSED -> 0L // 不自动隐藏
                else -> hideDelay
            }
            
            if (delay > 0) {
                handler.postDelayed({
                    if (IslandStateManager.currentState == state) {
                        IslandStateManager.resetToIdle()
                    }
                }, delay)
            }
        }
    }
    
    private fun updateIslandContent(state: IslandStateManager.IslandState, data: IslandStateManager.StateData?) {
        islandView ?: return
        
        val iconView = islandView?.findViewById<ImageView>(R.id.island_icon)
        val titleView = islandView?.findViewById<TextView>(R.id.island_title)
        val subtitleView = islandView?.findViewById<TextView>(R.id.island_subtitle)
        
        // K50适配：添加涡轮快充状态显示
        val (iconRes, title, subtitle) = when (state) {
            IslandStateManager.IslandState.CHARGING -> Triple(
                R.drawable.ic_charging, 
                "⚡ ${data?.subtitle ?: ""}", 
                "充电中"
            )
            IslandStateManager.IslandState.FAST_CHARGING -> Triple(
                R.drawable.ic_flash_fast,
                "⚡⚡ ${data?.subtitle ?: ""}",
                "快速充电"
            )
            // K50新增：涡轮快充状态（67W）
            IslandStateManager.IslandState.TURBO_CHARGING -> Triple(
                R.drawable.ic_flash_fast,
                "⚡⚡⚡ ${data?.subtitle ?: ""}",
                "涡轮快充 67W"
            )
            IslandStateManager.IslandState.FULL_BATTERY -> Triple(
                R.drawable.ic_battery_full,
                "✓ ${data?.subtitle ?: ""}",
                "已充满 (5500mAh)"
            )
            IslandStateManager.IslandState.LOW_BATTERY -> Triple(
                R.drawable.ic_battery_low,
                "⚠ ${data?.subtitle ?: ""}",
                "低电量"
            )
            IslandStateManager.IslandState.NO_NETWORK -> Triple(
                R.drawable.ic_no_network,
                "✕",
                "无网络"
            )
            IslandStateManager.IslandState.NETWORK_CONNECTED -> Triple(
                R.drawable.ic_wifi,
                "✓",
                data?.subtitle ?: "已连接"
            )
            IslandStateManager.IslandState.NOTIFICATION -> Triple(
                R.drawable.ic_notification,
                data?.title ?: "通知",
                data?.subtitle ?: ""
            )
            IslandStateManager.IslandState.ALARM -> Triple(
                R.drawable.ic_alarm,
                "⏰",
                data?.subtitle ?: "闹钟"
            )
            IslandStateManager.IslandState.TORCH_ON -> Triple(
                R.drawable.ic_flashlight_on,
                "🔦",
                "手电筒开启"
            )
            IslandStateManager.IslandState.TORCH_OFF -> Triple(
                R.drawable.ic_flashlight_off,
                "○",
                "手电筒关闭"
            )
            IslandStateManager.IslandState.BLUETOOTH_CONNECTED -> Triple(
                R.drawable.ic_bluetooth,
                "📶",
                data?.subtitle?.ifEmpty { "蓝牙已连接" } ?: "蓝牙已连接"
            )
            IslandStateManager.IslandState.BLUETOOTH_DISCONNECTED -> Triple(
                R.drawable.ic_bluetooth_off,
                "○",
                "蓝牙已断开"
            )
            IslandStateManager.IslandState.UNLOCK -> Triple(
                R.drawable.ic_unlock,
                "✓",
                "已解锁"
            )
            IslandStateManager.IslandState.MUSIC_PLAYING -> Triple(
                R.drawable.ic_music,
                "♪ ${data?.title ?: ""}",
                data?.subtitle ?: ""
            )
            IslandStateManager.IslandState.MUSIC_PAUSED -> Triple(
                R.drawable.ic_music_paused,
                "❚❚ ${data?.title ?: ""}",
                data?.subtitle ?: ""
            )
            IslandStateManager.IslandState.IDLE -> Triple(
                R.drawable.ic_idle,
                "●",
                "K50灵动岛"
            )
            else -> Triple(R.drawable.ic_idle, "●", "")
        }
        
        iconView?.setImageResource(iconRes)
        titleView?.text = title
        subtitleView?.text = subtitle
    }
    
    private fun showIslandWithAnimation(state: IslandStateManager.IslandState, data: IslandStateManager.StateData?) {
        if (islandView == null) {
            showIslandView()
        }
        
        islandView?.let { view ->
            // 显示动画
            view.visibility = View.VISIBLE
            view.alpha = 0f
            view.scaleX = 0.8f
            view.scaleY = 0.8f
            
            view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(AccelerateDecelerateInterpolator())
                .start()
            
            // 播放动画效果
            playStateAnimation(view, state)
        }
    }
    
    private fun playStateAnimation(view: View, state: IslandStateManager.IslandState) {
        // K50适配：涡轮快充使用特殊动画
        val animation = when (state) {
            IslandStateManager.IslandState.CHARGING,
            IslandStateManager.IslandState.FAST_CHARGING -> AnimationUtils.loadAnimation(this, R.anim.pulse)
            // K50涡轮快充动画
            IslandStateManager.IslandState.TURBO_CHARGING -> AnimationUtils.loadAnimation(this, R.anim.pulse_fast)
            IslandStateManager.IslandState.LOW_BATTERY -> AnimationUtils.loadAnimation(this, R.anim.shake)
            IslandStateManager.IslandState.MUSIC_PLAYING -> AnimationUtils.loadAnimation(this, R.anim.music_wave)
            IslandStateManager.IslandState.ALARM -> AnimationUtils.loadAnimation(this, R.anim.pulse_fast)
            else -> null
        }
        
        animation?.let {
            view.findViewById<View>(R.id.island_icon)?.startAnimation(it)
        }
    }
    
    private fun showIsland(stateStr: String, title: String, subtitle: String) {
        val state = try {
            IslandStateManager.IslandState.valueOf(stateStr)
        } catch (e: Exception) {
            IslandStateManager.IslandState.IDLE
        }
        
        val data = IslandStateManager.StateData(title, subtitle)
        IslandStateManager.setState(state, data)
    }
    
    private fun hideIslandTemporary() {
        islandView?.animate()
            ?.alpha(0.5f)
            ?.setDuration(500)
            ?.withEndAction {
                islandView?.alpha = 1f
            }
            ?.start()
    }
}
