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
import com.zhengyang.redbook.databinding.ActivityPublishLongFormBinding
import com.zhengyang.redbook.usecase.SaveDraftParams
import com.zhengyang.redbook.usecase.SaveDraftUseCase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 长文编辑页。
 *
 * 负责承接短内容页传来的草稿内容，
 * 并把当前输入保存为一篇长文草稿。
 */
@AndroidEntryPoint
class PublishLongFormActivity : AppCompatActivity() {

    /**
     * 页面 ViewBinding。
     */
    private lateinit var binding: ActivityPublishLongFormBinding

    /**
     * 保存草稿的用例。
     */
    @Inject
    lateinit var saveDraftUseCase: SaveDraftUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublishLongFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBarInsets()
        binding.contentInput.setText(intent.getStringExtra(EXTRA_DRAFT_CONTENT).orEmpty())

        binding.buttonBack.setOnClickListener { finish() }
        binding.actionButton.setOnClickListener { saveDraft() }
    }

    /**
     * 处理状态栏和导航栏 inset。
     *
     * 顶部栏避开状态栏，
     * 底部保存按钮避开导航栏。
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

        val bottomBase = (binding.actionButton.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(binding.actionButton) { view, insets ->
            val navigationBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = bottomBase + navigationBars.bottom
            }
            insets
        }

        binding.topBar.doOnAttach { ViewCompat.requestApplyInsets(it) }
        binding.actionButton.doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    /**
     * 保存当前长文草稿。
     *
     * 标题和正文都为空时不允许继续保存。
     */
    private fun saveDraft() {
        val title = binding.titleInput.text?.toString()?.trim().orEmpty()
        val content = binding.contentInput.text?.toString()?.trim().orEmpty()
        if (title.isBlank() && content.isBlank()) {
            Toast.makeText(this, "请输入标题或正文", Toast.LENGTH_SHORT).show()
            return
        }

        setSavingState(true)
        lifecycleScope.launch {
            when (val result = saveDraftUseCase(buildSaveDraftParams(title, content))) {
                is Resource.Success -> {
                    Toast.makeText(this@PublishLongFormActivity, "草稿已保存", Toast.LENGTH_SHORT).show()
                    finish()
                }

                is Resource.Error -> {
                    setSavingState(false)
                    Toast.makeText(this@PublishLongFormActivity, result.message, Toast.LENGTH_SHORT).show()
                }

                // 当前用例实际上只会返回 Success / Error，这里保留分支是为了穷尽密封类。
                is Resource.Loading -> Unit
            }
        }
    }

    /**
     * 构造保存草稿所需的参数。
     *
     * 如果标题为空，则回退为正文前 20 个字符。
     *
     * @param title 当前标题输入。
     * @param content 当前正文输入。
     * @return 保存草稿用的参数对象。
     */
    private fun buildSaveDraftParams(title: String, content: String): SaveDraftParams {
        return SaveDraftParams(
            title = title.ifBlank { content.take(20) },
            content = content,
            mediaType = "LONG_FORM",
            autoSaved = false
        )
    }

    /**
     * 切换“保存中”按钮状态。
     *
     * @param isSaving 当前是否处于保存流程中。
     */
    private fun setSavingState(isSaving: Boolean) {
        binding.actionButton.isEnabled = !isSaving
        binding.actionButton.text = if (isSaving) SAVING_BUTTON_TEXT else DEFAULT_BUTTON_TEXT
    }

    companion object {
        /**
         * 启动参数键：从短内容页传来的草稿正文。
         */
        private const val EXTRA_DRAFT_CONTENT = "extra_draft_content"

        /**
         * 默认按钮文案。
         */
        private const val DEFAULT_BUTTON_TEXT = "保存草稿"

        /**
         * 保存中按钮文案。
         */
        private const val SAVING_BUTTON_TEXT = "保存中..."

        /**
         * 创建长文编辑页 Intent。
         *
         * @param context 启动上下文。
         * @param draftContent 需要带入编辑器的初始草稿内容。
         * @return 对应的页面 Intent。
         */
        fun createIntent(context: Context, draftContent: String): Intent {
            return Intent(context, PublishLongFormActivity::class.java)
                .putExtra(EXTRA_DRAFT_CONTENT, draftContent)
        }
    }
}
