package com.dynamicoasis.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dynamicoasis.app.*
import com.dynamicoasis.app.databinding.ActivityMainBinding
import com.dynamicoasis.app.service.DynamicIslandService
import com.dynamicoasis.app.utils.PreferencesManager
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * 主界面Activity
 * 灵动岛主控制界面
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    
    private val requiredPermissions = mutableListOf(
        Manifest.permission.POST_NOTIFICATIONS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.RECEIVE_BOOT_COMPLETED)
        }
    }
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            prefs.isPermissionGranted = true
            updateUI()
        } else {
            showPermissionDeniedDialog()
        }
    }
    
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            checkOtherPermissions()
        } else {
            showOverlayPermissionDeniedDialog()
        }
    }
    
    private val notificationListenerPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkOtherPermissions()
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PreferencesManager.getInstance(this)
        
        setupUI()
        setupListeners()
        
        if (prefs.isFirstRun) {
            startOnboarding()
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateUI()
        checkPermissions()
    }
    
    private fun setupUI() {
        // 隐藏系统栏
        hideSystemBars()
        
        // 设置RecyclerView
        binding.settingsRecyclerView.layoutManager = LinearLayoutManager(this)
    }
    
    private fun setupListeners() {
        // 主开关
        binding.mainSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.isEnabled = isChecked
            updateIslandService(isChecked)
            updateUI()
        }
        
        // 设置按钮
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // 权限状态
        binding.permissionStatusCard.setOnClickListener {
            checkPermissions()
        }
    }
    
    private fun updateUI() {
        // 更新开关状态
        binding.mainSwitch.isChecked = prefs.isEnabled
        
        // 更新服务状态
        val isServiceRunning = DynamicIslandService.isServiceRunning()
        binding.serviceStatusText.text = if (isServiceRunning) "运行中" else "已停止"
        binding.serviceStatusIndicator.setBackgroundResource(
            if (isServiceRunning) R.drawable.status_indicator_online else R.drawable.status_indicator_offline
        )
        
        // 更新权限状态
        updatePermissionStatus()
    }
    
    private fun updatePermissionStatus() {
        val hasOverlay = Settings.canDrawOverlays(this)
        val hasNotificationAccess = checkNotificationListenerPermission()
        val hasAllPermissions = hasOverlay && hasNotificationAccess
        
        binding.permissionStatusText.text = when {
            !hasOverlay -> "需要悬浮窗权限"
            !hasNotificationAccess -> "需要通知访问权限"
            hasAllPermissions -> "权限已就绪"
            else -> "部分权限未获取"
        }
        
        binding.permissionStatusIndicator.setBackgroundResource(
            if (hasAllPermissions) R.drawable.status_indicator_online 
            else R.drawable.status_indicator_warning
        )
    }
    
    private fun checkPermissions() {
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
        } else if (!checkNotificationListenerPermission()) {
            requestNotificationListenerPermission()
        } else {
            requestOtherPermissions()
        }
    }
    
    private fun checkOtherPermissions() {
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    private fun requestOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle("需要悬浮窗权限")
            .setMessage("灵动岛需要在其他应用上层显示，请授权。")
            .setPositiveButton("去授权") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
            .setNegativeButton("稍后", null)
            .show()
    }
    
    private fun requestNotificationListenerPermission() {
        AlertDialog.Builder(this)
            .setTitle("需要通知访问权限")
            .setMessage("灵动岛需要访问通知来显示消息通知，请授权。")
            .setPositiveButton("去授权") { _, _ ->
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                notificationListenerPermissionLauncher.launch(intent)
            }
            .setNegativeButton("稍后", null)
            .show()
    }
    
    private fun checkNotificationListenerPermission(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return flat?.contains(packageName) == true
    }
    
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("权限不足")
            .setMessage("部分权限未获取，灵动岛可能无法正常工作。")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }
    
    private fun showOverlayPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("悬浮窗权限未授权")
            .setMessage("灵动岛需要悬浮窗权限来显示，请前往设置授权。")
            .setPositiveButton("确定", null)
            .show()
    }
    
    private fun updateIslandService(enable: Boolean) {
        if (enable) {
            DynamicIslandService.start(this)
        } else {
            DynamicIslandService.stop(this)
        }
    }
    
    private fun hideSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }
    
    private fun startOnboarding() {
        startActivity(Intent(this, OnboardingActivity::class.java))
    }
}
