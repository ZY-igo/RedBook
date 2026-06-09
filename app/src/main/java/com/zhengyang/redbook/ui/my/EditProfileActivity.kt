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
import com.zhengyang.redbook.utils.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

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
        binding.avatarContainer.setOnClickListener { avatarPicker.launch("image/*") }
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
        binding.avatarText.text = fallbackText
        if (source == null) {
            binding.avatarImage.setImageDrawable(null)
            binding.avatarImage.alpha = 0f
            binding.avatarText.alpha = 1f
            return
        }
        binding.avatarImage.alpha = 1f
        binding.avatarText.alpha = 0f
        binding.avatarImage.load(source)
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
