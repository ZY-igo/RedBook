package com.zhengyang.redbook.ui.publish

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import com.zhengyang.redbook.core.common.Resource
import com.zhengyang.redbook.databinding.ActivityPublishTextBinding
import com.zhengyang.redbook.usecase.CreateTextNoteParams
import com.zhengyang.redbook.usecase.CreateTextNoteUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 短内容发布页。
 *
 * 负责处理文本型笔记的快速发布，
 * 也支持从当前输入内容跳转到长文编辑页继续完善。
 */
@AndroidEntryPoint
class PublishTextActivity : AppCompatActivity() {

    /**
     * 页面 ViewBinding。
     */
    private lateinit var binding: ActivityPublishTextBinding

    /**
     * 创建文本笔记的用例。
     */
    @Inject
    lateinit var createTextNote: CreateTextNoteUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublishTextBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBarInsets()
        bindEntryMode()

        binding.buttonClose.setOnClickListener { finish() }
        binding.buttonNext.setOnClickListener { publishNote() }
        binding.longFormEntry.setOnClickListener {
            startActivity(
                PublishLongFormActivity.createIntent(
                    context = this,
                    draftContent = binding.contentInput.text?.toString().orEmpty()
                )
            )
        }
    }

    /**
     * 根据入口模式调整标题和提示文案。
     *
     * 不同入口会复用同一个页面，但 UI 文案会略有区别。
     */
    private fun bindEntryMode() {
        when (intent.getStringExtra(EXTRA_MODE)?.let(Mode::valueOf)) {
            Mode.ALBUM -> {
                binding.editorTitle.text = "从相册选择"
                binding.contentInput.hint = "写点配文，记录这一刻"
            }

            Mode.CAMERA -> {
                binding.editorTitle.text = "拍摄或直播"
                binding.contentInput.hint = "记录拍摄灵感或直播话题"
            }

            else -> {
                binding.editorTitle.text = "写想法"
                binding.contentInput.hint = "说点什么或提个问题"
            }
        }
    }

    /**
     * 处理沉浸式布局下的系统栏安全区域。
     *
     * 顶部栏需要避开状态栏，
     * 底部长文入口需要避开导航栏。
     */
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

    /**
     * 发布当前输入内容。
     *
     * 会先做空内容校验，再根据当前模式推导媒体类型，
     * 最后调用用例完成创建请求。
     */
    private fun publishNote() {
        val content = binding.contentInput.text?.toString()?.trim().orEmpty()
        if (content.isBlank()) {
            binding.contentInput.error = "请输入内容"
            return
        }

        val title = buildTitle(content)
        val mediaType = when (intent.getStringExtra(EXTRA_MODE)?.let(Mode::valueOf)) {
            Mode.ALBUM -> "IMAGE"
            Mode.CAMERA -> "VIDEO"
            else -> "TEXT"
        }

        // 提交期间先禁用按钮，避免重复点击触发多次请求。
        binding.buttonNext.isEnabled = false
        lifecycleScope.launch {
            when (
                val result = createTextNote(
                    CreateTextNoteParams(
                        title = title,
                        content = content,
                        mediaType = mediaType
                    )
                )
            ) {
                is Resource.Success -> {
                    Toast.makeText(this@PublishTextActivity, "发布成功", Toast.LENGTH_SHORT).show()
                    finish()
                }

                is Resource.Error -> {
                    binding.buttonNext.isEnabled = true
                    Toast.makeText(this@PublishTextActivity, result.message, Toast.LENGTH_SHORT).show()
                }

                is Resource.Loading -> Unit
            }
        }
    }

    /**
     * 从正文中推导一个默认标题。
     *
     * 优先使用第一行文本，最多截取 20 个字符。
     *
     * @param content 用户输入的正文。
     * @return 用于发布的标题。
     */
    private fun buildTitle(content: String): String {
        val firstLine = content.lineSequence().firstOrNull()?.trim().orEmpty()
        return firstLine.takeIf { it.isNotBlank() }?.take(20) ?: content.take(20)
    }

    /**
     * 发布页入口模式。
     */
    enum class Mode {
        /** 从相册进入。 */
        ALBUM,

        /** 从拍摄/直播入口进入。 */
        CAMERA,

        /** 纯文字发布入口。 */
        TEXT
    }

    companion object {
        /**
         * 启动参数键：入口模式。
         */
        private const val EXTRA_MODE = "extra_mode"

        /**
         * 创建短内容发布页 Intent。
         *
         * @param context 启动上下文。
         * @param initialMode 入口模式。
         * @return 对应的页面 Intent。
         */
        fun createIntent(context: Context, initialMode: Mode): Intent {
            return Intent(context, PublishTextActivity::class.java)
                .putExtra(EXTRA_MODE, initialMode.name)
        }
    }
}
