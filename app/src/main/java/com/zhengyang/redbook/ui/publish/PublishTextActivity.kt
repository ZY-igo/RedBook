/**
 * 文件说明： PublishTextActivity.kt
 * 作用： 承载内容发布流程及其配套界面逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
package com.zhengyang.redbook.ui.publish

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import com.zhengyang.redbook.databinding.ActivityPublishTextBinding

class PublishTextActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPublishTextBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublishTextBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBarInsets()
        bindEntryMode()

        binding.buttonClose.setOnClickListener { finish() }
        binding.longFormEntry.setOnClickListener {
            startActivity(
                PublishLongFormActivity.createIntent(
                    context = this,
                    draftContent = binding.contentInput.text?.toString().orEmpty()
                )
            )
        }
    }

    private fun bindEntryMode() {
        when (intent.getStringExtra(EXTRA_MODE)?.let(Mode::valueOf)) {
            Mode.ALBUM -> {
                binding.editorTitle.text = "从相册选图"
                binding.contentInput.hint = "写点配文，记录这一刻..."
            }
            Mode.CAMERA -> {
                binding.editorTitle.text = "拍摄与直播"
                binding.contentInput.hint = "记录拍摄灵感或直播话题..."
            }
            else -> {
                binding.editorTitle.text = "写想法"
                binding.contentInput.hint = "说点什么或提个问题..."
            }
        }
    }

    private fun applySystemBarInsets() {
        val topBase = (binding.topBar.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        ViewCompat.setOnApplyWindowInsetsListener(binding.topBar) { view, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = topBase + statusBars.top
            }
            insets
        }

        val bottomBase = (binding.longFormEntry.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(binding.longFormEntry) { view, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = bottomBase + navigationBars.bottom
            }
            insets
        }

        binding.topBar.doOnAttach { ViewCompat.requestApplyInsets(it) }
        binding.longFormEntry.doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    enum class Mode {
        ALBUM,
        CAMERA,
        TEXT
    }

    companion object {
        private const val EXTRA_MODE = "extra_mode"

        fun createIntent(context: Context, initialMode: Mode): Intent {
            return Intent(context, PublishTextActivity::class.java)
                .putExtra(EXTRA_MODE, initialMode.name)
        }
    }
}
