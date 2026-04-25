package com.dynamicoasis.app.utils

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.database.ContentObserver
import android.net.Uri
import android.os.HandlerThread
import com.dynamicoasis.app.receiver.TorchReceiver
import com.dynamicoasis.app.service.DynamicIslandService
import com.dynamicoasis.app.manager.IslandStateManager

/**
 * 手电筒状态管理器
 * 监听并检测手电筒开关状态
 * 适配澎湃OS手电筒控制
 */
class TorchManager private constructor(private val context: Context) {
    
    private val cameraManager: CameraManager by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    
    private val prefs: PreferencesManager by lazy {
        PreferencesManager.getInstance(context)
    }
    
    private var cameraId: String? = null
    private var isListening = false
    private var lastTorchState = false
    
    private var torchStateObserver: ContentObserver? = null
    private var handlerThread: HandlerThread? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    companion object {
        @Volatile
        private var instance: TorchManager? = null
        
        fun getInstance(context: Context): TorchManager {
            return instance ?: synchronized(this) {
                instance ?: TorchManager(context.applicationContext).also { instance = it }
            }
        }
    }
    
    init {
        initCameraId()
    }
    
    private fun initCameraId() {
        try {
            for (id in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
                if (hasFlash) {
                    cameraId = id
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun startListening() {
        if (isListening) return
        
        cameraId?.let { id ->
            try {
                cameraManager.registerTorchCallback(object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        if (cameraId == this@TorchManager.cameraId) {
                            handleTorchStateChange(enabled)
                        }
                    }
                    
                    override fun onTorchModeUnavailable(cameraId: String) {
                        if (cameraId == this@TorchManager.cameraId) {
                            handleTorchStateChange(false)
                        }
                    }
                }, mainHandler)
                
                isListening = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun stopListening() {
        if (!isListening) return
        
        try {
            cameraManager.unregisterTorchCallback(object : CameraManager.TorchCallback() {})
            isListening = false
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun handleTorchStateChange(enabled: Boolean) {
        if (enabled != lastTorchState) {
            lastTorchState = enabled
            TorchReceiver.isTorchOn = enabled
            
            if (prefs.isEnabled && prefs.showTorch) {
                IslandStateManager.showTorch(enabled)
                
                // 发送到服务显示
                val intent = Intent(context, DynamicIslandService::class.java).apply {
                    action = DynamicIslandService.ACTION_SHOW
                    putExtra("state", if (enabled) "TORCH_ON" else "TORCH_OFF")
                    putExtra("title", if (enabled) "手电筒已开启" else "手电筒已关闭")
                    putExtra("subtitle", "")
                }
                context.startService(intent)
            }
        }
    }
    
    fun isTorchAvailable(): Boolean {
        return cameraId != null
    }
    
    fun setTorch(enabled: Boolean): Boolean {
        cameraId ?: return false
        
        return try {
            cameraManager.setTorchMode(cameraId!!, enabled)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    fun toggleTorch(): Boolean {
        return setTorch(!lastTorchState)
    }
}
