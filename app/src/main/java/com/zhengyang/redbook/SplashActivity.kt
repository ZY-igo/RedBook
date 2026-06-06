/**
 * 文件说明： SplashActivity.kt
 * 作用： 定义当前源码文件的核心实现，承担对应功能模块中的结构声明或行为编排职责。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.zhengyang.redbook.databinding.ActivitySplashBinding
import com.zhengyang.redbook.utils.dpToPx

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private var hasStartedMain = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        startSplashAnimation()
    }

    private fun startSplashAnimation() {
        binding.logoText.apply {
            alpha = 0f
            scaleX = 0.82f
            scaleY = 0.82f
            translationY = 18.dpToPx().toFloat()
        }
        binding.subtitleText.apply {
            alpha = 0f
            translationY = 12.dpToPx().toFloat()
        }

        binding.logoText.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .translationY(0f)
            .setDuration(520L)
            .start()

        binding.subtitleText.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(220L)
            .setDuration(420L)
            .start()

        binding.root.postDelayed({
            launchMain()
        }, 1300L)
    }

    private fun launchMain() {
        if (hasStartedMain || isFinishing || isDestroyed) return
        hasStartedMain = true
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
