package com.zhengyang.redbook.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.load
import com.zhengyang.redbook.databinding.ActivityImagePreviewBinding

/**
 * 通用单图预览页。
 *
 * 用于头像等单图场景，统一提供缩放、拖拽与沉浸式返回体验。
 */
class ImagePreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImagePreviewBinding
    private var chromeVisible = false
    private var dragStartY = 0f
    private var dragOffsetY = 0f
    private var isDraggingToDismiss = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImagePreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.buttonBack.setOnClickListener { finish() }
        binding.previewImage.setOnClickListener { toggleChrome() }
        binding.previewImage.setMinimumScale(1f)
        binding.previewImage.setMediumScale(2f)
        binding.previewImage.setMaximumScale(4f)
        binding.previewImage.setOnTouchListener(::handleDragToDismiss)

        val imageUri = intent.getStringExtra(EXTRA_IMAGE_URI)?.takeIf { it.isNotBlank() }?.let(Uri::parse)
        val imageUrl = intent.getStringExtra(EXTRA_IMAGE_URL)?.takeIf { it.isNotBlank() }
        val source: Any = imageUri ?: imageUrl ?: run {
            finish()
            return
        }
        binding.previewImage.load(source) {
            // 预览页只负责展示原图内容，不额外做裁剪和复杂占位。
            crossfade(true)
        }
        applyChromeVisibility()
    }

    override fun onResume() {
        super.onResume()
        applyChromeVisibility()
    }

    private fun toggleChrome() {
        chromeVisible = !chromeVisible
        applyChromeVisibility()
    }

    private fun applyChromeVisibility() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (chromeVisible) {
            WindowCompat.setDecorFitsSystemWindows(window, true)
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        } else {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        binding.topBar.visibility = if (chromeVisible) View.VISIBLE else View.GONE
        binding.previewHint.animate()
            .alpha(if (chromeVisible) 1f else 0f)
            .setDuration(160L)
            .withStartAction {
                if (chromeVisible) binding.previewHint.visibility = View.VISIBLE
            }
            .withEndAction {
                if (!chromeVisible) binding.previewHint.visibility = View.GONE
            }
            .start()
    }

    private fun handleDragToDismiss(view: View, event: MotionEvent): Boolean {
        // 图片被放大时，优先把手势留给缩放和平移，避免误触下拉关闭。
        if (binding.previewImage.scale > 1.05f || event.pointerCount > 1) {
            if (isDraggingToDismiss &&
                (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL)
            ) {
                resetDragDismiss()
                return true
            }
            return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                dragStartY = event.rawY
                dragOffsetY = 0f
                isDraggingToDismiss = false
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.rawY - dragStartY
                if (!isDraggingToDismiss && kotlin.math.abs(deltaY) > DRAG_SLOP_PX) {
                    isDraggingToDismiss = true
                    chromeVisible = false
                    applyChromeVisibility()
                }
                if (isDraggingToDismiss) {
                    dragOffsetY = deltaY
                    val progress = (kotlin.math.abs(deltaY) / view.height).coerceAtMost(0.45f)
                    val scale = 1f - (progress * 0.4f)
                    view.translationY = deltaY
                    view.scaleX = scale
                    view.scaleY = scale
                    binding.root.alpha = 1f - progress
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isDraggingToDismiss) {
                    if (kotlin.math.abs(dragOffsetY) >= DISMISS_THRESHOLD_PX) {
                        finish()
                        overridePendingTransition(0, android.R.anim.fade_out)
                    } else {
                        resetDragDismiss()
                    }
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                if (isDraggingToDismiss) {
                    resetDragDismiss()
                    return true
                }
            }
        }
        return false
    }

    private fun resetDragDismiss() {
        isDraggingToDismiss = false
        dragOffsetY = 0f
        binding.previewImage.animate()
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(180L)
            .setInterpolator(DecelerateInterpolator())
            .start()
        binding.root.animate()
            .alpha(1f)
            .setDuration(180L)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    companion object {
        private const val EXTRA_IMAGE_URL = "extra_image_url"
        private const val EXTRA_IMAGE_URI = "extra_image_uri"
        private const val DRAG_SLOP_PX = 24f
        private const val DISMISS_THRESHOLD_PX = 220f

        fun createIntent(context: Context, imageUrl: String): Intent {
            return Intent(context, ImagePreviewActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_URL, imageUrl)
            }
        }

        fun createIntent(context: Context, imageUri: Uri): Intent {
            return Intent(context, ImagePreviewActivity::class.java).apply {
                putExtra(EXTRA_IMAGE_URI, imageUri.toString())
            }
        }
    }
}
