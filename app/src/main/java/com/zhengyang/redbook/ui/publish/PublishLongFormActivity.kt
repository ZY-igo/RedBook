/**
 * 文件说明： PublishLongFormActivity.kt
 * 作用： 承载内容发布流程及其配套界面逻辑。
 * 备注：该注释用于说明当前文件在项目中的职责，方便后续维护时快速建立上下文。
 */
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
import com.zhengyang.redbook.data.repository.PublishRepository
import com.zhengyang.redbook.databinding.ActivityPublishLongFormBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PublishLongFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPublishLongFormBinding

    @Inject
    lateinit var publishRepository: PublishRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPublishLongFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySystemBarInsets()
        binding.contentInput.setText(intent.getStringExtra(EXTRA_DRAFT_CONTENT).orEmpty())

        binding.buttonBack.setOnClickListener { finish() }
        binding.actionButton.setOnClickListener { saveDraft() }
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

    private fun saveDraft() {
        val title = binding.titleInput.text?.toString()?.trim().orEmpty()
        val content = binding.contentInput.text?.toString()?.trim().orEmpty()
        if (title.isBlank() && content.isBlank()) {
            Toast.makeText(this, "请输入标题或正文", Toast.LENGTH_SHORT).show()
            return
        }
        binding.actionButton.isEnabled = false
        lifecycleScope.launch {
            runCatching {
                publishRepository.saveDraft(
                    title = title.ifBlank { content.take(20) },
                    content = content,
                    mediaType = "LONG_FORM",
                    autoSaved = false
                )
            }.onSuccess {
                Toast.makeText(this@PublishLongFormActivity, "草稿已保存", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { error ->
                binding.actionButton.isEnabled = true
                Toast.makeText(
                    this@PublishLongFormActivity,
                    error.message ?: "保存失败，请稍后重试",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        private const val EXTRA_DRAFT_CONTENT = "extra_draft_content"

        fun createIntent(context: Context, draftContent: String): Intent {
            return Intent(context, PublishLongFormActivity::class.java)
                .putExtra(EXTRA_DRAFT_CONTENT, draftContent)
        }
    }
}
