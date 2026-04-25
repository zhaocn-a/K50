package com.dynamicoasis.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.dynamicoasis.app.R
import com.dynamicoasis.app.databinding.ActivitySettingsBinding
import com.dynamicoasis.app.service.DynamicIslandService
import com.dynamicoasis.app.utils.PreferencesManager

/**
 * 设置界面Activity
 * 配置灵动岛各项功能
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PreferencesManager.getInstance(this)
        
        setupUI()
        loadSettings()
    }
    
    private fun setupUI() {
        // 返回按钮
        binding.backButton.setOnClickListener {
            finish()
        }
        
        // 功能开关
        setupFeatureSwitches()
        
        // 显示设置
        setupDisplaySettings()
        
        // 关于
        setupAboutSection()
    }
    
    private fun setupFeatureSwitches() {
        binding.switchCharging.setOnCheckedChangeListener { _, isChecked ->
            prefs.showCharging = isChecked
        }
        
        binding.switchLowBattery.setOnCheckedChangeListener { _, isChecked ->
            prefs.showLowBattery = isChecked
        }
        
        binding.switchNetwork.setOnCheckedChangeListener { _, isChecked ->
            prefs.showNetwork = isChecked
        }
        
        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.showNotifications = isChecked
        }
        
        binding.switchAlarm.setOnCheckedChangeListener { _, isChecked ->
            prefs.showAlarm = isChecked
        }
        
        binding.switchTorch.setOnCheckedChangeListener { _, isChecked ->
            prefs.showTorch = isChecked
        }
        
        binding.switchBluetooth.setOnCheckedChangeListener { _, isChecked ->
            prefs.showBluetooth = isChecked
        }
        
        binding.switchUnlock.setOnCheckedChangeListener { _, isChecked ->
            prefs.showUnlock = isChecked
        }
        
        binding.switchAutoStart.setOnCheckedChangeListener { _, isChecked ->
            prefs.autoStart = isChecked
        }
    }
    
    private fun setupDisplaySettings() {
        // 尺寸滑块
        binding.sizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = 0.5f + (progress / 100f) * 1.0f
                prefs.size = size
                updateSizePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 透明度滑块
        binding.opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                prefs.opacity = progress / 100f
                updateOpacityPreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 动画风格选择
        binding.animationStyleGroup.setOnCheckedChangeListener { _, checkedId ->
            val style = when (checkedId) {
                R.id.animationStyleDefault -> 0
                R.id.animationStylePulse -> 1
                R.id.animationStyleWave -> 2
                else -> 0
            }
            prefs.animationStyle = style
        }
        
        // 重启服务按钮
        binding.restartServiceButton.setOnClickListener {
            DynamicIslandService.stop(this)
            DynamicIslandService.start(this)
        }
    }
    
    private fun setupAboutSection() {
        binding.versionText.text = "版本: ${getAppVersion()}"
        
        binding.resetSettingsButton.setOnClickListener {
            prefs.resetToDefaults()
            loadSettings()
        }
    }
    
    private fun loadSettings() {
        // 功能开关
        binding.switchCharging.isChecked = prefs.showCharging
        binding.switchLowBattery.isChecked = prefs.showLowBattery
        binding.switchNetwork.isChecked = prefs.showNetwork
        binding.switchNotifications.isChecked = prefs.showNotifications
        binding.switchAlarm.isChecked = prefs.showAlarm
        binding.switchTorch.isChecked = prefs.showTorch
        binding.switchBluetooth.isChecked = prefs.showBluetooth
        binding.switchUnlock.isChecked = prefs.showUnlock
        binding.switchAutoStart.isChecked = prefs.autoStart
        
        // 显示设置
        val sizeProgress = ((prefs.size - 0.5f) / 1.0f * 100).toInt().coerceIn(0, 100)
        binding.sizeSeekBar.progress = sizeProgress
        
        val opacityProgress = (prefs.opacity * 100).toInt().coerceIn(0, 100)
        binding.opacitySeekBar.progress = opacityProgress
        
        when (prefs.animationStyle) {
            0 -> binding.animationStyleDefault.isChecked = true
            1 -> binding.animationStylePulse.isChecked = true
            2 -> binding.animationStyleWave.isChecked = true
        }
    }
    
    private fun updateSizePreview() {
        binding.islandPreview.layoutParams = binding.islandPreview.layoutParams.apply {
            width = (120 * prefs.size * resources.displayMetrics.density).toInt()
            height = (30 * prefs.size * resources.displayMetrics.density).toInt()
        }
    }
    
    private fun updateOpacityPreview() {
        binding.islandPreview.alpha = prefs.opacity
    }
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
}
