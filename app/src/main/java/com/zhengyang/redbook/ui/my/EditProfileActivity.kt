package com.zhengyang.redbook.ui.my

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.zhengyang.redbook.data.repository.MyRepository
import com.zhengyang.redbook.databinding.ActivityEditProfileBinding
import com.zhengyang.redbook.ui.common.ImagePreviewActivity
import com.zhengyang.redbook.utils.AppLogger
import com.zhengyang.redbook.utils.applyCircleAvatarDefaults
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

/**
 * 编辑个人资料页面。
 *
 * 当前主要负责头像选择、预览和上传保存。
 */
@AndroidEntryPoint
class EditProfileActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "EditProfileAvatar"
    }

    private lateinit var binding: ActivityEditProfileBinding

    @Inject
    lateinit var myRepository: MyRepository

    private var selectedAvatarUri: Uri? = null
    private var currentProfile: MyProfileHeader? = null
    private var currentAvatarSource: Any? = null

    private val avatarPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedAvatarUri = uri
                renderAvatar(uri, currentProfile?.avatarText.orEmpty())
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonBack.setOnClickListener { finish() }
        binding.buttonPreview.text = "保存"
        binding.buttonPreview.setOnClickListener { saveAvatar() }
        binding.avatarContainer.setOnClickListener {
            when (val source = currentAvatarSource) {
                is Uri -> startActivity(ImagePreviewActivity.createIntent(this, source))
                is String -> startActivity(ImagePreviewActivity.createIntent(this, source))
                else -> avatarPicker.launch("image/*")
            }
        }
        binding.avatarContainer.setOnLongClickListener {
            avatarPicker.launch("image/*")
            true
        }
        binding.avatarAction.setOnClickListener { avatarPicker.launch("image/*") }

        lifecycleScope.launch {
            runCatching { myRepository.getProfile() }
                .onSuccess {
                    currentProfile = it
                    renderProfile(it)
                }
                .onFailure {
                    Toast.makeText(this@EditProfileActivity, "加载资料失败", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun renderProfile(profile: MyProfileHeader) {
        binding.avatarText.text = profile.avatarText
        renderAvatar(profile.avatarUrl, profile.avatarText)
    }

    private fun renderAvatar(source: Any?, fallbackText: String) {
        AppLogger.d(TAG, "render edit avatar, source=$source")
        currentAvatarSource = source
        binding.avatarText.text = fallbackText
        binding.avatarImage.setImageDrawable(null)
        if (source == null) {
            binding.avatarImage.alpha = 0f
            binding.avatarText.alpha = 1f
            return
        }

        binding.avatarImage.load(source) {
            applyCircleAvatarDefaults()
            listener(
                onStart = {
                    // 编辑头像时先保留文字兜底，避免本地/远程图片切换时闪白。
                    binding.avatarImage.alpha = 0f
                    binding.avatarText.alpha = 1f
                },
                onSuccess = { _, _ ->
                    AppLogger.d(TAG, "edit avatar load success, source=$source")
                    binding.avatarImage.alpha = 1f
                    binding.avatarText.alpha = 0f
                },
                onError = { _, result ->
                    AppLogger.w(
                        TAG,
                        "edit avatar load error, source=$source, message=${result.throwable.message}",
                        result.throwable
                    )
                    binding.avatarImage.setImageDrawable(null)
                    binding.avatarImage.alpha = 0f
                    binding.avatarText.alpha = 1f
                }
            )
        }
    }

    private fun saveAvatar() {
        val uri = selectedAvatarUri
        if (uri == null) {
            finish()
            return
        }

        lifecycleScope.launch {
            runCatching {
                val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("avatar read failed")
                val contentType = contentResolver.getType(uri) ?: "image/jpeg"
                myRepository.uploadAvatar(
                    fileName = buildAvatarFileName(contentType),
                    contentType = contentType,
                    bytes = bytes
                )
            }.onSuccess {
                Toast.makeText(this@EditProfileActivity, "头像已更新", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }.onFailure {
                Toast.makeText(this@EditProfileActivity, "头像上传失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun buildAvatarFileName(contentType: String): String {
        val extension = when (contentType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        return "avatar.$extension"
    }
}
