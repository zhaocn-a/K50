package com.dynamicoasis.app.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.dynamicoasis.app.R
import com.dynamicoasis.app.databinding.ActivityOnboardingBinding
import com.dynamicoasis.app.service.DynamicIslandService
import com.dynamicoasis.app.utils.PreferencesManager

/**
 * 引导界面Activity
 * 首次使用时引导用户完成设置
 */
class OnboardingActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var prefs: PreferencesManager
    private var currentPage = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        prefs = PreferencesManager.getInstance(this)
        
        hideSystemBars()
        setupViewPager()
        setupButtons()
    }
    
    private fun hideSystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let {
                it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            }
        }
    }
    
    private fun setupViewPager() {
        val pages = listOf(
            OnboardingPage(
                R.drawable.ic_onboarding_welcome,
                "欢迎使用 DynamicOasis",
                "灵动岛功能专为红米K40澎湃OS优化"
            ),
            OnboardingPage(
                R.drawable.ic_onboarding_features,
                "丰富功能",
                "充电提示 · 网络状态 · 通知显示 · 闹钟提醒"
            ),
            OnboardingPage(
                R.drawable.ic_onboarding_permissions,
                "权限说明",
                "需要悬浮窗权限和通知访问权限才能正常工作"
            ),
            OnboardingPage(
                R.drawable.ic_onboarding_ready,
                "准备就绪",
                "开始体验灵动岛功能"
            )
        )
        
        binding.onboardingViewPager.adapter = OnboardingAdapter(pages)
        binding.dotsIndicator.attachTo(binding.onboardingViewPager)
        
        binding.onboardingViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                updateButtons()
            }
        })
    }
    
    private fun setupButtons() {
        binding.nextButton.setOnClickListener {
            if (currentPage < 3) {
                binding.onboardingViewPager.currentItem = currentPage + 1
            } else {
                finishOnboarding()
            }
        }
        
        binding.skipButton.setOnClickListener {
            finishOnboarding()
        }
    }
    
    private fun updateButtons() {
        binding.skipButton.visibility = if (currentPage < 3) View.VISIBLE else View.GONE
        binding.nextButton.text = if (currentPage < 3) "下一步" else "开始使用"
    }
    
    private fun finishOnboarding() {
        prefs.isFirstRun = false
        
        // 启动灵动岛服务
        DynamicIslandService.start(this)
        
        Toast.makeText(this, "灵动岛已启动", Toast.LENGTH_SHORT).show()
        
        finish()
    }
    
    override fun onBackPressed() {
        if (currentPage > 0) {
            binding.onboardingViewPager.currentItem = currentPage - 1
        } else {
            super.onBackPressed()
        }
    }
    
    data class OnboardingPage(
        val iconRes: Int,
        val title: String,
        val description: String
    )
    
    inner class OnboardingAdapter(private val pages: List<OnboardingPage>) :
        androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingAdapter.ViewHolder>() {
        
        inner class ViewHolder(private val binding: com.dynamicoasis.app.databinding.ItemOnboardingPageBinding) :
            androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {
            
            fun bind(page: OnboardingPage) {
                binding.pageIcon.setImageResource(page.iconRes)
                binding.pageTitle.text = page.title
                binding.pageDescription.text = page.description
            }
        }
        
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
            val binding = com.dynamicoasis.app.databinding.ItemOnboardingPageBinding.inflate(
                android.view.LayoutInflater.from(parent.context), parent, false
            )
            return ViewHolder(binding)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(pages[position])
        }
        
        override fun getItemCount() = pages.size
    }
}
