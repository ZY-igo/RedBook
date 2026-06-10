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

/**
 * 编辑个人资料页面。
 *
 * 当前主要负责头像选择、预览和上传保存。
 */
@AndroidEntryPoint
class EditProfileActivity : AppCompatActivity() {

    companion object {
        /**
         * 头像加载日志标签。
         */
        private const val TAG = "EditProfileAvatar"
    }

    /**
     * 页面 ViewBinding。
     */
    private lateinit var binding: ActivityEditProfileBinding

    /**
     * “我的”模块仓库，用于读取资料和上传头像。
     */
    @Inject
    lateinit var myRepository: MyRepository

    /**
     * 用户本次新选择的头像 Uri。
     */
    private var selectedAvatarUri: Uri? = null

    /**
     * 当前正在编辑的个人资料快照。
     */
    private var currentProfile: MyProfileHeader? = null

    /**
     * 系统图片选择器。
     *
     * 选择完成后立即本地预览头像效果。
     */
    private val avatarPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                selectedAvatarUri = uri
                renderAvatar(uri, currentProfile?.avatarText.orEmpty())
            }
        }

    /**
     * 初始化编辑资料页。
     *
     * 这里会绑定返回、保存、头像选择事件，
     * 并在页面首次进入时读取当前用户资料。
     */
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

    /**
     * 渲染已有个人资料。
     *
     * @param profile 当前加载到的资料。
     */
    private fun renderProfile(profile: MyProfileHeader) {
        binding.avatarText.text = profile.avatarText
        renderAvatar(profile.avatarUrl, profile.avatarText)
    }

    /**
     * 渲染头像预览。
     *
     * @param source 头像来源，可以是远程 URL，也可以是本地 Uri。
     * @param fallbackText 没有头像图时展示的文字头像内容。
     */
    private fun renderAvatar(source: Any?, fallbackText: String) {
        AppLogger.d(TAG, "render edit avatar, source=$source")
        binding.avatarText.text = fallbackText
        if (source == null) {
            binding.avatarImage.setImageDrawable(null)
            binding.avatarImage.alpha = 0f
            binding.avatarText.alpha = 1f
            return
        }

        // 有图片源时直接显示图片预览，同时隐藏文字头像。
        binding.avatarImage.alpha = 1f
        binding.avatarText.alpha = 0f
        binding.avatarImage.load(source)
    }

    /**
     * 保存当前选择的头像。
     *
     * 如果用户没有选择新头像，则直接关闭页面。
     */
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

    /**
     * 根据内容类型生成上传文件名。
     *
     * @param contentType 图片 MIME 类型。
     * @return 带后缀的头像文件名。
     */
    private fun buildAvatarFileName(contentType: String): String {
        val extension = when (contentType.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        return "avatar.$extension"
    }
}
