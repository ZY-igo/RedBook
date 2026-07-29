package com.zhengyang.redbook.ui.my

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.zhengyang.redbook.R
import com.zhengyang.redbook.databinding.FragmentMyBinding
import com.zhengyang.redbook.databinding.LayoutMyInterestPersonBinding
import com.zhengyang.redbook.media.MediaPlayerFactory
import com.zhengyang.redbook.utils.AppLogger
import com.zhengyang.redbook.utils.applyCircleAvatarDefaults
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class MyFragment : Fragment() {

    companion object {
        private const val TAG = "MyAvatar"
    }

    private var _binding: FragmentMyBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MyViewModel by viewModels()
    private var currentState: MyUiState = MyUiState()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSystemBarInsets()
        setupTopBarScrollEffects()

        binding.buttonMenu.setOnClickListener { showLogoutAndClearCacheDialog() }
        binding.buttonEditHome.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }
        binding.buttonPreview.setOnClickListener {
            Toast.makeText(requireContext(), R.string.me_toast_preview, Toast.LENGTH_SHORT).show()
        }
        binding.buttonDismissInterest.setOnClickListener {
            viewModel.dismissInterestSection()
        }
        binding.emptyAction.setOnClickListener {
            Toast.makeText(requireContext(), R.string.toast_add_placeholder, Toast.LENGTH_SHORT).show()
        }
        binding.buttonWechatLogin.setOnClickListener {
            showLoginDialog(LoginMethod.WECHAT, recoverMode = false)
        }
        binding.buttonAppleLogin.setOnClickListener {
            showLoginDialog(LoginMethod.APPLE, recoverMode = false)
        }
        binding.buttonOtherLogin.setOnClickListener {
            viewModel.toggleOtherMethods()
        }
        binding.buttonHelp.setOnClickListener {
            viewModel.onHelpClick()
        }
        binding.loginRecoverText.setOnClickListener {
            viewModel.onRecoverAccountClick()
            showLoginDialog(currentState.loginState.selectedMethod, recoverMode = true)
        }
        binding.loginAgreementIndicator.setOnClickListener {
            viewModel.toggleAgreement()
        }
        binding.loginAgreementText.setOnClickListener {
            viewModel.toggleAgreement()
        }
        binding.buttonPhoneLogin.setOnClickListener {
            showLoginDialog(LoginMethod.PHONE, recoverMode = false)
        }
        binding.buttonQqLogin.setOnClickListener {
            showLoginDialog(LoginMethod.QQ, recoverMode = false)
        }

        collectUiState()
        setupTab(binding.tabNote, R.string.me_empty_note)
        setupTab(binding.tabCollect, R.string.me_empty_collect)
        setupTab(binding.tabLiked, R.string.me_empty_liked)
        selectTab(binding.tabNote, R.string.me_empty_note)
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    private fun setupSystemBarInsets() {
        val baseTopPadding = binding.topActionBar.paddingTop
        ViewCompat.setOnApplyWindowInsetsListener(binding.topActionBar) { view, insets ->
            val statusBarInsets = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            view.setPadding(
                view.paddingLeft,
                baseTopPadding + statusBarInsets.top,
                view.paddingRight,
                view.paddingBottom
            )
            insets
        }
        binding.topActionBar.doOnAttach { ViewCompat.requestApplyInsets(it) }
    }

    private fun setupTopBarScrollEffects() {
        val quickFadeDistance = 36.dpToPx()
        val toneShiftDistance = 140.dpToPx()
        val startColor = Color.TRANSPARENT
        val midColor = ContextCompat.getColor(requireContext(), R.color.xhs_profile_scrim_mid)
        val endColor = ContextCompat.getColor(requireContext(), R.color.xhs_profile_scrim_end)

        binding.scrollContainer.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val quickProgress = (scrollY / quickFadeDistance.toFloat()).coerceIn(0f, 1f)
            val toneProgress = (scrollY / toneShiftDistance.toFloat()).coerceIn(0f, 1f)
            val fastAlpha = 1f - (1f - quickProgress) * (1f - quickProgress)
            val backgroundColor = ColorUtils.blendARGB(
                ColorUtils.blendARGB(startColor, midColor, fastAlpha),
                endColor,
                toneProgress
            )

            binding.topActionBarScrim.setBackgroundColor(backgroundColor)
            binding.topActionBarScrim.alpha = fastAlpha
            val avatarAlpha = ((scrollY - 18.dpToPx()) / 42.dpToPx().toFloat()).coerceIn(0f, 1f)
            binding.topBarAvatar.alpha = avatarAlpha
            binding.topBarAvatarImage.alpha = avatarAlpha
            binding.buttonEditHome.alpha = (1f - quickProgress * 1.15f).coerceIn(0f, 1f)
        }
    }

    private fun collectUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: MyUiState) {
        val binding = _binding ?: return
        currentState = state
        if (!state.isLoggedIn) {
            renderLoggedOutState(state.loginState)
            return
        }

        renderLoggedInChrome()
        binding.profileNameText.text = state.profile.name
        binding.profileUserIdText.text = "小红书号: ${state.profile.id}"
        binding.profileAvatarText.text = state.profile.avatarText
        binding.topBarAvatar.text = state.profile.avatarText
        binding.profileBioText.text = state.profile.bio?.takeIf { it.isNotBlank() }
            ?: getString(R.string.me_no_bio)

        val avatarBackground = createAvatarBackground(state.profile.avatarColorHex)
        binding.profileAvatarText.background = avatarBackground
        binding.topBarAvatar.background = createAvatarBackground(state.profile.avatarColorHex)
        bindAvatar(binding.profileAvatarImage, binding.profileAvatarText, state.profile.avatarUrl)
        bindAvatar(binding.topBarAvatarImage, binding.topBarAvatar, state.profile.avatarUrl)

        binding.followingCountText.text = state.stats.followingCount
        binding.fansCountText.text = state.stats.fansCount
        binding.likesCountText.text = state.stats.likesCount

        val showInterestSection =
            state.isInterestSectionVisible && state.interestPeople.isNotEmpty()
        binding.interestPeopleSection.isVisible = showInterestSection
        binding.interestPeopleContainer.removeAllViews()

        if (showInterestSection) {
            state.interestPeople.forEach { person ->
                val personBinding = LayoutMyInterestPersonBinding.inflate(
                    layoutInflater,
                    binding.interestPeopleContainer,
                    false
                )
                personBinding.avatarText.text = person.avatarText
                personBinding.avatarText.setBackgroundResource(person.avatarBackgroundRes)
                personBinding.nameText.text = person.name
                personBinding.fansText.text = person.fansText
                personBinding.followButton.setOnClickListener { showFollowToast() }
                binding.interestPeopleContainer.addView(personBinding.root)
            }
        }
    }

    private fun renderLoggedOutState(state: MyLoginUiState) {
        binding.scrollContainer.isVisible = false
        binding.loginGuestContainer.isVisible = true
        binding.topActionBarScrim.alpha = 0f
        binding.topActionBarScrim.setBackgroundColor(Color.TRANSPARENT)
        binding.buttonMenu.isVisible = false
        binding.buttonEditHome.isVisible = false
        binding.buttonPreview.isVisible = false
        binding.buttonHelp.isVisible = true
        binding.topBarAvatar.alpha = 0f
        binding.topBarAvatarImage.alpha = 0f
        renderLoginState(state)
    }

    private fun renderLoggedInChrome() {
        binding.scrollContainer.isVisible = true
        binding.loginGuestContainer.isVisible = false
        binding.buttonMenu.isVisible = true
        binding.buttonEditHome.isVisible = true
        binding.buttonPreview.isVisible = true
        binding.buttonHelp.isVisible = false
    }

    private fun renderLoginState(state: MyLoginUiState) {
        binding.loginAgreementIndicator.setBackgroundResource(
            if (state.isAgreementChecked) {
                R.drawable.bg_xhs_login_agreement_checked
            } else {
                R.drawable.bg_xhs_login_agreement_unchecked
            }
        )
        binding.loginAgreementText.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (state.showAgreementError) {
                    R.color.xhs_accent
                } else {
                    R.color.xhs_login_text_tertiary
                }
            )
        )
        binding.buttonOtherLogin.text = getString(
            if (state.isOtherMethodsExpanded) {
                R.string.me_login_other_collapse
            } else {
                R.string.me_login_other
            }
        )
        binding.otherLoginMethodsContainer.isVisible = state.isOtherMethodsExpanded
        binding.loginHelperText.text = state.helperText
        binding.loginLoadingIndicator.isVisible = state.isSubmitting

        binding.buttonWechatLogin.isEnabled = !state.isSubmitting
        binding.buttonAppleLogin.isEnabled = !state.isSubmitting
        binding.buttonPhoneLogin.isEnabled = !state.isSubmitting
        binding.buttonQqLogin.isEnabled = !state.isSubmitting
        binding.buttonOtherLogin.isEnabled = !state.isSubmitting
        binding.loginRecoverText.alpha = if (state.isSubmitting) 0.45f else 1f
        binding.buttonHelp.alpha = if (state.isSubmitting) 0.45f else 1f

        val selectedAlpha = if (state.isSubmitting) 0.6f else 1f
        binding.buttonWechatLogin.alpha =
            if (state.selectedMethod == LoginMethod.WECHAT) selectedAlpha else 0.92f
        binding.buttonAppleLogin.alpha =
            if (state.selectedMethod == LoginMethod.APPLE) selectedAlpha else 0.92f
        binding.buttonPhoneLogin.alpha =
            if (state.selectedMethod == LoginMethod.PHONE) selectedAlpha else 0.92f
        binding.buttonQqLogin.alpha =
            if (state.selectedMethod == LoginMethod.QQ) selectedAlpha else 0.92f
    }

    private fun setupTab(tab: TextView, emptyTextRes: Int) {
        tab.setOnClickListener { selectTab(tab, emptyTextRes) }
    }

    private fun selectTab(selectedTab: TextView, emptyTextRes: Int) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.xhs_text_primary)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.xhs_text_secondary)
        listOf(binding.tabNote, binding.tabCollect, binding.tabLiked).forEach { tab ->
            val selected = tab === selectedTab
            tab.setTextColor(if (selected) activeColor else inactiveColor)
            tab.paint.isFakeBoldText = selected
            tab.background = ContextCompat.getDrawable(
                requireContext(),
                if (selected) R.drawable.bg_xhs_tab_indicator else android.R.color.transparent
            )
        }
        binding.emptyTitle.setText(emptyTextRes)
    }

    private fun showFollowToast() {
        Toast.makeText(requireContext(), R.string.message_toast_follow, Toast.LENGTH_SHORT).show()
    }

    private fun showLoginDialog(method: LoginMethod, recoverMode: Boolean) {
        viewModel.selectLoginMethod(method)
        val loginStateSnapshot = viewModel.uiState.value.loginState
        val dialogContext = requireContext()
        val container = LinearLayout(dialogContext).apply {
            orientation = LinearLayout.VERTICAL
            val padding = 20.dpToPx()
            setPadding(padding, 12.dpToPx(), padding, 0)
        }
        val userIdInput = EditText(dialogContext).apply {
            hint = "账号 ID，可留空自动创建"
            setText(loginStateSnapshot.suggestedUserId)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val nicknameInput = EditText(dialogContext).apply {
            hint = "昵称"
            setText(loginStateSnapshot.suggestedNickname)
            inputType = InputType.TYPE_CLASS_TEXT
        }
        container.addView(
            userIdInput,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        val nicknameParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 12.dpToPx()
        }
        container.addView(nicknameInput, nicknameParams)

        val title = when {
            recoverMode -> "找回账号"
            method == LoginMethod.PHONE -> "手机号入口登录"
            method == LoginMethod.APPLE -> "Apple 登录"
            method == LoginMethod.QQ -> "QQ 登录"
            else -> "微信登录"
        }

        AlertDialog.Builder(dialogContext)
            .setTitle(title)
            .setMessage("输入已有账号 ID 可继续登录，留空账号 ID 会自动创建新账号。")
            .setView(container)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("继续") { _, _ ->
                viewModel.submitLogin(
                    method = method,
                    rawUserId = userIdInput.text?.toString().orEmpty(),
                    rawNickname = nicknameInput.text?.toString().orEmpty()
                )
            }
            .show()
    }

    private fun showLogoutDialog() {
        if (!currentState.isLoggedIn) return
        AlertDialog.Builder(requireContext())
            .setTitle("退出登录")
            .setMessage("确认退出当前账号吗？")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("退出") { _, _ ->
                viewModel.logout()
                Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun createAvatarBackground(colorHex: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(runCatching { Color.parseColor(colorHex) }.getOrDefault(Color.parseColor("#FF8A9F")))
        }
    }

    private fun bindAvatar(imageView: ImageView, textView: TextView, avatarUrl: String?) {
        AppLogger.d(TAG, "bind my avatar, avatarUrl=$avatarUrl")
        if (avatarUrl.isNullOrBlank()) {
            imageView.setImageDrawable(null)
            imageView.isVisible = false
            textView.isVisible = true
            return
        }
        // 我的页头像逻辑比较直接：有 URL 就请求，失败再回退到文字头像。
        imageView.isVisible = true
        textView.isVisible = false
        imageView.load(avatarUrl) {
            applyCircleAvatarDefaults()
            listener(
                onSuccess = { _, _ ->
                    AppLogger.d(TAG, "my avatar load success, avatarUrl=$avatarUrl")
                },
                onError = { _, _ ->
                    AppLogger.w(TAG, "my avatar load error, avatarUrl=$avatarUrl")
                    imageView.isVisible = false
                    textView.isVisible = true
                }
            )
        }
    }

    private fun showLogoutAndClearCacheDialog() {
        if (!currentState.isLoggedIn) return
        AlertDialog.Builder(requireContext())
            .setTitle("退出登录")
            .setMessage("确认退出当前账号，并清理本机视频缓存吗？")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("退出") { _, _ ->
                val appContext = requireContext().applicationContext
                lifecycleScope.launch {
                    val cacheCleared = withContext(Dispatchers.IO) {
                        MediaPlayerFactory.clear(appContext)
                    }
                    viewModel.logout()
                    val toastText = if (cacheCleared) {
                        "已退出登录，并清理视频缓存"
                    } else {
                        "已退出登录，视频缓存清理失败"
                    }
                    Toast.makeText(requireContext(), toastText, Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
