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

@AndroidEntryPoint
class PublishTextActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPublishTextBinding

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

    private fun buildTitle(content: String): String {
        val firstLine = content.lineSequence().firstOrNull()?.trim().orEmpty()
        return firstLine.takeIf { it.isNotBlank() }?.take(20) ?: content.take(20)
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
